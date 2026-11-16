package com.sbybj.monopanel;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private final int BLACK = Color.rgb(3, 3, 3);
    private final int CARD = Color.rgb(20, 20, 20);
    private final int WHITE = Color.rgb(248, 248, 248);
    private final int MUTED = Color.rgb(150, 150, 150);
    private LinearLayout page;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BLACK);
        getWindow().setNavigationBarColor(BLACK);
        getWindow().getDecorView().setSystemUiVisibility(0);
        showHome();
    }

    private int dp(float v) {
        return (int)(v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private GradientDrawable gradient(int top, int bottom, int radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{top, bottom});
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), Color.rgb(38, 38, 38));
        return d;
    }

    private TextView txt(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout horizontal() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h));
    }

    private void showHome() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BLACK);
        page = vertical();
        page.setPadding(dp(18), dp(18), dp(18), dp(20));
        scroll.addView(page);
        setContentView(scroll);

        addHeader();
        addStatus();
        addFeatures();
        addDevice();
        addShortcuts();

        TextView footer = txt("—   极致体验 · 简约设计   —", 13, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams fp = lp(-1, 48);
        fp.topMargin = dp(16);
        page.addView(footer, fp);
    }

    private void addHeader() {
        LinearLayout header = horizontal();
        header.setPadding(dp(14), dp(12), dp(8), dp(14));
        header.setBackground(gradient(Color.rgb(18,18,18), Color.rgb(5,5,5), 20));
        TextView logo = txt("△", 58, WHITE, true);
        logo.setGravity(Gravity.CENTER);
        header.addView(logo, lp(82, 82));

        LinearLayout titleBox = vertical();
        TextView title = txt("游戏内核辅助", 23, WHITE, true);
        titleBox.addView(title);
        TextView sub = txt("稳定 · 高效 · 专注", 14, MUTED, false);
        LinearLayout.LayoutParams subp = lp(-1, 30);
        titleBox.addView(sub, subp);
        LinearLayout.LayoutParams tb = new LinearLayout.LayoutParams(0, -2, 1);
        tb.leftMargin = dp(8);
        header.addView(titleBox, tb);

        TextView gear = txt("⚙", 31, WHITE, false);
        gear.setGravity(Gravity.CENTER);
        gear.setBackground(bg(Color.rgb(35,35,35), 18));
        header.addView(gear, lp(58, 58));
        gear.setOnClickListener(v -> showSettings());
        page.addView(header, lp(-1, 116));
    }

    private void addStatus() {
        LinearLayout card = horizontal();
        card.setPadding(dp(18), dp(18), dp(16), dp(18));
        card.setBackground(gradient(Color.rgb(27,27,27), Color.rgb(15,15,15), 22));

        TextView ring = txt("✓", 42, WHITE, true);
        ring.setGravity(Gravity.CENTER);
        GradientDrawable circle = bg(Color.rgb(12,12,12), 100);
        circle.setStroke(dp(7), Color.rgb(220,220,225));
        ring.setBackground(circle);
        card.addView(ring, lp(112, 112));

        LinearLayout state = vertical();
        state.setPadding(dp(16), 0, dp(8), 0);
        state.addView(txt("运行状态", 14, MUTED, false));
        TextView started = txt("已启动", 25, WHITE, true);
        LinearLayout.LayoutParams st = lp(-1, 38);
        st.topMargin = dp(3);
        state.addView(started, st);
        state.addView(txt("独立界面运行中…", 13, MUTED, false));
        LinearLayout.LayoutParams statep = new LinearLayout.LayoutParams(0, -2, 1);
        card.addView(state, statep);

        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(75,75,75));
        card.addView(divider, lp(1, 76));

        LinearLayout time = vertical();
        time.setPadding(dp(12), 0, 0, 0);
        time.addView(txt("ϟ 运行时长", 12, MUTED, false));
        time.addView(txt("00:00:32", 18, WHITE, true));
        card.addView(time, lp(112, -2));

        LinearLayout.LayoutParams cp = lp(-1, 154);
        cp.topMargin = dp(16);
        page.addView(card, cp);
    }

    private void addFeatures() {
        LinearLayout card = vertical();
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.setBackground(gradient(Color.rgb(26,26,26), Color.rgb(15,15,15), 22));

        LinearLayout heading = horizontal();
        heading.addView(txt("⬡  功能控制", 19, WHITE, true), new LinearLayout.LayoutParams(0, dp(42), 1));
        TextView all = txt("全部开启  ▶", 14, WHITE, true);
        all.setGravity(Gravity.CENTER);
        all.setPadding(dp(14), 0, dp(12), 0);
        all.setBackground(bg(Color.rgb(57,57,57), 30));
        heading.addView(all, lp(112, 38));
        all.setOnClickListener(v -> Toast.makeText(this, "仅为界面演示", Toast.LENGTH_SHORT).show());
        card.addView(heading);

        addFeatureRow(card, "⊕", "透视功能", "显示敌人位置与骨骼");
        addFeatureRow(card, "◎", "自瞄功能", "自动锁定目标演示");
        addFeatureRow(card, "ϟ", "人物加速", "移动速度设置演示");
        addFeatureRow(card, "〽", "无后坐力", "稳定性设置演示");

        LinearLayout.LayoutParams cp = lp(-1, -2);
        cp.topMargin = dp(16);
        page.addView(card, cp);
    }

    private void addFeatureRow(LinearLayout parent, String icon, String name, String desc) {
        LinearLayout row = horizontal();
        row.setPadding(dp(2), dp(8), dp(2), dp(8));
        TextView symbol = txt(icon, 31, WHITE, true);
        symbol.setGravity(Gravity.CENTER);
        row.addView(symbol, lp(52, 58));

        LinearLayout labels = vertical();
        labels.setPadding(dp(8), 0, 0, 0);
        labels.addView(txt(name, 18, WHITE, true));
        labels.addView(txt(desc, 13, MUTED, false));
        LinearLayout.LayoutParams lab = new LinearLayout.LayoutParams(0, -2, 1);
        row.addView(labels, lab);

        FrameLayout toggle = new FrameLayout(this);
        toggle.setBackground(bg(Color.rgb(175,175,182), 40));
        View knob = new View(this);
        knob.setBackground(bg(WHITE, 40));
        FrameLayout.LayoutParams kp = new FrameLayout.LayoutParams(dp(38), dp(38), Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        kp.rightMargin = dp(2);
        toggle.addView(knob, kp);
        toggle.setTag(Boolean.TRUE);
        toggle.setOnClickListener(v -> {
            boolean on = Boolean.TRUE.equals(toggle.getTag());
            toggle.setTag(!on);
            toggle.setBackground(bg(on ? Color.rgb(75,75,75) : Color.rgb(175,175,182), 40));
            FrameLayout.LayoutParams pos = new FrameLayout.LayoutParams(dp(38), dp(38),
                    (on ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL);
            pos.leftMargin = dp(2); pos.rightMargin = dp(2);
            knob.setLayoutParams(pos);
            Toast.makeText(this, "界面演示开关", Toast.LENGTH_SHORT).show();
        });
        row.addView(toggle, lp(84, 38));
        parent.addView(row, lp(-1, 74));
    }

    private void addDevice() {
        LinearLayout card = vertical();
        card.setPadding(dp(18), dp(15), dp(18), dp(16));
        card.setBackground(gradient(Color.rgb(26,26,26), Color.rgb(15,15,15), 22));

        LinearLayout head = horizontal();
        head.addView(txt("▣  设备信息", 18, WHITE, true), new LinearLayout.LayoutParams(0, dp(38), 1));
        head.addView(txt("更多  ›", 14, MUTED, false));
        card.addView(head);

        LinearLayout row1 = horizontal();
        row1.addView(infoCell("▣", "设备型号", "一加 Ace 5 至尊版"), new LinearLayout.LayoutParams(0, dp(70), 1));
        row1.addView(infoCell("▦", "处理器", "骁龙 8 至尊版"), new LinearLayout.LayoutParams(0, dp(70), 1));
        card.addView(row1);
        LinearLayout row2 = horizontal();
        row2.addView(infoCell("⚙", "Android 版本", "16"), new LinearLayout.LayoutParams(0, dp(70), 1));
        row2.addView(infoCell("▤", "运行内存", "16 GB"), new LinearLayout.LayoutParams(0, dp(70), 1));
        card.addView(row2);

        LinearLayout.LayoutParams cp = lp(-1, -2);
        cp.topMargin = dp(16);
        page.addView(card, cp);
    }

    private View infoCell(String icon, String label, String value) {
        LinearLayout cell = horizontal();
        TextView ic = txt(icon, 27, WHITE, true);
        ic.setGravity(Gravity.CENTER);
        cell.addView(ic, lp(48, 56));
        LinearLayout words = vertical();
        words.addView(txt(label, 13, MUTED, false));
        words.addView(txt(value, 14, WHITE, false));
        cell.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
        return cell;
    }

    private void addShortcuts() {
        LinearLayout card = vertical();
        card.setPadding(dp(18), dp(14), dp(18), dp(14));
        card.setBackground(gradient(Color.rgb(26,26,26), Color.rgb(15,15,15), 22));
        LinearLayout head = horizontal();
        head.addView(txt("☷  快捷设置", 18, WHITE, true), new LinearLayout.LayoutParams(0, dp(38), 1));
        head.addView(txt("更多  ›", 14, MUTED, false));
        card.addView(head);

        LinearLayout actions = horizontal();
        addShortcut(actions, "✎", "界面设置", () -> showSettings());
        addShortcut(actions, "✣", "功能管理", () -> Toast.makeText(this, "仅提供界面演示", Toast.LENGTH_SHORT).show());
        addShortcut(actions, "▤", "运行日志", () -> Toast.makeText(this, "暂无运行日志", Toast.LENGTH_SHORT).show());
        addShortcut(actions, "⏻", "退出应用", () -> finish());
        card.addView(actions, lp(-1, 94));

        LinearLayout.LayoutParams cp = lp(-1, -2);
        cp.topMargin = dp(16);
        page.addView(card, cp);
    }

    private void addShortcut(LinearLayout row, String icon, String label, Runnable action) {
        LinearLayout item = vertical();
        item.setGravity(Gravity.CENTER);
        TextView glyph = txt(icon, 27, WHITE, true);
        glyph.setGravity(Gravity.CENTER);
        glyph.setBackground(bg(Color.rgb(37,37,37), 50));
        item.addView(glyph, lp(54, 54));
        TextView caption = txt(label, 12, WHITE, false);
        caption.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams cp = lp(-1, 28);
        item.addView(caption, cp);
        item.setOnClickListener(v -> action.run());
        row.addView(item, new LinearLayout.LayoutParams(0, -1, 1));
    }

    private void showSettings() {
        showHome();
        Toast.makeText(this, "设置入口为界面演示", Toast.LENGTH_SHORT).show();
    }
}
