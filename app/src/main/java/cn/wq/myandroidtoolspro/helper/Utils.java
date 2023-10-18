package cn.wq.myandroidtoolspro.helper;

import android.Manifest;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;

import java.io.File;
import java.lang.reflect.Method;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.wq.myandroidtoolspro.BaseActivity;
import cn.wq.myandroidtoolspro.BuildConfig;
import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.glide.ApkIconFetcher;
import cn.wq.myandroidtoolspro.model.AppEntry;
import cn.wq.myandroidtoolspro.model.ComponentModel;
import eu.chainfire.libsuperuser.Shell;
import moe.shizuku.api.ShizukuActivityManagerV26;
import moe.shizuku.api.ShizukuClient;

public class Utils {
    private static final String TAG = "Utils";
    //	public final static String ACTION_RECEIVER_CHANGED="cn.wq.myandroidtoolspro.receiver_changed";
//	private final static String EXTERNAL_STORAGE = "EXTERNAL_STORAGE";
//	public final static String tempDBPath=System.getenv(EXTERNAL_STORAGE)
//			+ "/temp2.db";
//	public final static String tempSPfrefsPath=System.getenv(EXTERNAL_STORAGE)
//			+ "/temp2.xml";
//	public final static String BACKUP_FILE_DIR=Environment.getExternalStorageDirectory()+"/myandroidtoolspro_backup.txt";
    public final static String ACTION_SORT_CHANGE = "cn.wq.myandroidtoolspro.action_sort_change";
    //	public final static String ACTION_APP_CHANGE="cn.wq.myandroidtoolspro.action_app_change";
    public final static String ACTION_APP_SORT = "cn.wq.myandroidtoolspro.action_app_sort";
    public final static String ACTION_RECEIVER_FINISH = "cn.wq.myandroidtoolspro.action_receiver_finish";
    private final static String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";

    public final static String ACTION_ACTIVITY_FULL_NAME = "ACTION_ACTIVITY_FULL_NAME";
    public final static String ACTION_ACTIVITY_MOVE = "ACTION_ACTIVITY_MOVE";
    public final static String ACTION_ACTIVITY_TEXTSIZE = "ACTION_ACTIVITY_TEXTSIZE";

    private final static String SERVICE_START_MARK = "  * ServiceRecord{";
    //    private final static String SERVICE_START_MARK_PACKAGE = "    packageName=";
//    private final static String SERVICE_START_MARK_PROCESS = "    processName=";
    private final static String SERVICE_START_MARK_APP = "    app=";
    private final static Pattern SERVICE_RECORD_PATTERN = Pattern.compile("[\\w]+ [\\w]+ ([\\w.]+)/([\\w.]+)\\}");
    private final static Pattern SERVICE_APP_PATTERN = Pattern.compile(" +app=ProcessRecord\\{[\\w]+ ([\\d]+):[\\S]+");
    //adb 中 grep 只能用-E 支持多个搜索条件，而且不用转义;不能直接用|分隔
    //"dumpsys activity services | grep -E \"ServiceRecord{|processName=\"";
    private final static String COMMAND_GET_RUNNING_SERVICE_FOR_ALL = "dumpsys activity s | grep -E \"ServiceRecord{|app=\"";
    private final static String COMMAND_GET_RUNNING_SERVICE_FOR_SINGLE = "dumpsys activity s %s| grep -E \"ServiceRecord{|app=\"";

    public static String trimFor160(String name) {
        return name.replaceAll(String.valueOf((char) 160), "");
    }

    public static String getTempDBPath(Context context) {
        return context.getExternalCacheDir() + "/temp2.db";
    }

    public static String getTempDBWalPath(Context context) {
        return context.getExternalCacheDir() + "/temp2.db-wal";
    }

    public static String getTempSPfrefsPath(Context context) {
        return context.getExternalCacheDir() + "/temp2.xml";
    }

    /**
     * 从 attr 中获取颜色
     */
    public static @ColorInt int getColorFromAttr(Context context, @AttrRes int attr) {
        TypedValue value = new TypedValue();
        if(context.getTheme().resolveAttribute(attr, value, true)){
//            return value.data;    //不是Color是ColorStateList
            if (value.resourceId != 0) {
                return ContextCompat.getColor(context, value.resourceId);
            }
        }
        return ContextCompat.getColor(context, R.color.text_primary_black);
    }

//    @ColorRes
//    public static int getPrimaryTextColorRes(Context context) {
//        TypedValue typedValue = new TypedValue();
//        context.getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
//        return typedValue.resourceId;
//    }

//    private static Picasso mPicasso;
//
//    public static synchronized Picasso getPicassoInstance(Context context) {
//        if (mPicasso == null) {
//            final Context appContext = context.getApplicationContext();
//            mPicasso = new Picasso.Builder(appContext).addRequestHandler(new RequestHandler() {
//                @Override
//                public boolean canHandleRequest(Request data) {
//                    return "icon".equals(data.uri.getScheme());
//                }
//
//                @Override
//                public Result load(Request request, int networkPolicy){
//                    Drawable drawable = null;
//                    try {
//                        drawable = appContext.getPackageManager().getApplicationIcon(request.uri.toString().replace("icon:", ""));
//                    } catch (PackageManager.NameNotFoundException e) {
//                        e.printStackTrace();
//                    }
//
//                    if (drawable instanceof BitmapDrawable) {
//                        return new Result(((BitmapDrawable) drawable).getBitmap(), Picasso.LoadedFrom.DISK);
//                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable instanceof AdaptiveIconDrawable) {
//                        //https://stackoverflow.com/a/46448831/1263423
//                        //android 0 adaptive launch icon
//                        AdaptiveIconDrawable aiDrawable = (AdaptiveIconDrawable) drawable;
//                        Drawable backgroudDr = aiDrawable.getBackground();
//                        Drawable foregroundDr = aiDrawable.getForeground();
//                        Drawable[] drs = new Drawable[]{backgroudDr, foregroundDr};
//
//                        LayerDrawable layerDrawable = new LayerDrawable(drs);
//                        Bitmap bitmap = Bitmap.createBitmap(layerDrawable.getIntrinsicWidth(),
//                                layerDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
//                        Canvas canvas = new Canvas(bitmap);
//                        layerDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
//                        layerDrawable.draw(canvas);
//
//                        return new Result(bitmap, Picasso.LoadedFrom.DISK);
//                    }
//
//                    //default
//                    drawable = appContext.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
//                    return new Result(((BitmapDrawable) drawable).getBitmap(), Picasso.LoadedFrom.DISK);
//                }
//            }).build();
//        }
//        return mPicasso;
//    }

    public static String getAppLabel(@NonNull PackageManager pm, @NonNull ApplicationInfo aInfo) {
        try {
            CharSequence label = aInfo.loadLabel(pm);
            if (label != null) {
                return Utils.trimFor160(label.toString());
            }
        } catch (Exception e) {
            Utils.debug(TAG, "getAppLabel error," + aInfo.packageName + "," + e.toString());
        }

        return aInfo.packageName;
    }

    public static void loadApkIcon(Fragment fragment, String packageName, ImageView imageView) {
        Glide.with(fragment).load(ApkIconFetcher.PREFIX + packageName)
                .placeholder(android.R.drawable.sym_def_app_icon)
                .error(android.R.drawable.sym_def_app_icon).into(imageView);
    }
    public static void loadApkIcon(Context context, String packageName, ImageView imageView) {
        Glide.with(context).load(ApkIconFetcher.PREFIX + packageName)
                .placeholder(android.R.drawable.sym_def_app_icon)
                .error(android.R.drawable.sym_def_app_icon).into(imageView);
    }


    public static String getUninstallBackupDir() {
        File dir = new File(Environment.getExternalStorageDirectory() + File.separator + "MyAndroidTools"+File.separator+"uninstalled");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.getAbsolutePath();
    }

    public static String getUninstallBackupDirInShell() {
        File dir = new File(File.separator + "sdcard" + File.separator + "MyAndroidTools"+File.separator+"uninstalled");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.getAbsolutePath();
    }

    public static List<AppEntry> getAppsWithType(Context context, boolean isSystemApp,
                                                 int type) {
        switch (type) {
            case 0:
                return getAppsAsService(context, isSystemApp);
            case 1:
                return getAppsAsReceiver(context, isSystemApp);
            case 2:
                return getAppsAsActivity(context, isSystemApp);
            case 3:
                return getAppsAsProvider(context, isSystemApp);
            default:
                return null;
        }
    }

    public static List<AppEntry> getAppsAsService(Context context, boolean isSystemApp) {
        List<RunningServiceInfo> runningServiceInfos = getRunningServiceInfos(context, null);

        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> aInfos = pm.getInstalledApplications(0);
        List<AppEntry> result = new ArrayList<>();

        for (ApplicationInfo aInfo : aInfos) {
            if (aInfo == null) {
                continue;
            }
            if (isSystemApp ? (aInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                    : (aInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                List<ComponentModel> models = Utils.getComponentModels(context, aInfo.packageName, 0);
                if (models.size() == 0)
                    continue;

                AppEntry entry = new AppEntry();
                entry.totalNum = models.size();
                entry.label = getAppLabel(pm, aInfo);
                if (!Utils.isPmByIfw(context)) {
                    entry.disabledNum = Utils.getComponentDisabledNum(models, pm);
                }
                entry.runningNum = Utils.getRunningNum(aInfo.packageName,
                        runningServiceInfos);
                entry.packageName = aInfo.packageName;
                result.add(entry);
            }
        }

        aInfos.clear();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        final int sortType = sp.getInt("sort_service", 0);
        Locale locale = context.getResources().getConfiguration().locale;

        final Collator collator = Collator.getInstance(locale);
        Collections.sort(result, new Comparator<AppEntry>() {
            @Override
            public int compare(AppEntry left, AppEntry right) {
                if (sortType == 1
                        && left.runningNum != right.runningNum) {
                    return right.runningNum - left.runningNum;
                } else if (sortType == 2
                        && left.disabledNum != right.disabledNum) {
                    return right.disabledNum - left.disabledNum;
                }

                return collator.compare(left.label, right.label);
            }
        });

        return result;
    }

    private static List<AppEntry> getAppsAsReceiver(Context context, boolean isSystemApp) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> aInfos = pm.getInstalledApplications(0);
        List<AppEntry> result = new ArrayList<>();

        SQLiteDatabase db = DBHelper.getInstance(context).getWritableDatabase();
        db.beginTransaction();
        for (ApplicationInfo aInfo : aInfos) {
            if (aInfo == null) {
                continue;
            }
            if (isSystemApp ? (aInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                    : (aInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                List<ComponentModel> models = Utils.getComponentModels(context, aInfo.packageName, 1);
                if (models.size() == 0)
                    continue;

                AppEntry entry = new AppEntry();
                entry.totalNum = models.size();
                entry.label = getAppLabel(pm, aInfo);
                if (!Utils.isPmByIfw(context)) {
                    entry.disabledNum = Utils.getComponentDisabledNum(models, pm);
                }
                entry.packageName = aInfo.packageName;
                result.add(entry);

                ContentValues values = new ContentValues();
                values.put("app_name", entry.label);
                values.put("package_name", entry.packageName);
                values.put("is_system", isSystemApp ? 1 : 0);
                db.insert(DBHelper.APPS_TABLE_NAME, null, values);
            }
        }
        db.setTransactionSuccessful();
        db.endTransaction();

        aInfos.clear();

        Locale locale = context.getResources().getConfiguration().locale;

        final Collator collator = Collator.getInstance(locale);
        Collections.sort(result, new Comparator<AppEntry>() {
            @Override
            public int compare(AppEntry left, AppEntry right) {
                String lString = left.label;
                String rString = right.label;
                return collator.compare(lString, rString);
            }
        });

        return result;
    }

    private static List<AppEntry> getAppsAsActivity(Context context, boolean isSystemApp) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> aInfos = pm.getInstalledApplications(0);
        List<AppEntry> result = new ArrayList<>();

        for (ApplicationInfo aInfo : aInfos) {
            if (aInfo == null) {
                continue;
            }
            if (isSystemApp ? (aInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                    : (aInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                List<ComponentModel> models = Utils.getComponentModels(context, aInfo.packageName, 2);
                if (models.size() == 0)
                    continue;

                AppEntry entry = new AppEntry();
                entry.totalNum = models.size();
                entry.label = getAppLabel(pm, aInfo);
                if (!Utils.isPmByIfw(context)) {
                    entry.disabledNum = Utils.getComponentDisabledNum(models, pm);
                }
                entry.packageName = aInfo.packageName;
                result.add(entry);
            }
        }

        aInfos.clear();

        Locale locale = context.getResources().getConfiguration().locale;

        final Collator collator = Collator.getInstance(locale);
        Collections.sort(result, new Comparator<AppEntry>() {
            @Override
            public int compare(AppEntry left, AppEntry right) {
                String lString = left.label;
                String rString = right.label;
                return collator.compare(lString, rString);
            }
        });

        return result;
    }

    public static List<AppEntry> getAppsAsProvider(Context context, boolean isSystemApp) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> aInfos = pm.getInstalledApplications(0);
        List<AppEntry> result = new ArrayList<>();

        for (ApplicationInfo aInfo : aInfos) {
            //用了某些xposed框架可能导致 未知错误：Attempt to read from field 'java.lang.String android.content.pm.PackageItemInfo.packageName' on a null object reference
            //https://play.google.com/apps/publish/?dev_acc=04519573207763184232#ErrorClusterDetailsPlace:p=cn.wq.myandroidtools&et=CRASH&lr=LAST_7_DAYS&ecn=java.lang.NullPointerException&tf=unknown&tc=cn.wq.myandroidtools.b.b&tm=a&nid&an&c&s=new_status_desc
            if (aInfo == null) {
                continue;
            }
            if (isSystemApp ? (aInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                    : (aInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                List<ComponentModel> models = Utils.getComponentModels(context, aInfo.packageName, 3);
                if (models.size() == 0)
                    continue;
                AppEntry entry = new AppEntry();
                entry.totalNum = models.size();
                entry.label = getAppLabel(pm, aInfo);
                entry.disabledNum = Utils.getComponentDisabledNum(models, pm);
                entry.packageName = aInfo.packageName;
                result.add(entry);
            }
        }

        aInfos.clear();

        Locale locale = context.getResources().getConfiguration().locale;

        final Collator collator = Collator.getInstance(locale);
        Collections.sort(result, new Comparator<AppEntry>() {
            @Override
            public int compare(AppEntry left, AppEntry right) {
                String lString = left.label;
                String rString = right.label;
                return collator.compare(lString, rString);
            }
        });

        return result;
    }

    public static int getComponentDisabledNum(List<ComponentModel> cModels, PackageManager pm) {
        int num = 0;
        for (ComponentModel component : cModels) {
            if (component == null) {
                continue;
            }

            if ((!isComponentEnabled(component, pm))) {
                num++;
            }
        }
        return num;
    }

    public static int getDisabledNum(int type, String packageName, Context context) {
        return getComponentDisabledNum(getComponentModels(context, packageName, type), context.getPackageManager());
    }

//    public static List<RunningServiceInfo> getRunningServiceInfos(
//            Context context) {
//        return getRunningServiceInfos(context, null);
//    }

    public static boolean isPmByShizuku(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(BaseActivity.PREFERENCE_PM_CHANNER, BaseActivity.PM_CHANNEL_ROOT_COMMAND) == BaseActivity.PM_CHANNEL_SHIZUKU;
    }

    public static boolean isPmByIfw(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(BaseActivity.PREFERENCE_PM_CHANNER, BaseActivity.PM_CHANNEL_ROOT_COMMAND) == BaseActivity.PM_CHANNEL_IFW;
    }

    /**
     * 只获取制定包名的running service；针对Android O开始只能用root命令的情况可以限定包名
     * @param currentPkgName 需要获取正在运行服务的包名，可以为空
     */
    public static List<RunningServiceInfo> getRunningServiceInfos(
            Context context,@Nullable String currentPkgName) {
        final int maxNum;
//            if (!TextUtils.isEmpty(currentPkgName)) {
//                maxNum = 50;    //限制包名时不需要那么多
//            }else{
        maxNum = 500;
//            }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            ActivityManager aManager = (ActivityManager) context
                    .getSystemService(Context.ACTIVITY_SERVICE);

            return aManager.getRunningServices(maxNum);
        } else {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                if (isPmByShizuku(context) && ShizukuClient.getState().isAuthorized()) {
                    try {
                        return ShizukuActivityManagerV26.getServices(maxNum, 0);
                    } catch (Exception e) {
                        com.tencent.mars.xlog.Log.e(TAG, "getRunningServiceInfos by shizuku error", e);
                        Utils.err(TAG, "getRunningServiceInfos by shizuku error", e);
                    }
                }
            }

            //Android O（Android 8）开始变了,必须root才行
            //adb 中 grep 只能用-E 支持多个搜索条件，而且不用转义;不能直接用|分隔
            String cmd;
            if (TextUtils.isEmpty(currentPkgName)) {
                cmd = COMMAND_GET_RUNNING_SERVICE_FOR_ALL;
            }else{
                cmd = String.format(COMMAND_GET_RUNNING_SERVICE_FOR_SINGLE, currentPkgName);
            }
            List<String> result = runRootCommandForResult(cmd);

            List<RunningServiceInfo> rInfoList = new ArrayList<>();
            if (result != null) {
                RunningServiceInfo rInfo = null;
                for (String s : result) {
                    if (TextUtils.isEmpty(s)) {
                        continue;
                    }
                    if (s.startsWith(SERVICE_START_MARK)) {
                        if (rInfo != null) {
                            rInfoList.add(rInfo);
                        }
                        rInfo = new RunningServiceInfo();
                        Matcher matcher = SERVICE_RECORD_PATTERN.matcher(s.substring(SERVICE_START_MARK.length()));
                        if (matcher.matches()) {
                            String packageName = matcher.group(1);
                            String serviceName = matcher.group(2);
                            if (serviceName != null && serviceName.startsWith(".")) {
                                serviceName = packageName + serviceName;
                            }
                            if (serviceName != null) {
                                rInfo.service = new ComponentName(packageName, serviceName);
                            }
                        }
//                    } else if (s.startsWith(START_MARK_PROCESS)) {  //其实不需要processName
//                        if (rInfo != null) {
//                            rInfo.process = s.substring(START_MARK_PROCESS.length());
//                            rInfoList.add(rInfo);
//                            rInfo = null;
//                        }
                    } else if (s.startsWith(SERVICE_START_MARK_APP)) {
                        if (rInfo != null) {
                            Matcher matcher = SERVICE_APP_PATTERN.matcher(s);
                            if (matcher.matches()) {
                                String pid = matcher.group(1);
                                try {
                                    rInfo.pid = Integer.parseInt(pid);
                                } catch (Exception e) {
                                    rInfo.pid = 0;
                                }
                                rInfoList.add(rInfo);
                                rInfo = null;
                            }
                        }
                    }
                }
            }

            //返回未空的话，保存返回数据到文件
//            if (rInfoList.size() == 0) {
//                Utils.debug("-------mat test begin-------");
//                if (result != null && result.size() > 0) {
//                    Utils.debug("running_result 1 size:" + result.size());
//                    String path = context.getExternalCacheDir() + File.separator + "running_result_1";
//                    try {
//                        FileWriter writer = new FileWriter(path);
//                        for (String line : result) {
//                            writer.write(line);
//                            writer.write("\r\n");
//                        }
//                        writer.flush();
//                        writer.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                } else {
//                    Utils.debug("running_result 1 empty");
//                }
//
//                List<String> rr = runRootCommandForResult("dumpsys activity services");
//                if (rr != null && rr.size() > 0) {
//                    Utils.debug("running_result 2 size:" + rr.size());
//                    String path = context.getExternalCacheDir() + File.separator + "running_result_2";
//                    try {
//                        FileWriter writer = new FileWriter(path);
//                        for (String line : rr) {
//                            writer.write(line);
//                            writer.write("\r\n");
//                        }
//                        writer.flush();
//                        writer.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                } else {
//                    Utils.debug("running_result 2 empty");
//                }
//
//                Utils.debug("-------mat test end-------");
//            }

            return rInfoList;
        }

    }


    /**
     * @param componentType 0:service 1:receiver 2:activity 3:provider 4:all four types
     */
    public static List<ComponentModel> getComponentModels(Context context, String packageName, int componentType) {
        List<ComponentModel> result = new ArrayList<>();
        try {
            Context targetContext = context.createPackageContext(packageName, 0);
            AssetManager assetManager = targetContext.getAssets();

            Method addAssetPath = AssetManager.class.getMethod("addAssetPath", String.class);
            String sourceDir = context.getPackageManager().getApplicationInfo(packageName, 0).sourceDir;
            int cookie = (int) addAssetPath.invoke(assetManager, sourceDir);
            XmlResourceParser parser = assetManager.openXmlResourceParser(cookie, "AndroidManifest.xml");
            Resources resources = new Resources(assetManager, targetContext.getResources().getDisplayMetrics(), null);

            int type;
            while ((type = parser.next()) != XmlResourceParser.END_DOCUMENT) {
                if (type == XmlResourceParser.START_TAG) {
                    if ((componentType == 0 || componentType == 4) && "service".equals(parser.getName())
                            || (componentType == 1 || componentType == 4) && "receiver".equals(parser.getName())
                            || (componentType == 2 || componentType == 4) && ("activity".equals(parser.getName()) || "activity-alias".equals(parser.getName()))
                            || (componentType == 3 || componentType == 4) & "provider".equals(parser.getName())) {
//                        String name = parser.getAttributeValue(ANDROID_NAMESPACE, "name");
//                        //cmb.pb招商银行客户端因为乱码可能有问题
//                        //https://github.com/miracle2k/android-autostarts/blob/master/src/com/elsdoerfer/android/autostarts/ReceiverReader.java
//                        if (name == null) {
//                            for (int i = 0; i < parser.getAttributeCount(); i++) {
//                                if (TextUtils.isEmpty(parser.getAttributeName(i))) {
//                                    int res = parser.getAttributeNameResource(i);
//                                    if (res != 0 && resources.getResourceEntryName(res).equals("name")) {
//                                        name = parser.getAttributeValue(i);
//                                        break;
//                                    }
//                                }
//                            }
//                        }

                        String name = Utils.getAttributeValueByName(parser, resources, "name");
                        if (name == null) {
                            continue;
                        }

                        ComponentModel component = new ComponentModel();
                        if (!name.contains(".")) {
                            component.className = packageName + "." + name;
                        } else if (name.startsWith(".")) {
                            component.className = name.startsWith(".") ? packageName + name : name;
                        } else {
                            component.className = name;
                        }
                        component.packageName = packageName;

                        component.enabledInManifest = parser.getAttributeBooleanValue(ANDROID_NAMESPACE, "enabled",true);

                        result.add(component);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        Collections.sort(result, new Comparator<ComponentModel>() {
            @Override
            public int compare(ComponentModel lhs, ComponentModel rhs) {
                String l = lhs.className
                        .substring(lhs.className.lastIndexOf(".") + 1);
                String r = rhs.className
                        .substring(rhs.className.lastIndexOf(".") + 1);
                return l.compareTo(r);
            }
        });
        return result;
    }

    public static @Nullable String getAttributeValueByName(XmlResourceParser parser, Resources resources, String name) {
        String value = parser.getAttributeValue(ANDROID_NAMESPACE, name);
        //cmb.pb招商银行客户端因为乱码可能有问题
        //https://github.com/miracle2k/android-autostarts/blob/master/src/com/elsdoerfer/android/autostarts/ReceiverReader.java
        if (value == null) {
            for (int i = 0; i < parser.getAttributeCount(); i++) {
                if (TextUtils.isEmpty(parser.getAttributeName(i))) {
                    int res = parser.getAttributeNameResource(i);
                    if (res != 0 && resources.getResourceEntryName(res).equals(name)) {
                        value = parser.getAttributeValue(i);
                        break;
                    }
                }
            }
        }
        return value;
    }
    
    public static boolean isComponentEnabled(ComponentModel cInfo,
                                             PackageManager pm) {
        int enabled = pm.getComponentEnabledSetting(new ComponentName(
                cInfo.packageName, cInfo.className));
        if (enabled == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
            return cInfo.enabledInManifest;
        }else {
            return enabled <= PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        }
    }

    /**
     * @see #isRunning
     */
    public static int getRunningNum(String packageName,
                                    List<RunningServiceInfo> runningServiceInfos) {
        int num = 0;
        if (runningServiceInfos != null) {
            for (RunningServiceInfo rInfo : runningServiceInfos) {
                if (rInfo.restarting != 0 || rInfo.pid == 0) {
                    continue;
                }
                ComponentName cName = rInfo.service;
                if (cName != null && TextUtils.equals(cName.getPackageName(), packageName)) {
                    num++;
                }
            }
        }

        return num;
    }

    public static boolean isRunning(String packageName, String className,
                                    List<RunningServiceInfo> rInfos) {
        for (RunningServiceInfo rInfo : rInfos) {
            // FIXME: 2018/3/17 被ifw阻止后会被getRunningServices获取到，但是pid=0 && restarting=0，该service其实没有走生命周期
            //正在重启的时候会有两个，com.netease.cloudmusic.service.PlayService
            if (rInfo.restarting != 0 || rInfo.pid == 0) {
                continue;
            }
            if (rInfo.service != null && rInfo.service.getPackageName().equals(packageName)
                    && rInfo.service.getClassName().equals(className)) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkSDcard(Context context) {
        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            Toast.makeText(context, R.string.media_not_mounted,
                    Toast.LENGTH_LONG).show();
            return false;
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, R.string.permission_request_failed, Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

//	public static boolean runRootCommand(String command) {
//        Process process = null;
//        DataOutputStream os = null;
//        try {
//            process = Runtime.getRuntime().exec("su");
//            os = new DataOutputStream(process.getOutputStream());
//
//            // 类似Titanium的中文高8位会丢失
////             os.writeBytes(command + "\n");
//            os.write(command.getBytes());
//            os.writeBytes("exit\n");
//            os.flush();
//            
//            process.waitFor();
//        } catch (Exception e) {
//        	e.printStackTrace();
//            return false;
//        } finally {
//            try {
//                if (os != null) {
//                    os.close();
//                }
//                if (process != null) {
//                    process.destroy();
//                }
//            } catch (Exception e) {
//                // nothing
//            }
//        }
//        return true;
//    }

    /**
     * use root-command-library to get root access
     */
//	public static boolean runRootCommand2(String command) {
//		try {
//			Shell shell=Shell.startRootShell();
//			shell.add(new SimpleCommand(command)).waitForFinish();
//			shell.close();
//		} catch (Exception e) {
//			e.printStackTrace();
//			return false;
//		}
//		
//		return true;
//	}

//    public static List<String> runRootCommandForResult(String cmd) {
//        final List<String> stdout = new ArrayList<>();
//        try {
//            Command command = new Command(0, cmd) {
//                @Override
//                public void commandOutput(int id, String line) {
//                    super.commandOutput(id, line);
//                    if (Utils.isTestForDatabase()) {
//                        Log.e("wangqi", line);
//                    }
//                    stdout.add(line);
//                }
//
//                @Override
//                public void commandTerminated(int id, String reason) {
//                    super.commandTerminated(id, reason);
//                    if (Utils.isTestForDatabase()) {
//                        Log.e("wangqi2", reason);
//                    }
//                }
//
//                @Override
//                public void commandCompleted(int id, int exitcode) {
//                    super.commandCompleted(id, exitcode);
//                    if (Utils.isTestForDatabase()) {
//                        Log.e("wangqi3", "exitcode:"+exitcode);
//                    }
//                }
//            };
//
//            RootTools.getShell(true).add(command);
//
//            while (true) {
//                if (command.isFinished()) {
//                    break;
//                }
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        } catch (IOException | TimeoutException | RootDeniedException e) {
//            e.printStackTrace();
//            if (Utils.isTestForDatabase()) {
//                Log.e("wangqi4", cmd + " , result exception:" + (e.toString()));
//            }
//            return null;
//        }
//        if (Utils.isTestForDatabase()) {
//            Log.e("wangqi5", cmd + " , result size:" + (stdout.size()));
//        }
//        return stdout;
//    }

    public static List<String> runRootCommandForResult(String cmd) {
        if (eu.chainfire.libsuperuser.Shell.SU.available()) {
            return eu.chainfire.libsuperuser.Shell.SU.run(cmd);
        }else{
            return null;
        }
    }

    /**
     * 包括stderr\stdout
     */
    public static List<String> runRootCommandForAllResult(String cmd) {
        if (eu.chainfire.libsuperuser.Shell.SU.available()) {
            List<String> result =   eu.chainfire.libsuperuser.Shell.SU.runGetAll(cmd);
            if (BuildConfig.DEBUG) {
                Utils.debug(TAG, "runRootCorunRootCommandForAllResultmmand start:" + cmd);
                int index = 0;
                if (result != null) {
                    for (String s : result) {
                        Utils.debug(TAG, index++ + ":" + s);
                    }
                } else {
                    Utils.debug(TAG, "runRootCommandForAllResult return null");
                }
                Utils.debug(TAG, "runRootCommandForAllResult end:" + cmd);
            }
            return result;
        }else{
            return null;
        }
    }

    public static List<String> runCommandForResult(String cmd) {
        List<String> result =  eu.chainfire.libsuperuser.Shell.SH.run(cmd);
        if (BuildConfig.DEBUG) {
            Utils.debug(TAG, "runCommandForResult start:" + cmd);
            int index = 0;
            if (result != null) {
                for (String s : result) {
                    Utils.debug(TAG, index++ + ":" + s);
                }
            } else {
                Utils.debug(TAG, "runCommandForResult return null");
            }
            Utils.debug(TAG, "runCommandForResult end:" + cmd);
        }
        return result;
    }

    public static boolean runRootCommand(String... cmd) {
        if (eu.chainfire.libsuperuser.Shell.SU.available()) {
            List<String> result = eu.chainfire.libsuperuser.Shell.SU.run(cmd);
            if (BuildConfig.DEBUG) {
                String cmdStr = Arrays.toString(cmd);
                Utils.debug(TAG, "runRootCommand start:" + cmdStr);
                if (result != null) {
                    int index = 0;
                    for (String s : result) {
                        Utils.debug(TAG, index++ + ":" + s);
                    }
                } else {
                    Utils.debug(TAG, "runRootCommand return null");
                }

                Utils.debug(TAG, "runRootCommand end:" + cmdStr);
            }
            return result != null;
        }else{
            if (BuildConfig.DEBUG) {
                Utils.debug(TAG, "su is not available");
            }
            return false;
        }
    }

//    public static List<String> runCommandForResult(String cmd) {
//        final List<String> stdout = new ArrayList<>();
//        try {
//            Command command = new Command(0, cmd) {
//                @Override
//                public void commandOutput(int id, String line) {
//                    super.commandOutput(id, line);
//                    stdout.add(line);
//                }
//
//                @Override
//                public void commandTerminated(int id, String reason) {
//                    super.commandTerminated(id, reason);
//                }
//
//                @Override
//                public void commandCompleted(int id, int exitcode) {
//                    super.commandCompleted(id, exitcode);
//                }
//            };
//
//            RootTools.getShell(false).add(command);
//
//            while (true) {
//                if (command.isFinished()) {
//                    break;
//                }
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        } catch (IOException | TimeoutException | RootDeniedException e) {
//            e.printStackTrace();
//            return null;
//        }
//        return stdout;
//    }

//    public static boolean runRootCommand(String... cmd) {
//        try {
//            //Command command = new Command(0, cmd);
//            Command command;
//            if(Utils.isTestForUninstallSysApp()){
//                if (Utils.isTestForDatabase()) {
//                    Log.e("wangqi0", Arrays.toString(cmd));
//                }
//                command = new Command(0, cmd){
//                    @Override
//                    public void commandOutput(int id, String line) {
//                        super.commandOutput(id, line);
//                        if (Utils.isTestForDatabase()) {
//                            Log.e("wangqi", line);
//                        }
//                    }
//
//                    @Override
//                    public void commandTerminated(int id, String reason) {
//                        super.commandTerminated(id, reason);
//                        if (Utils.isTestForDatabase()) {
//                            Log.e("wangqi2", reason);
//                        }
//                    }
//
//                    @Override
//                    public void commandCompleted(int id, int exitcode) {
//                        super.commandCompleted(id, exitcode);
//                        if (Utils.isTestForDatabase()) {
//                            Log.e("wangqi3", "exitcode:"+exitcode);
//                        }
//                    }
//                };
//            }else{
//                command = new Command(0, cmd);
//            }
//
//
//            Shell shell = RootTools.getShell(true);
//            shell.add(command);
//
//            while (true) {
//                if (command.isFinished()) {
//                    break;
//                }
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//            return command.getExitCode() == 0;
//        } catch (IOException | TimeoutException | RootDeniedException e) {
//            e.printStackTrace();
//            return false;
//        }
//    }

    //https://github.com/shadowsocks/shadowsocks-android/issues/826
    //http://blog.csdn.net/zyp009/article/details/17397383
    // 例如/system/app/Email/下面有oat目录，里面有odex，在UninstalledRecyclerListFragment卸载时才删除
    /**
     * 已经mount system rw了，只需要后面unmount
     */
    public static String getUnmountSystemCommand(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int mountSysMode = preferences.getInt("mountSysMode", 0);
        if (mountSysMode == 1) {
            String command1 = Utils.getUnmountSystemCommand1(preferences, mountSysMode);
            if (command1 != null) {
                return command1;
            }
        } else if (mountSysMode == 2) {
            String command2 = Utils.getUnmountSystemCommand2(preferences, mountSysMode);
            if (command2 != null) {
                return command2;
            }
        }

        //Device or resource busy 是err
        List<String> mountSysResult = Utils.runRootCommandForAllResult("mount -o remount,rw /system");
        if (mountSysResult == null || mountSysResult.size() == 0) {
            if (mountSysMode != 0) {
                preferences.edit().putInt("mountSysMode", 0).apply();
            }
            return "mount -o remount,ro /system";
        }
        if (mountSysResult.size() > 0) {
            if (mountSysResult.get(0).endsWith("Device or resource busy")) {
                //从mount命令的返回结果中找
                //每个版本返回的格式不一样，先直接去第一个空格之前的吧
                ///dev/block/platform/msm_sdcc.1/by-name/system on /system type ext4 (ro,seclabel,noatime,data=ordered)
                //用root命令返回的不一样，而且有问题
                String command1 = Utils.getUnmountSystemCommand1(preferences, mountSysMode);
                if (command1 != null) {
                    return command1;
                }

                //从/proc/mounts中获取
                String command2 = Utils.getUnmountSystemCommand2(preferences, mountSysMode);
                if (command2 != null) {
                    return command2;
                }
            }else{
                com.tencent.mars.xlog.Log.e(TAG, "normal mount0 sys return:" + TextUtils.join(",", mountSysResult));
            }
        }
        preferences.edit().putInt("mountSysMode", 0).apply();
        return null;


//        String mountCommand = Build.VERSION.SDK_INT>=24? "mount -o rw,remount /dev/block/platform/msm_sdcc.1/by-name/system /system" :"mount -o remount,rw /system";
//        String unmountCommand = Build.VERSION.SDK_INT>=24? "mount -o ro,remount /dev/block/platform/msm_sdcc.1/by-name/system /system" :"mount -o remount,ro /system";
//
//        //每个版本返回的格式不一样，先直接去第一个空格之前的吧
//        ///dev/block/platform/msm_sdcc.1/by-name/system on /system type ext4 (ro,seclabel,noatime,data=ordered)
//        if (Build.VERSION.SDK_INT >= 24) {
//            //用root命令返回的不一样，而且有问题
//            List<String> mountList= Utils.runCommandForResult("mount |grep '/system '");
//            if (mountList != null && mountList.size() > 0) {
//                if (mountList.size() > 1) {
//                    Log.e("matpro", "get system mount size>1," + TextUtils.join(",", mountList));
//                }
//                String location = mountList.get(0).substring(0, mountList.get(0).indexOf(" "));
//
//                mountCommand = "mount -o rw,remount " + location +" /system";
//                unmountCommand = "mount -o ro,remount " + location+" /system";
//            }
//        }
//
//        return new String[]{mountCommand, unmountCommand};
    }

    private static String getUnmountSystemCommand2(SharedPreferences preferences,int mode){
        //从/proc/mounts中获取
        List<String> mountList= Utils.runCommandForResult("cat /proc/mounts |grep '/system '");
        if (mountList != null && mountList.size() > 0) {
            String location = mountList.get(0).substring(0, mountList.get(0).indexOf(" "));
            String mountCommand = "mount -o rw,remount " + location +" /system";
            List<String> mountSysResult = Utils.runRootCommandForResult(mountCommand);
            if (mountSysResult == null || mountSysResult.size() == 0) {
                if (mode != 2) {
                    preferences.edit().putInt("mountSysMode", 2).apply();
                }
                return "mount -o ro,remount " + location +" /system";
            }else{
                com.tencent.mars.xlog.Log.e(TAG, "mount2 sys return:" + TextUtils.join(",", mountSysResult));
            }
        }
        return null;
    }

    /**
     * 执行一个或者多个pm enable/disable 命令，要注意组件不存在时会报错 java.lang.IllegalArgumentException: Unknown component: ComponentInfo{com.zhihu.android/com.avos.avoscloud.PushService}
     * @param command
     * @return 正常成功1、正常失败-1，组件不存在的失败-2
     */
    public static int runMultiPmDisableCommand(String command){
        final List<String> ss = eu.chainfire.libsuperuser.Shell.SU.runGetOnlyErr(command);
        final Pattern pattern = Pattern.compile("[\\w\\W]+Component class [^ ]+ does not exist in [^ ]+");
        //禁用多个时 只有最后一个不存在时会影响process.exitValue()返回1
        if (ss != null && ss.size() > 0 && pattern.matcher(ss.get(ss.size() - 1)).matches()) {
            return -2;
        }else {
            return ss != null ? 1 : -1;
        }
    }

    //https://android.stackexchange.com/a/158890/228460
    private static String getUnmountSystemCommand1(SharedPreferences preferences,int mode){
        //从mount命令的返回结果中找
        //每个版本返回的格式不一样，先直接去第一个空格之前的吧
        ///dev/block/platform/msm_sdcc.1/by-name/system on /system type ext4 (ro,seclabel,noatime,data=ordered)
        //用root命令返回的不一样，而且有问题
        List<String> mountList= Utils.runCommandForResult("mount |grep '/system '");
        if (mountList != null && mountList.size() > 0) {
            if (mountList.size() > 1) {
                com.tencent.mars.xlog.Log.e(TAG, "get system mount size>1," + TextUtils.join(",", mountList));
            }
            String location = mountList.get(0).substring(0, mountList.get(0).indexOf(" "));
            String mountCommand = "mount -o rw,remount " + location + " /system";
            List<String>mountSysResult = Utils.runRootCommandForResult(mountCommand);
            if (mountSysResult == null || mountSysResult.size() == 0) {
                if (mode != 1) {
                    preferences.edit().putInt("mountSysMode", 1).apply();
                }
                return "mount -o ro,remount " + location + " /system";
            } else {
                com.tencent.mars.xlog.Log.e(TAG, "mount1 sys return:" + TextUtils.join(",", mountSysResult));
            }
        }
        return null;
    }

//    public static Locale getSystemLocale() {
//        if (Build.VERSION.SDK_INT >= 24) {
//            return Resources.getSystem().getConfiguration().getLocales().get(0);
//        } else {
//            return Resources.getSystem().getConfiguration().locale;
//        }
//    }

    public static int dp2px(Context context,int dp) {
        return Math.round(context.getResources().getDisplayMetrics().density * dp);
    }

    public static void debug(String msg) {
        if ((BuildConfig.DEBUG || isDebugForUser()) && !TextUtils.isEmpty(msg)) {
            com.tencent.mars.xlog.Log.e("matpro", msg);
        }
    }
    public static void debug(String tag,String msg) {
        if ((BuildConfig.DEBUG || isDebugForUser()) && !TextUtils.isEmpty(msg)) {
            com.tencent.mars.xlog.Log.d(tag, msg);
        }
    }
    public static void err(String tag, String msg, Exception e) {
        if ((BuildConfig.DEBUG || isDebugForUser()) && !TextUtils.isEmpty(msg)) {
            Log.e(tag, msg, e);
        }
    }

    public static boolean isDebugForUser() {
        return false;
    }

    public static boolean isTestForWindowManager() {
        return false;
    }
    public static boolean isTestForDatabase(){
        return false;
    }
    public static boolean isTestCurrentFragment(){
        return false;
    }

    public static boolean isTestForUninstallSysApp(){
        return true;
    }

    public static boolean isRemoveAccessibilityForGoogle(){
        return true;
    }
}

