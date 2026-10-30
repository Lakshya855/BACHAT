package com.example.microfleetapp.ui;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.microfleetapp.data.DispatchService;
import com.example.microfleetapp.data.Stats;
import com.example.microfleetapp.data.Store;
import com.example.microfleetapp.engine.Dispatch;
import com.example.microfleetapp.engine.Rl;
import com.example.microfleetapp.engine.Types;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardActivity extends BaseActivity {

    private boolean training;
    private String stage = "";

    @Override
    protected String screenTitle() {
        return "Savings dashboard";
    }

    @Override
    protected void render() {
        final Store store = Store.get(this);
        Stats st = Stats.compute(store);

        // ---- Savings from pooling
        body.addView(Ui.h2(this, "Savings from pooling"));
        LinearLayout sav = Ui.card(this);
        if (st.plannedOrders == 0) {
            sav.addView(Ui.text(this, "No dispatched orders yet", 15, Ui.INK, true));
            sav.addView(Ui.caption(this, "Run the dispatcher to see how many trips and kilometres pooling saves "
                    + "compared with one trip per order."));
        } else {
            LinearLayout k1 = Ui.hbox(this);
            k1.addView(kpi(String.valueOf(st.tripsSaved()), "trips saved",
                    st.plannedTrips + " trips for " + st.plannedOrders + " orders", Ui.GOOD), Ui.weight(this, 1, 8));
            k1.addView(kpi(String.format(Locale.US, "%.0f%%", st.kmSavedPct()), "less distance",
                    String.format(Locale.US, "%.1f km saved", st.kmSaved()), Ui.GOOD), Ui.weight(this, 1, 0));
            sav.addView(k1);
            LinearLayout k2 = Ui.hbox(this);
            k2.setPadding(0, Ui.dp(this, 8), 0, 0);
            k2.addView(kpi(String.format(Locale.US, "₹%.0f", st.costSolo() - st.costPooled()), "estimated cost saved",
                    String.format(Locale.US, "%.0f%% lower", st.costSavedPct()), Ui.LILAC), Ui.weight(this, 1, 8));
            k2.addView(kpi(String.format(Locale.US, "%.1f / %.1f", st.pooledKm, st.soloKm), "km pooled / separate",
                    "all dispatch rounds", Ui.LILAC), Ui.weight(this, 1, 0));
            sav.addView(k2);
            TextView note = Ui.caption(this, String.format(Locale.US, "Cost is an estimate using an assumed ₹%.0f "
                    + "per trip plus ₹%.0f per km. Real rates will differ.", Stats.FEE_PER_TRIP, Stats.RATE_PER_KM));
            note.setPadding(0, Ui.dp(this, 10), 0, 0);
            sav.addView(note);
        }
        body.addView(sav);

        // ---- Delivery quality
        body.addView(Ui.h2(this, "Delivery quality"));
        LinearLayout q = Ui.card(this);
        LinearLayout q1 = Ui.hbox(this);
        q1.addView(kpi(String.valueOf(st.ordersTotal), "orders", st.pending + " waiting in pool", Ui.LILAC),
                Ui.weight(this, 1, 8));
        q1.addView(kpi(String.valueOf(st.delivered), "delivered", st.open + " still open", Ui.GOOD),
                Ui.weight(this, 1, 0));
        q.addView(q1);
        LinearLayout q2 = Ui.hbox(this);
        q2.setPadding(0, Ui.dp(this, 8), 0, 0);
        q2.addView(kpi(st.delivered > 0 ? String.format(Locale.US, "%.0f%%", st.onTimePct()) : "–", "on time",
                st.delivered > 0 ? st.onTime + " of " + st.delivered : "no deliveries yet",
                st.delivered == 0 || st.onTimePct() >= 90 ? Ui.GOOD : Ui.WARN), Ui.weight(this, 1, 8));
        q2.addView(kpi(st.delivered > 0 ? Ui.minutes(st.avgDeliveryMin) : "–", "avg order-to-door",
                st.cancelled + " cancelled", Ui.LILAC), Ui.weight(this, 1, 0));
        q.addView(q2);
        body.addView(q);

        // ---- Dispatch history
        body.addView(Ui.h2(this, "Dispatch rounds"));
        LinearLayout hist = Ui.card(this);
        if (store.runs.isEmpty()) {
            hist.addView(Ui.caption(this, "No rounds yet."));
        } else {
            DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
            int shown = 0;
            for (int i = store.runs.size() - 1; i >= 0 && shown < 8; i--, shown++) {
                Store.RunRec r = store.runs.get(i);
                if (shown > 0) {
                    hist.addView(Ui.divider(this));
                }
                double pct = r.soloKm > 0 ? 100 * (r.soloKm - r.pooledKm) / r.soloKm : 0;
                hist.addView(Ui.row(this, r.orders + " orders in " + r.trips + " trip(s)",
                        df.format(new Date(r.at)) + String.format(Locale.US, " · %.1f km vs %.1f km (%.0f%% less)",
                                r.pooledKm, r.soloKm, pct), null));
            }
        }
        body.addView(hist);

        // ---- Learned policy
        body.addView(Ui.h2(this, "Rider-assignment policy (RL)"));
        LinearLayout rl = Ui.card(this);
        if (store.rlWeights == null) {
            rl.addView(Ui.text(this, "Not trained yet", 15, Ui.INK, true));
            rl.addView(Ui.caption(this, "The first dispatch trains it automatically on the phone, or train it now."));
        } else {
            rl.addView(Ui.caption(this, "Learned from " + store.rlEpisodes + " simulated dispatch episodes. "
                    + "Compared on 60 unseen random scenarios; a higher score is better."));
            rl.addView(policyRow("Policy", "Score", "Km", "Late", "Assigned", true, false));
            double best = -Double.MAX_VALUE;
            for (Store.PolicyRow p : store.policyRows) {
                best = Math.max(best, p.mean);
            }
            for (Store.PolicyRow p : store.policyRows) {
                rl.addView(policyRow(p.name, String.format(Locale.US, "%.1f", p.mean),
                        String.format(Locale.US, "%.1f", p.dist), String.format(Locale.US, "%.1f", p.late),
                        String.format(Locale.US, "%.0f%%", p.asg), false, p.mean >= best - 1e-9));
            }
        }
        if (training) {
            LinearLayout row = Ui.hbox(this);
            row.setPadding(0, Ui.dp(this, 12), 0, 0);
            row.addView(new ProgressBar(this), Ui.lp(this, Ui.dp(this, 30), Ui.dp(this, 30), 0, 0, 12, 0));
            row.addView(Ui.caption(this, stage), new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));
            rl.addView(row);
        } else {
            rl.addView(Ui.button(this, store.rlWeights == null ? "Train policy now" : "Retrain policy", false,
                    new View.OnClickListener() {
                        public void onClick(View v) {
                            retrain(store);
                        }
                    }));
        }
        body.addView(rl);
    }

    private void retrain(final Store store) {
        training = true;
        stage = "Starting…";
        refresh();
        final Types.Params params = new Types.Params();
        DispatchService.retrain(params, new DispatchService.Listener() {
            public void onStage(String message) {
                stage = message;
                refresh();
            }

            public void onDone(Dispatch.Plan plan, double[] w, List<Rl.PolicyRow> rows) {
                store.setRlPolicy(w, params.episodes, rows);
                training = false;
                refresh();
            }

            public void onError(Exception e) {
                training = false;
                toast("Training failed: " + e.getMessage());
                refresh();
            }
        });
    }

    private View kpi(String value, String label, String sub, int color) {
        LinearLayout box = Ui.vbox(this);
        box.setBackground(Ui.rect(this, Ui.CARD_HI, 14, Ui.STROKE, 1));
        box.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        box.addView(Ui.text(this, value, 22, color, true));
        box.addView(Ui.text(this, label, 12, Ui.INK, true));
        box.addView(Ui.caption(this, sub));
        return box;
    }

    private View policyRow(String a, String b, String c, String d, String e, boolean header, boolean best) {
        LinearLayout row = Ui.hbox(this);
        row.setPadding(0, Ui.dp(this, 7), 0, Ui.dp(this, 7));
        int col = header ? Ui.MUTE : best ? Ui.GOOD : Ui.INK;
        float[] w = {2.2f, 1f, 1f, 1f, 1.3f};
        String[] t = {a, b, c, d, e};
        for (int i = 0; i < 5; i++) {
            TextView v = Ui.text(this, t[i], header ? 11 : 12, col, header || best);
            v.setGravity(i == 0 ? Gravity.START : Gravity.END);
            row.addView(v, new LinearLayout.LayoutParams(0, Ui.WRAP, w[i]));
        }
        return row;
    }
}
