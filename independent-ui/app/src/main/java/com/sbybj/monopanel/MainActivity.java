package com.sbybj.monopanel;

import android.app.Activity;
import android.app.ActivityManager;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    public interface ModuleListener {
        void onModuleChanged(String moduleId, boolean enabled);
        void onAction(String actionId);
    }
    private static ModuleListener moduleListener;
    public static void setModuleListener(ModuleListener listener) { moduleListener = listener; }

    private final int BLACK=Color.rgb(4,4,4), WHITE=Color.rgb(245,245,245), MUTED=Color.rgb(150,150,150);
    private final Handler handler=new Handler(Looper.getMainLooper());
    private LinearLayout root;
    private TextView cpuValue, cpuDetail, runtimeValue, statusValue, rootStatus;
    private long startedAt, previousTotal=-1, previousIdle=-1;
    private final ArrayList<String> logs=new ArrayList<>();
    private final String[] ids={"module_1","module_2","module_3","module_4"};
    private final boolean[] states={false,false,false,false};
    private final String[] names={"模块一","模块二","模块三","模块四"};
    private final String[] descriptions={"独立模块接口 A","独立模块接口 B","独立模块接口 C","独立模块接口 D"};
    private final Runnable monitor=new Runnable(){ public void run(){
        if(cpuValue!=null){ sampleCpu(); updateRuntime(); }
        handler.postDelayed(this,2000);
    }};

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        startedAt=SystemClock.elapsedRealtime();
        getWindow().setStatusBarColor(BLACK); getWindow().setNavigationBarColor(BLACK);
        log("应用启动"); showHome(); handler.post(monitor); requestRootAccess();
    }
    @Override protected void onDestroy(){ handler.removeCallbacks(monitor); super.onDestroy(); }

    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int color,int radius){
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private GradientDrawable cardBg(){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.rgb(29,29,29),Color.rgb(13,13,13)});
        d.setCornerRadius(dp(20)); d.setStroke(dp(1),Color.rgb(42,42,42)); return d;
    }
    private TextView text(String s,int size,int color,boolean bold){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL); if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    private LinearLayout horizontal(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));}
    private void initPage(String title){
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BLACK);
        root=vertical(); root.setPadding(dp(18),dp(18),dp(18),dp(24)); scroll.addView(root); setContentView(scroll);
        LinearLayout bar=horizontal();
        TextView back=text("‹",36,WHITE,true); back.setGravity(Gravity.CENTER); bar.addView(back,lp(44,48));
        back.setOnClickListener(v->showHome());
        TextView heading=text(title,22,WHITE,true); bar.addView(heading,new LinearLayout.LayoutParams(0,dp(48),1));
        root.addView(bar,lp(-1,52));
    }
    private void addCard(LinearLayout parent,View child,int top){
        child.setBackground(cardBg()); LinearLayout.LayoutParams p=lp(-1,-2);p.topMargin=dp(top);parent.addView(child,p);
    }
    private void log(String s){
        logs.add(String.format(Locale.getDefault(),"%tT  %s",System.currentTimeMillis(),s));
        while(logs.size()>100)logs.remove(0);
    }

    private void showHome(){
        initPage("游戏内核辅助");
        LinearLayout header=horizontal(); header.setPadding(dp(12),dp(8),dp(8),dp(8));
        TextView logo=text("△",48,WHITE,true);logo.setGravity(Gravity.CENTER);header.addView(logo,lp(72,70));
        LinearLayout titles=vertical();titles.addView(text("游戏内核辅助",22,WHITE,true));titles.addView(text("系统监控 · 模块管理",13,MUTED,false));
        header.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        TextView gear=text("⚙",28,WHITE,false);gear.setGravity(Gravity.CENTER);gear.setBackground(shape(Color.rgb(40,40,40),16));header.addView(gear,lp(52,52));
        gear.setOnClickListener(v->{log("打开设置");showSettings();});
        addCard(root,header,0);

        LinearLayout status=vertical();status.setPadding(dp(16),dp(14),dp(16),dp(14));
        LinearLayout top=horizontal();top.addView(text("运行状态",15,MUTED,false),new LinearLayout.LayoutParams(0,dp(30),1));
        statusValue=text("● 运行中",15,WHITE,true);top.addView(statusValue);status.addView(top);
        LinearLayout stats=horizontal();
        LinearLayout cpu=vertical();cpu.addView(text("CPU 总占用率",13,MUTED,false));cpuValue=text("--%",30,WHITE,true);cpu.addView(cpuValue);cpuDetail=text("读取系统统计中",11,MUTED,false);cpu.addView(cpuDetail);
        stats.addView(cpu,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout rt=vertical();rt.addView(text("本次运行时长",13,MUTED,false));runtimeValue=text("00:00:00",23,WHITE,true);rt.addView(runtimeValue);
        stats.addView(rt,new LinearLayout.LayoutParams(0,-2,1));status.addView(stats);
        addCard(root,status,14);

        LinearLayout rootCard=horizontal(); rootCard.setPadding(dp(16),dp(10),dp(16),dp(10));
        rootStatus=text("Root 权限：检测中…",13,WHITE,true); rootCard.addView(rootStatus,new LinearLayout.LayoutParams(0,dp(38),1));
        TextView refreshRoot=text("检测 / 授权",12,WHITE,true); refreshRoot.setGravity(Gravity.CENTER); refreshRoot.setPadding(dp(10),0,dp(10),0); refreshRoot.setBackground(shape(Color.rgb(55,55,55),18)); rootCard.addView(refreshRoot,lp(-2,32));
        refreshRoot.setOnClickListener(v->requestRootAccess());
        addCard(root,rootCard,10);

        LinearLayout feature=vertical();feature.setPadding(dp(16),dp(12),dp(16),dp(12));
        LinearLayout fh=horizontal();fh.addView(text("模块接口",19,WHITE,true),new LinearLayout.LayoutParams(0,dp(42),1));
        TextView all=text("全部启用",13,WHITE,true);all.setGravity(Gravity.CENTER);all.setBackground(shape(Color.rgb(55,55,55),22));all.setPadding(dp(12),0,dp(12),0);fh.addView(all,lp(92,34));all.setOnClickListener(v->{for(int i=0;i<4;i++)setModule(i,true);});
        feature.addView(fh);
        for(int i=0;i<4;i++)addModuleRow(feature,i);
        addCard(root,feature,14);

        LinearLayout device=vertical();device.setPadding(dp(16),dp(12),dp(16),dp(14));
        device.addView(text("设备信息",19,WHITE,true),lp(-1,38));
        device.addView(infoLine("设备型号",Build.MANUFACTURER+" "+Build.MODEL));
        device.addView(infoLine("Android 版本",Build.VERSION.RELEASE+" (API "+Build.VERSION.SDK_INT+")"));
        device.addView(infoLine("处理器架构",Build.SUPPORTED_ABIS.length>0?Build.SUPPORTED_ABIS[0]:"未知"));
        ActivityManager.MemoryInfo mi=new ActivityManager.MemoryInfo();((ActivityManager)getSystemService(ACTIVITY_SERVICE)).getMemoryInfo(mi);
        device.addView(infoLine("可用内存",String.format(Locale.getDefault(),"%.1f GB",mi.availMem/1073741824.0)));
        TextView more=text("查看完整设备信息  ›",13,WHITE,false);device.addView(more,lp(-1,38));more.setOnClickListener(v->showDevice());
        addCard(root,device,14);

        LinearLayout shortcuts=vertical();shortcuts.setPadding(dp(16),dp(12),dp(16),dp(12));
        shortcuts.addView(text("快捷入口",19,WHITE,true),lp(-1,38));
        addAction(shortcuts,"界面设置","设置界面参数",()->showSettings());
        addAction(shortcuts,"功能管理","管理模块开关及接口",()->showModules());
        addAction(shortcuts,"运行日志","查看本地操作记录",()->showLogs());
        addAction(shortcuts,"退出应用","关闭当前面板",()->{log("退出应用");finish();});
        addCard(root,shortcuts,14);
        TextView footer=text("MONOPANEL  ·  LOCAL SYSTEM MONITOR",10,MUTED,false);footer.setGravity(Gravity.CENTER);
        root.addView(footer,lp(-1,48));
    }
    private View infoLine(String k,String v){
        LinearLayout row=horizontal();row.setPadding(0,dp(4),0,dp(4));
        row.addView(text(k,13,MUTED,false),new LinearLayout.LayoutParams(0,dp(30),1));
        row.addView(text(v,13,WHITE,false));return row;
    }
    private void addModuleRow(LinearLayout parent,int index){
        LinearLayout row=horizontal();row.setPadding(0,dp(5),0,dp(5));
        LinearLayout labels=vertical();labels.addView(text(names[index],16,WHITE,true));labels.addView(text(descriptions[index],11,MUTED,false));
        row.addView(labels,new LinearLayout.LayoutParams(0,dp(54),1));
        TextView toggle=text(states[index]?"开启":"关闭",12,WHITE,true);toggle.setGravity(Gravity.CENTER);
        toggle.setBackground(shape(states[index]?Color.rgb(230,230,230):Color.rgb(65,65,65),22));
        toggle.setTextColor(states[index]?Color.BLACK:WHITE);row.addView(toggle,lp(76,34));
        toggle.setOnClickListener(v->setModule(index,!states[index]));parent.addView(row,lp(-1,60));
    }
    private void setModule(int i,boolean enabled){
        states[i]=enabled;log(names[i]+(enabled?" 已启用":" 已关闭"));
        if(moduleListener!=null)moduleListener.onModuleChanged(ids[i],enabled);
        showHome();
    }
    private void addAction(LinearLayout parent,String title,String desc,Runnable action){
        LinearLayout row=horizontal();LinearLayout labels=vertical();labels.addView(text(title,15,WHITE,true));labels.addView(text(desc,11,MUTED,false));
        row.addView(labels,new LinearLayout.LayoutParams(0,dp(54),1));row.addView(text("›",24,MUTED,false),lp(30,48));
        row.setOnClickListener(v->action.run());parent.addView(row,lp(-1,58));
    }
    private void sampleCpu(){
        try{
            BufferedReader br=new BufferedReader(new FileReader("/proc/stat"));String line=br.readLine();br.close();
            if(line==null||!line.startsWith("cpu "))throw new Exception("cpu stats unavailable");
            String[] p=line.trim().split("\\s+");long total=0;for(int i=1;i<p.length;i++)total+=Long.parseLong(p[i]);
            long idle=Long.parseLong(p[4])+(p.length>5?Long.parseLong(p[5]):0);
            if(previousTotal>=0){long dt=total-previousTotal,di=idle-previousIdle;if(dt>0){
                int percent=(int)Math.max(0,Math.min(100,Math.round((dt-di)*100.0/dt)));
                cpuValue.setText(percent+"%");cpuDetail.setText("系统总体 CPU · 每 2 秒更新");
            }}previousTotal=total;previousIdle=idle;
        }catch(Exception e){cpuValue.setText("N/A");cpuDetail.setText("系统未开放 CPU 统计权限");}
    }
    private void updateRuntime(){
        long sec=(SystemClock.elapsedRealtime()-startedAt)/1000;
        runtimeValue.setText(String.format(Locale.getDefault(),"%02d:%02d:%02d",sec/3600,(sec/60)%60,sec%60));
    }
    private void showSettings(){
        initPage("界面设置");LinearLayout c=vertical();c.setPadding(dp(16),dp(12),dp(16),dp(12));
        c.addView(text("监控刷新间隔",16,WHITE,true));addAction(c,"2 秒","CPU 与运行时长刷新频率",()->{log("刷新间隔保持 2 秒");showSettings();});
        addAction(c,"重置模块开关","将全部模块接口设为关闭",()->{for(int i=0;i<4;i++)setModule(i,false);showSettings();});
        addAction(c,"清空运行日志","删除本地日志记录",()->{logs.clear();log("日志已清空");showSettings();});
        addCard(root,c,14);
    }
    private void showModules(){
        initPage("功能管理");LinearLayout c=vertical();c.setPadding(dp(16),dp(12),dp(16),dp(12));
        for(int i=0;i<4;i++)addModuleRow(c,i);addCard(root,c,14);
        addAction(root,"接口说明","通过 ModuleListener 接收模块状态变化",()->showApi());
    }
    private void showApi(){
        initPage("接口说明");LinearLayout c=vertical();c.setPadding(dp(16),dp(16),dp(16),dp(16));
        c.addView(text("模块回调接口",18,WHITE,true));
        c.addView(text("MainActivity.setModuleListener(new MainActivity.ModuleListener() {\n  public void onModuleChanged(String moduleId, boolean enabled) { }\n  public void onAction(String actionId) { }\n});",13,WHITE,false));
        c.addView(text("模块 ID：module_1、module_2、module_3、module_4。外部模块需在应用进程内注册监听器。",12,MUTED,false));
        addCard(root,c,14);
    }
    private void showDevice(){
        initPage("设备信息");LinearLayout c=vertical();c.setPadding(dp(16),dp(12),dp(16),dp(12));
        c.addView(infoLine("品牌",Build.BRAND));c.addView(infoLine("制造商",Build.MANUFACTURER));c.addView(infoLine("型号",Build.MODEL));
        c.addView(infoLine("设备代号",Build.DEVICE));c.addView(infoLine("产品",Build.PRODUCT));c.addView(infoLine("Android",Build.VERSION.RELEASE));
        c.addView(infoLine("SDK",String.valueOf(Build.VERSION.SDK_INT)));c.addView(infoLine("CPU ABI",Build.SUPPORTED_ABIS[0]));
        c.addView(infoLine("Root 状态", RootInfoReader.getLastStatus()));
        c.addView(infoLine("Root 系统信息", RootInfoReader.getLastDetails()));
        addAction(c,"重新检测 Root","由系统 Root 管理器决定是否授权",()->requestRootAccess());
        addCard(root,c,14);
    }

    private void requestRootAccess(){
        if(rootStatus!=null) rootStatus.setText("Root 权限：等待系统授权…");
        RootInfoReader.read(this,(granted,details)->{
            String status=granted?"Root 权限：已授权":"Root 权限：未授权 / 不可用";
            RootInfoReader.setLastResult(status,details);
            if(rootStatus!=null) rootStatus.setText(status+" · "+details);
            log(granted?"Root 授权成功":"Root 未授权或读取失败");
        });
    }

    private void showLogs(){
        initPage("运行日志");LinearLayout c=vertical();c.setPadding(dp(16),dp(12),dp(16),dp(12));
        if(logs.isEmpty())c.addView(text("暂无日志",14,MUTED,false));
        for(String line:logs)c.addView(text(line,12,WHITE,false),lp(-1,28));
        addCard(root,c,14);
    }
}