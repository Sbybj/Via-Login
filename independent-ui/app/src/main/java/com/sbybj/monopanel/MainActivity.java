package com.sbybj.monopanel;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Switch;
import android.widget.Toast;

public class MainActivity extends Activity {
    private final int BG = Color.rgb(12, 12, 12);
    private final int PANEL = Color.rgb(27, 27, 27);
    private final int WHITE = Color.WHITE;
    private final int MUTED = Color.rgb(155, 155, 155);
    private LinearLayout content;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        showHome();
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private void base(String section) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = column();
        content.setPadding(22, 28, 22, 28);
        content.setBackgroundColor(BG);
        scroll.addView(content);
        setContentView(scroll);

        TextView brand = text("MONO / PANEL", 13, MUTED, true);
        brand.setLetterSpacing(0.18f);
        content.addView(brand);
        TextView title = text(section, 30, WHITE, true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.topMargin = 14;
        content.addView(title, tp);
        TextView sub = text("独立界面演示  ·  BUILD 01", 12, MUTED, false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = 6;
        content.addView(sub, sp);
    }

    private void card(String title, String subtitle, View trailing) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(16, 15, 14, 15);
        row.setBackground(shape(PANEL, 18));
        LinearLayout left = column();
        left.addView(text(title, 15, WHITE, true));
        TextView detail = text(subtitle, 12, MUTED, false);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, -2);
        dp.topMargin = 5;
        left.addView(detail, dp);
        row.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
        if (trailing != null) row.addView(trailing);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.topMargin = 12;
        content.addView(row, rp);
    }

    private Switch toggle(String label, boolean checked) {
        Switch s = new Switch(this);
        s.setText(label);
        s.setTextColor(WHITE);
        s.setChecked(checked);
        s.setOnCheckedChangeListener((b, on) ->
            Toast.makeText(this, on ? "演示开关：已开启" : "演示开关：已关闭", Toast.LENGTH_SHORT).show());
        return s;
    }

    private void showHome() {
        base("控制中心");
        TextView status = text("●  UI READY", 12, WHITE, true);
        status.setPadding(12, 9, 12, 9);
        status.setBackground(shape(Color.rgb(48,48,48), 30));
        LinearLayout.LayoutParams st = new LinearLayout.LayoutParams(-2, -2);
        st.topMargin = 20;
        content.addView(status, st);

        card("界面状态", "独立运行 · 不连接游戏进程", text("ACTIVE", 11, WHITE, true));
        card("模块开关", "仅用于界面交互演示", toggle("启用演示", false));
        card("显示设置", "调整面板显示状态", toggle("简洁模式", true));
        card("触感反馈", "本地开关演示", toggle("按键反馈", false));

        TextView settings = button("设置与设备信息");
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, 52);
        bp.topMargin = 22;
        content.addView(settings, bp);
        settings.setOnClickListener(v -> showSettings());
        TextView foot = text("MONO PANEL  •  ANDROID", 10, MUTED, true);
        foot.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
        fp.topMargin = 26;
        content.addView(foot, fp);
    }

    private TextView button(String label) {
        TextView b = text(label, 14, WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(shape(Color.rgb(42,42,42), 14));
        return b;
    }

    private void showSettings() {
        base("设置");
        card("设备信息", "Android 版本与设备型号", text("查看", 12, WHITE, true));
        card("应用信息", "MonoPanel · 版本 1.0", text("INFO", 11, MUTED, true));
        TextView back = button("返回控制中心");
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, 52);
        p.topMargin = 22;
        content.addView(back, p);
        back.setOnClickListener(v -> showHome());
    }
}
