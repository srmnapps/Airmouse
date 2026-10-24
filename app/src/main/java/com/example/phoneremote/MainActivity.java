package com.example.phoneremote;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Phone Remote: shows the remote page from the laptop and turns the
 * hardware volume buttons into mouse clicks.
 *   Volume Up   = left click  (hold = button held, so you can drag)
 *   Volume Down = right click
 * Press Back to return to the address screen.
 */
public class MainActivity extends Activity {

    private WebView web;
    private LinearLayout setup;
    private EditText addr;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        prefs = getSharedPreferences("remote", MODE_PRIVATE);

        // ---- address screen ----
        setup = new LinearLayout(this);
        setup.setOrientation(LinearLayout.VERTICAL);
        setup.setGravity(Gravity.CENTER);
        setup.setBackgroundColor(Color.parseColor("#111111"));
        setup.setPadding(60, 20, 60, 20);

        TextView label = new TextView(this);
        label.setText("Laptop address (shown when you run remote.py)");
        label.setTextColor(Color.WHITE);
        label.setGravity(Gravity.CENTER);

        addr = new EditText(this);
        addr.setHint("192.168.1.5:8000");
        addr.setTextColor(Color.WHITE);
        addr.setHintTextColor(Color.GRAY);
        addr.setSingleLine(true);
        addr.setText(prefs.getString("addr", ""));

        Button go = new Button(this);
        go.setText("Connect");
        go.setOnClickListener(v -> connect());

        setup.addView(label);
        setup.addView(addr, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        setup.addView(go);

        // ---- web view ----
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        web.setWebViewClient(new WebViewClient());
        web.setBackgroundColor(Color.BLACK);
        web.setVisibility(View.GONE);

        android.widget.FrameLayout root = new android.widget.FrameLayout(this);
        root.addView(web);
        root.addView(setup);
        setContentView(root);

        hideBars();
    }

    private void connect() {
        String a = addr.getText().toString().trim();
        if (a.isEmpty()) return;
        prefs.edit().putString("addr", a).apply();
        if (!a.startsWith("http")) a = "http://" + a;
        setup.setVisibility(View.GONE);
        web.setVisibility(View.VISIBLE);
        web.loadUrl(a);
        web.requestFocus();
    }

    private void hideBars() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideBars();
    }

    /** Catch the volume buttons before Android turns them into volume changes. */
    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int code = e.getKeyCode();
        boolean up = code == KeyEvent.KEYCODE_VOLUME_UP;
        boolean down = code == KeyEvent.KEYCODE_VOLUME_DOWN;
        if ((up || down) && web.getVisibility() == View.VISIBLE) {
            int action = e.getAction();
            if (action == KeyEvent.ACTION_DOWN && e.getRepeatCount() > 0) return true; // ignore auto-repeat
            String state = action == KeyEvent.ACTION_DOWN ? "down" : "up";
            String button = up ? "left" : "right";
            web.evaluateJavascript(
                    "window.volKey && window.volKey('" + button + "','" + state + "')", null);
            return true; // swallow: no volume bar, no volume change
        }
        return super.dispatchKeyEvent(e);
    }

    @Override
    public void onBackPressed() {
        if (web.getVisibility() == View.VISIBLE) {
            web.loadUrl("about:blank");
            web.setVisibility(View.GONE);
            setup.setVisibility(View.VISIBLE);
        } else {
            super.onBackPressed();
        }
    }
}
