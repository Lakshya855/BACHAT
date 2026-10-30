package com.example.microfleetapp.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Shared screen frame: title bar with a back arrow and a scrolling column called {@code body}. */
public abstract class BaseActivity extends Activity {

    protected FrameLayout root;
    protected LinearLayout body;
    protected ScrollView scroll;

    private boolean rendered;

    protected abstract String screenTitle();

    /** Home hides the back arrow. */
    protected boolean showBack() {
        return true;
    }

    /** Forms return false so typed text survives leaving and re-entering the app. */
    protected boolean rebuildOnResume() {
        return true;
    }

    /** Fill {@code body}; it is cleared before every call. */
    protected abstract void render();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);

        root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);
        LinearLayout column = Ui.vbox(this);
        root.addView(column, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));

        LinearLayout bar = Ui.hbox(this);
        bar.setPadding(Ui.dp(this, 4), Ui.dp(this, 6), Ui.dp(this, 16), Ui.dp(this, 6));
        TextView back = Ui.text(this, "‹", 34, Ui.LILAC, true);
        back.setGravity(Gravity.CENTER);
        back.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 14), Ui.dp(this, 4));
        back.setContentDescription("Back");
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
        if (showBack()) {
            bar.addView(back);
        } else {
            bar.setPadding(Ui.dp(this, 20), Ui.dp(this, 10), Ui.dp(this, 16), Ui.dp(this, 6));
        }
        bar.addView(Ui.text(this, screenTitle(), 20, Ui.INK, true));
        column.addView(bar, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));

        scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        body = Ui.vbox(this);
        body.setPadding(Ui.dp(this, 16), Ui.dp(this, 4), Ui.dp(this, 16), Ui.dp(this, 28));
        scroll.addView(body, new FrameLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        column.addView(scroll, new LinearLayout.LayoutParams(Ui.MATCH, 0, 1f));

        // Edge-to-edge on new Android versions: keep content clear of the system bars.
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
                return insets.consumeSystemWindowInsets();
            }
        });
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!rendered || rebuildOnResume()) {
            refresh();
        }
    }

    /** Rebuild the screen while keeping the scroll position. */
    protected final void refresh() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        final int y = scroll.getScrollY();
        body.removeAllViews();
        rendered = true;
        render();
        scroll.post(new Runnable() {
            public void run() {
                scroll.scrollTo(0, y);
            }
        });
    }

    protected void toast(String msg) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();
    }
}
