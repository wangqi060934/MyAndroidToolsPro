package cn.wq.myandroidtoolspro.recyclerview.fragment.current;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
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
import android.text.ClipboardManager;
import android.text.TextUtils;
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
 */

public class FragmentWatchingService extends Service {
    private static final String TAG = "FragmentWatchingService";
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mParamsFragment;
    private TextView mFragmentTv;
    private SharedPreferences sharedPreferences;
    private int mStatusbarHeight;
    private BroadcastReceiver screenReceiver, timeReceiver, textsizeReceiver,moveReceiver;
    public final static String ACTION_FRAGMENT_TIME = "ACTION_FRAGMENT_TIME";
    public final static String ACTION_FRAGMENT_TEXTSIZE = "ACTION_FRAGMENT_TEXTSIZE";
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private Handler mUIHandler;
    private String EMPTY_MESSAGE;
    private Shell.Interactive mRootSession;
    public static boolean isRunning;
//    private final static String COMMAND_FRAGMENT = "dumpsys activity top | grep -E \"#[0-9]{1,}: .+{|mState=[0-9]\"";
    private final static String COMMAND_FRAGMENT_ACTIVITY = "dumpsys activity top | grep -E \" *ACTIVITY +([^ ]+)\"";
    private final static String COMMAND_FRAGMENT_ALL = "dumpsys activity top | grep -E \" *ACTIVITY +([^ ]+)|#[0-9]{1,}: .+{|mState=[0-9]\"";
    //    private final static String PATTERN_FOR_NUM = "( +)#(\\d+): +([\\w]+)\\{\\w+ #\\d+ id=[ :\\w]+\\}";
    private final static String PATTERN_FOR_NUM = "( +)#(\\d+): +([\\w]+)\\{\\w+ #\\d+ .+\\}";  //dialogfragment后面可能是dialog 或者 choose
    private final static String PATTERN_FOR_STATE = " +mState=(\\d)+ [\\S ]+";
    private static final String ESCAPE_FRAGMENT_TAG_SUFFIX = ".lifecycle.LifecycleDispatcher.report_fragment_tag}";
    private static final String ESCAPE_FRAGMENT_TAG_PREFIX_1 = " androidx";
    private static final String ESCAPE_FRAGMENT_TAG_PREFIX_2 = " android.arch";

    private int delayTime;
    // For Activity
    private TextView mActivityTv;
    private WindowManager.LayoutParams mParamsActivity;
    private final static String SEPERATE_MARK = "/\r\n";
    private boolean isShowFragment;
    private final static int MSG_FAILED_ROOT = 1;
    private final static int MSG_ACTIVITY = 2;
    private final static int MSG_FRAGMENT = 3;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;

        EMPTY_MESSAGE = getString(R.string.current_fragment_empty);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        delayTime = sharedPreferences.getInt("current_fragment_interval", 1500);
        mStatusbarHeight = getStatusBarHeight();

        isShowFragment = sharedPreferences.getBoolean("current_fragment_show", false);

        if (Utils.isTestForWindowManager()) {
            mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        } else {
            mWindowManager = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
        }

//        mParamsFragment = new WindowManager.LayoutParams(
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

        if (isShowFragment) {
            initFragmentUI();
        }

        mUIHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case MSG_FAILED_ROOT:
                        Toast.makeText(getApplicationContext(), R.string.failed_to_gain_root, Toast.LENGTH_SHORT).show();
                        stopSelf();
                        break;
                    case MSG_ACTIVITY:
                        if (mActivityTv == null) {
                            return;
                        }
                        if (msg.obj == null || "".equals(msg.obj.toString())) {
                            mActivityTv.setText("");
                        } else {
                            mActivityTv.setText(msg.obj.toString());
                        }
                        break;
                    case MSG_FRAGMENT:
                        if (mFragmentTv == null) {
                            return;
                        }
                        if (msg.obj == null || "".equals(msg.obj.toString())) {
                            mFragmentTv.setText(EMPTY_MESSAGE);
                        } else {
                            mFragmentTv.setText(msg.obj.toString());
                        }
                        break;
                }
            }
        };

        mHandlerThread = new HandlerThread("current_fragment");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if (Build.VERSION.SDK_INT < 21) {  //lollipop 开始通过命令获取，但是需要 android.permission.GET_TASKS
                    ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                    List< ActivityManager.RunningTaskInfo> tInfos = am.getRunningTasks(1);
                    String aLabel = "";
                    if (tInfos != null && tInfos.size() > 0) {
                        ComponentName cName = tInfos.get(0).topActivity;
                        aLabel = cName.getPackageName() + SEPERATE_MARK + cName.getClassName();
                    }

                    Message message = mUIHandler.obtainMessage(MSG_ACTIVITY);
                    message.obj = aLabel;
                    mUIHandler.sendMessage(message);

                    if (!isShowFragment) {
                        mHandler.sendEmptyMessageDelayed(0, delayTime);
                        return;
                    }
                }

                String command;
                if (isShowFragment) {
                    command = COMMAND_FRAGMENT_ALL;
                }else{
                    command = COMMAND_FRAGMENT_ACTIVITY;
                }
                //过滤 #0： 和 mState=5 这种
                mRootSession.addCommand(command, 0,
                        new Shell.OnCommandResultListener() {
                            @Override
                            public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                                if (mHandler == null) {
                                    return;
                                }

//                                if (2 > 1) {
//                                    output.clear();
//                                    output.add("  ACTIVITY com.google.android.apps.nexuslauncher/.NexusLauncherActivity eb340cb pid=2250");
//                                    output.add("  ACTIVITY com.android.documentsui/.files.FilesActivity 3a91e03 pid=3760");
//                                    output.add("          #42: LoaderInfo{7480e33 #42 : DirectoryLoader{c73c5f0}}");
//                                    output.add("        #0: RootsFragment{b0d3507 #0 id=0x7f090031}");
//                                    output.add("          mState=5 mIndex=0 mWho=android:fragment:0 mBackStackNesting=0");
//                                    output.add("              #2: LoaderInfo{cd10ab #2 : RootsLoader{1a66308}}");
//                                    output.add("        #1: DirectoryFragment{7c570a8 #1 id=0x7f090030}");
//                                    output.add("          mState=5 mIndex=1 mWho=android:fragment:1 mBackStackNesting=0");
//                                    output.add("        #0: RootsFragment{b0d3507 #0 id=0x7f090031}");
//                                    output.add("        #1: DirectoryFragment{7c570a8 #1 id=0x7f090030}");
//                                }

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
                                    int currentSpace = -1;  //"#0: "等开头的空格数量
                                    boolean foundNum = false;   //先匹配 PATTERN_FOR_NUM
                                    String currentString = "";
                                    int firstSpace = -1;

                                    final int size = output.size();
                                    int curActivityLine = 0;    //Android 8.0之后Activity的顺序反了
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        for (int i = size - 1; i >= 0; i--) {
                                            String line = output.get(i).trim();
                                            if (line.startsWith("ACTIVITY ")) {
                                                curActivityLine = i;
                                                break;
                                            }
                                        }
                                    }

                                    for (int i = curActivityLine; i < size; i++) {
//      #0: AboutFragment{61e1239 #0 id=0x7f0f0075}
//          mState=5 mIndex=0 mWho=android:fragment:0 mBackStackNesting=0
//            #0: ChooseDialogFragment{5750aa5 #0 choose}
//                mState=5 mIndex=0 mWho=android:fragment:0:0 mBackStackNesting=0
//            #0: ChooseDialogFragment{5750aa5 #0 choose}
//      #0: AboutFragment{61e1239 #0 id=0x7f0f0075}
//      #0: AboutFragment{61e1239 #0 id=0x7f0f0075}

                                        String line = output.get(i);
                                        if (curActivityLine == i && Build.VERSION.SDK_INT >= 21) {
//                                            Matcher matcher = Pattern.compile(" *ACTIVITY +([^ ]+) .+").matcher(line);
//                                            String aLabel = "";
//                                            if(matcher.matches()){
//                                                aLabel = matcher.group(1);
//                                                final int index = aLabel.indexOf("/");
//                                                if (index >= 1 && index < aLabel.length()-1) {
//                                                    final String pkg = aLabel.substring(0, index - 1);
//                                                    final String cls = aLabel.substring(index + 1);
//                                                    aLabel = pkg + SEPERATE_MARK + pkg + cls;
//                                                }
//                                            }else{
//                                                Log.e("matpro", "First line not match activity, isShowF:" + isShowFragment + ",line:" + line);
//                                            }

                                            String aLabel = parseLine(line);
                                            if ("".equals(aLabel)) {
                                                com.tencent.mars.xlog.Log.e(TAG, i + ",First line not match activity, isShowF:" + isShowFragment + ",line:" + line);
                                            }

                                            Message message = mUIHandler.obtainMessage(MSG_ACTIVITY);
                                            message.obj = aLabel;
                                            mUIHandler.sendMessage(message);
//                                            isFirstLine = false;
                                            if (!isShowFragment) {
                                                break;
                                            }
                                            continue;
                                        }

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
                                                int state = Integer.parseInt(matcher.group(1));
                                                // 查看android.app.Fragment源代码：(和android.support.v4.app.Fragment中不一致)
//                                                static final int INVALID_STATE = -1;   // Invalid state used as a null value.
//                                                static final int INITIALIZING = 0;     // Not yet created.
//                                                static final int CREATED = 1;          // Created.
//                                                static final int ACTIVITY_CREATED = 2; // The activity has finished its creation.
//                                                static final int STOPPED = 3;          // Fully created, not started.
//                                                static final int STARTED = 4;          // Created and started, not resumed.
//                                                static final int RESUMED = 5;          // Created started and resumed.
                                                if (state > 1) {//state=1应该不是当前的Fragment，是之前打开的

                                                    //还需要排除 androidx.lifecycle.ReportFragment (android.arch.lifecycle.Fragment)
                                                    //tag是 androidx.lifecycle.LifecycleDispatcher.report_fragment_tag
                                                    // 或者 android.arch.lifecycle.LifecycleDispatcher.report_fragment_tag}
                                                    if (!TextUtils.isEmpty(sBuilder)) {
                                                        sBuilder.append("\r\n");
                                                    }
                                                    if (!TextUtils.isEmpty(currentString)) {
                                                        for (int j = 0; j < currentSpace - firstSpace; j++) {
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
                                    mUIHandler.sendEmptyMessage(MSG_FRAGMENT);
                                } else {
                                    Message message = mUIHandler.obtainMessage(MSG_FRAGMENT);
                                    message.obj = sBuilder.toString();
                                    mUIHandler.sendMessage(message);
                                }

                            }


                            /**
                             * 解析  ACTIVITY eu.chainfire.supersu/.MainActivity-Material 3593b87 pid=1639
                             */
                            private String parseLine(String line){
                                String result = "";
                                final int index = line.indexOf("ACTIVITY");
                                if(index >= 0){
                                    final int length = line.length();
                                    int start = index + "ACTIVITY".length();
                                    boolean find = false;
                                    for(int i=start;i<length;i++){	//跳过前面的空格
                                        if(' '==line.charAt(i)){
                                            continue;
                                        }
                                        start = i;
                                        find = true;
                                        break;
                                    }
                                    if(find){
                                        final int end = line.indexOf(" ", start);
                                        if(end >= 0 ){
                                            result = line.substring(start,end);

                                            final int slash = result.indexOf("/");
                                            if (slash >= 1 && slash < result.length()-1) {
                                                final String pkg = result.substring(0, slash);
                                                final String cls =  result.substring(slash + 1);
                                                if (cls.startsWith(".")) {
                                                    result = pkg + SEPERATE_MARK + pkg + cls;
                                                }else{
                                                    result = pkg + SEPERATE_MARK + cls;
                                                }
                                            }

                                        }
                                    }
                                }
                                return result;
                            }

                        });

            }
        };

        initActivityUI();

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
                            if (mUIHandler != null) {
                                mUIHandler.sendEmptyMessage(MSG_FAILED_ROOT);
                            }
                        } else {
                            if (mHandler != null) {
                                mHandler.sendEmptyMessageDelayed(0, 100);
                            }
                        }
                    }
                });
    }

    private void initFragmentUI(){
        mParamsFragment = initParams();
        final int winHeight = getResources().getDisplayMetrics().heightPixels;
        mParamsFragment.x = sharedPreferences.getInt("current_fragment_x", 60);
        mParamsFragment.y = sharedPreferences.getInt("current_fragment_y", (winHeight - mStatusbarHeight) >> 1);

        mFragmentTv = new TextView(getApplicationContext());
        mFragmentTv.setPadding(20, 20, 20, 20);
//        mFragmentTv.setBackgroundColor(
//                ContextCompat.getColor(getApplicationContext(), R.color.floating_background_default)
//        );
        mFragmentTv.setBackgroundResource(R.drawable.floating_round_bg);
        mFragmentTv.setTextColor(
                ContextCompat.getColor(getApplicationContext(), android.R.color.white)
        );
        mFragmentTv.setTextSize(sharedPreferences.getInt("current_activity_textsize", 14));
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
                        mParamsFragment.x = (int) (event.getRawX() - lastX);
                        mParamsFragment.y = (int) (event.getRawY() - lastY - mStatusbarHeight);
                        mWindowManager.updateViewLayout(mFragmentTv, mParamsFragment);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:

                        break;
                }
                return false;
            }
        });

        mWindowManager.addView(mFragmentTv, mParamsFragment);
    }

    private WindowManager.LayoutParams initParams() {
        WindowManager.LayoutParams mParams = new WindowManager.LayoutParams();
        mParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mParams.format = PixelFormat.RGBA_8888;

        mParams.gravity = Gravity.START | Gravity.TOP;

        // TODO: 2017/11/26 可能有的机型适配有问题，像一加
        if (Build.VERSION.SDK_INT <= 21) { //lollipop 5.0
            mParams.type = WindowManager.LayoutParams.TYPE_TOAST;
        } else if (Build.VERSION.SDK_INT < 26) { //o 8.0
            mParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        } else {
            mParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY; //2038
        }

        final boolean moveEnable = sharedPreferences.getBoolean("current_move", true);
        updateParamsForMove(moveEnable,mParams);

        return mParams;
    }

    private void initActivityUI(){
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mParamsActivity = initParams();
        mParamsActivity.x = sharedPreferences.getInt("current_activity_x", 0);
        mParamsActivity.y = sharedPreferences.getInt("current_activity_y", 0);


        mActivityTv = new TextView(getApplicationContext());
//        mActivityTv.setBackgroundColor(
//                ContextCompat.getColor(getApplicationContext(), R.color.floating_background_default)
//        );
        mActivityTv.setBackgroundResource(R.drawable.floating_round_bg);
        mActivityTv.setTextSize(sharedPreferences.getInt("current_activity_textsize",14));
        mActivityTv.setPadding(16,12,16,12);
        mActivityTv.setTextColor(
                ContextCompat.getColor(getApplicationContext(), android.R.color.white)
        );

        mActivityTv.setOnTouchListener(new View.OnTouchListener() {
            float lastX, lastY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = event.getX();
                        lastY = event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        mParamsActivity.x = (int) (event.getRawX() - lastX);
                        mParamsActivity.y = (int) (event.getRawY() - lastY - mStatusbarHeight);
                        mWindowManager.updateViewLayout(mActivityTv, mParamsActivity);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:

                        break;
                }
                return false;
            }
        });
        mActivityTv.setOnClickListener(new View.OnClickListener() {
            private int count =2;
            private long lastTime;
            @Override
            public void onClick(View v) {
                final long current = System.currentTimeMillis();
                if (current - lastTime < 500 ) {
                    count--;
                    if (count <= 0) {
                        ClipboardManager clipboardManager = (ClipboardManager)
                                getSystemService(Activity.CLIPBOARD_SERVICE);
                        String text;
                        if (!TextUtils.isEmpty(mActivityTv.getText())) {
                            text = mActivityTv.getText().toString();
                            text = text.replace("\r\n", "");
                        }else{
                            text="";
                        }

                        clipboardManager.setText(text);
                        Toast.makeText(getApplicationContext(),R.string.copy_toast,Toast.LENGTH_SHORT).show();
                        count = 2;
                    }
                } else if (count != 2) {
                    count = 2;
                }
                lastTime = current;
            }
        });

        mWindowManager.addView(mActivityTv, mParamsActivity);

        registerReceivers();
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
                if (mFragmentTv != null) {
                    mFragmentTv.setTextSize(intent.getIntExtra("textsize", 14));
                }
                if (mActivityTv != null) {
                    mActivityTv.setTextSize(intent.getIntExtra("textsize", 14));
                }
            }
        };
        localBroadcastManager.registerReceiver(textsizeReceiver, new IntentFilter(Utils.ACTION_ACTIVITY_TEXTSIZE));

        moveReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean enable = intent.getBooleanExtra("enable", false);
                if (mParamsActivity != null) {
                    updateParamsForMove(enable,mParamsActivity);
                    mWindowManager.updateViewLayout(mActivityTv, mParamsActivity);
                }

                if (mParamsFragment != null) {
                    updateParamsForMove(enable, mParamsFragment);
                    mWindowManager.updateViewLayout(mFragmentTv, mParamsFragment);
                }
                if (!enable) {
                    savePosition();
                }
            }
        };
        localBroadcastManager.registerReceiver(moveReceiver, new IntentFilter(Utils.ACTION_ACTIVITY_MOVE));
    }

    private void updateParamsForMove(boolean moveable,WindowManager.LayoutParams params){
        if (moveable) {
            params.flags =
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
//                                    | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            ;
        } else {
            params.flags =
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE     //不影响背后view 的点击事件
            ;
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        savePosition();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;

        savePosition();

        if (mFragmentTv != null) {
            mWindowManager.removeView(mFragmentTv);
        }
        if (mActivityTv != null) {
            mWindowManager.removeView(mActivityTv);
        }

        mWindowManager = null;
        mFragmentTv = null;
        mActivityTv = null;
        mParamsFragment = null;
        mParamsActivity = null;
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
        if (moveReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(moveReceiver);
            moveReceiver = null;
        }
        mHandler.removeCallbacksAndMessages(null);
        mHandler = null;
        mHandlerThread.quit();
    }

    /**
     * 保存位置x，y
     */
    private void savePosition() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (mParamsActivity != null) {
            editor.putInt("current_activity_x", mParamsActivity.x)
                    .putInt("current_activity_y", mParamsActivity.y);
        }
        if (mParamsFragment != null) {
            editor.putInt("current_fragment_x", mParamsFragment.x)
                    .putInt("current_fragment_y", mParamsFragment.y);
        }
        editor.apply();
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
