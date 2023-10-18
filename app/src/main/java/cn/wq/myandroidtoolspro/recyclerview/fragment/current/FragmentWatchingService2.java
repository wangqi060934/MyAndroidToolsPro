package cn.wq.myandroidtoolspro.recyclerview.fragment.current;

import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.Utils;
import eu.chainfire.libsuperuser.Shell;

/**
 * Created by wangqi on 2017/7/13.
 * 备份
 */
@Deprecated
public class FragmentWatchingService2 extends Service {
    private static final String TAG = "FragmentWatchingService2";
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mParams;
    private TextView mFragmentTv;
    private SharedPreferences sharedPreferences;
    private int mStatusbarHeight;
    private BroadcastReceiver screenReceiver, timeReceiver, textsizeReceiver;
    public final static String ACTION_FRAGMENT_TIME = "ACTION_FRAGMENT_TIME";
    public final static String ACTION_FRAGMENT_TEXTSIZE = "ACTION_FRAGMENT_TEXTSIZE";
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private Handler mUIHandler;
    private String EMPTY_MESSAGE;
    private Shell.Interactive mRootSession;
    public static boolean isRunning;
    private final static String COMMAND_FRAGMENT = "dumpsys activity top | grep -E \"#[0-9]{1,}: .+{|mState=[0-9]\"";
    //    private final static String PATTERN_FOR_NUM = "( +)#(\\d+): +([\\w]+)\\{\\w+ #\\d+ id=[ :\\w]+\\}";
    private final static String PATTERN_FOR_NUM = "( +)#(\\d+): +([\\w]+)\\{\\w+ #\\d+ .+\\}";  //dialogfragment后面可能是dialog 或者 choose
    private final static String PATTERN_FOR_STATE = " +mState=(\\d)+ [\\S ]+";
    private int delayTime;
    private Toast mFinishToast;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        delayTime = sharedPreferences.getInt("current_fragment_interval", 1500);
        mStatusbarHeight = getStatusBarHeight();
        EMPTY_MESSAGE = getString(R.string.current_fragment_empty);

        int winHeight = getResources().getDisplayMetrics().heightPixels;

        if (Utils.isTestForWindowManager()) {
            mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        } else {
            mWindowManager = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
        }

//        mParams = new WindowManager.LayoutParams(
//                WindowManager.LayoutParams.WRAP_CONTENT,
//                WindowManager.LayoutParams.WRAP_CONTENT,
//                sharedPreferences.getInt("current_fragment_x", 60),
//                sharedPreferences.getInt("current_fragment_y", (winHeight - mStatusbarHeight) >> 1),
//                WindowManager.LayoutParams.TYPE_TOAST,
//                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
////                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
//                ,
//                PixelFormat.RGBA_8888
//        );

        mParams = new WindowManager.LayoutParams();
        mParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mParams.x = sharedPreferences.getInt("current_fragment_x", 60);
        mParams.y = sharedPreferences.getInt("current_fragment_y", (winHeight - mStatusbarHeight) >> 1);
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mParams.format = PixelFormat.RGBA_8888;

        mParams.gravity = Gravity.START | Gravity.TOP;

        // TODO: 2017/11/26 可能有的机型适配有问题，像一加
        if (Build.VERSION.SDK_INT <= 21) { //lollipop
            mParams.type = WindowManager.LayoutParams.TYPE_TOAST;
        } else if (Build.VERSION.SDK_INT <= 26) { //o
            mParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        } else {
            mParams.type = 2038; //TYPE_APPLICATION_OVERLAY
        }

        mFragmentTv = new TextView(getApplicationContext());
        mFragmentTv.setPadding(20, 20, 20, 20);
//        mFragmentTv.setBackgroundColor(
//                ContextCompat.getColor(getApplicationContext(), R.color.floating_background_default)
//        );
        mFragmentTv.setBackgroundResource(R.drawable.floating_round_bg);
        mFragmentTv.setTextColor(
                ContextCompat.getColor(getApplicationContext(), android.R.color.white)
        );
        mFragmentTv.setTextSize(sharedPreferences.getInt("current_fragment_textsize", 14));
        mFragmentTv.setText(EMPTY_MESSAGE);

        mFragmentTv.setOnTouchListener(new View.OnTouchListener() {
            float lastX, lastY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = event.getX();
                        lastY = event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        mParams.x = (int) (event.getRawX() - lastX);
                        mParams.y = (int) (event.getRawY() - lastY - mStatusbarHeight);
                        mWindowManager.updateViewLayout(mFragmentTv, mParams);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:

                        break;
                }
                return false;
            }
        });
        mFragmentTv.setOnClickListener(new View.OnClickListener() {
            private int count = 7;
            private long lastTime;

            @Override
            public void onClick(View v) {
                long current = System.currentTimeMillis();
                if (current - lastTime < 500) {
                    count--;
                    if (count <= 0) {
                        stopSelf();
                    } else {
                        if (mFinishToast == null) {
                            mFinishToast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
                        }
                        mFinishToast.setText("click " + count + " times to close!");
                        mFinishToast.show();
                    }
                } else if (count != 7) {
                    count = 7;
                }

                lastTime = current;
            }
        });

        Utils.debug("fragment addview before");
        mWindowManager.addView(mFragmentTv, mParams);
        Utils.debug("fragment addview after");

        registerReceivers();

        mUIHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == 1) {
                    Toast.makeText(getApplicationContext(), R.string.failed_to_gain_root, Toast.LENGTH_SHORT).show();
                    stopSelf();
                    return;
                }
                if (msg.obj == null || "".equals(msg.obj.toString())) {
                    mFragmentTv.setText(EMPTY_MESSAGE);
                } else {
                    mFragmentTv.setText(msg.obj.toString());
                }
            }
        };

        mHandlerThread = new HandlerThread("current_fragment");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                //过滤 #0： 和 mState=5 这种
                mRootSession.addCommand(COMMAND_FRAGMENT, 0,
                        new Shell.OnCommandResultListener() {
                            @Override
                            public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                                if (mHandler == null) {
                                    return;
                                }

                                if (Utils.isDebugForUser()) {
                                    File file = new File(getExternalCacheDir() + File.separator + "current_fragment_result");

                                    if (!file.exists()) {
                                        try {
                                            file.createNewFile();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    FileOutputStream out = null;
                                    try {
                                        out = new FileOutputStream(file, true);
                                        for (int i = 0; i < output.size(); i++) {
                                            out.write((output.get(i) + "\r\n").getBytes());
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } finally {
                                        if (out != null) {
                                            try {
                                                out.close();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }

                                mHandler.sendEmptyMessageDelayed(0, delayTime);
                                StringBuilder sBuilder = new StringBuilder();
                                if (exitCode < 0) {
                                    com.tencent.mars.xlog.Log.e(TAG, "Current Fragment err exitCode:" + exitCode);
                                } else {
//                                    int firstSpace = -1;    //第一个#0:前面的空格个数，找空格数相同的
//                                    String maxString = "";  //找到标号后 看下一行 mState 是否大于1
//                                    boolean foundNum = false;   //先匹配 PATTERN_FOR_NUM
//                                    for (String line : output) {
//                                        if (!foundNum) {
//                                            //      #1: ServiceRecyclerListFragment{3552298 #1 id=0x7f0f0074 android:switcher:2131427781:0}
//                                            Matcher matcher = Pattern.compile(PATTERN_FOR_NUM).matcher(line);
//                                            if (matcher.matches()) {
//                                                int currentSpace = matcher.group(1).length();
//                                                if (firstSpace < 0) {
//                                                    firstSpace = currentSpace;
//                                                } else if (firstSpace != currentSpace) {
//                                                    continue;
//                                                }
//
//                                                foundNum = true;
//                                                maxString = matcher.group(3);
//                                            }
//                                        } else {
//                                            //        mState=1 mIndex=0 mWho=android:fragment:0 mBackStackNesting=1
//                                            Matcher matcher = Pattern.compile(PATTERN_FOR_STATE).matcher(line);
//                                            if (matcher.matches()) {
//                                                Integer state = Integer.parseInt(matcher.group(1));
//                                                if (state > 1) {
//                                                    if (!TextUtils.isEmpty(maxString)) {
//                                                        Message message = mUIHandler.obtainMessage(0);
//                                                        message.obj = maxString;
//                                                        mUIHandler.sendMessage(message);
//                                                        return;
//                                                    }
//                                                }
//                                                foundNum = false;
//                                            }
//                                        }
//
//                                    }

                                    //找嵌套fragment
                                    int currentSpace = -1;
                                    boolean foundNum = false;   //先匹配 PATTERN_FOR_NUM
                                    String currentString = "";
                                    int firstSpace = -1;

                                    for (String line : output) {
//      #0:AboutFragment {61e1239 #0 id=0x7f0f0075}
//          mState=5 mIndex=0 mWho=android:fragment:0 mBackStackNesting=0
//            #0: ChooseDialogFragment{5750aa5 #0 choose}
//                mState=5 mIndex=0 mWho=android:fragment:0:0 mBackStackNesting=0
//            #0: ChooseDialogFragment{5750aa5 #0 choose}
//      #0: AboutFragment{61e1239 #0 id=0x7f0f0075}
//      #0: AboutFragment{61e1239 #0 id=0x7f0f0075}


                                        if (line.trim().startsWith("#")) {
                                            Matcher matcher = Pattern.compile(PATTERN_FOR_NUM).matcher(line);
                                            if (matcher.matches()) {
                                                currentSpace = matcher.group(1).length();
                                                if (firstSpace < 0) {
                                                    firstSpace = currentSpace;
                                                }

                                                foundNum = true;
                                                currentString = matcher.group(3);
                                            }
                                        } else {
                                            if (!foundNum) {
                                                continue;
                                            }

                                            //        mState=1 mIndex=0 mWho=android:fragment:0 mBackStackNesting=1
                                            Matcher matcher = Pattern.compile(PATTERN_FOR_STATE).matcher(line);
                                            if (matcher.matches()) {
                                                Integer state = Integer.parseInt(matcher.group(1));
                                                if (state > 1) {
                                                    if (!TextUtils.isEmpty(sBuilder)) {
                                                        sBuilder.append("\r\n");
                                                    }
                                                    if (!TextUtils.isEmpty(currentString)) {
                                                        for (int i = 0; i < currentSpace - firstSpace; i++) {
                                                            sBuilder.append(" ");
                                                        }


                                                        sBuilder.append(currentString);
                                                    }
                                                }
                                                foundNum = false;
                                            }
                                        }

                                    }
                                }

                                if (TextUtils.isEmpty(sBuilder)) {
                                    mUIHandler.sendEmptyMessage(0);
                                } else {
                                    Message message = mUIHandler.obtainMessage(0);
                                    message.obj = sBuilder.toString();
                                    mUIHandler.sendMessage(message);
                                }

                            }
                        });

            }
        }

        ;

        mRootSession = new Shell.Builder()
                .useSU()
                .setWantSTDERR(true)
                .setWatchdogTimeout(5)
                .setMinimalLogging(true)
                .open(new Shell.OnCommandResultListener() {
                    @Override
                    public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                        if (exitCode != Shell.OnCommandResultListener.SHELL_RUNNING) {
                            Toast.makeText(getApplicationContext(), R.string.failed_to_gain_root, Toast.LENGTH_SHORT).show();
                            mUIHandler.sendEmptyMessage(1);
                        } else {
                            mHandler.sendEmptyMessageDelayed(0, delayTime);
                        }
                    }
                });
    }

    private void registerReceivers() {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);

        screenReceiver = new BroadcastReceiver() {
            private boolean hasKeyguard ;

            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_SCREEN_OFF.equals(action)) {  //屏幕灭了
                    mHandler.removeCallbacksAndMessages(null);
                } else if (Intent.ACTION_USER_PRESENT.equals(action)) {     //解锁了
                    mHandler.sendEmptyMessageDelayed(0, delayTime);
                    if (hasKeyguard) {
                        mHandler.sendEmptyMessageDelayed(0, delayTime);
                        hasKeyguard = false;
                    }
                } else if (Intent.ACTION_SCREEN_ON.equals(action)) {   //屏幕亮了
                    KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
                    hasKeyguard = keyguardManager.inKeyguardRestrictedInputMode();  //是否需要解锁，不需要解锁时没有ACTION_USER_PRESENT
                    if (!hasKeyguard) {
                        mHandler.sendEmptyMessageDelayed(0, delayTime);
                    }
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(screenReceiver, intentFilter);

        timeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                delayTime = intent.getIntExtra("num", 0);
            }
        };
        localBroadcastManager.registerReceiver(timeReceiver, new IntentFilter(ACTION_FRAGMENT_TIME));

        textsizeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mFragmentTv.setTextSize(intent.getIntExtra("textsize", 14));
            }
        };
        localBroadcastManager.registerReceiver(textsizeReceiver, new IntentFilter(ACTION_FRAGMENT_TEXTSIZE));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        sharedPreferences.edit()
                .putInt("current_fragment_x", mParams.x)
                .putInt("current_fragment_y", mParams.y)
                .apply();

        mWindowManager.removeView(mFragmentTv);
        mWindowManager = null;
        mFragmentTv = null;
        mParams = null;
        sharedPreferences = null;
        if (screenReceiver != null) {
            unregisterReceiver(screenReceiver);
            screenReceiver = null;
        }
        if (timeReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(timeReceiver);
            timeReceiver = null;
        }
        if (textsizeReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(textsizeReceiver);
            textsizeReceiver = null;
        }
        mHandler.removeCallbacksAndMessages(null);
        mHandler = null;
        mHandlerThread.quit();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }

    private int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelOffset(resourceId);
        } else {
            return 0;
        }
    }
}
