package org.telegram.ui;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.TimeZone;

/** Settings > Device Info. Read from the phone and shown only on this screen; nothing is stored or shared. */
public class OmnigramDeviceInfoActivity extends OmnigramSettingsBase {

    private static String size(long bytes) {
        if (bytes <= 0) return "-";
        double gb = bytes / (1024.0 * 1024 * 1024);
        if (gb >= 1) return String.format(Locale.US, "%.2f GB", gb);
        return String.format(Locale.US, "%.0f MB", bytes / (1024.0 * 1024));
    }

    private void line(Context context, LinearLayout card, String label, String value) {
        if (value == null || value.trim().isEmpty()) return;
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(10));
        row.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 2));
        TextView l = new TextView(context);
        l.setText(label);
        l.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        l.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
        row.addView(l);
        TextView v = new TextView(context);
        v.setText(value);
        v.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        v.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        row.addView(v);
        row.setOnClickListener(x -> {
            AndroidUtilities.addToClipboard(value);
            Toast.makeText(context, label + " copied", Toast.LENGTH_SHORT).show();
        });
        card.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    @Override
    public View createView(Context context) {
        View root = buildRoot(context, "Device Info");

        section(context, "Device");
        LinearLayout device = card(context);
        line(context, device, "Name", Build.MODEL);
        line(context, device, "Manufacturer", Build.MANUFACTURER);
        line(context, device, "Brand", Build.BRAND);
        line(context, device, "Model", Build.MODEL + " (" + Build.DEVICE + ")");
        line(context, device, "Hardware", Build.HARDWARE);
        line(context, device, "Android", Build.VERSION.RELEASE + "  (API " + Build.VERSION.SDK_INT + ")");
        if (Build.VERSION.SDK_INT >= 23) line(context, device, "Security patch", Build.VERSION.SECURITY_PATCH);
        line(context, device, "Build", Build.DISPLAY);
        try {
            line(context, device, "CPU", (Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "") + "  \u00B7  " + Runtime.getRuntime().availableProcessors() + " cores");
        } catch (Throwable ignore) {}

        section(context, "Memory and screen");
        LinearLayout mem = card(context);
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            line(context, mem, "RAM", size(mi.totalMem) + "  (free " + size(mi.availMem) + ")");
        } catch (Throwable ignore) {}
        try {
            StatFs fs = new StatFs(Environment.getDataDirectory().getPath());
            line(context, mem, "Storage", size(fs.getTotalBytes()) + "  (free " + size(fs.getAvailableBytes()) + ")");
        } catch (Throwable ignore) {}
        try {
            DisplayMetrics dm = new DisplayMetrics();
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            wm.getDefaultDisplay().getRealMetrics(dm);
            line(context, mem, "Screen", dm.widthPixels + " x " + dm.heightPixels + "  \u00B7  " + dm.densityDpi + " dpi  \u00B7  " + String.format(Locale.US, "%.0f Hz", wm.getDefaultDisplay().getRefreshRate()));
        } catch (Throwable ignore) {}
        try {
            Intent b = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (b != null) {
                int level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                int status = b.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
                if (level >= 0) line(context, mem, "Battery", (level * 100 / Math.max(1, scale)) + "%" + (charging ? "  \u00B7  charging" : ""));
            }
        } catch (Throwable ignore) {}

        section(context, "Network");
        LinearLayout net = card(context);
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
            String type = "Offline";
            if (caps != null) {
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) type = "Wi-Fi";
                else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) type = "Mobile data";
                else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) type = "Ethernet";
                else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) type = "VPN";
                else type = "Connected";
            }
            line(context, net, "Connection", type);
        } catch (Throwable ignore) {}
        try {
            ArrayList<String> v4 = new ArrayList<>();
            ArrayList<String> v6 = new ArrayList<>();
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a.isLoopbackAddress() || a.isLinkLocalAddress()) continue;
                    String h = a.getHostAddress();
                    if (h == null) continue;
                    int pct = h.indexOf('%');
                    if (pct > 0) h = h.substring(0, pct);
                    (a instanceof Inet4Address ? v4 : v6).add(ni.getName() + ": " + h);
                }
            }
            line(context, net, "IP address (this device)", android.text.TextUtils.join("\n", v4));
            line(context, net, "IPv6 address", android.text.TextUtils.join("\n", v6));
        } catch (Throwable ignore) {}

        section(context, "System and app");
        LinearLayout sys = card(context);
        line(context, sys, "Language", Locale.getDefault().toString());
        line(context, sys, "Time zone", TimeZone.getDefault().getID());
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            line(context, sys, "Omnigram", pi.versionName + "  (" + (Build.VERSION.SDK_INT >= 28 ? pi.getLongVersionCode() : pi.versionCode) + ")");
            line(context, sys, "Package", context.getPackageName());
        } catch (Throwable ignore) {}

        footnote(context, "This information is read from your phone and shown only on this screen. Omnigram does not store it or send it anywhere. Tap a row to copy it.");
        return root;
    }
}
