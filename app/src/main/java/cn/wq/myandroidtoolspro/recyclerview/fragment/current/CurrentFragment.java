package cn.wq.myandroidtoolspro.recyclerview.fragment.current;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.recyclerview.fragment.CustomProgressDialogFragment;
import cn.wq.myandroidtoolspro.recyclerview.toolbar.BaseFragment;

/**
 * Created by wangqi on 2017/7/6.
 */

public class CurrentFragment extends BaseFragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private SwitchCompat mActivitySwitch, mFragmentSwitch,mMoveSwitch;
//    private ComponentName mActivityServiceComponentName;
    private View mFragmentTimeParent, mActivityTextsizeParent,mFragmentParent,mMoveParent;
    private TextView mFragmentTimeTv;
    private SharedPreferences sharedPreferences;
    private final static int REQUEST_CODE_NUM_PICKER = 100;
    private TextView mActSizeTv;
    private AppCompatSeekBar mActSeekBar;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //参考：https://android.googlesource.com/platform/frameworks/base/+/master/core/res/res/layout/preference.xml
        return inflater.inflate(R.layout.fragment_current, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.current_activity_parent).setOnClickListener(this);
        mActivitySwitch = (SwitchCompat) view.findViewById(R.id.current_activity_switch);
        mActivitySwitch.setOnCheckedChangeListener(this);
        mFragmentParent = view.findViewById(R.id.current_fragment_parent);
        mFragmentParent.setOnClickListener(this);
        mFragmentSwitch = (SwitchCompat) view.findViewById(R.id.current_fragment_switch);
        mFragmentSwitch.setOnCheckedChangeListener(this);
        mActivityTextsizeParent = view.findViewById(R.id.current_activity_textsize_parent);

        mFragmentTimeParent = view.findViewById(R.id.current_fragment_time_parent);
        mFragmentTimeParent.setOnClickListener(this);

        mActSizeTv = (TextView) view.findViewById(R.id.current_activity_textsize);
        // 真实值从 5~30,progress从 0~5
        mActSeekBar = (AppCompatSeekBar) view.findViewById(R.id.current_activity_seekbar);
        mActSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                final int newValue = progress + 5;
                mActSizeTv.setText(getString(R.string.textsize) + ": " + newValue);

                sharedPreferences.edit()
                        .putInt("current_activity_textsize", newValue).apply();

                Intent intent = new Intent(Utils.ACTION_ACTIVITY_TEXTSIZE);
                intent.putExtra("textsize", newValue);
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mFragmentTimeTv = (TextView) view.findViewById(R.id.current_fragment_time_summary);

        mMoveParent = view.findViewById(R.id.current_move_parent);
        mMoveParent.setOnClickListener(this);
        mMoveSwitch = view.findViewById(R.id.current_move_switch);

    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//        mActivityServiceComponentName = new ComponentName(getContext().getPackageName(), ActivityWatchingService.class.getCanonicalName());

        initActionbar(0, getString(R.string.current));

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        int interval = sharedPreferences.getInt("current_fragment_interval", NumberPickerDialog.DEFAUT_VALUE * 100);
        mFragmentTimeTv.setText(String.valueOf(interval));

        mActSeekBar.setProgress(sharedPreferences.getInt("current_activity_textsize", 14) - 5);
        mMoveSwitch.setChecked(sharedPreferences.getBoolean("current_move", true));
    }

    @Override
    public void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= 23) { //Android 6.0 M
            //在红米note5（8.0）上测试，授权允许后需要延迟一会儿才能得到新值
            mActivitySwitch.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!Settings.canDrawOverlays(getContext())) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                        intent.setData(Uri.parse("package:" + getContext().getPackageName()));
                        FloatingPermissionDialogFragment.newInstance(getString(R.string.floating_permission_hint), intent)
                                .show(getChildFragmentManager(), "permission");
                    }
                }
            },500);
        }

        mActivitySwitch.setChecked(FragmentWatchingService.isRunning);

        if (FragmentWatchingService.isRunning) {
            if (mFragmentTimeParent.getVisibility() == View.GONE) {
                mFragmentTimeParent.setVisibility(View.VISIBLE);
            }
            if (mFragmentParent.getVisibility() == View.GONE) {
                mFragmentParent.setVisibility(View.VISIBLE);
                mFragmentSwitch.setChecked(sharedPreferences.getBoolean("current_fragment_show", false));
            }
            if (mActivityTextsizeParent.getVisibility() == View.GONE) {
                mActivityTextsizeParent.setVisibility(View.VISIBLE);
            }
            if (mMoveParent.getVisibility() == View.GONE) {
                mMoveParent.setVisibility(View.VISIBLE);
            }
        } else {
            if (mFragmentTimeParent.getVisibility() == View.VISIBLE) {
                mFragmentTimeParent.setVisibility(View.GONE);
            }
            if (mFragmentParent.getVisibility() == View.VISIBLE) {
                mFragmentParent.setVisibility(View.GONE);
            }
            if (mActivityTextsizeParent.getVisibility() == View.VISIBLE) {
                mActivityTextsizeParent.setVisibility(View.GONE);
            }
            if (mMoveParent.getVisibility() == View.VISIBLE) {
                mMoveParent.setVisibility(View.GONE);
            }
        }
//        if (FragmentWatchingService.isRunning && mFragmentTimeParent.getVisibility() == View.GONE) {
//            mFragmentTimeParent.setVisibility(View.VISIBLE);
//        } else if (!FragmentWatchingService.isRunning && mFragmentTimeParent.getVisibility() == View.VISIBLE) {
//            mFragmentTimeParent.setVisibility(View.GONE);
//        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.current_activity_parent:
                if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(getContext())) {
                    Toast.makeText(getContext(), R.string.floating_permission_hint, Toast.LENGTH_SHORT).show();
                } else {
                    handleActivity();
                }
                break;
            case R.id.current_fragment_parent:
                if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(getContext())) {
                    Toast.makeText(getContext(), R.string.floating_permission_hint, Toast.LENGTH_SHORT).show();
                } else {
                    handleFragment();
                }
                break;
            case R.id.current_fragment_time_parent:
                NumberPickerDialog dialog = new NumberPickerDialog();
//                dialog.show(getChildFragmentManager(), "picker");
                dialog.show(getFragmentManager(), "picker");
                dialog.setTargetFragment(this, REQUEST_CODE_NUM_PICKER);
                break;
            case R.id.current_move_parent:
                boolean newState = !mMoveSwitch.isChecked();
                sharedPreferences.edit()
                        .putBoolean("current_move", newState).apply();
                Intent intent = new Intent(Utils.ACTION_ACTIVITY_MOVE);
                intent.putExtra("enable", newState);
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
                mMoveSwitch.setChecked(newState);
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_NUM_PICKER && resultCode == Activity.RESULT_OK) {
            int num = data.getIntExtra("num", 0);
            if (num < NumberPickerDialog.MIN_VALUE * 100 || num > NumberPickerDialog.MAX_VALUE * 100) {
                return;
            }
            mFragmentTimeTv.setText(String.valueOf(num));

            Intent intent = new Intent(FragmentWatchingService.ACTION_FRAGMENT_TIME);
            intent.putExtra("num", num);
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
        }
    }

    private void handleActivity() {
        //google不允许辅助服务
        if (Utils.isRemoveAccessibilityForGoogle()) {
            boolean newState = !mActivitySwitch.isChecked();
            if (newState) {
                getContext().stopService(new Intent(getContext(), FragmentWatchingService.class));
                getContext().startService(new Intent(getContext(), FragmentWatchingService.class));
            } else {
                getContext().stopService(new Intent(getContext(), FragmentWatchingService.class));
            }
            mActivitySwitch.setChecked(newState);
            return;
        }

        /**
         final boolean isServiceEnabled = checkServiceIsEnable();
         //root方式，绕过系统设置开启页面
        if (Build.VERSION.SDK_INT >= 23) {
            new AsyncTask<Void, Void, Boolean>() {
                private CustomProgressDialogFragment dialog;

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    dialog = new CustomProgressDialogFragment();
                    dialog.setCancelable(false);
                    dialog.show(getChildFragmentManager(), "dialog");
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    super.onPostExecute(result);
                    dialog.dismissAllowingStateLoss();
                    if (result) {
                        mActivitySwitch.setChecked(!isServiceEnabled);
                    } else {
                        handleActivityAsNotRoot(!isServiceEnabled);
                    }
                }

                @Override
                protected void onCancelled() {
                    super.onCancelled();
                    dialog.dismissAllowingStateLoss();
                }

                @Override
                protected Boolean doInBackground(Void... params) {
                    return handleActivityAsRoot(!isServiceEnabled);
                }
            }.execute();
        } else {
            handleActivityAsNotRoot(!isServiceEnabled);
        }
        */

    }

//    private boolean handleActivityAsRoot(boolean enable) {
//        List<String> stdout = Utils.runRootCommandForResult("settings get secure enabled_accessibility_services");
//        if (stdout == null) {
//            return false;
//        }
//
//        if (stdout.size() == 0) {
//            if (enable) {
//                return Utils.runRootCommand(
//                        "settings put secure enabled_accessibility_services " + mActivityServiceComponentName.flattenToString() +
//                                " && settings put secure accessibility_enabled 1"
//                );
//            }
//        } else {
//            String before = stdout.get(0).trim();
//            if (enable) {
//                String allService;
//                if (TextUtils.isEmpty(before)) {
//                    allService = mActivityServiceComponentName.flattenToString();
//                } else {
//                    allService = before + ":" + mActivityServiceComponentName.flattenToString();
//                }
//                return Utils.runRootCommand(
//                        "settings put secure enabled_accessibility_services " + allService +
//                                " && settings put secure accessibility_enabled 1"
//                );
//            } else {
//                boolean fullExist = checkExists(mActivityServiceComponentName.flattenToString(), before);
//                if (!fullExist) {
//                    boolean shortExist = checkExists(mActivityServiceComponentName.flattenToShortString(), before);
//                    if (!shortExist) {
//                        Log.e("myandroidtoolspro", "close current activity but not found");
//                    }
//                }
//            }
//        }
//
//        return true;
//
//
////        return Utils.runRootCommand(
////                "settings put secure enabled_accessibility_services " + allService +
////                        " && settings put secure accessibility_enabled " + (enable ? "1" : "0")
////        );
//    }

//    private boolean checkExists(String specific, String before) {
//        int fullIndex = before.indexOf(specific);
//        if (fullIndex == 0) {
//            if (before.length() > specific.length()) {  //去掉后面的:
//                String remain = before.substring(specific.length() + 1);
//                return Utils.runRootCommand(
//                        "settings put secure enabled_accessibility_services " + remain +
//                                " && settings put secure accessibility_enabled 1"
//                );
//            } else {
//                return Utils.runRootCommand(
//                        "settings put secure enabled_accessibility_services '' " +
//                                " && settings put secure accessibility_enabled 1"
//                );
//            }
//        } else if (fullIndex > 0) {
//            String remain = before.replace(":" + specific, "");
//            return Utils.runRootCommand(
//                    "settings put secure enabled_accessibility_services " + remain +
//                            " && settings put secure accessibility_enabled 1"
//            );
//        }
//
//        return false;
//    }

//    private void handleActivityAsNotRoot(boolean enable) {
//        if (!enable) {
//            if (Build.VERSION.SDK_INT >= 24) {
//                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(ActivityWatchingService.ACTION_ACTIVITY_STOP));
//                mActivitySwitch.setChecked(false);
//            } else {
//                AccessibilityDialogFragment.newInstance(false).show(getChildFragmentManager(), "disable");
//            }
//        } else {
//            AccessibilityDialogFragment.newInstance(true).show(getChildFragmentManager(), "enable");
//        }
//    }


    private void handleFragment() {
        getContext().stopService(new Intent(getContext(), FragmentWatchingService.class));
        getContext().startService(new Intent(getContext(), FragmentWatchingService.class));

        mFragmentSwitch.setChecked(!mFragmentSwitch.isChecked());
    }

//    private boolean checkServiceIsEnable() {
//        int accessibilityEnabled = 0;
//        try {
//            accessibilityEnabled = Settings.Secure.getInt(
//                    getContext().getContentResolver(),
//                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
//        } catch (Settings.SettingNotFoundException e) {
//            e.printStackTrace();
//        }
//        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');
//
//        if (accessibilityEnabled == 1) {
//            String settingValue = Settings.Secure.getString(
//                    getContext().getContentResolver(),
//                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
//
//            if (settingValue != null) {
//                mStringColonSplitter.setString(settingValue);
//                while (mStringColonSplitter.hasNext()) {
//                    String accessibilityService = mStringColonSplitter.next();
//                    if (accessibilityService.equals(mActivityServiceComponentName.flattenToString())
//                            || accessibilityService.equals(mActivityServiceComponentName.flattenToShortString())) {
//                        return true;
//                    }
//                }
//            }
//        }
//        return false;
//    }

    //>=api 23 ,6.0
//    private boolean checkFloatPermission() {
//        try {
//            Method canDrawOverlays = Settings.class.getDeclaredMethod("canDrawOverlays", Context.class);
//            return (Boolean)canDrawOverlays.invoke(null, getContext());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return true;
//    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.current_activity_switch:
                mFragmentParent.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                mFragmentSwitch.setChecked(sharedPreferences.getBoolean("current_fragment_show", false));
                mFragmentTimeParent.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                mActivityTextsizeParent.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                mMoveParent.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                break;
            case R.id.current_fragment_switch:
                sharedPreferences.edit().putBoolean("current_fragment_show",isChecked).apply();
                break;
        }
    }
}
