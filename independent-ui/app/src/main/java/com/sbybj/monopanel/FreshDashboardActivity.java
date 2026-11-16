package com.sbybj.monopanel;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Switch;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Locale;

public class FreshDashboardActivity extends Activity {
    private final int bg = Color.rgb(8, 9, 12);
    private final int panel = Color.rgb(19, 21, 26);
    private final int muted = Color.rgb(135, 141, 151);
    private final int white = Color.rgb(242, 244, 247);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView cpuValue, ramValue, batteryValue, fpsValue, clockValue;
    private long previousIdle, previousTotal;
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            refreshMetrics();
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR & 0);
        buildScreen();
        refreshMetrics();
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);
        setContentView(root);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(24), dp(22), dp(20));
        scroll.addView(content);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setOrientation(LinearLayout.HORIZONTAL);
        content.addView(top, matchWrap());
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        brand.addView(label("MONOPANEL", 12, muted, true));
        TextView heading = label("系统控制台", 27, white, true);
        heading.setPadding(0, dp(5), 0, 0);
        brand.addView(heading);
        TextView live = label("●  LIVE", 10, Color.rgb(156, 210, 177), true);
        live.setPadding(dp(11), dp(7), dp(11), dp(7));
        live.setBackground(round(Color.rgb(22, 43, 34), 30));
        top.addView(live);

        TextView date = label("DEVICE OVERVIEW     /     实时状态", 10, muted, true);
        date.setPadding(0, dp(30), 0, dp(13));
        content.addView(date);

        LinearLayout hero = card();
        hero.setOrientation(LinearLayout.VERTICAL);
        content.addView(hero, matchWrap());
        LinearLayout heroHead = row();
        hero.addView(heroHead);
        heroHead.addView(label("设备性能", 15, white, true), new LinearLayout.LayoutParams(0, -2, 1));
        heroHead.addView(label("刷新间隔  1.0s", 10, muted, false));
        cpuValue = label("--%", 48, white, true);
        cpuValue.setPadding(0, dp(18), 0, 0);
        hero.addView(cpuValue);
        hero.addView(label("CPU 使用率", 11, muted, false));
        View barBack = new View(this);
        barBack.setBackground(round(Color.rgb(42, 45, 52), 20));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(5));
        bp.topMargin = dp(18);
        hero.addView(barBack, bp);

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams gp = matchWrap();
        gp.topMargin = dp(12);
        content.addView(grid, gp);
        LinearLayout left = metricCard("MEMORY", "运行内存", "--", "RAM");
        LinearLayout right = metricCard("BATTERY", "电池电量", "--", "POWER");
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, dp(112), 1);
        half.rightMargin = dp(6);
        grid.addView(left, half);
        LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, dp(112), 1);
        half2.leftMargin = dp(6);
        grid.addView(right, half2);
        ramValue = (TextView) left.getChildAt(2);
        batteryValue = (TextView) right.getChildAt(2);

        LinearLayout stats = card();
        stats.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams sp = matchWrap();
        sp.topMargin = dp(12);
        content.addView(stats, sp);
        stats.addView(label("运行状态", 15, white, true));
        addStat(stats, "图形处理器 GPU", "等待接入");
        addDivider(stats);
        addStat(stats, "画面帧率 FPS", "等待接入");
        addDivider(stats);
        addStat(stats, "Root 环境", "待检测");

        TextView featureTitle = label("快捷功能", 15, white, true);
        featureTitle.setPadding(0, dp(24), 0, dp(10));
        content.addView(featureTitle);
        LinearLayout feature = card();
        feature.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams fp = matchWrap();
        content.addView(feature, fp);
        LinearLayout featureText = new LinearLayout(this);
        featureText.setOrientation(LinearLayout.VERTICAL);
        feature.addView(featureText, new LinearLayout.LayoutParams(0, -2, 1));
        featureText.addView(label("透视", 16, white, true));
        featureText.addView(label("功能接口预留 · 尚未连接模块", 10, muted, false));
        Switch esp = new Switch(this);
        esp.setText("");
        esp.setOnCheckedChangeListener((button, checked) ->
            Toast.makeText(this, checked ? "透视接口尚未连接" : "已关闭", Toast.LENGTH_SHORT).show());
        feature.addView(esp);

        TextView foot = label("MONOPANEL  /  INDEPENDENT BUILD", 9, Color.rgb(82, 87, 96), true);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, dp(24), 0, dp(8));
        content.addView(foot);

        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(dp(8), dp(12), dp(8), dp(12));
        nav.setBackgroundColor(Color.rgb(13, 14, 18));
        root.addView(nav, new LinearLayout.LayoutParams(-1, dp(62)));
        String[] tabs = {"首页", "监控", "功能", "设置"};
        for (int i = 0; i < tabs.length; i++) {
            TextView t = label((i == 0 ? "◆  " : "◇  ") + tabs[i], 11,
                i == 0 ? white : muted, i == 0);
            t.setGravity(Gravity.CENTER);
            nav.addView(t, new LinearLayout.LayoutParams(0, -1, 1));
        }
    }

    private LinearLayout metricCard(String tag, String title, String value, String unit) {
        LinearLayout c = card();
        c.setOrientation(LinearLayout.VERTICAL);
        c.addView(label(tag, 9, muted, true));
        TextView t = label(title, 11, muted, false);
        t.setPadding(0, dp(9), 0, 0);
        c.addView(t);
        TextView v = label(value, 25, white, true);
        v.setPadding(0, dp(3), 0, 0);
        c.addView(v);
        return c;
    }

    private void refreshMetrics() {
        try {
            long[] now = readCpu();
            if (now != null && previousTotal > 0) {
                long total = now[0] - previousTotal;
                long idle = now[1] - previousIdle;
                if (total > 0) cpuValue.setText(String.format(Locale.US, "%.0f%%", 100f * (total - idle) / total));
            }
            if (now != null) { previousTotal = now[0]; previousIdle = now[1]; }
        } catch (Exception ignored) {}
        try {
            android.app.ActivityManager.MemoryInfo info = new android.app.ActivityManager.MemoryInfo();
            ((android.app.ActivityManager)getSystemService(ACTIVITY_SERVICE)).getMemoryInfo(info);
            long used = info.totalMem - info.availMem;
            ramValue.setText(String.format(Locale.US, "%.1fG", used / 1073741824.0));
        } catch (Exception ignored) {}
        try {
            android.content.Intent b = registerReceiver(null, new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));
            if (b != null) {
                int level = b.getIntExtra("level", 0), scale = b.getIntExtra("scale", 100);
                batteryValue.setText((level * 100 / Math.max(1, scale)) + "%");
            }
        } catch (Exception ignored) {}
    }

    private long[] readCpu() throws Exception {
        BufferedReader r = new BufferedReader(new FileReader("/proc/stat"));
        String line = r.readLine(); r.close();
        if (line == null || !line.startsWith("cpu ")) return null;
        String[] p = line.trim().split("\\s+");
        long total = 0; for (int i = 1; i < p.length; i++) total += Long.parseLong(p[i]);
        long idle = Long.parseLong(p[4]) + (p.length > 5 ? Long.parseLong(p[5]) : 0);
        return new long[]{total, idle};
    }

    private void addStat(LinearLayout parent, String name, String value) {
        LinearLayout r = row();
        r.setPadding(0, dp(13), 0, dp(13));
        r.addView(label(name, 12, muted, false), new LinearLayout.LayoutParams(0, -2, 1));
        r.addView(label(value, 11, white, true));
        parent.addView(r);
    }
    private void addDivider(LinearLayout p) {
        View v = new View(this); v.setBackgroundColor(Color.rgb(42,44,50));
        p.addView(v, new LinearLayout.LayoutParams(-1, dp(1)));
    }
    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setPadding(dp(16), dp(16), dp(16), dp(16));
        l.setBackground(round(panel, 18));
        return l;
    }
    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }
    private TextView label(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return t;
    }
    private GradientDrawable round(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), Color.rgb(34, 37, 44));
        return d;
    }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
    @Override protected void onResume() { super.onResume(); handler.post(ticker); }
    @Override protected void onPause() { handler.removeCallbacks(ticker); super.onPause(); }
}
