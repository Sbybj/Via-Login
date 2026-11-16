package com.sbybj.monopanel;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/** Reads a small, fixed set of system diagnostics through the user's Root manager. */
public final class RootInfoReader {
    public interface Callback { void onResult(boolean granted, String details); }
    private static volatile String lastStatus="尚未检测";
    private static volatile String lastDetails="暂无 Root 读取结果";
    private RootInfoReader() {}

    public static String getLastStatus(){ return lastStatus; }
    public static String getLastDetails(){ return lastDetails; }
    public static void setLastResult(String status,String details){ lastStatus=status; lastDetails=details; }


    /** Reads aggregate CPU counters through su. Values are total ticks and idle ticks. */
    public static void readCpuStat(Activity activity, CpuCallback callback){
        new Thread(() -> {
            boolean granted=false;
            long[] counters=null;
            Process process=null;
            try {
                process=new ProcessBuilder("su","-c","cat /proc/stat").redirectErrorStream(true).start();
                if(process.waitFor(5,TimeUnit.SECONDS)){
                    BufferedReader reader=new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line=reader.readLine();
                    if(line!=null && line.startsWith("cpu ")){
                        String[] fields=line.trim().split("\\s+");
                        long total=0;
                        for(int i=1;i<fields.length;i++) total+=Long.parseLong(fields[i]);
                        long idle=Long.parseLong(fields[4])+(fields.length>5?Long.parseLong(fields[5]):0);
                        counters=new long[]{total,idle};
                        granted=true;
                    }
                } else process.destroyForcibly();
            } catch(Exception ignored) {
            } finally { if(process!=null)process.destroy(); }
            final boolean ok=granted;
            final long[] result=counters;
            new Handler(Looper.getMainLooper()).post(() -> callback.onResult(ok,result));
        }).start();
    }
    public interface CpuCallback { void onResult(boolean granted,long[] counters); }
\n    public static void read(Activity activity, Callback callback){
        new Thread(() -> {
            boolean granted=false;
            String details="未取得系统信息";
            Process process=null;
            try {
                // Fixed read-only commands only; no user-provided shell input is accepted.
                String command="id; echo __MEMINFO__; grep -E 'MemTotal|MemAvailable' /proc/meminfo 2>/dev/null; echo __THERMAL__; for f in /sys/class/thermal/thermal_zone*/temp; do if [ -r \"$f\" ]; then v=$(cat \"$f\" 2>/dev/null); case \"$v\" in ''|*[!0-9-]*) ;; *) echo TEMP:$v;; esac; fi; done";
                process=new ProcessBuilder("su","-c",command).redirectErrorStream(true).start();
                boolean finished=process.waitFor(8,TimeUnit.SECONDS);
                if(!finished){ process.destroyForcibly(); details="Root 请求超时"; }
                else {
                    StringBuilder output=new StringBuilder();
                    BufferedReader reader=new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while((line=reader.readLine())!=null){ output.append(line).append('\n'); }
                    String raw=output.toString();
                    granted=raw.contains("uid=0") || raw.contains("uid=0(");
                    if(granted){
                        String temp="温度节点不可读";
                        for(String item:raw.split("\\n")){
                            if(item.startsWith("TEMP:")){
                                try {
                                    double value=Double.parseDouble(item.substring(5).trim());
                                    if(value>1000)value/=1000.0;
                                    if(value>0 && value<200){ temp=String.format(java.util.Locale.getDefault(),"温度传感器 %.1f°C",value); break; }
                                } catch(Exception ignored) {}
                            }
                        }
                        details=temp+"；已尝试读取 /proc/meminfo";
                    } else details="未获得 uid=0；请在 Root 管理器中允许本应用";
                }
            } catch(Exception e){ details="未检测到可用 su，或授权被拒绝"; }
            finally { if(process!=null)process.destroy(); }
            final boolean ok=granted;
            final String result=details;
            new Handler(Looper.getMainLooper()).post(()->callback.onResult(ok,result));
        }).start();
    }
}
