package com.sbybj.monopanel;

import android.app.Activity;
import android.app.ActivityManager;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.FrameLayout;
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
    private TextView cpuDetail, runtimeValue, statusValue, rootStatus, averageCpu;
    private CpuHistoryView cpuHistory;
    private long startedAt, previousRootTotal=-1, previousRootIdle=-1;
    private final ArrayList<String> logs=new ArrayList<>();
    private final String[] ids={"module_1","module_2","module_3","module_4"};
    private final boolean[] states={false,false,false,false};
    private final String[] names={"模块一","模块二","模块三","模块四"};
    private final String[] descriptions={"独立模块接口 A","独立模块接口 B","独立模块接口 C","独立模块接口 D"};
    private final Runnable monitor=new Runnable(){ public void run(){
        if(cpuHistory!=null){ sampleRootCpu(); updateRuntime(); }
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
        // Translucent glass surface: deliberately no bright outline or edge glow.
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.argb(92,116,124,142),Color.argb(66,35,40,54),Color.argb(78,18,21,31)});
        d.setCornerRadius(dp(24));
        return d;
    }
    private TextView text(String s,int size,int color,boolean bold){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL); if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    private LinearLayout horizontal(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));}
    private void initPage(String title){
        FrameLayout frame=new FrameLayout(this);
        frame.setBackgroundResource(R.drawable.wallpaper);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        root=vertical(); root.setPadding(dp(18),dp(18),dp(18),dp(24)); scroll.addView(root);
        frame.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        setContentView(frame);
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
        initPage("MonoPanel");
        LinearLayout header=horizontal(); header.setPadding(dp(10),dp(4),dp(8),dp(8));
        TextView logo=text("M",40,WHITE,true);logo.setGravity(Gravity.CENTER);
        logo.setBackground(shape(Color.argb(105,72,78,94),22));header.addView(logo,lp(66,66));
        LinearLayout titles=vertical();titles.setPadding(dp(14),0,0,0);
        titles.addView(text("MonoPanel",25,WHITE,true));
        runtimeValue=text("运行时长 00:00:00",13,Color.rgb(210,214,225),false);titles.addView(runtimeValue);
        header.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        TextView gear=text("⚙",25,WHITE,false);gear.setGravity(Gravity.CENTER);
        gear.setBackground(shape(Color.argb(88,68,73,89),28));header.addView(gear,lp(48,48));
        gear.setOnClickListener(v->{log("打开设置");showSettings();});
        root.addView(header,lp(-1,78));

        LinearLayout summary=horizontal(); summary.setGravity(Gravity.TOP);
        LinearLayout cpu=vertical();cpu.setPadding(dp(16),dp(14),dp(14),dp(12));
        cpu.addView(text("后台 CPU",17,WHITE,true));
        averageCpu=text("平均 --.-%",16,WHITE,true);cpu.addView(averageCpu,lp(-1,26));
        cpuHistory=new CpuHistoryView();cpu.addView(cpuHistory,lp(-1,68));
        LinearLayout.LayoutParams cp= new LinearLayout.LayoutParams(0,dp(154),1.35f);
        cp.rightMargin=dp(8);summary.addView(cpu,cp);cpu.setBackground(cardBg());

        LinearLayout sys=vertical();sys.setPadding(dp(12),dp(13),dp(10),dp(10));
        rootStatus=text("● Root 检测中",13,WHITE,true);sys.addView(rootStatus,lp(-1,32));
        sys.addView(text("▣  内核版本",12,Color.rgb(218,222,232),false));
        sys.addView(text(Build.VERSION.RELEASE+" · API "+Build.VERSION.SDK_INT,12,WHITE,false),lp(-1,26));
        sys.addView(text("♨  CPU 温度",12,Color.rgb(218,222,232),false));
        sys.addView(text("读取设备温度节点",12,WHITE,false),lp(-1,26));
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,dp(154),1);
        summary.addView(sys,sp);sys.setBackground(cardBg());
        root.addView(summary,lp(-1,154));

        LinearLayout feature=vertical();feature.setPadding(dp(14),dp(10),dp(14),dp(10));
        LinearLayout fh=horizontal();fh.addView(text("模块接口",18,WHITE,true),new LinearLayout.LayoutParams(0,dp(34),1));
        TextView all=text("全部关闭",12,WHITE,true);all.setGravity(Gravity.CENTER);
        all.setBackground(shape(Color.argb(72,115,122,140),18));all.setPadding(dp(10),0,dp(10),0);fh.addView(all,lp(76,30));
        all.setOnClickListener(v->{for(int i=0;i<4;i++)setModule(i,false);});
        feature.addView(fh);
        for(int i=0;i<4;i++)addModuleRow(feature,i);
        addCard(root,feature,14);

        LinearLayout device=vertical();device.setPadding(dp(16),dp(12),dp(16),dp(14));
        device.addView(text("设备信息",18,WHITE,true),lp(-1,36));
        device.addView(infoLine("设备型号",Build.MANUFACTURER+" "+Build.MODEL));
        device.addView(infoLine("Android 版本",Build.VERSION.RELEASE));
        device.addView(infoLine("CPU 架构",Build.SUPPORTED_ABIS.length>0?Build.SUPPORTED_ABIS[0]:"未知"));
        ActivityManager.MemoryInfo mi=new ActivityManager.MemoryInfo();((ActivityManager)getSystemService(ACTIVITY_SERVICE)).getMemoryInfo(mi);
        device.addView(infoLine("可用内存",String.format(Locale.getDefault(),"%.1f GB",mi.availMem/1073741824.0)));
        device.addView(infoLine("运行时长",runtimeValue==null?"00:00:00":runtimeValue.getText().toString()));
        addCard(root,device,14);

        LinearLayout shortcuts=vertical();shortcuts.setPadding(dp(16),dp(10),dp(16),dp(10));
        shortcuts.addView(text("运行日志",18,WHITE,true),lp(-1,34));
        String latest=logs.isEmpty()?"等待系统采样":logs.get(logs.size()-1);
        shortcuts.addView(text("●  "+latest,12,WHITE,false),lp(-1,32));
        TextView more=text("查看全部日志  ›",12,Color.rgb(210,214,225),false);
        shortcuts.addView(more,lp(-1,28));more.setOnClickListener(v->showLogs());
        addCard(root,shortcuts,14);

        LinearLayout nav=horizontal();nav.setPadding(dp(8),dp(6),dp(8),dp(6));
        String[] navNames={"⌂\n首页","◇\n模块","▦\n更多","ⓘ\n关于"};
        for(int i=0;i<navNames.length;i++){
            final int index=i;TextView item=text(navNames[i],14,i==0?WHITE:Color.rgb(210,214,225),i==0);
            item.setGravity(Gravity.CENTER);nav.addView(item,new LinearLayout.LayoutParams(0,dp(54),1));
            item.setOnClickListener(v->{if(index==1)showModules();else if(index==2)showSettings();else if(index==3)showDevice();else showHome();});
        }
        addCard(root,nav,14);
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
    private final ArrayList<Integer> cpuSamples=new ArrayList<>();
    private void sampleRootCpu(){
        RootInfoReader.readCpuStat(this,(granted,counters)->{
            if(cpuHistory==null)return;
            if(!granted||counters==null||counters.length<2){
                cpuHistory.invalidate();averageCpu.setText("平均 --.-%");
                previousRootTotal=-1;previousRootIdle=-1;return;
            }
            long total=counters[0], idle=counters[1];
            if(previousRootTotal>=0){
                long dt=total-previousRootTotal, di=idle-previousRootIdle;
                if(dt>0){
                    int percent=(int)Math.max(0,Math.min(100,Math.round((dt-di)*100.0/dt)));
                    cpuSamples.add(percent);while(cpuSamples.size()>30)cpuSamples.remove(0);
                    cpuHistory.setSamples(cpuSamples);
                    double avg=0;for(int n:cpuSamples)avg+=n;
                    avg=cpuSamples.isEmpty()?0:avg/cpuSamples.size();
                    averageCpu.setText(String.format(Locale.getDefault(),"平均 %.1f%%",avg));
                }
            }
            previousRootTotal=total;previousRootIdle=idle;
        });
    }

    /** Compact live CPU history chart, styled like the supplied monitoring widget. */
    private class CpuHistoryView extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final ArrayList<Integer> values=new ArrayList<>();
        CpuHistoryView(){super(MainActivity.this);}
        void setSamples(ArrayList<Integer> samples){values.clear();values.addAll(samples);invalidate();}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            float w=getWidth(),h=getHeight();
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2.5f));p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
            p.setColor(Color.rgb(38,157,229));
            if(values.size()==1){float y=h*.55f;c.drawLine(0,y,w,y,p);return;}
            if(values.size()>1){
                android.graphics.Path path=new android.graphics.Path();
                for(int i=0;i<values.size();i++){
                    float x=(w-dp(4))*i/(float)(Math.max(1,values.size()-1))+dp(2);
                    float y=h-dp(8)-(values.get(i)/100f)*(h-dp(16));
                    if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
                }
                c.drawPath(path,p);
            } else {
                p.setColor(Color.argb(110,38,157,229));
                c.drawLine(0,h*.65f,w,h*.65f,p);
            }
        }
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