package com.example.microfleetapp.ui;

import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.example.microfleetapp.data.Catalog;
import com.example.microfleetapp.data.Store;
import com.example.microfleetapp.engine.Road;

import java.util.List;
import java.util.Locale;

public class RiderActivity extends BaseActivity {

    @Override
    protected String screenTitle() {
        return "Rider";
    }

    @Override
    protected void render() {
        final Store store = Store.get(this);
        final int rid = store.lastRider;
        final Catalog.Rider r = Catalog.RIDERS[rid];

        body.addView(Ui.label(this, "I AM"));
        String[] names = new String[Catalog.RIDERS.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = Catalog.RIDERS[i].name;
        }
        Spinner sp = Ui.spinner(this, names);
        sp.setSelection(rid);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (pos != store.lastRider) {
                    store.setLastRider(pos);
                    refresh();
                }
            }

            public void onNothingSelected(AdapterView<?> parent) { }
        });
        body.addView(sp);

        LinearLayout me = Ui.card(this);
        LinearLayout head = Ui.hbox(this);
        LinearLayout info = Ui.vbox(this);
        info.addView(Ui.text(this, r.name, 17, Ui.INK, true));
        info.addView(Ui.caption(this, "Now near " + Road.NAMES[store.riderNode[rid]] + " · carries up to "
                + MerchantActivity.trim(r.capKg) + " kg / " + r.capPk + " pkgs"));
        info.addView(Ui.caption(this, store.deliveredCount(rid) + " deliveries completed"));
        head.addView(info, new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));
        LinearLayout duty = Ui.vbox(this);
        duty.setGravity(Gravity.CENTER_HORIZONTAL);
        Switch sw = new Switch(this);
        sw.setChecked(store.riderOnDuty[rid]);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean on) {
                store.setOnDuty(rid, on);
                refresh();
            }
        });
        duty.addView(sw);
        duty.addView(Ui.caption(this, store.riderOnDuty[rid] ? "On duty" : "Off duty"));
        head.addView(duty);
        me.addView(head);
        body.addView(me);

        List<Store.BatchRec> trips = store.activeBatches(rid);
        if (trips.isEmpty()) {
            LinearLayout none = Ui.card(this);
            none.addView(Ui.text(this, "No trips assigned", 16, Ui.INK, true));
            none.addView(Ui.caption(this, "When the dispatcher sends a plan, your trip and its stops show up "
                    + "here. Check back after the next dispatch round."));
            body.addView(none);
            return;
        }

        final Store.BatchRec cur = trips.get(0);
        body.addView(Ui.h2(this, "Current trip"));
        body.addView(nextStopCard(store, cur));

        FrameLayout box = new FrameLayout(this);
        box.setBackground(Ui.rect(this, 0xFF0B0A12, 18, Ui.STROKE, 1));
        box.setClipToOutline(true);
        RouteMapView map = new RouteMapView(this);
        box.addView(map, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        body.addView(box, Ui.lp(this, Ui.MATCH, Ui.dp(this, 280), 0, 0, 0, 12));
        MapBuilder.show(map, store, rid);

        LinearLayout stops = Ui.card(this);
        stops.addView(Ui.text(this, String.format(Locale.US, "Planned route %.1f km · %d stops", cur.plannedKm,
                cur.stops.size()), 14, Ui.INK, true));
        for (int i = 0; i < cur.stops.size(); i++) {
            Store.StopRec s = cur.stops.get(i);
            Store.OrderRec o = store.order(s.orderId);
            String what = (s.kind == 'P' ? "Pick up #" : "Deliver #") + s.orderId + " · " + Road.NAMES[s.node];
            boolean done = i < cur.done;
            boolean next = i == cur.done;
            TextView t = Ui.text(this, (done ? "✓  " : next ? "▶  " : (i + 1) + ".  ") + what, 13,
                    done ? Ui.MUTE : next ? Ui.LILAC : Ui.INK, next);
            if (done) {
                t.setPaintFlags(t.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            }
            t.setPadding(0, Ui.dp(this, 5), 0, Ui.dp(this, 1));
            stops.addView(t);
            if (o != null && !done && s.kind == 'D') {
                stops.addView(Ui.caption(this, "     " + Ui.dueText(o.deadlineAt)));
            }
        }
        body.addView(stops);

        if (trips.size() > 1) {
            body.addView(Ui.h2(this, "Next in your queue"));
            LinearLayout q = Ui.card(this);
            for (int i = 1; i < trips.size(); i++) {
                Store.BatchRec b = trips.get(i);
                if (i > 1) {
                    q.addView(Ui.divider(this));
                }
                q.addView(Ui.row(this, "Trip with " + (b.stops.size() / 2) + " order(s)",
                        String.format(Locale.US, "%.1f km · %d stops left", b.plannedKm, b.stops.size() - b.done),
                        null));
            }
            body.addView(q);
        }
    }

    private View nextStopCard(final Store store, final Store.BatchRec b) {
        final Store.StopRec s = b.next();
        LinearLayout c = Ui.card(this);
        if (s == null) {
            c.addView(Ui.text(this, "Trip complete", 16, Ui.GOOD, true));
            return c;
        }
        final Store.OrderRec o = store.order(s.orderId);
        boolean pick = s.kind == 'P';
        c.setBackground(Ui.rect(this, Ui.CARD, 18, Ui.withAlpha(pick ? Ui.TEAL : Ui.ORANGE, 0xA0), 1));
        c.addView(Ui.caption(this, "NEXT STOP"));
        c.addView(Ui.text(this, (pick ? "Pick up order #" : "Deliver order #") + s.orderId, 18,
                pick ? Ui.TEAL : Ui.ORANGE, true));
        String place = pick && o != null ? Catalog.MERCHANTS[o.merchantId].name + " (" + Road.NAMES[s.node] + ")"
                : pick ? Road.NAMES[s.node] : "Customer in " + Road.NAMES[s.node];
        TextView where = Ui.text(this, place, 14, Ui.INK, false);
        where.setPadding(0, Ui.dp(this, 4), 0, 0);
        c.addView(where);
        if (o != null) {
            c.addView(Ui.caption(this, Catalog.pretty(o.pkg) + " · " + MerchantActivity.trim(o.kg) + " kg · "
                    + o.pk + " pkg · " + Catalog.pretty(o.cust) + " customer"));
            LinearLayout pills = Ui.pills(this);
            pills.addView(Ui.pill(this, Ui.dueText(o.deadlineAt), Ui.isLate(o.deadlineAt) ? Ui.BAD : Ui.LILAC));
            if (pick) {
                long wait = Math.round((o.readyAt - System.currentTimeMillis()) / 60000.0);
                pills.addView(wait > 0 ? Ui.pill(this, "Ready in " + Ui.minutes(wait), Ui.WARN)
                        : Ui.pill(this, "Ready", Ui.GOOD));
            }
            c.addView(pills);
        }
        c.addView(Ui.button(this, pick ? "Mark picked up" : "Mark delivered", true, new View.OnClickListener() {
            public void onClick(View v) {
                store.completeNextStop(b);
                if (b.finished) {
                    toast("Trip complete. Nice work!");
                }
                refresh();
            }
        }));
        return c;
    }
}
