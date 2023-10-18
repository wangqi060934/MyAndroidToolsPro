package cn.wq.myandroidtoolspro.recyclerview.fragment.current;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;
import android.widget.Toast;

import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.Utils;

/**
 * Created by wangqi on 2017/7/9.
 */
@Deprecated
public class ActivityWatchingService extends AccessibilityService {
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mParams;
    private TextView mActivityTv;
    private BroadcastReceiver activityStopReceiver, activityNameReceiver, activityMoveReceiver,activityTextsizeReceiver;
    public final static String ACTION_ACTIVITY_STOP = "ACTION_STOP_ACTIVITY";
    public final static String ACTION_ACTIVITY_FULL_NAME = "ACTION_ACTIVITY_FULL_NAME";
    public final static String ACTION_ACTIVITY_MOVE = "ACTION_ACTIVITY_MOVE";
    public final static String ACTION_ACTIVITY_TEXTSIZE = "ACTION_ACTIVITY_TEXTSIZE";
    private SharedPreferences sharedPreferences;
    private boolean isActivityFullName;
    private final static String SEPERATE_MARK = "/\r\n";
    private int mStatusbarHeight;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sharedPreferences.edit()
                .putInt("current_activity_x", mParams.x)
                .putInt("current_activity_y", mParams.y)
                .apply();

        mWindowManager.removeView(mActivityTv);
        mWindowManager = null;
        mActivityTv = null;
        mParams = null;
        sharedPreferences = null;
        if (activityStopReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(activityStopReceiver);
            activityStopReceiver = null;
        }
        if (activityNameReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(activityNameReceiver);
            activityNameReceiver = null;
        }
        if (activityMoveReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(activityMoveReceiver);
            activityMoveReceiver = null;
        }
        if (activityTextsizeReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(activityTextsizeReceiver);
            activityTextsizeReceiver = null;
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.getPackageName() != null && event.getClassName() != null) {
                ComponentName cName = new ComponentName(event.getPackageName().toString(), event.getClassName().toString());
                ActivityInfo activityInfo = tryGetActivity(cName);
                if (activityInfo != null) {
                    mActivityTv.setText(cName.getPackageName() + SEPERATE_MARK +
                            (isActivityFullName ? cName.getClassName() : cName.getShortClassName())
                    );
                }
            }
        }
    }

    private ActivityInfo tryGetActivity(ComponentName componentName) {
        try {
            return getPackageManager().getActivityInfo(componentName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo config = new AccessibilityServiceInfo();
        config.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        config.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;

        if (Build.VERSION.SDK_INT >= 16) {
            config.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        }
        setServiceInfo(config);

        init();
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    private void init() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        isActivityFullName = sharedPreferences.getBoolean("current_activity_name_full", false);

        mStatusbarHeight = getStatusBarHeight();
//        mWindowManager = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
        if (Utils.isTestForWindowManager()) {
            mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        }else{
            mWindowManager = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
        }

        mParams = new WindowManager.LayoutParams();
//        mParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;

        if (Build.VERSION.SDK_INT <= 21) { //lollipop
            mParams.type = WindowManager.LayoutParams.TYPE_TOAST;
        }else if(Build.VERSION.SDK_INT<=26){ //o
            mParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }else{
            mParams.type= 2038; //TYPE_APPLICATION_OVERLAY
        }

        mParams.format = PixelFormat.RGBA_8888;
//        mParams.format = PixelFormat.TRANSLUCENT;

        final boolean moveEnable = sharedPreferences.getBoolean("current_activity_move", false);
        if (moveEnable) {
            mParams.flags =
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            ;
        } else {
            mParams.flags =
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE     //不影响背后view 的点击事件
            ;
        }

        mParams.gravity = Gravity.TOP | Gravity.START;

        mParams.x = sharedPreferences.getInt("current_activity_x", 0);
        mParams.y = sharedPreferences.getInt("current_activity_y", 0);

        mParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mParams.height = WindowManager.LayoutParams.WRAP_CONTENT;


        mActivityTv = new TextView(getApplicationContext());
//        mActivityTv.setBackgroundColor(
//                ContextCompat.getColor(getApplicationContext(), R.color.floating_background_default)
//        );
        mActivityTv.setBackgroundResource(R.drawable.floating_round_bg);
        mActivityTv.setTextSize(sharedPreferences.getInt("current_activity_textsize",14));
        mActivityTv.setPadding(10,10,10,10);
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
                        mParams.x = (int) (event.getRawX() - lastX);
                        mParams.y = (int) (event.getRawY() - lastY - mStatusbarHeight);
                        mWindowManager.updateViewLayout(mActivityTv, mParams);
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
                long current = System.currentTimeMillis();
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
                    }
                } else if (count != 2) {
                    count = 2;
                }
                lastTime = current;
            }
        });

        Utils.debug("activity addview before");
        mWindowManager.addView(mActivityTv, mParams);
        Utils.debug("activity addview after");

        registerReceivers();
    }

    private void registerReceivers() {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        if (Build.VERSION.SDK_INT >= 24) {
            activityStopReceiver = new BroadcastReceiver() {
                @RequiresApi(api = Build.VERSION_CODES.N)
                @Override
                public void onReceive(Context context, Intent intent) {
                    disableSelf();
                }
            };
            localBroadcastManager.registerReceiver(activityStopReceiver, new IntentFilter(ACTION_ACTIVITY_STOP));
        }

        activityNameReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                isActivityFullName = intent.getBooleanExtra("enable", false);

                //立刻转换当前显示
                String currentText = mActivityTv.getText().toString();
                if (TextUtils.isEmpty(currentText)) {
                    return;
                }
                int index = currentText.indexOf(SEPERATE_MARK);
                if (index >= currentText.length() - 1) {
                    return;
                }
                String packageName = currentText.substring(0, index);
                String className = currentText.substring(index + SEPERATE_MARK.length());
                if (isActivityFullName && className.startsWith(".")) {
                    mActivityTv.setText(packageName + SEPERATE_MARK + packageName + className);
                } else if (!isActivityFullName && !className.startsWith(".")) {
                    int in = className.indexOf(packageName);
                    if (in >= 0) {
                        className = className.substring(packageName.length());
                        mActivityTv.setText(packageName + SEPERATE_MARK + className);
                    }
                }
            }
        };

        localBroadcastManager.registerReceiver(activityNameReceiver, new IntentFilter(ACTION_ACTIVITY_FULL_NAME));

        activityMoveReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean enable = intent.getBooleanExtra("enable", false);
                if (enable) {
                    mParams.flags =
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
//                                    | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                    ;
                } else {
                    mParams.flags =
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE     //不影响背后view 的点击事件
                    ;
                }

                mWindowManager.updateViewLayout(mActivityTv, mParams);
            }
        };
        localBroadcastManager.registerReceiver(activityMoveReceiver, new IntentFilter(ACTION_ACTIVITY_MOVE));

        activityTextsizeReceiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mActivityTv.setTextSize(intent.getIntExtra("textsize",14));
            }
        };
        localBroadcastManager.registerReceiver(activityTextsizeReceiver, new IntentFilter(ACTION_ACTIVITY_TEXTSIZE));
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
