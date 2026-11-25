package com.much.launcher;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * MUCH Launcher: a full-screen web page (assets/index.html) that acts as the Fire TV home screen.
 * The page talks to this activity through the "FireLauncher" JavaScript bridge to open apps.
 */
public class MainActivity extends Activity {

    private WebView web;
    private boolean pageReady = false;
    private final Map<String, String> iconCache = new HashMap<>();

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        web = new WebView(this);
        web.setBackgroundColor(Color.BLACK);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false); // lets the ICQ sound play straight away
        s.setAllowFileAccess(true);
        // Show the page at the TV's real size; the page scales itself to fit the screen
        s.setUseWideViewPort(false);
        s.setLoadWithOverviewMode(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setTextZoom(100);
        web.setInitialScale(0);
        web.setVerticalScrollBarEnabled(false);
        web.setHorizontalScrollBarEnabled(false);

        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pageReady = true;
                view.evaluateJavascript("window.fit&&window.fit()", null);
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Never leave the launcher page; hand web links to an app instead
                if (url != null && url.startsWith("http")) {
                    openViewIntent(url, null);
                    return true;
                }
                return false;
            }
        });

        web.addJavascriptInterface(new Bridge(), "FireLauncher");
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        web.loadUrl("file:///android_asset/index.html");
        hideSystemUi();
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private void js(String code) {
        if (web != null && pageReady) web.evaluateJavascript(code, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
        web.onResume();
        web.requestFocus();
        js("window.fit&&window.fit()");
        js("window.onLauncherResume&&window.onLauncherResume()");
    }

    @Override
    protected void onPause() {
        web.onPause();
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        js("window.onLauncherResume&&window.onLauncherResume()");
    }

    @Override
    public void onBackPressed() {
        // This is the home screen, so Back never closes it; the page decides what Back does
        js("window.onBack&&window.onBack()");
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        // The remote's centre (select) button arrives as DPAD_CENTER; the page listens for Enter
        if (e.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER) {
            KeyEvent enter = new KeyEvent(e.getDownTime(), e.getEventTime(), e.getAction(),
                    KeyEvent.KEYCODE_ENTER, e.getRepeatCount(), e.getMetaState(),
                    e.getDeviceId(), e.getScanCode(), e.getFlags(), e.getSource());
            return super.dispatchKeyEvent(enter);
        }
        return super.dispatchKeyEvent(e);
    }

    @Override
    protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }

    // ------------------------------------------------------------------ app helpers

    /** Installed apps that can be opened, label -> package, sorted by name. */
    private LinkedHashMap<String, String> installedApps() {
        PackageManager pm = getPackageManager();
        Map<String, String> byPkg = new HashMap<>();
        String[] categories = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        for (String cat : categories) {
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(cat);
            List<ResolveInfo> found = pm.queryIntentActivities(q, 0);
            for (ResolveInfo ri : found) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(getPackageName()) || byPkg.containsKey(pkg)) continue;
                CharSequence label = ri.loadLabel(pm);
                byPkg.put(pkg, label == null ? pkg : label.toString());
            }
        }
        List<Map.Entry<String, String>> entries = new ArrayList<>(byPkg.entrySet());
        Collections.sort(entries, (a, b) -> a.getValue().compareToIgnoreCase(b.getValue()));
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> en : entries) out.put(en.getValue() + "\u0000" + en.getKey(), en.getKey());
        return out;
    }

    private boolean startPackage(String pkg) {
        if (pkg == null || pkg.trim().isEmpty()) return false;
        PackageManager pm = getPackageManager();
        try {
            Intent i = pm.getLeanbackLaunchIntentForPackage(pkg.trim());
            if (i == null) i = pm.getLaunchIntentForPackage(pkg.trim());
            if (i == null) return false;
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean openViewIntent(String url, String pkg) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            if (pkg != null && !pkg.trim().isEmpty()) i.setPackage(pkg.trim());
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private String launchByHint(String pkgsCsv, String name) {
        if (pkgsCsv != null) {
            for (String pkg : pkgsCsv.split(",")) if (startPackage(pkg)) return "ok";
        }
        if (name == null || name.trim().isEmpty()) return "missing";
        String want = name.trim().toLowerCase(Locale.ROOT);
        LinkedHashMap<String, String> apps = installedApps();
        // exact name first, then a name that contains it (e.g. "GBTV" matches "GBTV: Glenn Beck")
        for (Map.Entry<String, String> en : apps.entrySet()) {
            String label = en.getKey().split("\u0000")[0].toLowerCase(Locale.ROOT);
            if (label.equals(want) && startPackage(en.getValue())) return "ok";
        }
        for (Map.Entry<String, String> en : apps.entrySet()) {
            String label = en.getKey().split("\u0000")[0].toLowerCase(Locale.ROOT);
            if (label.contains(want) && startPackage(en.getValue())) return "ok";
        }
        return "missing";
    }

    // ------------------------------------------------------------------ JavaScript bridge

    private class Bridge {

        /** JSON list of installed apps: [{"label":"Netflix","pkg":"com.netflix.ninja"}, ...] */
        @JavascriptInterface
        public String getApps() {
            JSONArray arr = new JSONArray();
            try {
                for (Map.Entry<String, String> en : installedApps().entrySet()) {
                    JSONObject o = new JSONObject();
                    o.put("label", en.getKey().split("\u0000")[0]);
                    o.put("pkg", en.getValue());
                    arr.put(o);
                }
            } catch (Exception ignored) {
            }
            return arr.toString();
        }

        /** An app's icon as a small PNG data URI, or "" if it has none. */
        @JavascriptInterface
        public String getIcon(String pkg) {
            if (pkg == null) return "";
            synchronized (iconCache) {
                String hit = iconCache.get(pkg);
                if (hit != null) return hit;
            }
            String uri = "";
            try {
                Drawable d = getPackageManager().getApplicationIcon(pkg);
                int size = 64;
                Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(bmp);
                d.setBounds(0, 0, size, size);
                d.draw(c);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.PNG, 100, bos);
                uri = "data:image/png;base64," + Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
            } catch (Exception ignored) {
            }
            synchronized (iconCache) {
                iconCache.put(pkg, uri);
            }
            return uri;
        }

        /** Opens an app by package name(s) (comma separated), falling back to matching its name. */
        @JavascriptInterface
        public String launch(String pkgsCsv, String name) {
            return launchByHint(pkgsCsv, name);
        }

        /** Opens a link (e.g. a YouTube channel) in the given app(s), else anything that can show it. */
        @JavascriptInterface
        public String openUrl(String url, String pkgsCsv) {
            if (url == null || url.isEmpty()) return "missing";
            if (pkgsCsv != null) {
                for (String pkg : pkgsCsv.split(",")) if (!pkg.trim().isEmpty() && openViewIntent(url, pkg)) return "ok";
            }
            if (openViewIntent(url, null)) return "ok";
            return launchByHint(pkgsCsv, "");
        }

        /** Opens the Fire TV settings screen. */
        @JavascriptInterface
        public String openSettings() {
            try {
                Intent i = new Intent(Settings.ACTION_SETTINGS);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                return "ok";
            } catch (Exception ex) {
                return startPackage("com.amazon.tv.settings.v2") ? "ok" : "missing";
            }
        }
    }
}
