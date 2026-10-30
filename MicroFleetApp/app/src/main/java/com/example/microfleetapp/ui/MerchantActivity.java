package com.example.microfleetapp.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.microfleetapp.data.Catalog;
import com.example.microfleetapp.data.Store;
import com.example.microfleetapp.engine.Road;

import java.util.List;

public class MerchantActivity extends BaseActivity {

    @Override
    protected String screenTitle() {
        return "Merchant";
    }

    @Override
    protected void render() {
        final Store store = Store.get(this);
        final int mid = store.lastMerchant;
        final Catalog.Merchant m = Catalog.MERCHANTS[mid];

        body.addView(Ui.label(this, "SHOP"));
        String[] names = new String[Catalog.MERCHANTS.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = Catalog.MERCHANTS[i].name;
        }
        Spinner sp = Ui.spinner(this, names);
        sp.setSelection(mid);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (pos != store.lastMerchant) {
                    store.setLastMerchant(pos);
                    refresh();
                }
            }

            public void onNothingSelected(AdapterView<?> parent) { }
        });
        body.addView(sp);

        LinearLayout info = Ui.card(this);
        info.addView(Ui.text(this, m.name, 17, Ui.INK, true));
        info.addView(Ui.caption(this, m.type + " · pick-up point: " + Road.NAMES[m.node]));
        LinearLayout pills = Ui.pills(this);
        String[] prio = {"", "High-priority shop", "Standard shop", "Low-priority shop"};
        pills.addView(Ui.pill(this, prio[m.priority], m.priority == 1 ? Ui.ORANGE : m.priority == 2 ? Ui.LILAC : Ui.MUTE));
        pills.addView(Ui.pill(this, "Usually " + Catalog.pretty(m.defaultPkg), Ui.TEAL));
        info.addView(pills);
        body.addView(info);

        body.addView(Ui.button(this, "+  New delivery order", true, new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(MerchantActivity.this, NewOrderActivity.class);
                i.putExtra("merchant", mid);
                startActivity(i);
            }
        }));

        body.addView(Ui.h2(this, "Your orders"));
        List<Store.OrderRec> list = store.ordersOfMerchant(mid);
        if (list.isEmpty()) {
            LinearLayout empty = Ui.card(this);
            empty.addView(Ui.text(this, "No orders yet", 15, Ui.INK, true));
            empty.addView(Ui.caption(this, "Create one and it goes straight into the shared pool. The dispatcher "
                    + "will combine it with nearby orders."));
            body.addView(empty);
            return;
        }
        for (final Store.OrderRec o : list) {
            body.addView(orderCard(store, o));
        }
    }

    private View orderCard(final Store store, final Store.OrderRec o) {
        LinearLayout c = Ui.card(this);
        LinearLayout head = Ui.hbox(this);
        TextView title = Ui.text(this, "Order #" + o.id + " → " + Road.NAMES[o.drop], 15, Ui.INK, true);
        head.addView(title, new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));
        head.addView(statusPill(o));
        c.addView(head);
        c.addView(Ui.caption(this, Catalog.pretty(o.pkg) + " · " + trim(o.kg) + " kg · " + o.pk + " pkg · "
                + Catalog.pretty(o.cust) + " customer"));
        String when;
        if (o.status.equals(Store.DELIVERED)) {
            boolean ok = o.deliveredAt <= o.deadlineAt;
            when = ok ? "Delivered on time" : "Delivered late";
        } else if (o.status.equals(Store.CANCELLED)) {
            when = "Cancelled";
        } else {
            when = Ui.dueText(o.deadlineAt);
        }
        TextView w = Ui.text(this, when, 13, o.isOpen() && Ui.isLate(o.deadlineAt) ? Ui.BAD : Ui.INK2, false);
        w.setPadding(0, Ui.dp(this, 6), 0, 0);
        c.addView(w);
        if (o.riderIdx >= 0 && !o.status.equals(Store.CANCELLED)) {
            c.addView(Ui.caption(this, "Rider: " + Catalog.RIDERS[o.riderIdx].name));
        } else if (o.status.equals(Store.PENDING)) {
            c.addView(Ui.caption(this, "Waiting in the pool for the next dispatch round"));
        }
        if (o.status.equals(Store.PENDING)) {
            c.addView(Ui.button(this, "Cancel order", false, new View.OnClickListener() {
                public void onClick(View v) {
                    new AlertDialog.Builder(MerchantActivity.this)
                            .setTitle("Cancel order #" + o.id + "?")
                            .setMessage("It has not been assigned to a rider yet, so it can be cancelled.")
                            .setNegativeButton("Keep", null)
                            .setPositiveButton("Cancel order", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface d, int which) {
                                    store.cancelOrder(o.id);
                                    refresh();
                                }
                            }).show();
                }
            }));
        }
        return c;
    }

    private TextView statusPill(Store.OrderRec o) {
        String s = o.status;
        if (s.equals(Store.PENDING)) {
            return Ui.pill(this, "In pool", Ui.WARN);
        }
        if (s.equals(Store.ASSIGNED)) {
            return Ui.pill(this, "Rider assigned", Ui.LILAC);
        }
        if (s.equals(Store.PICKED_UP)) {
            return Ui.pill(this, "On the way", Ui.ORANGE);
        }
        if (s.equals(Store.DELIVERED)) {
            return Ui.pill(this, "Delivered", Ui.GOOD);
        }
        return Ui.pill(this, "Cancelled", Ui.MUTE);
    }

    static String trim(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(java.util.Locale.US, "%.1f", v);
    }
}
