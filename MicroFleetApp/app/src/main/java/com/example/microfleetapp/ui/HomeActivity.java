package com.example.microfleetapp.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.microfleetapp.data.Stats;
import com.example.microfleetapp.data.Store;

public class HomeActivity extends BaseActivity {

    @Override
    protected String screenTitle() {
        return "MicroFleet";
    }

    @Override
    protected boolean showBack() {
        return false;
    }

    @Override
    protected void render() {
        final Store store = Store.get(this);
        Stats st = Stats.compute(store);

        LinearLayout hero = Ui.card(this);
        hero.addView(Ui.text(this, "Shared deliveries for neighbourhood shops", 20, Ui.INK, true));
        TextView sub = Ui.text(this, "Pharmacies, bakeries, florists and boutiques put their orders into one "
                + "common pool. MicroFleet decides which orders ride together, plans the route and picks the "
                + "rider, so fewer trips cover the same deliveries, on time.", 13, Ui.INK2, false);
        sub.setPadding(0, Ui.dp(this, 8), 0, 0);
        hero.addView(sub);
        LinearLayout stats = Ui.hbox(this);
        stats.setPadding(0, Ui.dp(this, 14), 0, 0);
        stats.addView(stat("Open orders", String.valueOf(st.open)), Ui.weight(this, 1, 8));
        stats.addView(stat("Trips running", String.valueOf(store.allActiveBatches().size())), Ui.weight(this, 1, 8));
        stats.addView(stat("Delivered", String.valueOf(st.delivered)), Ui.weight(this, 1, 0));
        hero.addView(stats);
        body.addView(hero);

        body.addView(Ui.h2(this, "Choose your role"));
        body.addView(role("Merchant", "Place delivery orders for your shop and follow their status.",
                Ui.TEAL, MerchantActivity.class));
        body.addView(role("Dispatcher", "See the order pool, run the intelligent dispatch, send trips to riders.",
                Ui.VIOLET, DispatcherActivity.class));
        body.addView(role("Rider", "See your trip, the next stop on the map, and confirm pick-ups and drops.",
                Ui.ORANGE, RiderActivity.class));
        body.addView(role("Savings dashboard", "Trips, kilometres and cost saved by pooling, plus delivery quality.",
                Ui.GOOD, DashboardActivity.class));

        body.addView(Ui.h2(this, "How it works"));
        LinearLayout how = Ui.card(this);
        String[][] steps = {
                {"1  Fuzzy logic", "Scores how urgent each order is from deadline slack, waiting time and priority."},
                {"2  Genetic algorithm", "Decides which orders should be pooled into one shared trip."},
                {"3  Ant colony", "Finds the best pick-up and drop-off order inside each trip."},
                {"4  Reinforcement learning", "Chooses the rider by location, workload and capacity, or holds the trip back."}};
        for (int i = 0; i < steps.length; i++) {
            if (i > 0) {
                how.addView(Ui.divider(this));
            }
            how.addView(Ui.row(this, steps[i][0], steps[i][1], null));
        }
        body.addView(how);

        body.addView(Ui.h2(this, "Demo data"));
        body.addView(Ui.button(this, "Load classic demo (10 orders, 9 shops)", false, new View.OnClickListener() {
            public void onClick(View v) {
                store.seedDemo();
                toast("Added 10 demo orders to the pool");
                refresh();
            }
        }));
        body.addView(Ui.button(this, "Reset all data", false, new View.OnClickListener() {
            public void onClick(View v) {
                new AlertDialog.Builder(HomeActivity.this)
                        .setTitle("Reset all data?")
                        .setMessage("This deletes every order, trip and dispatch run on this phone. "
                                + "The learned rider-assignment policy is kept.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Reset", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                store.resetAll();
                                refresh();
                            }
                        }).show();
            }
        }));
        TextView foot = Ui.caption(this, "Everything runs on this phone. No server and no internet needed.");
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, Ui.dp(this, 12), 0, 0);
        body.addView(foot);
    }

    private View stat(String label, String value) {
        LinearLayout box = Ui.vbox(this);
        box.setBackground(Ui.rect(this, Ui.CARD_HI, 12, Ui.STROKE, 1));
        box.setPadding(Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 8));
        box.addView(Ui.text(this, value, 22, Ui.LILAC, true));
        box.addView(Ui.caption(this, label));
        return box;
    }

    private View role(String title, String desc, int color, final Class<?> target) {
        LinearLayout card = Ui.card(this);
        card.setBackground(Ui.rect(this, Ui.CARD, 18, Ui.withAlpha(color, 0x70), 1));
        card.addView(Ui.text(this, title, 17, color, true));
        TextView d = Ui.text(this, desc, 13, Ui.INK2, false);
        d.setPadding(0, Ui.dp(this, 4), 0, 0);
        card.addView(d);
        card.setClickable(true);
        card.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, target));
            }
        });
        return card;
    }
}
