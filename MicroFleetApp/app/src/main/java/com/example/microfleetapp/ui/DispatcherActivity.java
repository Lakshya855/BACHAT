package com.example.microfleetapp.ui;

import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.example.microfleetapp.data.Catalog;
import com.example.microfleetapp.data.DispatchService;
import com.example.microfleetapp.data.Store;
import com.example.microfleetapp.engine.Dispatch;
import com.example.microfleetapp.engine.Fuzzy;
import com.example.microfleetapp.engine.Rl;
import com.example.microfleetapp.engine.Road;
import com.example.microfleetapp.engine.Types;

import java.util.List;
import java.util.Locale;

public class DispatcherActivity extends BaseActivity {

    private Dispatch.Plan plan;
    private boolean running;
    private String stage = "";
    private TextView stageView;
    private RouteMapView mapView;

    @Override
    protected String screenTitle() {
        return "Dispatcher";
    }

    @Override
    protected void render() {
        final Store store = Store.get(this);
        renderRiders(store);
        renderPool(store);
        renderRunControls(store);
        if (plan != null && !running) {
            renderPlan(store);
        } else if (!running) {
            renderLive(store);
        }
    }

    // ------------------------------------------------------------------ riders

    private void renderRiders(final Store store) {
        body.addView(Ui.h2(this, "Riders"));
        LinearLayout card = Ui.card(this);
        for (int i = 0; i < Catalog.RIDERS.length; i++) {
            final Catalog.Rider r = Catalog.RIDERS[i];
            if (i > 0) {
                card.addView(Ui.divider(this));
            }
            LinearLayout row = Ui.hbox(this);
            row.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
            View dot = new View(this);
            dot.setBackground(Ui.rect(this, r.color, 8, 0, 0));
            row.addView(dot, Ui.lp(this, Ui.dp(this, 16), Ui.dp(this, 16), 0, 0, 12, 0));
            LinearLayout info = Ui.vbox(this);
            info.addView(Ui.text(this, r.name, 15, Ui.INK, true));
            double[] av = store.riderAvailability(r.id);
            int trips = store.activeBatches(r.id).size();
            info.addView(Ui.caption(this, "Up to " + MerchantActivity.trim(r.capKg) + " kg / " + r.capPk
                    + " pkgs · " + (trips == 0 ? "free at " : trips + " trip(s) queued, free at ")
                    + Road.NAMES[(int) av[1]]));
            row.addView(info, new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));
            Switch sw = new Switch(this);
            sw.setChecked(store.riderOnDuty[r.id]);
            sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean on) {
                    store.setOnDuty(r.id, on);
                    plan = null;
                    refresh();
                }
            });
            row.addView(sw);
            card.addView(row);
        }
        card.addView(Ui.caption(this, "Switch a rider off to exclude them from dispatching."));
        body.addView(card);
    }

    // ------------------------------------------------------------------ pool

    private void renderPool(Store store) {
        long now = System.currentTimeMillis();
        List<Types.Ord> pool = DispatchService.scorePending(store, now);
        int pending = store.pendingOrders().size();
        body.addView(Ui.h2(this, "Order pool (" + pending + ")"));
        LinearLayout card = Ui.card(this);
        if (pool.isEmpty()) {
            card.addView(Ui.text(this, "The pool is empty", 15, Ui.INK, true));
            card.addView(Ui.caption(this, "Orders from merchants appear here. You can also load the classic "
                    + "demo from the home screen."));
            body.addView(card);
            return;
        }
        // Most urgent first.
        java.util.Collections.sort(pool, new java.util.Comparator<Types.Ord>() {
            public int compare(Types.Ord a, Types.Ord b) {
                return Double.compare(b.u, a.u);
            }
        });
        for (int i = 0; i < pool.size(); i++) {
            final Types.Ord o = pool.get(i);
            if (i > 0) {
                card.addView(Ui.divider(this));
            }
            LinearLayout row = Ui.hbox(this);
            row.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
            LinearLayout info = Ui.vbox(this);
            info.addView(Ui.text(this, "#" + o.appId + "  " + o.merchant + " → " + Road.NAMES[o.drop], 14,
                    Ui.INK, true));
            info.addView(Ui.caption(this, Catalog.pretty(o.pkg) + " · " + MerchantActivity.trim(o.kg) + " kg · "
                    + (o.deadline >= 0 ? "due in " + Ui.minutes(o.deadline) : "late by " + Ui.minutes(-o.deadline))));
            row.addView(info, new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));
            int col = Ui.urgencyColor(o.ulabel);
            row.addView(Ui.pill(this, o.ulabel + " " + String.format(Locale.US, "%.2f", o.u), col));
            row.setClickable(true);
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    explainUrgency(o);
                }
            });
            card.addView(row);
        }
        card.addView(Ui.caption(this, "Tap an order to see how fuzzy logic scored its urgency."
                + (pending > DispatchService.MAX_POOL ? " Showing the " + DispatchService.MAX_POOL
                + " most urgent; the rest wait for the next round." : "")));
        body.addView(card);
    }

    private String memberships(double[] m, String[] names) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(names[i]).append(' ').append(String.format(Locale.US, "%.2f", m[i]));
        }
        return sb.toString();
    }

    private void explainUrgency(Types.Ord o) {
        Fuzzy.Result f = o.fz;
        StringBuilder sb = new StringBuilder();
        sb.append("Inputs\n");
        sb.append("• Deadline slack ").append(String.format(Locale.US, "%.0f", f.slack)).append(" min: ")
                .append(memberships(f.mSlack, Fuzzy.SLACK)).append('\n');
        sb.append("• Waiting time ").append(String.format(Locale.US, "%.0f", f.wait)).append(" min: ")
                .append(memberships(f.mWait, Fuzzy.WAIT)).append('\n');
        sb.append("• Priority ").append(String.format(Locale.US, "%.2f", f.prio)).append(": ")
                .append(memberships(f.mPrio, Fuzzy.PRIO)).append("\n\nRules that fired\n");
        int shown = 0;
        for (Fuzzy.Fired fr : f.fired) {
            if (fr.strength < 0.02) {
                continue;
            }
            sb.append("• ").append(Fuzzy.ruleText(fr.rule)).append("  (")
                    .append(String.format(Locale.US, "%.2f", fr.strength)).append(")\n");
            shown++;
        }
        if (shown == 0) {
            sb.append("• none\n");
        }
        sb.append("\nUrgency ").append(String.format(Locale.US, "%.2f", f.urgency)).append(" → ").append(o.ulabel);
        new AlertDialog.Builder(this)
                .setTitle("Order #" + o.appId + " urgency")
                .setMessage(sb.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    // ------------------------------------------------------------------ run

    private void renderRunControls(final Store store) {
        final int pending = store.pendingOrders().size();
        final int riders = DispatchService.onDutyRiders(store).size();
        if (running) {
            LinearLayout card = Ui.card(this);
            LinearLayout row = Ui.hbox(this);
            row.addView(new ProgressBar(this), Ui.lp(this, Ui.dp(this, 36), Ui.dp(this, 36), 0, 0, 14, 0));
            LinearLayout col = Ui.vbox(this);
            col.addView(Ui.text(this, "Running intelligent dispatch…", 15, Ui.INK, true));
            stageView = Ui.caption(this, stage);
            col.addView(stageView);
            row.addView(col, new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));
            card.addView(row);
            body.addView(card);
            return;
        }
        stageView = null;
        TextView b = Ui.button(this, plan == null ? "Run intelligent dispatch" : "Run again", plan == null,
                new View.OnClickListener() {
                    public void onClick(View v) {
                        startRun(store);
                    }
                });
        boolean can = pending > 0 && riders > 0;
        b.setEnabled(can);
        b.setAlpha(can ? 1f : 0.45f);
        body.addView(b);
        if (pending == 0) {
            body.addView(Ui.caption(this, "Nothing to dispatch: the pool is empty."));
        } else if (riders == 0) {
            body.addView(Ui.caption(this, "Switch at least one rider on duty."));
        }
    }

    private void startRun(final Store store) {
        running = true;
        plan = null;
        stage = "Scoring urgency with fuzzy logic";
        refresh();
        DispatchService.run(store, new DispatchService.Listener() {
            public void onStage(String message) {
                stage = message;
                if (stageView != null) {
                    stageView.setText(message);
                }
            }

            public void onDone(Dispatch.Plan p, double[] trainedWeights, List<Rl.PolicyRow> rows) {
                if (trainedWeights != null) {
                    store.setRlPolicy(trainedWeights, new Types.Params().episodes, rows);
                }
                running = false;
                plan = p;
                refresh();
            }

            public void onError(Exception e) {
                running = false;
                toast("Dispatch failed: " + e.getMessage());
                refresh();
            }
        });
    }

    // ------------------------------------------------------------------ plan

    private void renderPlan(final Store store) {
        final Dispatch.Plan p = plan;
        body.addView(Ui.h2(this, "Proposed plan"));

        double solo = 0;
        for (Dispatch.PlannedBatch pb : p.batches) {
            if (pb.riderPos >= 0) {
                solo += pb.soloKm;
            }
        }
        LinearLayout sum = Ui.card(this);
        if (p.trips == 0) {
            sum.addView(Ui.text(this, "No trips could be assigned this round", 16, Ui.INK, true));
            sum.addView(Ui.caption(this, p.deferredOrders + " order(s) stay in the pool. Riders may be busy or "
                    + "too small for the load. Try again when a rider finishes a trip."));
        } else {
            sum.addView(Ui.text(this, p.trips + " trip(s) for " + p.assignedOrders + " order(s)", 17, Ui.INK, true));
            double pct = solo > 0 ? 100 * (solo - p.pooledKm) / solo : 0;
            sum.addView(Ui.caption(this, String.format(Locale.US, "%.1f km pooled instead of %.1f km as separate "
                    + "trips (%.0f%% less)", p.pooledKm, solo, pct)));
            LinearLayout pills = Ui.pills(this);
            pills.addView(Ui.pill(this, String.format(Locale.US, "%.0f%% on time", p.onTimePct),
                    p.lateCount == 0 ? Ui.GOOD : Ui.WARN));
            pills.addView(Ui.pill(this, (p.assignedOrders - p.trips) + " trip(s) saved", Ui.LILAC));
            if (p.deferredOrders > 0) {
                pills.addView(Ui.pill(this, p.deferredOrders + " held back", Ui.ORANGE));
            }
            sum.addView(pills);
        }
        body.addView(sum);

        addMap();
        MapBuilder.show(mapView, p, -1);

        for (int i = 0; i < p.batches.size(); i++) {
            body.addView(batchCard(p, i));
        }

        if (p.trips > 0) {
            body.addView(Ui.button(this, "Send trips to riders", true, new View.OnClickListener() {
                public void onClick(View v) {
                    commit(store);
                }
            }));
        }
        body.addView(Ui.button(this, "Discard plan", false, new View.OnClickListener() {
            public void onClick(View v) {
                plan = null;
                refresh();
            }
        }));
    }

    private void commit(Store store) {
        for (Dispatch.PlannedBatch pb : plan.batches) {
            if (pb.riderPos < 0) {
                continue;
            }
            for (int idx : pb.batch.idxs) {
                Store.OrderRec o = store.order(plan.orders.get(idx).appId);
                if (o == null || !o.status.equals(Store.PENDING)) {
                    toast("The pool changed since this plan was made. Run dispatch again.");
                    plan = null;
                    refresh();
                    return;
                }
            }
        }
        int n = store.commitPlan(plan);
        plan = null;
        toast(n + " trip(s) sent to riders");
        refresh();
    }

    private View batchCard(final Dispatch.Plan p, final int bi) {
        final Dispatch.PlannedBatch pb = p.batches.get(bi);
        LinearLayout c = Ui.card(this);
        boolean assigned = pb.riderPos >= 0;
        int color = assigned ? MapBuilder.batchColor(bi) : Ui.MUTE;
        c.setBackground(Ui.rect(this, Ui.CARD, 18, Ui.withAlpha(color, 0x90), 1));

        LinearLayout head = Ui.hbox(this);
        TextView title = Ui.text(this, assigned ? "Trip " + (bi + 1) + " · " + p.riders.get(pb.riderPos).name
                : "Held back", 16, color, true);
        head.addView(title, new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));
        head.addView(Ui.pill(this, pb.batch.idxs.size() + (pb.batch.idxs.size() == 1 ? " order" : " orders"), color));
        c.addView(head);
        if (assigned) {
            c.setClickable(true);
            c.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mapView != null) {
                        MapBuilder.show(mapView, p, bi);
                    }
                }
            });
        }

        if (!assigned) {
            for (int idx : pb.batch.idxs) {
                Types.Ord o = p.orders.get(idx);
                TextView t = Ui.text(this, "#" + o.appId + "  " + o.merchant + " → " + Road.NAMES[o.drop], 13,
                        Ui.INK2, false);
                t.setPadding(0, Ui.dp(this, 4), 0, 0);
                c.addView(t);
            }
            TextView why = Ui.text(this, pb.deferReason == null ? "" : pb.deferReason, 12, Ui.ORANGE, false);
            why.setPadding(0, Ui.dp(this, 8), 0, 0);
            c.addView(why);
            if (pb.deferProb >= 0) {
                c.addView(Ui.caption(this, String.format(Locale.US, "Hold-back probability from the learned "
                        + "policy: %.0f%%. These orders stay in the pool for the next round.", 100 * pb.deferProb)));
            }
            c.addView(optionsToggle(p, pb));
            return c;
        }

        Types.Eval e = pb.route;
        c.addView(Ui.caption(this, String.format(Locale.US, "%.1f km · done in about %s", e.dist,
                Ui.minutes(e.finish))));
        LinearLayout stops = Ui.vbox(this);
        stops.setPadding(0, Ui.dp(this, 6), 0, 0);
        int n = 1;
        for (Types.Stop s : e.stops) {
            Types.Ord o = p.orders.get(s.ev.o);
            boolean pick = s.ev.k == 'P';
            String line = n + ".  " + (pick ? "Pick up #" + o.appId + " at " + o.merchant
                    : "Deliver #" + o.appId + " to " + Road.NAMES[o.drop]);
            String when = "  ·  in " + Ui.minutes(s.arr);
            boolean late = s.late > 0.01;
            if (late) {
                when += "  ·  late by " + Ui.minutes(s.late);
            } else if (pick && s.wait > 0.5) {
                when += "  ·  waits " + Ui.minutes(s.wait) + " for the order";
            }
            TextView t = Ui.text(this, line + when, 13, late ? Ui.BAD : Ui.INK, false);
            t.setPadding(0, Ui.dp(this, 3), 0, Ui.dp(this, 3));
            stops.addView(t);
            n++;
        }
        c.addView(stops);

        if (pb.batch.idxs.size() > 1 || pb.soloKm > e.dist + 0.05) {
            TextView t = Ui.text(this, String.format(Locale.US, "Pooling saves %.1f km on this trip (separate "
                    + "trips: %.1f km).", Math.max(0, pb.soloKm - e.dist), pb.soloKm), 12, Ui.GOOD, false);
            t.setPadding(0, Ui.dp(this, 6), 0, 0);
            c.addView(t);
        }
        if (pb.aco != null) {
            c.addView(Ui.caption(this, String.format(Locale.US, "Ant colony route %.1f km; plain "
                    + "nearest-next-stop would be %.1f km.", pb.aco.bestDist, pb.aco.greedyDist)));
        }
        c.addView(optionsToggle(p, pb));
        return c;
    }

    private View optionsToggle(final Dispatch.Plan p, Dispatch.PlannedBatch pb) {
        final LinearLayout wrap = Ui.vbox(this);
        final TextView toggle = Ui.text(this, "Why this decision?  ▾", 13, Ui.LILAC, true);
        toggle.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 4));
        final LinearLayout details = Ui.vbox(this);
        details.setVisibility(View.GONE);
        for (Dispatch.RiderOption op : pb.options) {
            String name = p.riders.get(op.riderPos).name;
            String text;
            int col = Ui.INK2;
            if ("capacity".equals(op.status)) {
                text = name + ": cannot carry this load";
                col = Ui.MUTE;
            } else if ("topk".equals(op.status)) {
                text = name + ": not shortlisted (others are closer or less busy)";
                col = Ui.MUTE;
            } else {
                text = String.format(Locale.US, "%s: %.1f km, done in %s", name, op.km, Ui.minutes(op.finishMin));
                if (op.lateMin > 0.01) {
                    text += ", late " + Ui.minutes(op.lateMin);
                }
                if (op.prob >= 0) {
                    text += String.format(Locale.US, " · chosen with %.0f%%", 100 * op.prob);
                }
            }
            if (op.chosen) {
                text = "✓ " + text;
                col = Ui.GOOD;
            }
            TextView t = Ui.text(this, text, 12, col, op.chosen);
            t.setPadding(0, Ui.dp(this, 3), 0, Ui.dp(this, 3));
            details.addView(t);
        }
        if (pb.deferProb >= 0 && pb.riderPos >= 0) {
            details.addView(Ui.caption(this, String.format(Locale.US, "Probability of holding the trip back: %.0f%%",
                    100 * pb.deferProb)));
        }
        if (pb.options.isEmpty()) {
            details.addView(Ui.caption(this, "No rider options."));
        }
        toggle.setClickable(true);
        toggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean show = details.getVisibility() != View.VISIBLE;
                details.setVisibility(show ? View.VISIBLE : View.GONE);
                toggle.setText(show ? "Why this decision?  ▴" : "Why this decision?  ▾");
            }
        });
        wrap.addView(toggle);
        wrap.addView(details);
        return wrap;
    }

    // ------------------------------------------------------------------ live

    private void renderLive(Store store) {
        List<Store.BatchRec> active = store.allActiveBatches();
        body.addView(Ui.h2(this, active.isEmpty() ? "Map" : "Trips on the road (" + active.size() + ")"));
        addMap();
        MapBuilder.show(mapView, store, -1);
        if (active.isEmpty()) {
            body.addView(Ui.caption(this, "Grey squares are shops with orders waiting in the pool. Coloured "
                    + "badges are riders."));
            return;
        }
        for (Store.BatchRec b : active) {
            LinearLayout c = Ui.card(this);
            Catalog.Rider r = Catalog.RIDERS[b.riderIdx];
            LinearLayout head = Ui.hbox(this);
            head.addView(Ui.text(this, r.name, 15, Ui.INK, true), new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));
            head.addView(Ui.pill(this, b.done + " / " + b.stops.size() + " stops", Ui.LILAC));
            c.addView(head);
            Store.StopRec s = b.next();
            if (s != null) {
                c.addView(Ui.caption(this, "Next: " + (s.kind == 'P' ? "pick up" : "deliver") + " #" + s.orderId
                        + " at " + Road.NAMES[s.node]));
            }
            body.addView(c);
        }
    }

    private void addMap() {
        FrameLayout box = new FrameLayout(this);
        box.setBackground(Ui.rect(this, 0xFF0B0A12, 18, Ui.STROKE, 1));
        box.setClipToOutline(true);
        mapView = new RouteMapView(this);
        box.addView(mapView, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        TextView hint = Ui.text(this, "Pinch to zoom · double-tap to reset", 10, Ui.MUTE, false);
        hint.setGravity(Gravity.END);
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(Ui.WRAP, Ui.WRAP, Gravity.BOTTOM | Gravity.END);
        hp.setMargins(0, 0, Ui.dp(this, 10), Ui.dp(this, 6));
        box.addView(hint, hp);
        LinearLayout.LayoutParams lp = Ui.lp(this, Ui.MATCH, Ui.dp(this, 320), 0, 0, 0, 12);
        body.addView(box, lp);
    }
}
