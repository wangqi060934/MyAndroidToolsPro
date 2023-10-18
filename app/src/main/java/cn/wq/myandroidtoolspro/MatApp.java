package cn.wq.myandroidtoolspro;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDexApplication;

import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.mars.xlog.Log;

import cn.wq.myandroidtoolspro.helper.Utils;
import moe.shizuku.api.ShizukuClient;

public class MatApp extends MultiDexApplication implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "MatApp";
    //系统默认的UncaughtException处理类
    private Thread.UncaughtExceptionHandler mDefaultHandler;

    @Override
    public void onCreate() {
        super.onCreate();

        initXlog();

        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(BaseActivity.PREFERENCE_CLOSE_BUGLY, false)) {
            CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(getApplicationContext());
            strategy.setAppChannel(BuildConfig.apk_channel);  //设置渠道
            CrashReport.initCrashReport(getApplicationContext(), "8278a7dec4", BuildConfig.DEBUG, strategy);
        }

        ShizukuClient.initialize(this);

    }

    //https://github.com/Tencent/mars/wiki/Mars-Android-%E6%8E%A5%E5%85%A5%E6%8C%87%E5%8D%97#xlog
    //https://github.com/Tencent/mars/wiki/Mars-Android-%E6%8E%A5%E5%8F%A3%E8%AF%A6%E7%BB%86%E8%AF%B4%E6%98%8E
    private void initXlog() {
        System.loadLibrary("stlport_shared");
        System.loadLibrary("marsxlog");
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (!handleException(e) && mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(t, e);
        }else{
            //退出程序
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
     *
     * @param ex
     * @return true:如果处理了该异常信息;否则返回false.
     */
    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }

        //收集设备参数信息
        collectDeviceInfo(this);
        //保存日志文件
        Log.printErrStackTrace(TAG,ex,"[crash info]");
        Log.appenderClose();
        return true;
    }

    private void collectDeviceInfo(Context ctx) {
        StringBuilder sb = new StringBuilder();
        try {
            PackageInfo pi = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = pi.versionCode + "";
                sb.append("appVerCode:").append(versionCode)
                        .append(",appVerName:").append(versionName).append(",");
            }
        } catch (PackageManager.NameNotFoundException e) {
            Utils.err(TAG, "an err occured when collect package info", e);
        }
        sb.append("model:").append(Build.MODEL).append(",sysVerCode:")
                .append(Build.VERSION.SDK_INT).append(",sysVerName:")
                .append(Build.VERSION.RELEASE);
        Utils.err(TAG, "[crash device info] " + sb.toString(), null);
    }

}
