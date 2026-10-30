package com.example.microfleetapp.ui;

import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.microfleetapp.data.Catalog;
import com.example.microfleetapp.data.Store;
import com.example.microfleetapp.engine.Core;
import com.example.microfleetapp.engine.Road;

public class NewOrderActivity extends BaseActivity {

    private Catalog.Merchant merchant;
    private Spinner dropSpinner, pkgSpinner, custSpinner;
    private EditText kgInput, pkInput, prepInput, deadlineInput;
    private TextView hint;

    @Override
    protected String screenTitle() {
        return "New delivery order";
    }

    @Override
    protected boolean rebuildOnResume() {
        return false;
    }

    @Override
    protected void render() {
        int mid = getIntent().getIntExtra("merchant", 0);
        if (mid < 0 || mid >= Catalog.MERCHANTS.length) {
            mid = 0;
        }
        merchant = Catalog.MERCHANTS[mid];

        LinearLayout from = Ui.card(this);
        from.addView(Ui.caption(this, "PICK-UP"));
        from.addView(Ui.text(this, merchant.name, 16, Ui.INK, true));
        from.addView(Ui.caption(this, Road.NAMES[merchant.node]));
        body.addView(from);

        body.addView(Ui.label(this, "DELIVER TO"));
        dropSpinner = Ui.spinner(this, Road.NAMES);
        dropSpinner.setSelection(defaultDrop());
        dropSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                updateHint();
            }

            public void onNothingSelected(AdapterView<?> p) { }
        });
        body.addView(dropSpinner);

        body.addView(Ui.label(this, "WHAT IS BEING SENT"));
        String[] pkgNames = new String[Catalog.PACKAGE_TYPES.length];
        int pkgSel = 0;
        for (int i = 0; i < pkgNames.length; i++) {
            pkgNames[i] = Catalog.pretty(Catalog.PACKAGE_TYPES[i]);
            if (Catalog.PACKAGE_TYPES[i].equals(merchant.defaultPkg)) {
                pkgSel = i;
            }
        }
        pkgSpinner = Ui.spinner(this, pkgNames);
        pkgSpinner.setSelection(pkgSel);
        body.addView(pkgSpinner);

        LinearLayout nums = Ui.hbox(this);
        LinearLayout a = Ui.vbox(this);
        a.addView(Ui.label(this, "WEIGHT (KG)"));
        kgInput = Ui.input(this, "e.g. 2.5", "2", true);
        a.addView(kgInput);
        LinearLayout b = Ui.vbox(this);
        b.addView(Ui.label(this, "PACKAGES"));
        pkInput = Ui.input(this, "e.g. 1", "1", false);
        b.addView(pkInput);
        nums.addView(a, Ui.weight(this, 1, 10));
        nums.addView(b, Ui.weight(this, 1, 0));
        nums.setGravity(android.view.Gravity.TOP);
        body.addView(nums);

        body.addView(Ui.label(this, "CUSTOMER"));
        String[] custNames = new String[Catalog.CUSTOMER_TYPES.length];
        for (int i = 0; i < custNames.length; i++) {
            custNames[i] = Catalog.pretty(Catalog.CUSTOMER_TYPES[i]);
        }
        custSpinner = Ui.spinner(this, custNames);
        body.addView(custSpinner);

        LinearLayout times = Ui.hbox(this);
        LinearLayout c1 = Ui.vbox(this);
        c1.addView(Ui.label(this, "READY IN (MIN)"));
        prepInput = Ui.input(this, "0", "5", false);
        c1.addView(prepInput);
        LinearLayout c2 = Ui.vbox(this);
        c2.addView(Ui.label(this, "DELIVER WITHIN (MIN)"));
        deadlineInput = Ui.input(this, "60", "60", false);
        c2.addView(deadlineInput);
        times.addView(c1, Ui.weight(this, 1, 10));
        times.addView(c2, Ui.weight(this, 1, 0));
        times.setGravity(android.view.Gravity.TOP);
        body.addView(times);

        hint = Ui.caption(this, "");
        hint.setPadding(Ui.dp(this, 2), 0, 0, Ui.dp(this, 10));
        body.addView(hint);
        updateHint();

        body.addView(Ui.button(this, "Add to delivery pool", true, new View.OnClickListener() {
            public void onClick(View v) {
                submit();
            }
        }));
        body.addView(Ui.caption(this, "The dispatcher pools it with other orders to save trips. Urgent or "
                + "high-priority orders are served first."));
    }

    private int defaultDrop() {
        // A different area than the shop's own, so the default form is valid.
        return (merchant.node + 4) % Road.N;
    }

    private int earliestMinutes(int prep) {
        int drop = dropSpinner.getSelectedItemPosition();
        return (int) Math.ceil(prep + Road.T[merchant.node][drop] + Core.SERVICE);
    }

    private void updateHint() {
        if (hint == null || dropSpinner == null) {
            return;
        }
        int drop = dropSpinner.getSelectedItemPosition();
        hint.setText("Shop to customer takes about " + Math.round(Road.T[merchant.node][drop])
                + " min by road (" + String.format(java.util.Locale.US, "%.1f", Road.D[merchant.node][drop])
                + " km). The deadline must leave room for preparation and the ride.");
    }

    private static int parseInt(EditText e, int fallback) {
        try {
            return Integer.parseInt(e.getText().toString().trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private void submit() {
        int drop = dropSpinner.getSelectedItemPosition();
        double kg;
        try {
            kg = Double.parseDouble(kgInput.getText().toString().trim());
        } catch (NumberFormatException ex) {
            kg = -1;
        }
        int pk = parseInt(pkInput, -1);
        int prep = parseInt(prepInput, -1);
        int deadline = parseInt(deadlineInput, -1);

        double maxKg = 0;
        int maxPk = 0;
        for (Catalog.Rider r : Catalog.RIDERS) {
            maxKg = Math.max(maxKg, r.capKg);
            maxPk = Math.max(maxPk, r.capPk);
        }
        String err = null;
        if (drop == merchant.node) {
            err = "Pick a drop-off area different from the shop's area.";
        } else if (kg <= 0) {
            err = "Enter the weight in kg.";
        } else if (kg > maxKg) {
            err = "No rider can carry more than " + MerchantActivity.trim(maxKg) + " kg.";
        } else if (pk < 1) {
            err = "Enter at least 1 package.";
        } else if (pk > maxPk) {
            err = "No rider can carry more than " + maxPk + " packages.";
        } else if (prep < 0 || prep > 240) {
            err = "Ready-in must be between 0 and 240 minutes.";
        } else if (deadline < 1 || deadline > 720) {
            err = "Deadline must be between 1 and 720 minutes.";
        } else if (deadline < earliestMinutes(prep)) {
            err = "That deadline is impossible: preparation plus the ride needs at least "
                    + earliestMinutes(prep) + " min. A rider also needs time to reach the shop.";
        }
        if (err != null) {
            toast(err);
            return;
        }
        String pkg = Catalog.PACKAGE_TYPES[pkgSpinner.getSelectedItemPosition()];
        String cust = Catalog.CUSTOMER_TYPES[custSpinner.getSelectedItemPosition()];
        Store.OrderRec o = Store.get(this).addOrder(merchant.id, drop, pkg, kg, pk, cust, prep, deadline);
        toast("Order #" + o.id + " added to the pool");
        finish();
    }
}
