package com.example.microfleetapp.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

/** Tiny view factory so every screen shares one dark look (no AndroidX needed). */
public final class Ui {

    public static final int BG = 0xFF08080B, CARD = 0xFF14111F, CARD_HI = 0xFF1D1930, STROKE = 0x24FFFFFF,
            INK = 0xFFF1EFFA, INK2 = 0xFFBDB9D3, MUTE = 0xFF8E8AA6, VIOLET = 0xFF8B5CF6,
            LILAC = 0xFFB79BFF, GOOD = 0xFF4ADE80, BAD = 0xFFFB7185, WARN = 0xFFFACC15, ORANGE = 0xFFFB923C,
            TEAL = 0xFF14B8A6, ROSE = 0xFFF43F5E, AMBER = 0xFFF59E0B, INDIGO = 0xFF6366F1, FUCHSIA = 0xFFD946EF;

    public static final int[] BATCH_COLORS = {0xFF60A5FA, 0xFFFB7185, 0xFFFBBF24, 0xFF34D399, 0xFFA78BFA,
            0xFFF472B6, 0xFF22D3EE, 0xFFA3E635, 0xFFFB923C, 0xFF818CF8};

    public static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT, WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private Ui() { }

    public static int dp(Context c, float v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static int urgencyColor(String label) {
        if ("Critical".equals(label)) {
            return BAD;
        }
        if ("High".equals(label)) {
            return ORANGE;
        }
        if ("Medium".equals(label)) {
            return WARN;
        }
        return GOOD;
    }

    public static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    public static TextView text(Context c, CharSequence s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) {
            t.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return t;
    }

    public static GradientDrawable rect(Context c, int fill, float radiusDp, int stroke, float strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(c, radiusDp));
        if (strokeDp > 0) {
            g.setStroke(Math.max(1, dp(c, strokeDp)), stroke);
        }
        return g;
    }

    public static LinearLayout vbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout hbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static LinearLayout.LayoutParams lp(Context c, int w, int h, float l, float t, float r, float b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
        return p;
    }

    public static LinearLayout.LayoutParams weight(Context c, float w, float marginEndDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, WRAP, w);
        p.setMargins(0, 0, dp(c, marginEndDp), 0);
        return p;
    }

    /** Rounded dark panel with padding; added with a 12dp bottom margin. */
    public static LinearLayout card(Context c) {
        LinearLayout l = vbox(c);
        l.setBackground(rect(c, CARD, 18, STROKE, 1));
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        l.setLayoutParams(lp(c, MATCH, WRAP, 0, 0, 0, 12));
        return l;
    }

    public static TextView h2(Context c, String s) {
        TextView t = text(c, s, 18, INK, true);
        t.setLayoutParams(lp(c, MATCH, WRAP, 2, 10, 0, 8));
        return t;
    }

    public static TextView caption(Context c, String s) {
        TextView t = text(c, s, 12, MUTE, false);
        t.setLayoutParams(lp(c, MATCH, WRAP, 0, 0, 0, 0));
        return t;
    }

    public static View gap(Context c, float h) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, h)));
        return v;
    }

    public static TextView button(Context c, String label, boolean primary, View.OnClickListener l) {
        TextView b = text(c, label, 14, primary ? 0xFFFFFFFF : LILAC, true);
        b.setGravity(Gravity.CENTER);
        int ph = dp(c, 18), pv = dp(c, 12);
        b.setPadding(ph, pv, ph, pv);
        GradientDrawable bg = primary ? rect(c, VIOLET, 14, 0, 0) : rect(c, 0x00000000, 14, LILAC, 1);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x40FFFFFF), bg, null));
        b.setClickable(true);
        b.setOnClickListener(l);
        b.setLayoutParams(lp(c, MATCH, WRAP, 0, 4, 0, 4));
        return b;
    }

    public static TextView pill(Context c, String label, int color) {
        TextView t = text(c, label, 11, color, true);
        t.setPadding(dp(c, 8), dp(c, 2), dp(c, 8), dp(c, 2));
        t.setBackground(rect(c, withAlpha(color, 0x2E), 9, withAlpha(color, 0x80), 1));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(WRAP, WRAP);
        p.setMargins(0, 0, dp(c, 6), 0);
        t.setLayoutParams(p);
        return t;
    }

    public static Spinner spinner(Context c, String[] items) {
        Spinner s = new Spinner(c);
        ArrayAdapter<String> a = new ArrayAdapter<String>(c, android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(a);
        s.setBackground(rect(c, CARD_HI, 12, STROKE, 1));
        s.setPadding(dp(c, 8), dp(c, 6), dp(c, 8), dp(c, 6));
        s.setLayoutParams(lp(c, MATCH, WRAP, 0, 2, 0, 8));
        return s;
    }

    public static EditText input(Context c, String hint, String value, boolean decimal) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setText(value);
        e.setTextColor(INK);
        e.setHintTextColor(MUTE);
        e.setSingleLine(true);
        e.setInputType(decimal ? (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL)
                : InputType.TYPE_CLASS_NUMBER);
        e.setBackground(rect(c, CARD_HI, 12, STROKE, 1));
        e.setPadding(dp(c, 12), dp(c, 10), dp(c, 12), dp(c, 10));
        e.setLayoutParams(lp(c, MATCH, WRAP, 0, 2, 0, 8));
        return e;
    }

    public static TextView label(Context c, String s) {
        TextView t = text(c, s, 12, MUTE, true);
        t.setLayoutParams(lp(c, MATCH, WRAP, 2, 6, 0, 0));
        return t;
    }

    public static String minutes(double m) {
        long r = Math.round(m);
        if (r < 60) {
            return r + " min";
        }
        return (r / 60) + " h " + (r % 60) + " min";
    }

    /** "due in 25 min" / "late by 5 min" for a deadline timestamp. */
    public static String dueText(long deadlineAt) {
        long m = Math.round((deadlineAt - System.currentTimeMillis()) / 60000.0);
        return m >= 0 ? "due in " + minutes(m) : "late by " + minutes(-m);
    }

    public static boolean isLate(long deadlineAt) {
        return deadlineAt < System.currentTimeMillis();
    }

    /** One tappable summary row used inside cards. */
    public static LinearLayout row(Context c, CharSequence title, CharSequence sub, View.OnClickListener l) {
        LinearLayout r = vbox(c);
        r.setPadding(0, dp(c, 8), 0, dp(c, 8));
        r.addView(text(c, title, 14, INK, true));
        if (sub != null && sub.length() > 0) {
            TextView s = text(c, sub, 12, INK2, false);
            s.setPadding(0, dp(c, 2), 0, 0);
            r.addView(s);
        }
        if (l != null) {
            r.setBackground(new RippleDrawable(ColorStateList.valueOf(0x30FFFFFF), null, null));
            r.setClickable(true);
            r.setOnClickListener(l);
        }
        return r;
    }

    public static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(STROKE);
        v.setLayoutParams(new LinearLayout.LayoutParams(MATCH, 1));
        return v;
    }

    /** Horizontal strip of pills that wraps never; keep them few. */
    public static LinearLayout pills(Context c) {
        LinearLayout l = hbox(c);
        l.setPadding(0, dp(c, 6), 0, 0);
        return l;
    }
}
