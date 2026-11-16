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
    private TextView cpuValue, ramValue, batteryValue, rootValue;
    private android.widget.ProgressBar cpuBar;
    private ScrollView mainScroll;
    private LinearLayout homePage;
    private TextView[] navLabels;
    private TextView monitorCpu, monitorRam, monitorBattery, monitorRoot;
    private TextView gpuStatusValue, refreshRateStatusValue;
    private boolean rootCheckStarted = false;
    private int activeTab = 0;
    private int refreshIntervalMs = 500;
    private long previousIdle, previousTotal;
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            refreshMetrics();
            handler.postDelayed(this, refreshIntervalMs);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(0);
        requestHighRefreshRate();
        buildScreen();
        refreshMetrics();
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);
        setContentView(root);

        ScrollView scroll = new ScrollView(this);
        mainScroll = scroll;
        scroll.setFillViewport(true);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout content = new LinearLayout(this);
        homePage = content;
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
        heroHead.addView(label("刷新间隔  0.5s", 10, muted, false));
        cpuValue = label("--%", 48, white, true);
        cpuValue.setPadding(0, dp(18), 0, 0);
        hero.addView(cpuValue);
        hero.addView(label("CPU 使用率", 11, muted, false));
        cpuBar = new android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        cpuBar.setMax(100);
        cpuBar.setProgressTintList(android.content.res.ColorStateList.valueOf(white));
        cpuBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(42, 45, 52)));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(5));
        bp.topMargin = dp(18);
        hero.addView(cpuBar, bp);
        TextView cpuStatus = label("数据源：/proc/stat · 等待采样", 10, muted, false);
        cpuStatus.setTag("cpu_status");
        LinearLayout.LayoutParams csp = matchWrap();
        csp.topMargin = dp(9);
        hero.addView(cpuStatus, csp);

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
        gpuStatusValue = addLiveStat(stats, "GPU 型号", readGpuModel());
        addDivider(stats);
        refreshRateStatusValue = addLiveStat(stats, "屏幕刷新率", readScreenRefreshRate());
        addDivider(stats);
        LinearLayout rootRow = row();
        rootRow.setPadding(0, dp(13), 0, dp(13));
        rootRow.addView(label("Root 环境", 12, muted, false), new LinearLayout.LayoutParams(0, -2, 1));
        rootValue = label("检测中", 11, white, true);
        rootRow.addView(rootValue);
        stats.addView(rootRow);

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
        navLabels = new TextView[tabs.length];
        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            TextView t = label((i == 0 ? "◆  " : "◇  ") + tabs[i], 11,
                i == 0 ? white : muted, i == 0);
            t.setGravity(Gravity.CENTER);
            navLabels[i] = t;
            nav.addView(t, new LinearLayout.LayoutParams(0, -1, 1));
            t.setOnClickListener(v -> selectTab(index));
        }
    }


    private void selectTab(int index) {
        activeTab = index;
        View page;
        if (index == 0) page = homePage;
        else if (index == 1) page = buildMonitorPage();
        else if (index == 2) page = buildFeaturePage();
        else page = buildSettingsPage();
        mainScroll.removeAllViews();
        mainScroll.addView(page);
        for (int i = 0; i < navLabels.length; i++) {
            navLabels[i].setText((i == index ? "◆  " : "◇  ") + new String[]{"首页","监控","功能","设置"}[i]);
            navLabels[i].setTextColor(i == index ? white : muted);
            navLabels[i].setTypeface(Typeface.create("sans-serif", i == index ? Typeface.BOLD : Typeface.NORMAL));
        }
        mainScroll.scrollTo(0, 0);
    }

    private LinearLayout pageBase(String title, String subtitle) {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(24), dp(22), dp(24));
        page.addView(label("MONOPANEL", 12, muted, true));
        TextView h = label(title, 27, white, true);
        h.setPadding(0, dp(5), 0, dp(5));
        page.addView(h);
        page.addView(label(subtitle, 10, muted, false));
        return page;
    }

    private LinearLayout buildMonitorPage() {
        LinearLayout p = pageBase("实时监控", "LIVE METRICS  /  每 0.5 秒刷新");
        LinearLayout c = card(); c.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams cp = matchWrap(); cp.topMargin = dp(22); p.addView(c, cp);
        c.addView(label("CPU 使用率", 13, muted, true));
        monitorCpu = label(cpuValue == null ? "--%" : cpuValue.getText().toString(), 38, white, true);
        monitorCpu.setPadding(0, dp(10), 0, dp(5)); c.addView(monitorCpu);
        c.addView(label("系统总内存使用量", 13, muted, true));
        monitorRam = label(ramValue == null ? "--" : ramValue.getText().toString(), 28, white, true);
        monitorRam.setPadding(0, dp(8), 0, dp(15)); c.addView(monitorRam);
        c.addView(label("电池电量", 13, muted, true));
        monitorBattery = label(batteryValue == null ? "--" : batteryValue.getText().toString(), 28, white, true);
        monitorBattery.setPadding(0, dp(8), 0, dp(15)); c.addView(monitorBattery);
        addDivider(c);
        addStat(c, "GPU 负载", "未接入传感器");
        addDivider(c); addStat(c, "实时 FPS", "未接入帧率源");
        addDivider(c); addStat(c, "屏幕刷新率", "请求 120Hz（系统可能限制）");
        addDivider(c); monitorRoot = label(rootValue == null ? "检测中" : rootValue.getText().toString(), 11, white, true);
        LinearLayout rr = row(); rr.setPadding(0, dp(13), 0, dp(13)); rr.addView(label("Root 状态", 12, muted, false), new LinearLayout.LayoutParams(0, -2, 1)); rr.addView(monitorRoot); c.addView(rr);
        TextView note = label("说明：未接入的数据不会用模拟数值填充。", 10, muted, false);
        note.setPadding(0, dp(16), 0, 0); p.addView(note);
        return p;
    }

    private LinearLayout buildFeaturePage() {
        LinearLayout p = pageBase("设备信息", "DEVICE INFORMATION  /  HARDWARE DETAILS");
        LinearLayout c = card();
        c.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams cp = matchWrap();
        cp.topMargin = dp(22);
        p.addView(c, cp);

        addInfoStat(c, "设备品牌", android.os.Build.MANUFACTURER);
        addDivider(c);
        addInfoStat(c, "设备型号", android.os.Build.MODEL);
        addDivider(c);
        addInfoStat(c, "设备代号", android.os.Build.DEVICE + " / " + android.os.Build.PRODUCT);
        addDivider(c);
        String soc = "";
        if (android.os.Build.VERSION.SDK_INT >= 31) soc = android.os.Build.SOC_MODEL;
        if (soc == null || soc.trim().isEmpty()) soc = android.os.Build.HARDWARE;
        addInfoStat(c, "SoC / 平台", displayOrUnavailable(soc));
        addDivider(c);
        addInfoStat(c, "CPU 型号", readCpuModel());
        addDivider(c);
        addInfoStat(c, "CPU 核心数", Runtime.getRuntime().availableProcessors() + " 核");
        addDivider(c);
        addInfoStat(c, "CPU 最高频率", readMaxCpuFrequency());
        addDivider(c);
        addInfoStat(c, "系统版本", "Android " + android.os.Build.VERSION.RELEASE
            + " (API " + android.os.Build.VERSION.SDK_INT + ")");
        addDivider(c);
        addInfoStat(c, "系统构建", displayOrUnavailable(android.os.Build.DISPLAY));
        addDivider(c);
        addInfoStat(c, "CPU ABI", android.os.Build.SUPPORTED_ABIS.length > 0
            ? android.os.Build.SUPPORTED_ABIS[0] : "未知");
        addDivider(c);
        addInfoStat(c, "总运行内存", readTotalMemory());
        addDivider(c);
        addInfoStat(c, "屏幕分辨率", readScreenResolution());
        addDivider(c);
        addInfoStat(c, "屏幕刷新率", readScreenRefreshRate());

        TextView note = label("以上信息来自 Android 系统属性、/proc 与设备显示参数。个别厂商可能限制部分硬件字段读取。", 10, muted, false);
        note.setPadding(dp(2), dp(16), dp(2), 0);
        p.addView(note);
        return p;
    }

    private void addInfoStat(LinearLayout parent, String name, String value) {
        LinearLayout r = row();
        r.setPadding(0, dp(12), 0, dp(12));
        TextView key = label(name, 11, muted, false);
        r.addView(key, new LinearLayout.LayoutParams(dp(104), -2));
        TextView val = label(displayOrUnavailable(value), 12, white, true);
        val.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        r.addView(val, new LinearLayout.LayoutParams(0, -2, 1));
        parent.addView(r);
    }

    private String displayOrUnavailable(String value) {
        return value == null || value.trim().isEmpty() ? "系统未提供" : value.trim();
    }

    private String readCpuModel() {
        String[] keys = {"Hardware", "model name", "Processor", "Model"};
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int split = line.indexOf(':');
                if (split <= 0) continue;
                String key = line.substring(0, split).trim();
                for (String candidate : keys) {
                    if (candidate.equalsIgnoreCase(key)) {
                        String value = line.substring(split + 1).trim();
                        if (!value.isEmpty()) return value;
                    }
                }
            }
        } catch (Exception ignored) {}
        return displayOrUnavailable(android.os.Build.HARDWARE);
    }

    private String readMaxCpuFrequency() {
        long maxKHz = 0;
        int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
        for (int i = 0; i < cores; i++) {
            String[] paths = {
                "/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_max_freq",
                "/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_max_freq"
            };
            for (String path : paths) {
                try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
                    String value = reader.readLine();
                    if (value != null) maxKHz = Math.max(maxKHz, Long.parseLong(value.trim()));
                    if (maxKHz > 0) break;
                } catch (Exception ignored) {}
            }
        }
        return maxKHz > 0 ? String.format(Locale.US, "%.2f GHz", maxKHz / 1000000.0)
            : "系统限制读取";
    }

    private String readTotalMemory() {
        try {
            android.app.ActivityManager.MemoryInfo info = new android.app.ActivityManager.MemoryInfo();
            ((android.app.ActivityManager)getSystemService(ACTIVITY_SERVICE)).getMemoryInfo(info);
            return String.format(Locale.US, "%.1f GB", info.totalMem / 1073741824.0);
        } catch (Exception e) {
            return "读取失败";
        }
    }

    private String readScreenResolution() {
        try {
            android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
            return metrics.widthPixels + " × " + metrics.heightPixels;
        } catch (Exception e) {
            return "读取失败";
        }
    }

    private String readScreenRefreshRate() {
        try {
            float rate = getWindowManager().getDefaultDisplay().getRefreshRate();
            return String.format(Locale.US, "%.0f Hz", rate);
        } catch (Exception e) {
            return "读取失败";
        }
    }

    private LinearLayout buildSettingsPage() {
        LinearLayout p = pageBase("设置", "DISPLAY  /  APPLICATION");
        LinearLayout c = card(); c.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams cp = matchWrap(); cp.topMargin = dp(22); p.addView(c, cp);
        addStat(c, "CPU / 内存采样间隔", "500 ms");
        addDivider(c);
        addStat(c, "目标屏幕刷新率", "120 Hz");
        addDivider(c);
        addStat(c, "应用版本", "1.4");
        addDivider(c);
        addStat(c, "主题", "黑白 · 深色");
        addDivider(c);
        addStat(c, "数据策略", "仅显示真实读取值");
        TextView refresh = label("重新请求高刷新率", 13, white, true);
        refresh.setGravity(Gravity.CENTER);
        refresh.setPadding(0, dp(16), 0, dp(16));
        refresh.setBackground(round(Color.rgb(30,32,38), 12));
        LinearLayout.LayoutParams rp = matchWrap(); rp.topMargin = dp(14); p.addView(refresh, rp);
        refresh.setOnClickListener(v -> {
            requestHighRefreshRate();
            Toast.makeText(this, "已向系统请求可用的 120Hz 显示模式", Toast.LENGTH_SHORT).show();
        });
        return p;
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

    private TextView findCpuStatusView() {
        if (homePage == null) return null;
        return findTaggedText(homePage, "cpu_status");
    }

    private TextView findTaggedText(View view, Object tag) {
        if (tag.equals(view.getTag()) && view instanceof TextView) return (TextView) view;
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = findTaggedText(group.getChildAt(i), tag);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void refreshMetrics() {
        try {
            long[] now = readCpu();
            if (now != null && previousTotal > 0) {
                long total = now[0] - previousTotal;
                long idle = now[1] - previousIdle;
                if (total > 0) {
                    int usage = (int)Math.max(0, Math.min(100, 100f * (total - idle) / total));
                    cpuValue.setText(String.format(Locale.US, "%d%%", usage));
                    TextView status = findCpuStatusView();
                    if (status != null) status.setText("Root 数据源：su → /proc/stat · 正常读取");
                    if (monitorCpu != null) monitorCpu.setText(cpuValue.getText());
                    cpuBar.setProgress(usage);
                }
            }
            if (now != null) { previousTotal = now[0]; previousIdle = now[1]; }
        } catch (Exception cpuError) {
            String detail = cpuError.getClass().getSimpleName();
            if (cpuError.getMessage() != null && !cpuError.getMessage().trim().isEmpty()) {
                detail += ": " + cpuError.getMessage().trim();
            }
            android.util.Log.e("MonoPanel-CPU", "Failed to read /proc/stat", cpuError);
            if (cpuValue != null) cpuValue.setText("读取失败");
            if (monitorCpu != null) monitorCpu.setText("读取失败");
            TextView status = findCpuStatusView();
            if (status != null) status.setText("CPU 读取异常 · " + detail);
        }
        try {
            android.app.ActivityManager.MemoryInfo info = new android.app.ActivityManager.MemoryInfo();
            ((android.app.ActivityManager)getSystemService(ACTIVITY_SERVICE)).getMemoryInfo(info);
            long used = info.totalMem - info.availMem;
            ramValue.setText(String.format(Locale.US, "%.1fG", used / 1073741824.0));
            if (monitorRam != null) monitorRam.setText(ramValue.getText());
        } catch (Exception ignored) {}
        try {
            android.content.Intent b = registerReceiver(null, new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));
            if (b != null) {
                int level = b.getIntExtra("level", 0), scale = b.getIntExtra("scale", 100);
                batteryValue.setText((level * 100 / Math.max(1, scale)) + "%");
                if (monitorBattery != null) monitorBattery.setText(batteryValue.getText());
            }
        } catch (Exception ignored) {}
        if (gpuStatusValue != null) gpuStatusValue.setText(readGpuModel());
        if (refreshRateStatusValue != null) refreshRateStatusValue.setText(readScreenRefreshRate());
        if (monitorRoot != null) monitorRoot.setText(rootValue == null ? "正在验证授权" : rootValue.getText());
        verifyRootAuthorization();
    }

    private void requestHighRefreshRate() {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            try {
                android.view.Display display = getWindowManager().getDefaultDisplay();
                android.view.Display.Mode best = null;
                for (android.view.Display.Mode mode : display.getSupportedModes()) {
                    if (mode.getRefreshRate() >= 119f && (best == null || mode.getRefreshRate() > best.getRefreshRate())) best = mode;
                }
                if (best != null) {
                    android.view.WindowManager.LayoutParams attrs = getWindow().getAttributes();
                    attrs.preferredDisplayModeId = best.getModeId();
                    getWindow().setAttributes(attrs);
                }
            } catch (Exception ignored) {}
        }
    }

    private long[] readCpu() throws Exception {
        // Read CPU counters through the granted Superuser (SukiSU/Magisk) shell.
        Process process = new ProcessBuilder("su", "-c", "cat /proc/stat")
            .redirectErrorStream(true).start();
        String firstCpuLine = null;
        String line;
        try (BufferedReader reader = new BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()))) {
            while ((line = reader.readLine()) != null) {
                if (firstCpuLine == null && line.startsWith("cpu ")) firstCpuLine = line;
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new java.io.IOException("su 命令退出码 " + exitCode
                + (firstCpuLine == null ? "；请检查 SukiSU 授权" : ""));
        }
        if (firstCpuLine == null) {
            throw new java.io.IOException("Root 已执行，但未取得 /proc/stat CPU 总计数");
        }
        String[] p = firstCpuLine.trim().split("\\s+");
        if (p.length < 5) throw new java.io.IOException("CPU 计数格式不完整");
        long total = 0;
        int count = Math.min(p.length - 1, 8);
        for (int i = 1; i <= count; i++) total += Long.parseLong(p[i]);
        long idle = Long.parseLong(p[4]);
        if (p.length > 5) idle += Long.parseLong(p[5]);
        return new long[]{total, idle};
    }


    private TextView addLiveStat(LinearLayout parent, String name, String value) {
        LinearLayout r = row();
        r.setPadding(0, dp(13), 0, dp(13));
        r.addView(label(name, 12, muted, false), new LinearLayout.LayoutParams(0, -2, 1));
        TextView v = label(displayOrUnavailable(value), 11, white, true);
        r.addView(v);
        parent.addView(r);
        return v;
    }

    private String readGpuModel() {
        String[] paths = {
            "/sys/class/kgsl/kgsl-3d0/gpu_model",
            "/sys/class/kgsl/kgsl-3d0/gpu_name",
            "/sys/devices/platform/soc/3d00000.gpu/gpu_model",
            "/sys/kernel/gpu/gpu_model"
        };
        for (String path : paths) {
            try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
                String value = reader.readLine();
                if (value != null && !value.trim().isEmpty()) return value.trim();
            } catch (Exception ignored) {}
        }
        return "GPU 型号读取受系统限制";
    }

    private void verifyRootAuthorization() {
        if (rootCheckStarted) return;
        rootCheckStarted = true;
        new Thread(() -> {
            String result = "Root 授权失败";
            Process process = null;
            try {
                process = new ProcessBuilder("su", "-c", "id")
                    .redirectErrorStream(true).start();
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) output.append(line);
                }
                int code = process.waitFor();
                result = code == 0 && output.toString().contains("uid=0")
                    ? "Root 授权正常" : "Root 未获授权";
            } catch (Exception e) {
                result = "Root 检测失败";
                android.util.Log.e("MonoPanel-ROOT", "Root authorization check failed", e);
            }
            final String status = result;
            handler.post(() -> {
                if (rootValue != null) rootValue.setText(status);
                if (monitorRoot != null) monitorRoot.setText(status);
            });
        }, "MonoPanel-RootCheck").start();
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
