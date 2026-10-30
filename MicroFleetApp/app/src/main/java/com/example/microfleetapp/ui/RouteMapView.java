package com.example.microfleetapp.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.example.microfleetapp.engine.Road;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Schematic map of Mumbai's western suburbs with delivery routes drawn on top. Pinch to zoom,
 * drag to pan (once zoomed), double-tap to reset.
 */
public class RouteMapView extends View {

    /** Numbered stop on a route: 'P' = pickup (square), 'D' = drop-off (circle). */
    public static final class Mark {
        public int node;
        public char kind;
        public String text;
        public boolean done, current;

        public Mark(int node, char kind, String text, boolean done, boolean current) {
            this.node = node;
            this.kind = kind;
            this.text = text;
            this.done = done;
            this.current = current;
        }
    }

    public static final class Route {
        public int color;
        public boolean dim;
        public List<Integer> path = new ArrayList<Integer>();
        public List<Mark> marks = new ArrayList<Mark>();
    }

    public static final class Pin {
        public int node, color;
        public String label;

        public Pin(int node, String label, int color) {
            this.node = node;
            this.label = label;
            this.color = color;
        }
    }

    /** Road nodes visited when riding from {@code start} through the stop nodes in order. */
    public static List<Integer> pathThrough(int start, List<Integer> stops) {
        List<Integer> out = new ArrayList<Integer>();
        out.add(start);
        int cur = start;
        for (int node : stops) {
            int[] seg = Road.pathNodes(cur, node);
            for (int i = 1; i < seg.length; i++) {
                out.add(seg[i]);
            }
            cur = node;
        }
        return out;
    }

    private final List<Route> routes = new ArrayList<Route>();
    private final List<Pin> pins = new ArrayList<Pin>();
    private final List<Mark> poolMarks = new ArrayList<Mark>();

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path tmp = new Path();
    private final RectF rect = new RectF();

    private float baseScale = 1, baseX, baseY;
    private float zoom = 1, panX, panY;
    private final float density;
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;
    private float lastX, lastY;
    private boolean dragging;

    public RouteMapView(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(Paint.Align.CENTER);
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector d) {
                float z = Math.max(1f, Math.min(6f, zoom * d.getScaleFactor()));
                float f = z / zoom;
                float fx = d.getFocusX(), fy = d.getFocusY();
                panX = fx - (fx - panX) * f;
                panY = fy - (fy - panY) * f;
                zoom = z;
                clampPan();
                invalidate();
                return true;
            }
        });
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                zoom = 1;
                panX = 0;
                panY = 0;
                invalidate();
                return true;
            }
        });
    }

    public void setData(List<Route> newRoutes, List<Pin> newPins, List<Mark> pool) {
        routes.clear();
        pins.clear();
        poolMarks.clear();
        if (newRoutes != null) {
            routes.addAll(newRoutes);
        }
        if (newPins != null) {
            pins.addAll(newPins);
        }
        if (pool != null) {
            poolMarks.addAll(pool);
        }
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float pad = 26 * density;
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int i = 0; i < Road.N; i++) {
            minX = Math.min(minX, Road.x(i));
            maxX = Math.max(maxX, Road.x(i));
            minY = Math.min(minY, Road.y(i));
            maxY = Math.max(maxY, Road.y(i));
        }
        float sx = (float) ((w - 2 * pad - 20 * density) / (maxX - minX));
        float sy = (float) ((h - 2 * pad) / (maxY - minY));
        baseScale = Math.min(sx, sy);
        baseX = (float) (w / 2f - (minX + maxX) / 2 * baseScale);
        baseY = (float) (h / 2f - (minY + maxY) / 2 * baseScale);
    }

    private float px(double x) {
        return (float) (baseX + x * baseScale) * zoom + panX;
    }

    private float py(double y) {
        return (float) (baseY + y * baseScale) * zoom + panY;
    }

    private float nx(int node) {
        return px(Road.x(node));
    }

    private float ny(int node) {
        return py(Road.y(node));
    }

    private void clampPan() {
        float w = getWidth(), h = getHeight();
        float minPx = w - w * zoom, minPy = h - h * zoom;
        panX = Math.max(minPx, Math.min(0, panX));
        panY = Math.max(minPy, Math.min(0, panY));
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        scaleDetector.onTouchEvent(e);
        gestureDetector.onTouchEvent(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = e.getX();
                lastY = e.getY();
                dragging = false;
                if (zoom > 1.01f && getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (e.getPointerCount() == 1 && !scaleDetector.isInProgress() && zoom > 1.01f) {
                    panX += e.getX() - lastX;
                    panY += e.getY() - lastY;
                    clampPan();
                    invalidate();
                    dragging = true;
                }
                lastX = e.getX();
                lastY = e.getY();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas c) {
        c.drawColor(0xFF0B0A12);
        drawSea(c);
        drawRoads(c);
        Set<Integer> important = new HashSet<Integer>();
        for (Route r : routes) {
            for (Mark m : r.marks) {
                important.add(m.node);
            }
            if (!r.path.isEmpty()) {
                important.add(r.path.get(0));
            }
        }
        for (Mark m : poolMarks) {
            important.add(m.node);
        }
        for (Pin p : pins) {
            important.add(p.node);
        }
        drawNodes(c, important);
        for (Route r : routes) {
            drawRoute(c, r);
        }
        drawPool(c);
        for (Route r : routes) {
            drawMarks(c, r);
        }
        for (Pin p : pins) {
            drawPin(c, p);
        }
    }

    private void drawSea(Canvas c) {
        tmp.reset();
        double[][] co = Road.COAST;
        tmp.moveTo(px(Road.toX(co[0][1])), py(Road.toY(co[0][0])));
        for (int i = 1; i < co.length; i++) {
            tmp.lineTo(px(Road.toX(co[i][1])), py(Road.toY(co[i][0])));
        }
        tmp.lineTo(px(-12), py(Road.toY(co[co.length - 1][0])));
        tmp.lineTo(px(-12), py(Road.toY(co[0][0])));
        tmp.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF10131F);
        c.drawPath(tmp, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f * density);
        paint.setColor(0x558B5CF6);
        tmp.reset();
        tmp.moveTo(px(Road.toX(co[0][1])), py(Road.toY(co[0][0])));
        for (int i = 1; i < co.length; i++) {
            tmp.lineTo(px(Road.toX(co[i][1])), py(Road.toY(co[i][0])));
        }
        c.drawPath(tmp, paint);
    }

    private void drawRoads(Canvas c) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int[] e : Road.EDGES) {
            paint.setColor(e[2] == 1 ? 0x66B79BFF : e[2] == 0 ? 0x44FFFFFF : 0x26FFFFFF);
            paint.setStrokeWidth((e[2] == 2 ? 1.5f : 2.5f) * density);
            c.drawLine(nx(e[0]), ny(e[0]), nx(e[1]), ny(e[1]), paint);
        }
    }

    private void drawNodes(Canvas c, Set<Integer> important) {
        textPaint.setTextSize(9.5f * density);
        textPaint.setTypeface(Typeface.DEFAULT);
        for (int i = 0; i < Road.N; i++) {
            float x = nx(i), y = ny(i);
            boolean imp = important.contains(i);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(imp ? 0xFFB79BFF : 0x88FFFFFF);
            c.drawCircle(x, y, (imp ? 3.2f : 2.4f) * density, paint);
            if (imp || zoom >= 1.7f) {
                textPaint.setColor(imp ? 0xFFE9E4FF : 0x99BDB9D3);
                textPaint.setTextAlign(Paint.Align.CENTER);
                c.drawText(Road.NAMES[i], x, y + 15 * density, textPaint);
            }
        }
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
    }

    private void drawRoute(Canvas c, Route r) {
        if (r.path.size() < 2) {
            return;
        }
        tmp.reset();
        tmp.moveTo(nx(r.path.get(0)), ny(r.path.get(0)));
        for (int i = 1; i < r.path.size(); i++) {
            tmp.lineTo(nx(r.path.get(i)), ny(r.path.get(i)));
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(8f * density);
        paint.setColor(Ui.withAlpha(r.color, r.dim ? 0x14 : 0x38));
        c.drawPath(tmp, paint);
        paint.setStrokeWidth(3.2f * density);
        paint.setColor(Ui.withAlpha(r.color, r.dim ? 0x55 : 0xF2));
        c.drawPath(tmp, paint);
    }

    /** Spread several stops that share a road node around it. */
    private Map<Mark, float[]> layout(List<Mark> marks) {
        Map<Integer, List<Mark>> byNode = new HashMap<Integer, List<Mark>>();
        for (Mark m : marks) {
            List<Mark> l = byNode.get(m.node);
            if (l == null) {
                l = new ArrayList<Mark>();
                byNode.put(m.node, l);
            }
            l.add(m);
        }
        Map<Mark, float[]> out = new HashMap<Mark, float[]>();
        for (Map.Entry<Integer, List<Mark>> en : byNode.entrySet()) {
            List<Mark> l = en.getValue();
            float cx = nx(en.getKey()), cy = ny(en.getKey());
            int k = l.size();
            for (int i = 0; i < k; i++) {
                if (k == 1) {
                    out.put(l.get(i), new float[]{cx, cy});
                } else {
                    double a = -Math.PI / 2 + 2 * Math.PI * i / k;
                    float rr = (k == 2 ? 10 : 14) * density;
                    out.put(l.get(i), new float[]{cx + (float) Math.cos(a) * rr, cy + (float) Math.sin(a) * rr});
                }
            }
        }
        return out;
    }

    private void drawMarks(Canvas c, Route r) {
        Map<Mark, float[]> pos = layout(r.marks);
        for (Mark m : r.marks) {
            float[] p = pos.get(m);
            drawMark(c, m, p[0], p[1], r.color, r.dim);
        }
    }

    private void drawPool(Canvas c) {
        Map<Mark, float[]> pos = layout(poolMarks);
        for (Mark m : poolMarks) {
            float[] p = pos.get(m);
            drawMark(c, m, p[0], p[1], 0xFF9A96B5, false);
        }
    }

    private void drawMark(Canvas c, Mark m, float x, float y, int color, boolean dim) {
        float s = 8.5f * density;
        int fillAlpha = m.done ? 0x55 : dim ? 0x77 : 0xFF;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Ui.withAlpha(color, fillAlpha));
        if (m.kind == 'P') {
            rect.set(x - s, y - s, x + s, y + s);
            c.drawRoundRect(rect, 3.5f * density, 3.5f * density, paint);
        } else {
            c.drawCircle(x, y, s, paint);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth((m.current ? 2.6f : 1.4f) * density);
        paint.setColor(m.current ? 0xFFFFFFFF : 0xAA0B0A12);
        if (m.kind == 'P') {
            c.drawRoundRect(rect, 3.5f * density, 3.5f * density, paint);
        } else {
            c.drawCircle(x, y, s, paint);
        }
        textPaint.setTextSize(9.5f * density);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(0xFF10101A);
        c.drawText(m.text, x, y + 3.4f * density, textPaint);
    }

    private void drawPin(Canvas c, Pin p) {
        float x = nx(p.node), y = ny(p.node) - 2 * density;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(p.color);
        c.drawCircle(x, y, 10.5f * density, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f * density);
        paint.setColor(0xFFFFFFFF);
        c.drawCircle(x, y, 10.5f * density, paint);
        textPaint.setTextSize(11f * density);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(0xFF10101A);
        String t = p.label.isEmpty() ? "" : p.label.substring(0, 1);
        c.drawText(t, x, y + 4f * density, textPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
