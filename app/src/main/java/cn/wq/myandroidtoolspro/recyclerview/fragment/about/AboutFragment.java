package cn.wq.myandroidtoolspro.recyclerview.fragment.about;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.ArrayRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.mars.xlog.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

import cn.wq.myandroidtoolspro.BaseActivity;
import cn.wq.myandroidtoolspro.BuildConfig;
import cn.wq.myandroidtoolspro.MainActivity;
import cn.wq.myandroidtoolspro.MainDarkActivity;
import cn.wq.myandroidtoolspro.PackageAddReceiver;
import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.IfwUtil;
import cn.wq.myandroidtoolspro.helper.LanguageUtils;
import cn.wq.myandroidtoolspro.helper.ShizukuUtil;
import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.helper.ZipUtil;
import cn.wq.myandroidtoolspro.recyclerview.fragment.CustomProgressDialogFragment;
import cn.wq.myandroidtoolspro.recyclerview.fragment.LicenseDialogFragment;
import cn.wq.myandroidtoolspro.recyclerview.fragment.UpdateDialogFragment;
import cn.wq.myandroidtoolspro.recyclerview.toolbar.BaseFragment;
import eu.chainfire.libsuperuser.Shell;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import moe.shizuku.api.ShizukuClient;
import moe.shizuku.api.ShizukuPackageManagerV26;

public class AboutFragment extends BaseFragment implements OnClickListener {
    private static final String TAG = "AboutFragment";
    private PackageManager pm;
    private final static int REQUEST_CODE_BACKUP = 0;
    private final static int REQUEST_CODE_RESOTRE = 1;
    private CustomProgressDialogFragment dialog;
    private AtomicInteger paralTaskCount;
    private MyBackupTask[] myBackupTasks;
    private AsyncTask<Void, Void, Boolean> myRestoreTask;
    private MyRestoreTask[] myRestoreTasks;
    private boolean[] checkedItems;
    //    private final static String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";
    private String backupFilePath;
    private boolean isOnly3rd;
    private Context mContext;
    //free
    private Button restoreBtn;
    private TextView backupInfoTv;
    private final static String BACKUP_FILE_DIR = Environment.getExternalStorageDirectory() + "/myandroidtools_backup.txt";
    private Spinner pmChannelSpinner;
    private AsyncTask<Intent, Void, Boolean> shizukuRestoreTask;
    private final static int REQUEST_CODE_SHIZUKU = 250;
    private final static int REQUEST_CODE_IFW_HINT = 251;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        initActionbar(0, getString(R.string.about));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_about, container, false);

        pm = mContext.getPackageManager();
        TextView version = (TextView) rootView.findViewById(R.id.version);
        try {
            String versionName = pm.getPackageInfo(mContext.getPackageName(), 0).versionName;
            version.setText(versionName);
        } catch (NameNotFoundException e) {
//            e.printStackTrace();
        }

        Button backupBtn = (Button) rootView.findViewById(R.id.backup);
        restoreBtn = (Button) rootView.findViewById(R.id.restore);

        if (BuildConfig.isFree) {
            backupInfoTv = (TextView) rootView.findViewById(R.id.backup_info);

            if (new File(BACKUP_FILE_DIR).exists()) {
                backupInfoTv.setText(BACKUP_FILE_DIR);
                backupInfoTv.setVisibility(View.VISIBLE);
            } else {
                backupInfoTv.setVisibility(View.GONE);
                restoreBtn.setEnabled(false);
            }

        }

        backupBtn.setOnClickListener(this);
        restoreBtn.setOnClickListener(this);

        final ComponentName cName = new ComponentName(mContext, PackageAddReceiver.class);
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        if (!BuildConfig.isFree && sharedPreferences.getInt(BaseActivity.PREFERENCE_THEME, 0) > 0) {
            ProgressBar progressBar = rootView.findViewById(R.id.icon);
            progressBar.setIndeterminateDrawable(ContextCompat.getDrawable(mContext, R.drawable.progress_dark));
        }

        initSpinnerLine(
                rootView,
                true,
                R.id.english,
                R.string.choose_language,
                R.array.language_options,
                LanguageUtils.getChoosedLanguage(sharedPreferences),
                new OnSpinnerItemSelectListener() {
                    @Override
                    public void onItemSelected(int defaultPos, int newPos) {
                        if (defaultPos == newPos) {
                            return;
                        }
                        sharedPreferences.edit().putInt(BaseActivity.PREFERENCE_LANGUAGE, newPos).apply();

//                        FragmentActivity activity = getActivity();
//                        activity.finish();
//
//                        Class newCls;
//
//                        if (sharedPreferences.getInt(BaseActivity.PREFERENCE_THEME, 0) > 0) {
//                            newCls = MainDarkActivity.class;
//                        } else {
//                            newCls = MainActivity.class;
//                        }
//                        Intent intent = new Intent();
//                        intent.putExtra("restart", true);
//                        intent.setClass(activity, newCls);
//                        startActivity(intent);
//
//                        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

                        restartActivity();
                    }
                }
        );

//        initSwitchLine(
//                rootView,
//                !"English".equals(Utils.getSystemLocale().getDisplayLanguage()),
//                R.id.english,
//                R.string.language_english,
//                sharedPreferences.getBoolean(BaseActivity.PREFERENCE_ENGLISH, false),
//                new CompoundButton.OnCheckedChangeListener() {
//                @Override
//                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                    sharedPreferences.edit().putBoolean(BaseActivity.PREFERENCE_ENGLISH, isChecked).apply();
//
//                    FragmentActivity activity = getActivity();
//                    activity.finish();
//
////                    if(sharedPreferences.getInt(BaseActivity.PREFERENCE_THEME,0) > 0){
////                        startActivity(new Intent(activity, MainDarkActivity.class).putExtra("restart", true));
////                    } else {
////                        startActivity(new Intent(activity, MainActivity.class).putExtra("restart", true));
////                    }
//
//                    Class newCls;
//
//                    if(sharedPreferences.getInt(BaseActivity.PREFERENCE_THEME,0) > 0){
//                        newCls = MainDarkActivity.class;
//                    }else{
//                        newCls = MainActivity.class;
//                    }
//                    Intent intent = new Intent();
//                    intent.putExtra("restart", true);
//                    intent.setClass(activity, newCls);
//                    startActivity(intent);
//
//                    activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
//                }
//            });

        initSwitchLine(
                rootView,
                Build.VERSION.SDK_INT < Build.VERSION_CODES.O,
                R.id.showAfterPackageAdd,
                R.string.showAfterPackageAdd,
                pm.getComponentEnabledSetting(cName) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        sharedPreferences.edit().putBoolean(BaseActivity.PREFERENCE_PACKAGE_RECEIVED, isChecked).apply();
                        pm.setComponentEnabledSetting(cName,
                                isChecked ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                PackageManager.DONT_KILL_APP);
                    }
                }
        );

        initSpinnerLine(
                rootView,
                true,
                R.id.theme_parent,
                R.string.theme,
                R.array.theme_options,
                sharedPreferences.getInt(BaseActivity.PREFERENCE_THEME, 0),
                new OnSpinnerItemSelectListener() {
                    @Override
                    public void onItemSelected(int defaultPos, int newPos) {
                        if (defaultPos == newPos) {
                            return;
                        }
                        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putInt(BaseActivity.PREFERENCE_THEME, newPos).apply();

                        FragmentActivity activity = (FragmentActivity) mContext;
                        activity.finish();
                        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

                        Intent intent = new Intent();
                        Class newCls, oldCls;
                        if (newPos > 0) {
                            newCls = MainDarkActivity.class;
                            oldCls = MainActivity.class;
                        } else {
                            newCls = MainActivity.class;
                            oldCls = MainDarkActivity.class;
                        }
                        intent.setClass(mContext, newCls);
                        pm.setComponentEnabledSetting(
                                new ComponentName(mContext, newCls), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
                        );
                        pm.setComponentEnabledSetting(
                                new ComponentName(mContext, oldCls), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
                        );

                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.putExtra("restart", true);
                        intent.putExtra("needKillSelf", true);
                        startActivity(intent);
                    }
                }
        );

        /*
        final Spinner themeSpinner = rootView.findViewById(R.id.theme_spinner);
        List<String> strings = new ArrayList<>();
        strings.add(getString(R.string.theme_default));
        strings.add(getString(R.string.theme_dark));
        strings.add(getString(R.string.theme_night));
        //文字大小保持一致
//        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, strings);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(), R.layout.spinner_list_item, strings);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        themeSpinner.setAdapter(arrayAdapter);

        View themeParent = rootView.findViewById(R.id.theme_parent);
        themeParent.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                themeSpinner.performClick();
            }
        });

        final int defaultTheme = sharedPreferences.getInt(BaseActivity.PREFERENCE_THEME, 0);
        themeSpinner.setSelection(defaultTheme);
        themeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (defaultTheme == position) {
                    return;
                }
                sharedPreferences.edit().putInt(BaseActivity.PREFERENCE_THEME, position).apply();

                FragmentActivity activity = (FragmentActivity) mContext;
                activity.finish();
                activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

                Intent intent = new Intent();
                Class newCls,oldCls;
                if (position > 0) {
                    newCls = MainDarkActivity.class;
                    oldCls = MainActivity.class;
                }else{
                    newCls = MainActivity.class;
                    oldCls = MainDarkActivity.class;
                }
                intent.setClass(mContext, newCls);
//                String newCls,oldCls;
//                if (position > 0) {
//                    newCls = mContext.getPackageName() + ".MainDarkActivity";
//                    oldCls = mContext.getPackageName() + ".MainActivity";
//                }else{
//                    newCls = mContext.getPackageName() + ".MainActivity";
//                    oldCls = mContext.getPackageName() + ".MainDarkActivity";
//                }
//                intent.setClassName(mContext, newCls);

                pm.setComponentEnabledSetting(
                        new ComponentName(mContext, newCls), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
                );
                pm.setComponentEnabledSetting(
                        new ComponentName(mContext, oldCls), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
                );

//                if (position > 0) {
//                    intent.setClass(activity, MainDarkActivity.class);
//                    pm.setComponentEnabledSetting(
//                            new ComponentName(activity, MainDarkActivity.class), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
//                    );
//                    pm.setComponentEnabledSetting(
//                            new ComponentName(activity, MainActivity.class), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
//                    );
//                } else {
//                    intent.setClass(activity, MainActivity.class);
//                    pm.setComponentEnabledSetting(
//                            new ComponentName(activity, MainActivity.class), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
//                    );
//                    pm.setComponentEnabledSetting(
//                            new ComponentName(activity, MainDarkActivity.class), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
//                    );
//                }

                //没有这一段的话 可能会造成launcher更新了icon会杀死activity
//                if (Build.VERSION.SDK_INT >= 11) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                }else{
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
//                }

                intent.putExtra("restart", true);
                intent.putExtra("needKillSelf", true);
                startActivity(intent);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        */


        int pmChannel = sharedPreferences.getInt(BaseActivity.PREFERENCE_PM_CHANNER, BaseActivity.PM_CHANNEL_ROOT_COMMAND);
        pmChannelSpinner = initSpinnerLine(
                rootView,
                true,
                R.id.pm_channel,
                R.string.pm_channel,
                R.array.pm_channel_options,
                pmChannel,
                new OnSpinnerItemSelectListener() {
                    @Override
                    public void onItemSelected(int defaultPos, final int newPos) {
                        Utils.debug(TAG, "111111" + defaultPos + "," + newPos);
                        if (newPos == BaseActivity.PM_CHANNEL_IFW) {
                            DialogFragment f = new LoadAllIfwDialog();
                            f.setTargetFragment(AboutFragment.this, REQUEST_CODE_IFW_HINT);
                            f.show(getFragmentManager(), "ifw_dialog");
                        }
                        if (sharedPreferences.getInt(BaseActivity.PREFERENCE_PM_CHANNER, BaseActivity.PM_CHANNEL_ROOT_COMMAND) == newPos) {
                            return;
                        }
                        if (newPos == BaseActivity.PM_CHANNEL_ROOT_COMMAND) {
                            sharedPreferences.edit().putInt(BaseActivity.PREFERENCE_PM_CHANNER, newPos).apply();
                        } else if (newPos == BaseActivity.PM_CHANNEL_SHIZUKU) {
                            if (!ShizukuClient.isManagerInstalled(getContext())) {
                                pmChannelSpinner.setSelection(0);
                                Toast.makeText(mContext, R.string.shizuku_not_installed, Toast.LENGTH_SHORT).show();
                                return;
                            }
//                            if (ShizukuClient.getState().isAuthorized()) {
//                                sharedPreferences.edit().putInt(BaseActivity.PREFERENCE_PM_CHANNER, newPos).apply();
//                            } else {
//                                ((MainActivity)getActivity()).requestShizukuAuthorize();
//                            }


                            Disposable disposable = ShizukuUtil.getIsAuthorised(new Consumer() {
                                @Override
                                public void accept(Object o) throws Exception {
                                    sharedPreferences.edit().putInt(BaseActivity.PREFERENCE_PM_CHANNER, newPos).apply();
                                }
                            }, new Consumer() {
                                @Override
                                public void accept(Object o) throws Exception {
                                    Utils.debug(TAG, "shizuku isAuthorize: false 1 ..");
                                    requestShizukuAuthorize();
                                }
                            });
                            compositeDisposable.add(disposable);

                        } else if (newPos == BaseActivity.PM_CHANNEL_IFW) {
                            sharedPreferences.edit().putInt(BaseActivity.PREFERENCE_PM_CHANNER, newPos).apply();
                        }
                    }
                }
        );
        if (pmChannel == 1) {
            Disposable disposable = ShizukuUtil.getIsAuthorised(null, new Consumer() {
                @Override
                public void accept(Object o) throws Exception {
                    Utils.debug(TAG, "shizuku isAuthorize: false 2 ..");
                    requestShizukuAuthorize();
                }
            });
            compositeDisposable.add(disposable);
        }

        return rootView;
    }

    private void restartActivity() {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.finish();
        }

        Class newCls;

        if (PreferenceManager.getDefaultSharedPreferences(mContext).getInt(BaseActivity.PREFERENCE_THEME, 0) > 0) {
            newCls = MainDarkActivity.class;
        } else {
            newCls = MainActivity.class;
        }
        Intent intent = new Intent();
        intent.putExtra("restart", true);
        intent.setClass(mContext, newCls);
        startActivity(intent);

        if (activity != null) {
            activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    //申请Shizuku授权
    private void requestShizukuAuthorize() {
        if (ShizukuClient.checkSelfPermission(getContext())) {
            ShizukuClient.requestAuthorization(this);
        } else {
            requestPermissions(new String[]{ShizukuClient.PERMISSION_V23}, REQUEST_CODE_SHIZUKU);
        }
    }

    private void initSwitchLine(View rootView, boolean isShown, @IdRes int id, @StringRes int titleStrRes, boolean defaultVal, final CompoundButton.OnCheckedChangeListener listener) {
        View root = rootView.findViewById(id);

        if (root != null) {
            if (!isShown) {
                root.setVisibility(View.GONE);
                return;
            }
            root.setVisibility(View.VISIBLE);
            TextView title = root.findViewById(R.id.title);
            title.setText(titleStrRes);

            final SwitchCompat swi = root.findViewById(R.id.switch_);
            swi.setChecked(defaultVal);

            root.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    swi.setChecked(!swi.isChecked());
                    if (listener != null) {
                        listener.onCheckedChanged(null, swi.isChecked());
                    }
                }
            });
        }
    }

    private Spinner initSpinnerLine(View rootView, boolean isShown, @IdRes int id, @StringRes final int labelStrRes, @ArrayRes int optionArrayRes, final int defaultValPos, final OnSpinnerItemSelectListener listener) {
        View root = rootView.findViewById(id);

        if (!(root instanceof ViewGroup)) {
            return null;
        }
        if (!isShown) {
            root.setVisibility(View.GONE);
            return null;
        }
        root.setVisibility(View.VISIBLE);
        TextView label = root.findViewById(R.id.label);
        label.setText(labelStrRes);

        ViewGroup group = (ViewGroup) root;
        int spinnerIndex = -1;
        for (int i = 0, count = group.getChildCount(); i < count; i++) {
            View child = group.getChildAt(i);
            if (child instanceof Spinner) {
                spinnerIndex = i;
                break;
            }
        }
        if (spinnerIndex < 0) {
            return null;
        }
        final Spinner spinner = (Spinner) group.getChildAt(spinnerIndex);
//        final Spinner spinner = root.findViewById(R.id.spinner);

        String[] options = getResources().getStringArray(optionArrayRes);

        //文字大小保持一致
//        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, strings);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(), R.layout.spinner_list_item, options);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);
        spinner.setSelection(defaultValPos);

        root.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                spinner.performClick();
            }
        });

//        SpinnerInteractionListener wrapperListener = new SpinnerInteractionListener(defaultValPos, listener);
//        spinner.setOnTouchListener(wrapperListener);
//        spinner.setOnItemSelectedListener(wrapperListener);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (listener != null) {
                    listener.onItemSelected(defaultValPos, position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        return spinner;

    }

//    private class SpinnerInteractionListener implements AdapterView.OnItemSelectedListener, View.OnTouchListener {
//        private OnSpinnerItemSelectListener listener;
//        private int defaultValPos;
//        private boolean userSelect = false;
//
//        public SpinnerInteractionListener(int defaultValPos,OnSpinnerItemSelectListener listener) {
//            this.defaultValPos = defaultValPos;
//            this.listener = listener;
//        }
//
//        @Override
//        public boolean onTouch(View v, MotionEvent event) {
//            userSelect = true;
//            return false;
//        }
//
//        @Override
//        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//            if (userSelect) {
//                // Your selection handling code here
//                if (listener != null) {
//                    listener.onItemSelected(defaultValPos, position);
//                }
//                userSelect = false;
//            }
//        }
//
//        @Override
//        public void onNothingSelected(AdapterView<?> parent) {
//        }
//
//    }

    @Override
    public void onClick(View view) {
//        Intent intent;
        switch (view.getId()) {
//            case R.id.share:
//                intent = new Intent(Intent.ACTION_SEND).setType("text/plain")
//                        .putExtra(Intent.EXTRA_TEXT,
//                                getString(R.string.share_information));
//                startActivity(intent);
//                break;
//            case R.id.rate:
//                intent = new Intent(Intent.ACTION_VIEW).setData(Uri
//                        .parse("market://details?id=cn.wq.myandroidtoolspro"));
//                if (pm.resolveActivity(intent, 0) != null) {
//                    startActivity(intent);
//                } else {
//                    Intent url = new Intent(Intent.ACTION_VIEW)
//                            .setData(
//                                    Uri.parse("https://play.google.com/store/apps/details?id=cn.wq.myandroidtoolspro"))
//                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    startActivity(url);
//                }
//                break;
            case R.id.backup: {
//                if (BuildConfig.DEBUG) {
//                    mContext.sendBroadcast(new Intent("cn.wq.myandroidtoolspro.SHOW_APP_INFO").putExtra("packageName", "com.coolapk.market"));
//                    return;
//                }

                if (!Utils.checkSDcard(mContext)) {
                    return;
                }

                if (Utils.isPmByIfw(mContext)) {
                    backupIfw();
                    return;
                } else {
                    showChooseDialog(true);
                }
            }
            break;
            case R.id.restore: {
                if (!Utils.checkSDcard(getContext())) {
                    return;
                }

                showChooseDialog(false);
            }
            break;
            default:
                break;
        }
    }

    private void backupIfw() {
        Disposable disposable = Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> emitter) throws Exception {
                //临时目录
                File destDir = new File(mContext.getExternalCacheDir(), UUID.randomUUID().toString());
                if (!destDir.exists()) {
                    destDir.mkdirs();
                }
                if (!IfwUtil.copySysIfwToTempDir(destDir)) {
                    emitter.onNext(false);
                } else {
                    File[] files = destDir.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            return name.endsWith(IfwUtil.BACKUP_SYSTEM_FILE_EXT_OF_MINE);
                        }
                    });
                    backupFilePath = generateBackupDir(true);
                    ZipUtil.zip(files, backupFilePath);
                    emitter.onNext(backupFilePath);
                }
                emitter.onComplete();
                //临时目录可以删除了
                if (destDir.exists()) {
                    destDir.delete();
                }
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) {
                        showProgressDialog();
                    }
                })
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object result) throws Exception {
                        if (result instanceof Boolean) {
                            hideProgressDialog(true, false);
                        } else {
                            renameBackupFile(true);
                            hideProgressDialog(true, true);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        throwable.printStackTrace();
                        hideProgressDialog(true, false);
                    }
                });

        compositeDisposable.add(disposable);
    }



    private void showChooseDialog(boolean isBackup) {
        final String tag = isBackup ? "choose_backup" : "choose_restore";
        if (getChildFragmentManager().findFragmentByTag(tag) == null) {
            DialogFragment dialog = new AboutChooseDialogFragment();
            dialog.setTargetFragment(this, isBackup ? REQUEST_CODE_BACKUP : REQUEST_CODE_RESOTRE);
            Bundle data = new Bundle();
            data.putInt("type", isBackup ? 0 : 1);
            dialog.setArguments(data);
//            dialog.show(getChildFragmentManager(), tag);
            dialog.show(getFragmentManager(), tag);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.about, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final Intent intent;
        switch (item.getItemId()) {
            case R.id.share:
                intent = new Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT,
                                getString(R.string.share_information));
                startActivity(intent);
                break;
            case R.id.donate:
                new AlertDialog.Builder(mContext)
                        .setTitle(R.string.donate)
                        .setItems(R.array.donate_options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 1) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW)
                                            .setData(Uri.parse("https://www.paypal.me/wangqi060934"));
                                    startActivity(intent);
                                } else if (which == 0) {
                                    try {
                                        pm.getPackageInfo("com.eg.android.AlipayGphone", 0);
                                        String INTENT_URL_FORMAT = "intent://platformapi/startapp?saId=10000007&" +
                                                "clientVersion=3.7.0.0718&qrcode=https%3A%2F%2Fqr.alipay.com%2Ffkx08330yuijh5bjtztfr6b%3F_s" +
                                                "%3Dweb-other&_t=1472443966571#Intent;" +
                                                "scheme=alipayqr;package=com.eg.android.AlipayGphone;end";
                                        Intent intent = Intent.parseUri(INTENT_URL_FORMAT, Intent.URI_INTENT_SCHEME);
                                        startActivity(intent);
                                    } catch (NameNotFoundException e) {
                                        e.printStackTrace();
                                        Toast.makeText(getContext(), "Not found Alipay installed", Toast.LENGTH_SHORT).show();
                                    } catch (URISyntaxException e) {
                                        e.printStackTrace();
                                        Log.e(TAG, "open alipay error", e);
                                    }
//                                    Intent intent = new Intent(Intent.ACTION_VIEW)
//                                            .setData(Uri.parse("https://qr.alipay.com/fkx08330yuijh5bjtztfr6b"));
//                                    startActivity(intent);

                                }
                            }
                        })
                        .show();
                break;
//            case R.id.rate:
//                intent = new Intent(Intent.ACTION_VIEW).setData(Uri
//                        .parse("market://details?id=" + getContext().getPackageName()));
//                if (pm.resolveActivity(intent, 0) != null) {
//                    startActivity(intent);
//                } else {
//                    Intent url = new Intent(Intent.ACTION_VIEW)
//                            .setData(
//                                    Uri.parse("https://play.google.com/store/apps/details?id=" + getContext().getPackageName()))
//                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    startActivity(url);
//                }
//                break;
            case R.id.lincense:
                LicenseDialogFragment f = new LicenseDialogFragment();
                f.show(getChildFragmentManager(), "license");
                break;
            case R.id.bugly:
                //必须commit
                PreferenceManager.getDefaultSharedPreferences(mContext)
                        .edit()
                        .putBoolean(BaseActivity.PREFERENCE_CLOSE_BUGLY, !item.isChecked())
                        .commit();
//                restartActivity();

                Class newCls;
                if (PreferenceManager.getDefaultSharedPreferences(mContext).getInt(BaseActivity.PREFERENCE_THEME, 0) > 0) {
                    newCls = MainDarkActivity.class;
                } else {
                    newCls = MainActivity.class;
                }
                intent = new Intent(mContext, newCls);
                intent.putExtra("restart", true);

                PendingIntent restartIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_ONE_SHOT);
                AlarmManager mgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
                if (mgr != null) {
                    mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, restartIntent);
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
                break;
            case R.id.updateCheck:
                checkUpdate();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkUpdate() {
        Disposable disposable = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                String url = getString(R.string.offical_website) + "/version";

                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                conn.disconnect();

                emitter.onNext(sb.toString());
                emitter.onComplete();
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) {
                        CustomProgressDialogFragment.showProgressDialog(
                                "check",
                                getFragmentManager(),
                                getString(R.string.checking_new_version)
                        );
                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        CustomProgressDialogFragment.hideProgressDialog("check", getFragmentManager());
                    }
                })
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String response) throws Exception {
                        JSONObject json = new JSONObject(response);
                        String version = json.getString("version");

                        if (version.compareTo(mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName) > 0) {
                            String url = json.getString("url");
                            String info = json.getString("info");
                            UpdateDialogFragment dialog = UpdateDialogFragment.create(version, info, url);
                            dialog.show(getFragmentManager(), "update");

                        } else {
                            Toast.makeText(getContext(), R.string.no_new_version, Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        throwable.printStackTrace();
                        Toast.makeText(getContext(), R.string.check_version_failed, Toast.LENGTH_SHORT).show();
                    }
                });

        compositeDisposable.add(disposable);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (menu == null) {
            return;
        }
        MenuItem menuItem = menu.findItem(R.id.bugly);
        if (menuItem == null) {
            return;
        }
        menuItem.setChecked(PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(BaseActivity.PREFERENCE_CLOSE_BUGLY, false));
    }

    /**
     * public static class ChooseDialogFragment extends DialogFragment {
     * private boolean[] checkedItems = new boolean[]{true, true, true, true};
     * private boolean isOnly3rd = true;
     * private int checkedItem;    //free
     * private int type;
     * private ListView listView;
     * <p>
     * private List<String> getAllBackupFileNames() {
     * List<String> result = new ArrayList<>();
     * File oldFile = new File(Environment.getExternalStorageDirectory() + "/myandroidtoolspro_backup.txt");
     * if (oldFile.exists()) {
     * result.add(oldFile.getAbsolutePath());
     * }
     * String dirString = Environment.getExternalStorageDirectory() + "/MyAndroidTools/";
     * File dir = new File(dirString);
     * if (!dir.exists()) {
     * return result;
     * }
     * //            File[] files=dir.listFiles(new FilenameFilter() {
     * //                @Override
     * //                public boolean accept(File dir, String filename) {
     * //                    return filename.startsWith("backup");
     * //                }
     * //            });
     * File[] files = dir.listFiles(new FileFilter() {
     *
     * @Override public boolean accept(File f) {
     * return f.isFile();
     * }
     * });
     * if (files != null && files.length > 0) {
     * Arrays.sort(files, new Comparator<File>() {
     * @Override public int compare(File lhs, File rhs) {
     * return (int) (rhs.lastModified() - lhs.lastModified());
     * }
     * });
     * for (File f : files) {
     * result.add(f.getName());
     * }
     * }
     * return result;
     * }
     * @Override public void onDestroyView() {
     * super.onDestroyView();
     * //onSaveIns..无效，只能在这里存
     * if (type == 1 && !BuildConfig.isFree && listView != null) {
     * getArguments().putInt("checkedPos", listView.getCheckedItemPosition());
     * }
     * }
     * @NonNull
     * @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
     * // type     0:backup    1:restore
     * type = getArguments().getInt("type");
     * if (type == 0) {
     * isOnly3rd = PreferenceManager.getDefaultSharedPreferences(getContext())
     * .getBoolean("backupOnly3rd", false);
     * <p>
     * View titleView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_backup_title, null);
     * SwitchCompat switchCompat = (SwitchCompat) titleView.findViewById(R.id.checkbox);
     * switchCompat.setChecked(isOnly3rd);
     * switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
     * @Override public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
     * isOnly3rd = b;
     * PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
     * .putBoolean("backupOnly3rd", isOnly3rd)
     * .apply();
     * }
     * });
     * <p>
     * return new AlertDialog.Builder(getContext())
     * //						.setTitle(R.string.backup)
     * .setCustomTitle(titleView)
     * .setMultiChoiceItems(R.array.backup_choose,
     * checkedItems, new OnMultiChoiceClickListener() {
     * @Override public void onClick(DialogInterface dialog,
     * int which, boolean isChecked) {
     * checkedItems[which] = isChecked;
     * }
     * })
     * .setPositiveButton(R.string.ok,
     * new DialogInterface.OnClickListener() {
     * @Override public void onClick(DialogInterface dialog,
     * int which) {
     * Intent data = new Intent();
     * data.putExtra("checkedItems",
     * checkedItems);
     * data.putExtra("isOnly3rd", isOnly3rd);
     * getTargetFragment().onActivityResult(
     * getTargetRequestCode(),
     * Activity.RESULT_OK, data);
     * dismiss();
     * }
     * })
     * .setNegativeButton(R.string.cancel,
     * new DialogInterface.OnClickListener() {
     * @Override public void onClick(DialogInterface dialog,
     * int which) {
     * dismiss();
     * }
     * }).create();
     * } else {
     * if (!BuildConfig.isFree) {
     * return createProDialog();
     * } else {
     * return createFreeDialog();
     * }
     * }
     * <p>
     * }
     * <p>
     * private Dialog createProDialog() {
     * View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_restore_view, null);
     * final RadioButton disableBtn = (RadioButton) view.findViewById(R.id.disable);
     * disableBtn.setChecked(true);
     * //                RadioButton enableBtn = (RadioButton) view.findViewById(R.id.enable);
     * listView = (ListView) view.findViewById(R.id.listview);
     * listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
     * final ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
     * android.R.layout.simple_list_item_single_choice,
     * getAllBackupFileNames());
     * listView.setAdapter(adapter);
     * <p>
     * final int pos = getArguments().getInt("checkedPos", -1);
     * if (pos >= 0 && pos < adapter.getCount()) {
     * listView.setItemChecked(pos, true);
     * }
     * <p>
     * view.findViewById(R.id.cancel).setOnClickListener(new OnClickListener() {
     * @Override public void onClick(View v) {
     * getDialog().dismiss();
     * }
     * });
     * view.findViewById(R.id.delete).setOnClickListener(new OnClickListener() {
     * @Override public void onClick(View v) {
     * int position = listView.getCheckedItemPosition();
     * if (position >= 0) {
     * String path = adapter.getItem(position);
     * if (path != null) {
     * File file = new File(Environment.getExternalStorageDirectory() + "/MyAndroidTools/" + path);
     * if (file.exists()) {
     * file.delete();
     * adapter.remove(path);
     * listView.clearChoices();
     * return;
     * }
     * }
     * }
     * <p>
     * Toast toast = Toast.makeText(getContext(), R.string.backup_delete_failed, Toast.LENGTH_SHORT);
     * toast.setGravity(Gravity.TOP, 0, 0);
     * toast.show();
     * }
     * });
     * view.findViewById(R.id.ok).setOnClickListener(new OnClickListener() {
     * @Override public void onClick(View v) {
     * int position = listView.getCheckedItemPosition();
     * if (position >= 0) {
     * String path = adapter.getItem(position);
     * if (path != null) {
     * Intent data = new Intent();
     * data.putExtra("checkedItem", disableBtn.isChecked() ? 0 : 1);
     * data.putExtra("path", Environment.getExternalStorageDirectory() + "/MyAndroidTools/" + adapter.getItem(position));
     * getTargetFragment().onActivityResult(
     * getTargetRequestCode(),
     * Activity.RESULT_OK, data);
     * getDialog().dismiss();
     * return;
     * }
     * }
     * <p>
     * Toast toast = Toast.makeText(getContext(), R.string.backup_delete_failed, Toast.LENGTH_SHORT);
     * toast.setGravity(Gravity.TOP, 0, 0);
     * toast.show();
     * }
     * });
     * view.findViewById(R.id.edit).setOnClickListener(new OnClickListener() {
     * @Override public void onClick(View v) {
     * int position = listView.getCheckedItemPosition();
     * if (position >= 0) {
     * String path = adapter.getItem(position);
     * if (path != null) {
     * dismissAllowingStateLoss();
     * <p>
     * BackupEditFragment f = new BackupEditFragment();
     * Bundle args = new Bundle();
     * args.putString("path", Environment.getExternalStorageDirectory() + "/MyAndroidTools/" + adapter.getItem(position));
     * f.setArguments(args);
     * <p>
     * FragmentTransaction ft = getActivity().getSupportFragmentManager()
     * .beginTransaction();
     * ft.replace(R.id.content, f);
     * //                            ft.setTransition(FragmentTransaction.TRANSIT_NONE);
     * ft.addToBackStack(null);
     * ft.commit();
     * return;
     * }
     * }
     * <p>
     * Toast toast = Toast.makeText(getContext(), R.string.backup_delete_failed, Toast.LENGTH_SHORT);
     * toast.setGravity(Gravity.TOP, 0, 0);
     * toast.show();
     * }
     * });
     * <p>
     * return new AlertDialog.Builder(getContext())
     * .setTitle(R.string.restore)
     * .setView(view)
     * .create();
     * }
     * <p>
     * private Dialog createFreeDialog() {
     * return new AlertDialog.Builder(getContext())
     * .setTitle(R.string.restore)
     * .setSingleChoiceItems(R.array.restore_choose, 0,
     * new DialogInterface.OnClickListener() {
     * @Override public void onClick(DialogInterface arg0,
     * int which) {
     * checkedItem = which;
     * }
     * })
     * .setPositiveButton(R.string.ok,
     * new DialogInterface.OnClickListener() {
     * @Override public void onClick(DialogInterface dialog,
     * int which) {
     * Intent data = new Intent();
     * data.putExtra("checkedItem",
     * checkedItem);
     * getTargetFragment().onActivityResult(
     * getTargetRequestCode(),
     * Activity.RESULT_OK, data);
     * dismiss();
     * }
     * })
     * .setNegativeButton(R.string.cancel,
     * new DialogInterface.OnClickListener() {
     * @Override public void onClick(DialogInterface dialog,
     * int which) {
     * dismiss();
     * }
     * }).create();
     * }
     * <p>
     * }
     */

    private String generateBackupDir(boolean isIfw) {
        if (!BuildConfig.isFree) {
            String dirString = Environment.getExternalStorageDirectory() + "/MyAndroidTools/";
            File dir = new File(dirString);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmm_SSS", Locale.getDefault());
            String string = dirString + "backup_" + dateFormat.format(new Date());
            if (isIfw) {
                string = string + IfwUtil.BACKUP_LOCAL_FILE_EXT;
            }
            File file = new File(string);
            if (file.exists()) {
                file.delete();
            }
            return file.getAbsolutePath();
        } else {
            File backupFile = new File(BACKUP_FILE_DIR);
            if (backupFile.exists()) {
                backupFile.delete();
            }
            return backupFile.getAbsolutePath();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_SHIZUKU) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ShizukuClient.requestAuthorization(this);
            } else if (pmChannelSpinner != null) {
                pmChannelSpinner.setSelection(0);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ShizukuClient.REQUEST_CODE_AUTHORIZATION) {
            if (resultCode == ShizukuClient.AUTH_RESULT_OK && data != null) {
                PreferenceManager.getDefaultSharedPreferences(getContext())
                        .edit()
                        .putInt(BaseActivity.PREFERENCE_PM_CHANNER, 1)
                        .apply();

                ShizukuClient.setToken(data);
                Toast.makeText(getContext(), R.string.shizuku_auth_success, Toast.LENGTH_SHORT).show();
            } else if (pmChannelSpinner != null) {
                pmChannelSpinner.setSelection(0);
                Toast.makeText(getContext(), R.string.shizuku_auth_failed, Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_BACKUP:
                    checkedItems = data
                            .getBooleanArrayExtra("checkedItems");
                    isOnly3rd = data.getBooleanExtra("isOnly3rd", false);
                    backupFilePath = generateBackupDir(false);
                    showProgressDialog();

                    List<ApplicationInfo> aInfos = pm
                            .getInstalledApplications(0);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        final int total = aInfos.size();
                        final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors() + 1;
                        final int chunckSize = (int) Math.ceil(total / (float) CORE_POOL_SIZE);
                        myBackupTasks = new MyBackupTask[CORE_POOL_SIZE];
                        paralTaskCount = new AtomicInteger(CORE_POOL_SIZE);
                        for (int i = 0; i < CORE_POOL_SIZE; i++) {
                            myBackupTasks[i] = new MyBackupTask();
                            if (i == CORE_POOL_SIZE - 1) {
                                myBackupTasks[i].executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                                        aInfos.subList(chunckSize * i, total).toArray(new ApplicationInfo[0]));
                            } else {
                                myBackupTasks[i].executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                                        aInfos.subList(chunckSize * i, chunckSize * (i + 1)).toArray(new ApplicationInfo[0]));
                            }
                        }
                    } else {
                        paralTaskCount = new AtomicInteger(1);

                        myBackupTasks = new MyBackupTask[1];
                        myBackupTasks[0] = new MyBackupTask();
                        myBackupTasks[0].execute(aInfos.toArray(new ApplicationInfo[0]));
                    }

                    break;
                case REQUEST_CODE_RESOTRE:
//                    if (!BuildConfig.isFree) {
//                        restoreForProByPm(data);
//                    }else{
//                        restoreForFree(data);
//                    }

                    if (Utils.isPmByShizuku(mContext)) {
                        restoreForProByShizuku(data);
                    } else if (Utils.isPmByIfw(mContext)) {
                        restoreForProByIfw(data);
                    } else {
                        restoreForProByPm(data);
                    }

                    break;
                case REQUEST_CODE_IFW_HINT:
                    mergeAllIfwData();
                    break;
                default:
                    break;
            }

        }
    }

    /**
     * 合并系统ifw目录下所有数据到自定义后缀的文件中
     */
    private void mergeAllIfwData() {
        Disposable disposable = Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> emitter) throws Exception {
                //临时目录
                File destDir = new File(mContext.getExternalCacheDir(), UUID.randomUUID().toString());
                if (!destDir.exists()) {
                    destDir.mkdirs();
                }
                if (!IfwUtil.copySysIfwToTempDir(destDir)) {
                    emitter.onNext(false);
                } else {
                    IfwUtil.IfwEntry ifwEntry = new IfwUtil.IfwEntry();
                    for (File file : destDir.listFiles()) {
                        if (!file.getName().endsWith(IfwUtil.BACKUP_SYSTEM_FILE_EXT_OF_STANDARD)) {
                            continue;
                        }
                        IfwUtil.parseIfwFile(ifwEntry, IfwUtil.COMPONENT_FLAG_ALL, null, file);
                        Utils.debug(TAG, "load file:" + file.toString());
                    }
                    //保存系统ifw目录所有数据到自定义名字的文件中
                    boolean result = IfwUtil.saveIfwEntryToMySystemFile(mContext, ifwEntry);
                    ifwEntry.clear();
                    emitter.onNext(result);
                }
                emitter.onComplete();

                //临时目录可以删除了
                if (destDir.exists()) {
                    destDir.delete();
                }
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) {
                        CustomProgressDialogFragment.showProgressDialog(
                                "ifw_load",
                                getFragmentManager()
                        );
                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run(){
                        CustomProgressDialogFragment.hideProgressDialog("ifw_load", getFragmentManager());
                    }
                })
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean result){
                        if (result != null && result) {
                            Toast.makeText(getContext(), R.string.operation_done, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), R.string.operation_failed, Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        throwable.printStackTrace();
                        Toast.makeText(getContext(), R.string.operation_failed, Toast.LENGTH_SHORT).show();
                    }
                });

        if (compositeDisposable == null) {
            compositeDisposable = new CompositeDisposable();
        }
        compositeDisposable.add(disposable);
    }

    //    private void restoreForFree(Intent data) {
//        final int checkedItem=data.getIntExtra("checkedItem", 0);
//        showProgressDialog();
//
//        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB) {
//            LineNumberReader lineNumberReader = null;
//            FileReader reader = null;
//            try {
//                reader = new FileReader(BACKUP_FILE_DIR);
//
//                lineNumberReader = new LineNumberReader(reader);
//                lineNumberReader.skip(Long.MAX_VALUE);
//                final int lineNum = lineNumberReader.getLineNumber();
//                if (lineNum == 0) {
//                    hideProgressDialog(false,true);
//                    return;
//                }
//
//                final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors() + 1;
//                final int chunckLineNum = (int) Math.ceil(lineNum / (float) CORE_POOL_SIZE);
////                        int[] chunckEndPositions = new int[CORE_POOL_SIZE - 1];
//                List<Integer> chunckEndPositions = new ArrayList<>();
//
//                reader = new FileReader(BACKUP_FILE_DIR);
//                BufferedReader bReader = new BufferedReader(reader);
//                String s;
//                StringBuilder sBuilder = new StringBuilder();
//                int lineCount = 0;
////                        int index = 0;
//                while ((s = bReader.readLine()) != null) {
//                    sBuilder.append("pm");
//                    sBuilder.append(checkedItem == 0 ? " disable " : " enable ");
//                    sBuilder.append(s);
//                    sBuilder.append("\n");
//                    lineCount++;
//                    if (lineCount == chunckLineNum) {
////                                chunckEndPositions[index++] = sBuilder.length();
//                        chunckEndPositions.add(sBuilder.length());
////                                Log.d(AboutFragment.class.getName(), "chunck:" + index + ":" + sBuilder.length());
//                        lineCount = 0;
//                    }
//                }
//                bReader.close();
//
//                int count=Math.min(CORE_POOL_SIZE,chunckEndPositions.size());
//                paralTaskCount=new AtomicInteger(count);
//                myRestoreTasks = new MyRestoreTask[count];
//                Log.d(AboutFragment.class.getName(), "backup sum:" + count);
//                for (int i = 0; i < count; i++) {
//                    myRestoreTasks[i] = new MyRestoreTask();
//                    if (i == 0) {
//                        myRestoreTasks[i].executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, sBuilder.substring(0, chunckEndPositions.get(0)));
//                    } else if (i == count - 1) {
//                        myRestoreTasks[i].executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, sBuilder.substring(chunckEndPositions.get(i-1), sBuilder.length()));
//                    } else {
//                        myRestoreTasks[i].executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, sBuilder.substring(chunckEndPositions.get(i-1), chunckEndPositions.get(i)));
//                    }
//                }
//            }catch (IOException e) {
//                e.printStackTrace();
//                hideProgressDialog(false,false);
//            }finally{
//                if(lineNumberReader!=null){
//                    try {
//                        lineNumberReader.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//                if(reader!=null){
//                    try {
//                        reader.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//
//        }else {
//            myRestoreTask=new AsyncTask<Void, Void, Boolean>(){
//                private boolean handleBefore11(){
//                    File file = new File(BACKUP_FILE_DIR);
//                    FileReader reader=null;
//                    try {
//                        reader = new FileReader(file);
//                        BufferedReader bReader = new BufferedReader(reader);
//                        String s;
////								StringBuilder builder = new StringBuilder(
////										"export LD_LIBRARY_PATH=/vendor/lib:/system/lib");
////								builder.append("\n");
//                        StringBuilder builder = new StringBuilder();
//                        while ((s = bReader.readLine()) != null) {
//                            builder.append("pm");
//                            builder.append(checkedItem==0?" disable ":" enable ");
//                            builder.append(s);
//                            builder.append("\n");
//                        }
//                        bReader.close();
//                        return Utils.runRootCommand(builder.toString());
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                        return false;
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                        return false;
//                    }finally{
//                        if(reader!=null){
//                            try {
//                                reader.close();
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }
//                }
//
//                @Override
//                protected void onPostExecute(Boolean result) {
//                    super.onPostExecute(result);
//                    hideProgressDialog(false,result);
//                }
//
//                @Override
//                protected Boolean doInBackground(Void... arg0) {
//                    return handleBefore11();
//                }
//            };
//            myRestoreTask.execute();
//
//        }
//    }

    private void restoreForProByIfw(final Intent data) {
        Disposable disposable = Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> emitter) throws Exception {
                //临时目录
                File dir = new File(mContext.getExternalCacheDir(), UUID.randomUUID().toString());
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                final String filePath = data.getStringExtra("path");
                ZipUtil.unzip(filePath, dir);
                boolean result = IfwUtil.copyToIfwDir(dir);
                if (dir.exists()) {
                    dir.delete();
                }
                emitter.onNext(result);
                emitter.onComplete();
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) {
                        showProgressDialog();
                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {

                    }
                })
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean result) throws Exception {
                        hideProgressDialog(false, result);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        throwable.printStackTrace();
                        Log.e(TAG, "restoreForProByIfw error", throwable);
                        hideProgressDialog(false, false);
                    }
                });

        compositeDisposable.add(disposable);

    }

    private void restoreForProByShizuku(Intent data) {
        if (shizukuRestoreTask != null) {
            shizukuRestoreTask.cancel(true);
        }
        shizukuRestoreTask = new AsyncTask<Intent, Void, Boolean>() {
            int errorType;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                showProgressDialog();
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                if (errorType == 0) {
                    hideProgressDialog(false, aBoolean);
                } else {
                    hideProgressDialog(false, aBoolean, R.string.shizuku_auth_failed);
                }

            }

            @Override
            protected Boolean doInBackground(Intent... params) {
                final int checkedItem = params[0].getIntExtra("checkedItem", 0);
                final String filePath = params[0].getStringExtra("path");

                LineNumberReader lineNumberReader = null;
                FileReader reader = null;
                try {
                    reader = new FileReader(filePath);

                    lineNumberReader = new LineNumberReader(reader);
                    lineNumberReader.skip(Long.MAX_VALUE);
                    final int lineNum = lineNumberReader.getLineNumber();
                    if (lineNum == 0) {
                        return true;
                    }

                    final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors() + 1;
                    final int chunckLineNum = (int) Math.ceil(lineNum / (float) CORE_POOL_SIZE);
                    List<Integer> chunckEndPositions = new ArrayList<>();

                    reader = new FileReader(filePath);
                    BufferedReader bReader = new BufferedReader(reader);
                    String s;
                    StringBuilder sBuilder = new StringBuilder();
                    int lineCount = 0;

                    while ((s = bReader.readLine()) != null) {
                        do {
                            ComponentName componentName = ComponentName.unflattenFromString(s);
                            if (componentName != null) {
                                try {
                                    int enable = pm.getComponentEnabledSetting(componentName);
                                    if (checkedItem == 0 && enable == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) { //has disabled
                                        break;
                                    } else if (checkedItem != 0 && enable == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {  //has enabled
                                        break;
                                    } else if (enable == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
                                        //TODO:包内className不存在，这里不会报错，TMD
                                    }
                                } catch (Exception e) {     //只有包名不存在会报：java.lang.IllegalArgumentException: Unknown component: ComponentInfo{com.zhihu.android/com.avos.avoscloud.PushService}
                                    if (BuildConfig.DEBUG) {
                                        e.printStackTrace();
                                    }
                                    break;
                                }
                            } else {
                                break;
                            }

                            int newState = checkedItem == 0 ?
                                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

                            try {
                                ShizukuPackageManagerV26.setComponentEnabledSetting(
                                        componentName,
                                        newState,
                                        PackageManager.DONT_KILL_APP,
                                        0);
                            } catch (Exception e) {
                                Log.e(TAG, "shizuku restore error", e);
                                errorType = 3;
                                return false;
                            }
                        } while (false);

                        lineCount++;
                        if (lineCount == chunckLineNum) {
                            chunckEndPositions.add(sBuilder.length());
                            lineCount = 0;
                        }
                    }
                    bReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    hideProgressDialog(false, false);
                } finally {
                    if (lineNumberReader != null) {
                        try {
                            lineNumberReader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                return true;
            }
        };
        shizukuRestoreTask.execute(data);


    }

    private void restoreForProByPm(Intent data) {
        final int checkedItem = data.getIntExtra("checkedItem", 0);
//        final String filePath = data.getStringExtra("path");
        final String filePath;
        if (BuildConfig.isFree) {
            filePath = BACKUP_FILE_DIR;
        } else {
            filePath = data.getStringExtra("path");
        }
        showProgressDialog();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            LineNumberReader lineNumberReader = null;
            FileReader reader = null;
            try {
                reader = new FileReader(filePath);

                lineNumberReader = new LineNumberReader(reader);
                lineNumberReader.skip(Long.MAX_VALUE);
                final int lineNum = lineNumberReader.getLineNumber();
                if (lineNum == 0) {
                    hideProgressDialog(false, true);
                    return;
                }

                final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors() + 1;
                final int chunckLineNum = (int) Math.ceil(lineNum / (float) CORE_POOL_SIZE);
//                        int[] chunckEndPositions = new int[CORE_POOL_SIZE - 1];
                List<Integer> chunckEndPositions = new ArrayList<>();

                reader = new FileReader(filePath);
                BufferedReader bReader = new BufferedReader(reader);
                String s;
                StringBuilder sBuilder = new StringBuilder();
                int lineCount = 0;
//                        int index = 0;
                while ((s = bReader.readLine()) != null) {
                    do {
                        String[] strs = s.trim().split(" ");
                        //支持组件类型
                        if (strs.length > 1) {
                            s = strs[0].trim();
                        }
                        ComponentName componentName = ComponentName.unflattenFromString(s);
                        if (componentName != null) {
                            try {
                                int enable = pm.getComponentEnabledSetting(componentName);
                                if (checkedItem == 0 && enable == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) { //has disabled
                                    break;
                                } else if (checkedItem != 0 && enable == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {  //has enabled
                                    break;
                                } else if (enable == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
                                    //TODO:包内className不存在，这里不会报错，TMD
                                }
                            } catch (Exception e) {     //只有包名不存在会报：java.lang.IllegalArgumentException: Unknown component: ComponentInfo{com.zhihu.android/com.avos.avoscloud.PushService}
                                if (BuildConfig.DEBUG) {
                                    e.printStackTrace();
                                }
                                break;
                            }
                        } else {
                            break;
                        }
                        sBuilder.append("pm");
                        sBuilder.append(checkedItem == 0 ? " disable " : " enable ");
                        sBuilder.append(s);
                        sBuilder.append("\n");
                    } while (false);

                    lineCount++;
                    if (lineCount == chunckLineNum) {
                        chunckEndPositions.add(sBuilder.length());
//                                Log.d(AboutFragment.class.getName(), "chunck:" + index + ":" + sBuilder.length());
                        lineCount = 0;
                    }
                }
                bReader.close();

                int count = Math.min(CORE_POOL_SIZE, chunckEndPositions.size());
                paralTaskCount = new AtomicInteger(count);
                myRestoreTasks = new MyRestoreTask[count];
//                        Log.d(AboutFragment.class.getName(), "backup sum:" + count);
                for (int i = 0; i < count; i++) {
                    myRestoreTasks[i] = new MyRestoreTask();
                    if (i == 0) {
                        myRestoreTasks[i].executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, sBuilder.substring(0, chunckEndPositions.get(0)));
                    } else if (i == count - 1) {
                        myRestoreTasks[i].executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, sBuilder.substring(chunckEndPositions.get(i - 1), sBuilder.length()));
                    } else {
                        myRestoreTasks[i].executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, sBuilder.substring(chunckEndPositions.get(i - 1), chunckEndPositions.get(i)));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                hideProgressDialog(false, false);
            } finally {
                if (lineNumberReader != null) {
                    try {
                        lineNumberReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        } /*else {
            myRestoreTask = new AsyncTask<Void, Void, Boolean>() {
                private boolean handleBefore11() {
                    File file = new File(filePath);
                    FileReader reader = null;
                    try {
                        reader = new FileReader(file);
                        BufferedReader bReader = new BufferedReader(reader);
                        String s;
//								StringBuilder builder = new StringBuilder(
//										"export LD_LIBRARY_PATH=/vendor/lib:/system/lib");
//								builder.append("\n");
                        StringBuilder builder = new StringBuilder();
                        while ((s = bReader.readLine()) != null) {
                            do {
                                ComponentName componentName = ComponentName.unflattenFromString(s);
                                if (componentName != null) {
                                    try {
                                        int enable = pm.getComponentEnabledSetting(componentName);
                                        Utils.debug(componentName.flattenToString() + "   , enable: " + enable);
                                        if (checkedItem == 0 && enable == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) { //has disabled
                                            break;
                                        } else if (checkedItem != 0 && enable == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {  //has enabled
                                            break;
                                        } else if (enable == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
                                            //className在其它保重
                                        }
                                    } catch (Exception e) {     //只有包名不存在会报：java.lang.IllegalArgumentException: Unknown component: ComponentInfo{com.zhihu.android/com.avos.avoscloud.PushService}
                                        if (BuildConfig.DEBUG) {
                                            e.printStackTrace();
                                        }
                                        break;
                                    }
                                } else {
                                    break;
                                }
                                builder.append("pm");
                                builder.append(checkedItem == 0 ? " disable " : " enable ");
                                builder.append(s);
                                builder.append("\n");
                            } while (false);
                        }
                        bReader.close();
//                        return Utils.runRootCommand(builder.toString());
                        return restoreMultiComponents(builder.toString()); //没有测试过，不知道11以下有没有问题
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    } finally {
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    super.onPostExecute(result);
                    hideProgressDialog(false, result);
                }

                @Override
                protected Boolean doInBackground(Void... arg0) {
                    return handleBefore11();
                }
            };
            myRestoreTask.execute();

        }*/
    }

    private class MyBackupTask extends AsyncTask<ApplicationInfo, Void, Boolean> {
        @Override
        protected void onCancelled() {
            super.onCancelled();
            paralTaskCount.decrementAndGet();
        }

        @Override
        protected Boolean doInBackground(ApplicationInfo... params) {
            StringBuilder sb = new StringBuilder();
            for (ApplicationInfo aInfo : params) {
                if (isCancelled()) {
                    return false;
                }

                if (aInfo == null) {
                    continue;
                }

                boolean isSystem = (aInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0;
                if (isOnly3rd && isSystem) {
                    continue;
                }

                try {
                    Context targetContext = mContext.createPackageContext(aInfo.packageName, 0);
                    AssetManager assetManager = targetContext.getAssets();

                    Method addAssetPath = AssetManager.class.getMethod("addAssetPath", String.class);
                    String sourceDir = pm.getApplicationInfo(aInfo.packageName, 0).sourceDir;
                    int cookie = (int) addAssetPath.invoke(assetManager, sourceDir);
                    XmlResourceParser parser = assetManager.openXmlResourceParser(cookie, "AndroidManifest.xml");
                    Resources resources = new Resources(assetManager, targetContext.getResources().getDisplayMetrics(), null);

                    int type;
                    while ((type = parser.next()) != XmlResourceParser.END_DOCUMENT) {
                        if (type == XmlResourceParser.START_TAG) {
                            String cName = parser.getName();
                            if ((cName.equals("service") && checkedItems[0])
                                    || (cName.equals("receiver") && checkedItems[1])
                                    || ((cName.equals("activity") || cName.equals("activity-alias")) && checkedItems[2])
                                    || (cName.equals("provider") && checkedItems[3])) {
                                String name = Utils.getAttributeValueByName(parser, resources, "name");

                                if (name == null) {
                                    continue;
                                }

                                if (!name.contains(".")) {
                                    name = aInfo.packageName + "." + name;
                                } else if (name.startsWith(".")) {
                                    name = aInfo.packageName + name;
                                }
//                                if ("com.tencent.mm".equals(aInfo.packageName)) {
//                                    System.out.println(name + " | " + parser.getAttributeValue(ANDROID_NAMESPACE, "name"));
//                                }
                                String backupType = (isSystem ? "s" : "3") + cName.charAt(0);
                                addComponent(sb, aInfo.packageName, name, backupType);
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            synchronized (AboutFragment.this) {
                try {
                    File file = new File(backupFilePath);
                    FileWriter writer = new FileWriter(file, true);
                    writer.write(sb.toString());
                    writer.flush();
                    writer.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return false;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                    //有可能多个同时进入onPostExecute
//				}finally {
//                    paralTaskCount.decrementAndGet();
                }
            }
            return true;
        }

        /**
         * @param type 两个字母，第一个字母（s或者3）,第二个字母（a,s,r,p)
         *             </br>例如： cn.wq.test/.MainActivity 3a
         */
        private void addComponent(StringBuilder sb, String packageName, String className,String type) {
            //默认禁用的忽略掉（不需要备份还原），只处理手动禁用的
            if (pm.getComponentEnabledSetting(new ComponentName(
                    packageName, className)) > PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                sb.append(packageName);
                sb.append("/");
                //备份的时候不需要处理$，推迟到还原的时候
//						sb.append(Matcher.quoteReplacement(className));
                sb.append(className);
                sb.append(" ").append(type);
                sb.append("\n");
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (result) {
                if (paralTaskCount.decrementAndGet() <= 0) {
                    hideProgressDialog(true, true);

                    if (!BuildConfig.isFree) {
                        renameBackupFile(false);

                    }
                }
            } else {
                hideProgressDialog(true, false);
                if (myBackupTasks != null) {
                    for (MyBackupTask task : myBackupTasks) {
                        if (task != null && !task.isCancelled()) {
                            task.cancel(true);
                        }
                    }
                }
            }
        }

    }

    private void renameBackupFile(boolean isIfw) {
        RenameDialogFragment renameDialog = RenameDialogFragment.newInstance(isIfw, backupFilePath);
        if (getActivity() != null && !getActivity().isFinishing()) {
            try {
                renameDialog.show(getActivity().getSupportFragmentManager(), "rename");
            } catch (Exception e) {
            }
        }
    }

    private void showProgressDialog() {
        showProgressDialog(false);
    }

    private void showProgressDialog(boolean cancelable) {
        if (dialog == null) {
            dialog = new CustomProgressDialogFragment();
            dialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
            dialog.setCancelable(cancelable);
        }
        dialog.show(getActivity().getSupportFragmentManager(), "dialog");
    }

    private void hideProgressDialog(boolean isBackup, boolean isSuccess, @StringRes int message) {
        if (dialog != null && getFragmentManager() != null) {
            dialog.dismissAllowingStateLoss();

            if (!isSuccess) {
                Toast toast = Toast.makeText(
                        mContext,
                        message != 0 ? message : R.string.operation_failed,
                        Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            } else if (!isBackup || BuildConfig.isFree) {  //free 需要
                Toast toast = Toast.makeText(
                        mContext,
                        message != 0 ? message : R.string.operation_done,
                        Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }

//            Toast toast = Toast.makeText(
//                    mActivity,
//                    isSuccess ? (isBackup?getString(R.string.backup_done,backupFilePath):getString(R.string.operation_done))
//                           :getString(R.string.operation_failed),
//					Toast.LENGTH_LONG);
//			toast.setGravity(Gravity.CENTER, 0, 0);
//			toast.show();

            if (isBackup && BuildConfig.isFree) {
                restoreBtn.setEnabled(true);
                if (new File(BACKUP_FILE_DIR).exists()) {
                    backupInfoTv.setText(BACKUP_FILE_DIR);
                    backupInfoTv.setVisibility(View.VISIBLE);
                } else {
                    backupInfoTv.setVisibility(View.GONE);
                }
            }
        }
    }

    private void hideProgressDialog(boolean isBackup, boolean isSuccess) {
        hideProgressDialog(isBackup, isSuccess, 0);
    }

    /**
     * 还原组件
     *
     * @param cmd 可能为空，可能是多行组件
     * @return 执行是否成功，空也认为成功，执行多行时只有最后一个不存在时报错返回-2但是依然认为是成功
     */
    private boolean restoreMultiComponents(String cmd) {
        if (cmd != null && TextUtils.isEmpty(cmd.trim())) { //pro中会先判断禁用状态，如果disable已经disabled的就直接跳过
            return true;
        }
        String command = Matcher.quoteReplacement(cmd);
//            return Shell.SU.run(command) != null;
        if (Shell.SU.available()) {
            int r = Utils.runMultiPmDisableCommand(command);
            if (r == -2) {  //忽略组件不存在的报错
                return true;
            }
            return r > 0;
        } else {
            return false;
        }
    }

    private class MyRestoreTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
//			return Utils.runRootCommand(params[0]);
//            if (params[0] != null && TextUtils.isEmpty(params[0].trim())) { //pro中会先判断禁用状态，如果disable已经disabled的就直接跳过
//                return true;
//            }
//            String command = Matcher.quoteReplacement(params[0]);
////            return Shell.SU.run(command) != null;
//            if (Shell.SU.available()) {
//                int r = Utils.runMultiPmDisableCommand(command);
//                if (r == -2) {  //忽略组件不存在的报错
//                    return true;
//                }
//                return r > 0;
//            }else{
//                return false;
//            }
            return restoreMultiComponents(params[0]);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            paralTaskCount.decrementAndGet();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            int remain = paralTaskCount.decrementAndGet();
            if (result) {
                if (remain <= 0) {
                    hideProgressDialog(false, true);
                }
            } else {
                hideProgressDialog(false, false);
                for (MyRestoreTask task : myRestoreTasks) {
                    if (task != null && !task.isCancelled()) {
                        task.cancel(true);
                    }
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();

        if (myRestoreTask != null) {
            myRestoreTask.cancel(true);
        }

        if (shizukuRestoreTask != null) {
            shizukuRestoreTask.cancel(true);
        }

        if (myBackupTasks != null) {
            for (MyBackupTask task : myBackupTasks) {
                if (task != null && !task.isCancelled()) {
                    task.cancel(true);
                }
            }
        }

        if (myRestoreTasks != null) {
            for (MyRestoreTask task : myRestoreTasks) {
                if (task != null && !task.isCancelled()) {
                    task.cancel(true);
                }
            }
        }
    }

    private interface OnSpinnerItemSelectListener {
        void onItemSelected(int defaultPos, int newPos);
    }

}