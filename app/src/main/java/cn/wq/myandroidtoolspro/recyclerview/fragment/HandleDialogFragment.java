package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.usage.StorageStats;
import android.app.usage.StorageStatsManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.mars.xlog.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import cn.wq.myandroidtoolspro.BuildConfig;
import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.DBHelper;
import cn.wq.myandroidtoolspro.helper.ShizukuUtil;
import cn.wq.myandroidtoolspro.helper.Utils;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import moe.shizuku.api.ShizukuPackageManagerV26;

public class HandleDialogFragment extends DialogFragment {
    private static final String TAG = "HandleDialogFragment";
    private boolean isDisabled;
    private String packageNameString;
    private String labelString;
    private Context mContext;
    private String sourceDir;
    private final LoadUsageHandler mHandler = new LoadUsageHandler();
    private TextView dataTv, cacheTv, apkPath, packageNameTitle, version;
    private Button clearDataBtn, clearCacheBtn;
    private View storageParent;
    private static final int REQUEST_CODE_UNINSTALL = 1001;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private class PkgSizeObserver extends IPackageStatsObserver.Stub {
        public void onGetStatsCompleted(PackageStats pStats, boolean succeeded) {
            // android o(8.0会返回 false 和 null)
            // http://blog.leanote.com/post/svenpaper/Android-O-%E7%89%88%E6%9C%AC%E5%AE%89%E8%A3%85%E5%8C%85%E5%A4%A7%E5%B0%8F%E8%8E%B7%E5%8F%96%E5%A4%B1%E8%B4%A5%E9%97%AE%E9%A2%98%E5%88%86%E6%9E%90%E5%8F%8A%E8%A7%A3%E5%86%B3
            if (!succeeded || pStats == null) {
                Utils.debug("package size return:" + succeeded + "," + (pStats == null));
                return;
            }
            Message msg = mHandler.obtainMessage(1);
            Bundle data = new Bundle();
//                    data.putLong("codeSize",pStats.codeSize);
            data.putLong("dataSize", pStats.dataSize);
            data.putLong("cacheSize", pStats.cacheSize);
            msg.setData(data);
            mHandler.sendMessage(msg);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (compositeDisposable != null) {
            compositeDisposable.clear();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mContext = getContext();

        Bundle args = getArguments();
        packageNameString = args.getString("packageName");
        isDisabled = args.getBoolean("isDisabled");
        labelString = args.getString("label");

        LayoutInflater inflater = LayoutInflater.from(mContext);
        View dialog_title = inflater.inflate(R.layout.dialog_app_manage_handle_title, null);
        View dialog_content = inflater.inflate(R.layout.dialog_app_manage_handle, null);

        ImageView icon = (ImageView) dialog_title.findViewById(R.id.icon);
        TextView label = (TextView) dialog_title.findViewById(R.id.label);
        version = (TextView) dialog_title.findViewById(R.id.version);
        ImageView info = (ImageView) dialog_title.findViewById(R.id.info);
        TextView last_update_time = (TextView) dialog_content.findViewById(R.id.last_update_time);
        TextView packageName = (TextView) dialog_content.findViewById(R.id.package_name);
        apkPath = (TextView) dialog_content.findViewById(R.id.apk_path);
        packageNameTitle = (TextView) dialog_content.findViewById(R.id.package_name_title);

        Utils.loadApkIcon(this, packageNameString, icon);

        Disposable disposable = Observable.create(new ObservableOnSubscribe<PkgInfo>() {
            @Override
            public void subscribe(ObservableEmitter<PkgInfo> emitter) throws Exception {
                PackageManager pm = mContext.getPackageManager();
                PkgInfo pkgInfo = new PkgInfo();
                ApplicationInfo aInfo = pm.getApplicationInfo(packageNameString, 0);
                pkgInfo.sourceDir = aInfo.sourceDir;
                pkgInfo.isSystem = (aInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0;

                PackageInfo pInfo = pm.getPackageInfo(packageNameString, 0);
                if (pInfo != null) {
                    long verionCode;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        verionCode = pInfo.getLongVersionCode();
                    } else {
                        verionCode = pInfo.versionCode;
                    }
                    pkgInfo.version = verionCode + " (" + pInfo.versionName + ")";
                }
                emitter.onNext(pkgInfo);
                emitter.onComplete();
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<PkgInfo>() {
                    @Override
                    public void accept(PkgInfo pkgInfo) {
                        apkPath.setText(pkgInfo.sourceDir);
                        sourceDir = pkgInfo.sourceDir;
                        if (pkgInfo.isSystem) {
                            packageNameTitle.setText(R.string.package_name_system);
                        } else {
                            packageNameTitle.setText(R.string.package_name_third_party);
                        }
                        version.setText(pkgInfo.version);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
        compositeDisposable.add(disposable);

        PackageManager pm = mContext.getPackageManager();

        label.setText(labelString);
        packageName.setText(packageNameString);
        last_update_time.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(args.getLong("time"))));

        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toAppDetailInSystemSettings(packageNameString);
            }
        });

        final Button disableBtn = (Button) dialog_content.findViewById(R.id.disable);
        Button all_infos = (Button) dialog_content.findViewById(R.id.all_infos);
        Button uninstall = (Button) dialog_content.findViewById(R.id.uninstall);
        Button openBtn = (Button) dialog_content.findViewById(R.id.open);

        disableBtn.setText(isDisabled ? R.string.enable : R.string.disable);
        disableBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                disable(false);
            }
        });

        all_infos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Fragment targetFragment = getTargetFragment();
                if (targetFragment instanceof AppForManageRecyclerFragment) {
                    //currentFocus是SearchView$SearchAutoComplete，不是SearchView，但是只有后者clearFocus才有效
                    ((AppForManageRecyclerFragment) targetFragment).clearSearchViewFocus();
                }

                dismissAllowingStateLoss();

                Bundle args = new Bundle();
                args.putString("packageName", packageNameString);
                args.putString("title", labelString);
                args.putBoolean("part", true);

                AppInfoForManageFragment2 fragment = AppInfoForManageFragment2.newInstance(args);

                AppCompatActivity activity = (AppCompatActivity) getActivity();
                FragmentTransaction ft = activity.getSupportFragmentManager()
                        .beginTransaction();
                ft.replace(R.id.content, fragment);
                ft.setTransition(FragmentTransaction.TRANSIT_NONE);
                ft.addToBackStack(null);
                ft.commit();

            }
        });

        if (BuildConfig.isFree) {
            uninstall.setVisibility(View.GONE);
            openBtn.setVisibility(View.GONE);
        } else {
            uninstall.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EnsureDialogFragment dialog = EnsureDialogFragment.newInstance(getString(R.string.warning), getString(R.string.uninstall_message, labelString));
                    dialog.setTargetFragment(HandleDialogFragment.this, REQUEST_CODE_UNINSTALL);
//                    dialog.show(getChildFragmentManager(), "uninstall");
                    dialog.show(getFragmentManager(), "uninstall");
                }
            });


            if (!isDisabled) {
                final Intent startIntent = getContext().getPackageManager().getLaunchIntentForPackage(packageNameString);
                if (startIntent != null) {
                    openBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(startIntent);
                        }
                    });
                } else {
                    openBtn.setEnabled(false);
                }
            } else {
                openBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        disable(true);
                    }
                });
            }

        }


        /////////////////////////////////////////////
        /// get cache
        /////////////////////////////////////////////
        dataTv = (TextView) dialog_content.findViewById(R.id.data_size);
        cacheTv = (TextView) dialog_content.findViewById(R.id.cache_size);
        storageParent = dialog_content.findViewById(R.id.storage_parent);
        clearDataBtn = (Button) dialog_content.findViewById(R.id.clear_data);
        clearCacheBtn = (Button) dialog_content.findViewById(R.id.clear_cache);

//      handlerThread = new HandlerThread("calculate", Process.THREAD_PRIORITY_BACKGROUND);
//      handlerThread.start();
//      mHandler = new LoadUsageHandler(handlerThread.getLooper());

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {  //8.0
            try {
                Method getPackageSizeInfo = pm.getClass()
                        .getMethod("getPackageSizeInfo", String.class,
                                IPackageStatsObserver.class);
                getPackageSizeInfo.invoke(pm, packageNameString,
                        new PkgSizeObserver());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            loadUsageDataForO();
        }

        if (BuildConfig.isFree) {
            clearCacheBtn.setVisibility(View.GONE);
            clearDataBtn.setVisibility(View.GONE);
        }


        /////////////////////////////////////////////
        /////////////////////////////////////////////
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setCustomTitle(dialog_title)
                .setView(dialog_content);
        return builder.create();
    }

    //android 8.0+之后获取使用大小
    //https://stackoverflow.com/questions/43472398/how-to-use-storagestatsmanager-querystatsforpackage-on-android-o
    @TargetApi(Build.VERSION_CODES.O)
    private void loadUsageDataForO() {
        Disposable disposable = Observable.create(new ObservableOnSubscribe<DataModel>() {
            @Override
            public void subscribe(ObservableEmitter<DataModel> emitter) {
                StorageStatsManager storageStatsManager = (StorageStatsManager) getContext().getSystemService(Context.STORAGE_STATS_SERVICE);
                StorageManager storageManager = (StorageManager) getContext().getSystemService(Context.STORAGE_SERVICE);
                List<StorageVolume> volumes = storageManager.getStorageVolumes();
                long cacheBytes = 0;
                long dataBytes = 0;
                for (StorageVolume vol : volumes) {
                    try {
                        //sony 有问题,https://play.google.com/apps/publish/?hl=zh-CN&account=7732266320238141460#AndroidMetricsErrorsPlace:p=cn.wq.myandroidtools&appVersion=PRODUCTION&lastReportedRange=LAST_24_HRS&showHidden=true&clusterName=apps/cn.wq.myandroidtools/clusters/f6372709&detailsSpan=1
                        UUID uuid = vol.getUuid() == null ? StorageManager.UUID_DEFAULT : UUID.fromString(vol.getUuid());
//                Log.d("AppLog", "storage:" + uuid + " : " + vol.getDescription(getContext()) + " : " + vol.getState());
//                Log.d("AppLog", "getFreeBytes:" + Formatter.formatShortFileSize(getContext(), storageStatsManager.getFreeBytes(uuid)));
//                Log.d("AppLog", "getTotalBytes:" + Formatter.formatShortFileSize(getContext(), storageStatsManager.getTotalBytes(uuid)));
//                Log.d("AppLog", "storage stats for app of package name:" + packageNameString + " : ");

                        final StorageStats storageStats = storageStatsManager.queryStatsForPackage(uuid, packageNameString, Process.myUserHandle());
                        cacheBytes += storageStats.getCacheBytes();
                        dataBytes += storageStats.getDataBytes();
//                Log.d("AppLog", "getAppBytes:" + Formatter.formatShortFileSize(getContext(), storageStats.getAppBytes()) +
//                        " getCacheBytes:" + Formatter.formatShortFileSize(getContext(), storageStats.getCacheBytes()) +
//                        " getDataBytes:" + Formatter.formatShortFileSize(getContext(), storageStats.getDataBytes()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                emitter.onNext(new DataModel(cacheBytes, dataBytes));
                emitter.onComplete();
            }
        })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<DataModel>() {
                    @Override
                    public void accept(DataModel dataModel) {
                        updateUsageViews(dataModel.dataBytes, dataModel.cacheBytes);
                    }
                });
        compositeDisposable.add(disposable);
    }

    class DataModel {
        long cacheBytes = 0;
        long dataBytes = 0;

        DataModel(long cacheBytes, long dataBytes) {
            this.cacheBytes = cacheBytes;
            this.dataBytes = dataBytes;
        }
    }

    class PkgInfo {
        String sourceDir, version;
        boolean isSystem;
    }

    private void toAppDetailInSystemSettings(String packageName) {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= 9) {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri =
//                    Uri.fromParts("package", packageName, null)
                    Uri.parse("package:" + packageName);
            intent.setData(uri);
        } else {
            String appPkgName = Build.VERSION.SDK_INT == 8 ?
                    "pkg" : "com.android.settings.ApplicationPkgName";
            intent.setAction(Intent.ACTION_VIEW);
            intent.setClassName("com.android.settings",
                    "com.android.settings.InstalledAppDetails");
            intent.putExtra(appPkgName, packageName);
        }
        startActivity(intent);
    }

    private void uninstall() {
        new AsyncTask<String, Void, Boolean>() {
            private byte[] bitmapToBytes(Bitmap bitmap) {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bout);
                return bout.toByteArray();
            }

            @Override
            protected Boolean doInBackground(String... param) {
                if (sourceDir == null) {
                    return Utils.runRootCommand("pm uninstall " + packageNameString);
                }

                boolean result;
                if (sourceDir.startsWith("/data/app")) {
                    result = Utils.runRootCommand("pm uninstall " + packageNameString);
                    if (result) {
                        SQLiteDatabase db = DBHelper.getInstance(mContext).getWritableDatabase();
                        db.delete("app_manage", "packageName=?", new String[]{packageNameString});
                    }
                } else {
//                            String apkDir;
//                            int splashPos=sourceDir.lastIndexOf("/");
//                            if(splashPos!=-1) {
//                                apkDir = sourceDir.substring(0, splashPos);
//                            }else {
//                                apkDir=sourceDir;
//                            }

//                    String backupPah = Utils.getUninstallBackupDir() + File.separator + UUID.randomUUID() + ".backup";

                    String backupPah = Utils.getUninstallBackupDir()
//                            + File.separator
//                            + sourceDir.substring(sourceDir.lastIndexOf("/") + 1, sourceDir.length() - 3)
//                            + System.currentTimeMillis()
                            ;
                    byte[] iconBytes = null;
                    try {
                        Drawable drawable = mContext.getPackageManager().getApplicationIcon(packageNameString);
                        if (drawable instanceof BitmapDrawable) {
//                            ByteArrayOutputStream bout = new ByteArrayOutputStream();
//                            ((BitmapDrawable) drawable).getBitmap().compress(Bitmap.CompressFormat.JPEG, 100, bout);
//                            iconBytes = bout.toByteArray();
                            iconBytes = bitmapToBytes(((BitmapDrawable) drawable).getBitmap());
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable instanceof AdaptiveIconDrawable) {
                            //https://stackoverflow.com/a/46448831/1263423
                            //android 0 adaptive launch icon
                            AdaptiveIconDrawable aiDrawable = (AdaptiveIconDrawable) drawable;
                            Drawable backgroudDr = aiDrawable.getBackground();
                            Drawable foregroundDr = aiDrawable.getForeground();
                            Drawable[] drs = new Drawable[]{backgroudDr, foregroundDr};

                            LayerDrawable layerDrawable = new LayerDrawable(drs);
                            Bitmap bitmap = Bitmap.createBitmap(layerDrawable.getIntrinsicWidth(),
                                    layerDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(bitmap);
                            layerDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                            layerDrawable.draw(canvas);
                            iconBytes = bitmapToBytes(bitmap);
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }

                    //注意UninstalledRecyclerListFragment.java中的还原
                    String unmountCommand = Utils.getUnmountSystemCommand(mContext);
                    if (unmountCommand == null) {
                        return null;
                    }

                    if (!Utils.runRootCommand("cd " + backupPah)) {
                        //有的手机/storage/emulated/0在 shell 里面不能访问
                        backupPah = Utils.getUninstallBackupDirInShell();
                        if (!Utils.runRootCommand("cd " + backupPah)) {
                            com.tencent.mars.xlog.Log.e(TAG, "err: cd " + backupPah);
                        }
                    }

                    backupPah = backupPah
                            + File.separator
                            + sourceDir.substring(sourceDir.lastIndexOf("/") + 1, sourceDir.length() - 3)
                            + System.currentTimeMillis();

                    //彻底删除时才删掉整个目录
                    result = Utils.runRootCommand(
                            "cat " + sourceDir + " > " + backupPah,
//                            mountCommand,
//                                    "rm -rf "+apkDir,
//                                    "rm -rf "+"/data/data/"+packageNameString
                            "rm -rf " + sourceDir
                    );

//                    if (Build.VERSION.SDK_INT >= 24) {
//                        Utils.runRootCommand("pm uninstall " + packageNameString);
//                    }else{
//                        Utils.runRootCommand(
//                                "pm uninstall " + packageNameString,
//                                "mount -o remount,ro /system"
//                        );
//                    }
                    Utils.runRootCommand(
                            "pm uninstall " + packageNameString,
                            unmountCommand
                    );

                    if (result) {
                        SQLiteDatabase db = DBHelper.getInstance(mContext).getWritableDatabase();
                        ContentValues cv = new ContentValues();
                        cv.put("packageName", packageNameString);
                        cv.put("appName", labelString);
                        cv.put("sourcePath", sourceDir);
                        cv.put("backupPath", backupPah);
                        if (iconBytes == null) {
                            cv.putNull("icon");
                        } else {
                            cv.put("icon", iconBytes);
                        }

                        db.insertWithOnConflict("uninstalled", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                    }
                }

                return result;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if (result != null && result) {
                    getTargetFragment().onActivityResult(0, Activity.RESULT_FIRST_USER, null);
                } else {
                    Toast.makeText(mContext, R.string.operation_failed, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                dismissAllowingStateLoss();
            }
        }.execute();
    }

    /**
     * @param open 是否是打开app（当打开禁用的app时需要先enable）
     */
    private void disable(final boolean open) {
        if (Utils.isPmByShizuku(mContext)) {
            ShizukuUtil.getIsAuthorised(new Consumer() {
                @Override
                public void accept(Object o) throws Exception {
                    handleDisable(open, true);
                }
            }, new Consumer() {
                @Override
                public void accept(Object o) throws Exception {
                    handleDisable(open, false);
                }
            });
        } else {
            handleDisable(open, false);
        }
    }

    //disable实际操作
    private void handleDisable(final boolean open, final boolean isByShizuku) {
        new AsyncTask<Boolean, Void, Boolean>() {
            private boolean handleByRootCommand() {
                return Utils.runRootCommand("pm " + (isDisabled ? "enable " : "disable ") + packageNameString);
            }

            private boolean handleByshizuku() {
                try {
                    ShizukuPackageManagerV26.setApplicationEnabledSetting(
                            packageNameString,
                            isDisabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            0,
                            0,
                            mContext.getPackageName()
                    );
                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "disable app by shizuku error:", e);
                    Utils.err(TAG, "disable app by shizuku error:", e);
                    return false;
                }
            }

            @Override
            protected Boolean doInBackground(Boolean... param) {
                boolean result;
                if (param != null && param.length > 0 && param[0]) {
                    result = handleByshizuku();
                    if (!result) {
                        //shizuku 失败了还是尝试root方式
                        result = handleByRootCommand();
                    }
                } else {
                    result = handleByRootCommand();
                }
                if (result) {
                    SQLiteDatabase db = DBHelper.getInstance(mContext).getWritableDatabase();
                    db.beginTransaction();
                    ContentValues cv1 = new ContentValues();
                    cv1.put("enabled", isDisabled ? 1 : 0);
                    db.update("app_manage", cv1, "packageName=?", new String[]{packageNameString});

                    ContentValues cv2 = new ContentValues();
                    cv2.put(isDisabled ? "e_time" : "d_time", System.currentTimeMillis());
                    cv2.put("packageName", packageNameString);
//                    db.update("app_history", cv2,"packageName=?", new String[]{packageNameString});
                    db.insertWithOnConflict("app_history", null, cv2, SQLiteDatabase.CONFLICT_REPLACE);


                    db.setTransactionSuccessful();
                    db.endTransaction();
                }
                return result;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if (result) {
                    if (isDisabled && open) {
                        Intent data = new Intent();
                        data.putExtra("isDisabled", isDisabled);
                        data.putExtra("packageName", packageNameString);
                        getTargetFragment().onActivityResult(0, Activity.RESULT_OK, data);
                    } else {
                        getTargetFragment().onActivityResult(0, Activity.RESULT_OK, null);
                    }
                } else {
                    Toast.makeText(mContext, R.string.failed_to_gain_root, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                dismissAllowingStateLoss();
            }
        }.execute(isByShizuku);
    }

    private void clearData() {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... param) {
                return Utils.runRootCommand("pm clear " + packageNameString);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if (result) {
                    dataTv.setText("0.00B");
                    cacheTv.setText("0.00B");
                    clearDataBtn.setEnabled(false);
                    clearCacheBtn.setEnabled(false);
                } else {
                    Toast.makeText(mContext, R.string.failed_to_gain_root, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }
        }.execute();
    }

    private void clearCache() {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... param) {
                String externalPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + packageNameString + "/cache";
                File externalDir = new File(externalPath);
                if (externalDir.exists()) {
                    if (!externalDir.delete()) {
                        Utils.runRootCommand("rm -r " + externalPath);
                    }
                }

                String internalPath = "/data/data/" + packageNameString + "/cache";
                return Utils.runRootCommand("rm -r " + internalPath);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if (result != null && result) {
                    cacheTv.setText("0.00B");
                    clearCacheBtn.setEnabled(false);
                } else {
                    Toast.makeText(mContext, R.string.failed_to_gain_root, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }
        }.execute();
    }

    private class LoadUsageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                storageParent.setVisibility(View.VISIBLE);
                Bundle data = msg.getData();
                long dataSize = data.getLong("dataSize");
                long cacheSize = data.getLong("cacheSize");

                updateUsageViews(dataSize, cacheSize);
            }
        }
    }

    //更新 data、cache
    private void updateUsageViews(long dataSize, long cacheSize) {
        dataTv.setText(Formatter.formatFileSize(mContext, dataSize));
        cacheTv.setText(Formatter.formatFileSize(mContext, cacheSize));
        if (dataSize == 0L) {
            clearDataBtn.setEnabled(false);
        } else {
            clearDataBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clearData();
                }
            });
        }
        if (cacheSize == 0L) {
            clearCacheBtn.setEnabled(false);
        } else {
            clearCacheBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clearCache();
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_UNINSTALL) {
            if (resultCode == Activity.RESULT_OK) {
                //需要备份卸载的app到sd卡
                if (sourceDir != null && !sourceDir.startsWith("/data/app")) {
                    if (!Utils.checkSDcard(mContext)) {
                        return;
                    }
                }
                uninstall();
            }
        }
    }
}