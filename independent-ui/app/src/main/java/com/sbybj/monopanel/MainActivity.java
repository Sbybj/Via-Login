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
import android.widget.ImageView;
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
        if(runtimeValue!=null){ updateRuntime(); }
        handler.postDelayed(this,100);
    }};

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        startedAt=SystemClock.elapsedRealtime();
        getWindow().setStatusBarColor(BLACK); getWindow().setNavigationBarColor(BLACK);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        log("应用启动"); showHome(); handler.post(monitor); requestRootAccess();
    }
    @Override protected void onDestroy(){ handler.removeCallbacks(monitor); RootInfoReader.stopCpuMonitor(); super.onDestroy(); }

    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int color,int radius){
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private GradientDrawable cardBg(){
        // Keep the supplied wallpaper crisp; use a lighter translucent glass tint without blur.
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.argb(52,116,124,142),Color.argb(34,35,40,54),Color.argb(42,18,21,31)});
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

    private TextView homeTime, homeDate, homeCpuPercent, homeRootStatus;
    private CpuRingView homeRing;
    private final Runnable homeClock = new Runnable(){ public void run(){
        if(homeTime!=null){
            java.text.SimpleDateFormat tf=new java.text.SimpleDateFormat("HH:mm",Locale.getDefault());
            java.text.SimpleDateFormat df=new java.text.SimpleDateFormat("M月d日  EEEE",Locale.CHINA);
            homeTime.setText(tf.format(new java.util.Date()));
            homeDate.setText(df.format(new java.util.Date()));
        }
        handler.postDelayed(this,1000);
    }};
    private View glassCard(){
        android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{Color.argb(112,150,150,160),Color.argb(78,50,50,58),Color.argb(88,18,18,24)});
        d.setCornerRadius(dp(26));
        d.setStroke(dp(1),Color.argb(42,255,255,255));
        View v=new View(this);v.setBackground(d);return v;
    }
    private LinearLayout cardContainer(){
        LinearLayout c=vertical();c.setPadding(dp(16),dp(12),dp(16),dp(12));
        c.setBackground(glassCard().getBackground());return c;
    }
    private void showHome(){
        FrameLayout frame=new FrameLayout(this);
        ImageView wallpaper=new ImageView(this);
        wallpaper.setImageResource(R.drawable.wallpaper);
        wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
        frame.addView(wallpaper,new FrameLayout.LayoutParams(-1,-1));
        View shade=new View(this);shade.setBackgroundColor(Color.argb(38,0,0,0));
        frame.addView(shade,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout page=vertical();
        page.setPadding(dp(18),dp(12),dp(18),dp(16));
        FrameLayout.LayoutParams pageParams=new FrameLayout.LayoutParams(-1,-1);
        frame.addView(page,pageParams);

        LinearLayout top=horizontal();
        LinearLayout dateCard=cardContainer();
        homeTime=text("--:--",48,WHITE,false);
        homeTime.setGravity(Gravity.CENTER_VERTICAL);
        homeDate=text("--月--日",17,WHITE,false);
        dateCard.addView(homeTime,lp(-1,68));dateCard.addView(homeDate,lp(-1,30));
        LinearLayout cpuCard=cardContainer();
        LinearLayout cpuRow=horizontal();
        homeRing=new CpuRingView();
        cpuRow.addView(homeRing,new LinearLayout.LayoutParams(dp(104),dp(104)));
        LinearLayout cpuLabels=vertical();cpuLabels.setPadding(dp(10),0,0,0);
        cpuLabels.addView(text("CPU",17,WHITE,false),lp(-1,30));
        homeCpuPercent=text("--%",22,WHITE,false);
        cpuLabels.addView(homeCpuPercent,lp(-1,34));
        cpuLabels.addView(text("实时占用率",10,Color.LTGRAY,false),lp(-1,24));
        cpuRow.addView(cpuLabels,new LinearLayout.LayoutParams(0,-1,1));
        cpuCard.addView(cpuRow,new LinearLayout.LayoutParams(-1,-1));
        top.addView(dateCard,new LinearLayout.LayoutParams(0,dp(124),1));
        View gap=new View(this);top.addView(gap,new LinearLayout.LayoutParams(dp(12),1));
        top.addView(cpuCard,new LinearLayout.LayoutParams(0,dp(124),1));
        page.addView(top,lp(-1,124));

        LinearLayout rootCard=cardContainer();rootCard.setOrientation(0);
        TextView shield=text("✓",30,WHITE,true);shield.setGravity(Gravity.CENTER);
        rootCard.addView(shield,lp(54,-1));
        LinearLayout rootLabels=vertical();
        rootLabels.addView(text("Root 权限状态",17,WHITE,true),lp(-1,28));
        homeRootStatus=text("正在检测授权…",13,Color.LTGRAY,false);
        rootLabels.addView(homeRootStatus,lp(-1,24));
        rootCard.addView(rootLabels,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout.LayoutParams rootParams=lp(-1,64);rootParams.topMargin=dp(18);page.addView(rootCard,rootParams);

        View spacer=new View(this);page.addView(spacer,new LinearLayout.LayoutParams(1,0,1));

        LinearLayout row1=horizontal(),row2=horizontal();
        addHomeButton(row1,"⊙","透视开关",()->{if(moduleListener!=null)moduleListener.onAction("toggle_overlay");showModules();});
        addHomeButton(row1,"◉","透视设置",()->{if(moduleListener!=null)moduleListener.onAction("overlay_settings");showSettings();});
        addHomeButton(row1,"⚙","功能设置",()->showSettings());
        addHomeButton(row2,"▣","实时监控",()->showCpuMonitor());
        addHomeButton(row2,"▦","更多功能",()->showModules());
        addHomeButton(row2,"⏻","退出应用",()->finish());
        page.addView(row1,lp(-1,132)); 
        page.addView(row2,lp(-1,132));
        LinearLayout.LayoutParams rowGap=lp(-1,dp(12));page.addView(new View(this),rowGap);

        LinearLayout footer=horizontal();footer.setGravity(Gravity.CENTER);
        footer.setPadding(dp(18),0,dp(18),0);footer.setBackground(glassCard().getBackground());
        TextView version=text("ⓘ  MonoPanel v1.0",14,WHITE,false);
        footer.addView(version,new LinearLayout.LayoutParams(0,dp(54),1));
        TextView divider=text("|",18,Color.LTGRAY,false);footer.addView(divider,lp(24,54));
        TextView status=text("●  稳定运行中…",14,WHITE,false);
        footer.addView(status,new LinearLayout.LayoutParams(0,dp(54),1));
        LinearLayout.LayoutParams footerP=lp(-1,54);footerP.topMargin=dp(18);page.addView(footer,footerP);

        setContentView(frame);
        handler.removeCallbacks(homeClock); homeClock.run();
        if(homeCpuPercent!=null&&!cpuSamples.isEmpty()){
            int v=cpuSamples.get(cpuSamples.size()-1);
            homeCpuPercent.setText(v+"%");homeRing.setPercent(v);
        }
        String rs=RootInfoReader.getLastStatus();
        if(homeRootStatus!=null)homeRootStatus.setText(rs==null||rs.isEmpty()?"等待 Root 授权":rs);
    }
    private void addHomeButton(LinearLayout row,String icon,String label,Runnable action){
        LinearLayout button=vertical();button.setGravity(Gravity.CENTER);
        button.setBackground(glassCard().getBackground());
        TextView glyph=text(icon,34,WHITE,true);glyph.setGravity(Gravity.CENTER);
        TextView caption=text(label,15,WHITE,false);caption.setGravity(Gravity.CENTER);
        button.addView(glyph,new LinearLayout.LayoutParams(-1,dp(58)));
        button.addView(caption,new LinearLayout.LayoutParams(-1,dp(34)));
        button.setOnClickListener(v->action.run());
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(132),1);
        p.setMargins(dp(0),0,dp(0),0);
        row.addView(button,p);
        if(row.getChildCount()<3){View gap=new View(this);row.addView(gap,new LinearLayout.LayoutParams(dp(14),1));}
    }
    private class CpuRingView extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private int percent=0;
        CpuRingView(){super(MainActivity.this);}
        void setPercent(int p){percent=Math.max(0,Math.min(100,p));invalidate();}
        @Override protected void onDraw(Canvas c){
            float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(cx,cy)-dp(9);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(10));paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(Color.argb(95,220,220,220));c.drawArc(cx-r,cy-r,cx+r,cy+r,-90,360,false,paint);
            paint.setColor(WHITE);c.drawArc(cx-r,cy-r,cx+r,cy+r,-90,percent*3.6f,false,paint);
            paint.setStyle(Paint.Style.FILL);paint.setColor(WHITE);paint.setTextSize(dp(18));paint.setTextAlign(Paint.Align.CENTER);
            c.drawText(percent+"%",cx,cy+dp(6),paint);
        }
    }

    private void addHotspot(FrameLayout frame,int width,int height,float x,float y,float w,float h,Runnable action){
        View hit=new View(this);
        hit.setBackgroundColor(Color.TRANSPARENT);
        hit.setClickable(true);
        hit.setOnClickListener(v->action.run());
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(Math.round(width*w),Math.round(height*h));
        p.leftMargin=Math.round(width*x);p.topMargin=Math.round(height*y);
        frame.addView(hit,p);
    }

    private void showCpuMonitor(){
        initPage("实时 CPU 监控");
        LinearLayout c=vertical();c.setPadding(dp(16),dp(12),dp(16),dp(12));
        c.addView(text("CPU 使用率",18,WHITE,true),lp(-1,38));
        averageCpu=text("CPU --.-%",28,WHITE,true);c.addView(averageCpu,lp(-1,54));
        cpuHistory=new CpuHistoryView();c.addView(cpuHistory,lp(-1,120));
        c.addView(text("采样间隔：0.5 秒",13,MUTED,false),lp(-1,30));
        c.addView(infoLine("Root 状态",RootInfoReader.getLastStatus()));
        c.addView(infoLine("系统信息",RootInfoReader.getLastDetails()));
        addCard(root,c,14);
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
    private void onCpuSample(boolean granted,long[] counters){
        if(!granted||counters==null||counters.length<2){
            if(cpuHistory!=null){ cpuHistory.invalidate(); averageCpu.setText("CPU --.-%"); }
            previousRootTotal=-1; previousRootIdle=-1; return;
        }
        long total=counters[0], idle=counters[1];
        if(previousRootTotal>=0){
            long dt=total-previousRootTotal, di=idle-previousRootIdle;
            if(dt>0){
                int percent=(int)Math.max(0,Math.min(100,Math.round((dt-di)*100.0/dt)));
                cpuSamples.add(percent);while(cpuSamples.size()>60)cpuSamples.remove(0);
                if(cpuHistory!=null)cpuHistory.setSamples(cpuSamples);
                double avg=0;for(int n:cpuSamples)avg+=n;
                avg=cpuSamples.isEmpty()?0:avg/cpuSamples.size();
                if(averageCpu!=null)averageCpu.setText(String.format(Locale.getDefault(),"CPU %.1f%%",(double)percent));
                if(homeCpuPercent!=null)homeCpuPercent.setText(percent+"%");
                if(homeRing!=null)homeRing.setPercent(percent);
            }
        }
        previousRootTotal=total;previousRootIdle=idle;
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
        c.addView(text("监控刷新间隔",16,WHITE,true));addAction(c,"0.5 秒","CPU 采样间隔（每秒 2 次）",()->{log("CPU 采样间隔为 500ms");showSettings();});
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
            if(homeRootStatus!=null) homeRootStatus.setText(granted?"已获取 Root 权限 · 功能已解锁":"Root 未授权 / 不可用");
            log(granted?"Root 授权成功":"Root 未授权或读取失败");
            if(granted){
                // Start the persistent CPU reader only after the permission check has completed.
                RootInfoReader.startCpuMonitor(this,(ok,counters)->onCpuSample(ok,counters));
            }else{
                RootInfoReader.stopCpuMonitor();
                previousRootTotal=-1; previousRootIdle=-1;
            }
        });
    }

    private void showLogs(){
        initPage("运行日志");LinearLayout c=vertical();c.setPadding(dp(16),dp(12),dp(16),dp(12));
        if(logs.isEmpty())c.addView(text("暂无日志",14,MUTED,false));
        for(String line:logs)c.addView(text(line,12,WHITE,false),lp(-1,28));
        addCard(root,c,14);
    }
}