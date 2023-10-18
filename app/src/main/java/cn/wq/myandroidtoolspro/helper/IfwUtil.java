package cn.wq.myandroidtoolspro.helper;

import android.content.ComponentName;
import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.Xml;

import com.tencent.mars.xlog.Log;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.wq.myandroidtoolspro.model.BackupEntry;
import cn.wq.myandroidtoolspro.model.ComponentEntry;
import cn.wq.myandroidtoolspro.recyclerview.adapter.AbstractComponentAdapter;
import cn.wq.myandroidtoolspro.recyclerview.fragment.AppInfoForManageFragment2;

public class IfwUtil {
    private static final String TAG = "IfwUtil";
    private static final String SYSTEM_PROPERTY_EFS_ENABLED = "persist.security.efs.enabled";

    public static final int COMPONENT_FLAG_EMPTY = 0x00;
    public static final int COMPONENT_FLAG_ACTIVITY = 0x01;
    public static final int COMPONENT_FLAG_RECEIVER = 0x10;
    public static final int COMPONENT_FLAG_SERVICE = 0x100;
    public static final int COMPONENT_FLAG_ALL = 0x111;

    public static final String BACKUP_LOCAL_FILE_EXT = ".ifw";
    public static final String BACKUP_SYSTEM_FILE_EXT_OF_STANDARD = ".xml"; //标准ifw备份后缀
    public static final String BACKUP_SYSTEM_FILE_EXT_OF_MINE = "$" + BACKUP_SYSTEM_FILE_EXT_OF_STANDARD;    //本App备份ifw使用的后缀

    /**
     * 组件列表中修改选中位置component的ifw状态，并保存所有component到ifw文件<br>
     * 每个类型的list中（例如ServiceRecyclerListFragment）的修改都需要完整保存整个ifw内容
     *
     * @param positions 某一类component的位置
     */
    public static boolean saveComponentIfw(Context context,
                                           @NonNull String packageName,
                                           IfwEntry ifwEntry,
                                           @NonNull AbstractComponentAdapter<? extends ComponentEntry> adapter,
                                           int cType,
                                           boolean useParentIfw,
                                           Integer... positions) {
        return saveComponentIfw(context, packageName, ifwEntry, adapter, cType, useParentIfw, null, positions);
    }

    /**
     * 组件列表中修改选中位置component的ifw状态，并保存所有component到系统ifw文件（先保存到本地临时文件）<br>
     * 每个类型的list中（例如ServiceRecyclerListFragment）的修改都需要完整保存整个ifw内容
     * @param localTempDir  所有app的ifw文件已经都复制到本地的目录，供组件全局禁用使用，{@link cn.wq.myandroidtoolspro.recyclerview.fragment.SearchComponentInAllFragment}
     * @param positions     某一类component的位置
     */
    public static boolean saveComponentIfw(Context context,
                                           @NonNull String packageName,
                                           IfwEntry ifwEntry,
                                           @NonNull AbstractComponentAdapter<? extends ComponentEntry> adapter,
                                           int cType,
                                           boolean useParentIfw,
                                           @Nullable File localTempDir,
                                           Integer... positions
                                           ) {
        if (positions == null || positions.length == 0) {
            //不应该为空
            return true;
        }
        if (ifwEntry == null || ifwEntry == IfwEntry.EMPTY) {
            ifwEntry = new IfwEntry();
            if (useParentIfw) {
                AppInfoForManageFragment2.mIfwEntry = ifwEntry;
            }
        }
        Map<String, Set<String>> componentMap = getComponentMapInIfw(cType, ifwEntry);
        ComponentEntry entry;
        for (Integer pos : positions) {
            entry = adapter.getItem(pos);
            if (entry == null) {
                continue;
            }
            String cls = entry.className;
            if (cls.startsWith(".")) {
                cls = entry.packageName + cls;
            }
            Set<String> componentSet = componentMap.get(packageName);
            if (componentSet == null) {
                componentSet = new HashSet<>();
                componentMap.put(packageName, componentSet);
            }
            if (entry.isIfwed) {
                componentSet.remove(cls);
            } else {
                componentSet.add(cls);
            }
        }

        return saveIfwEntryToSystemFile(context, packageName, ifwEntry, localTempDir);
    }

    /**
     * 备份使用,先保存一个app下的禁用组件到本地文件，最后一次cp到系统目录中
     */
    public static void saveComponentIfwToLocal(@NonNull String packageName,
                                               List<BackupEntry> list,
                                               @NonNull String tempDir
    ) throws IOException {
        if (list == null) {
            return;
        }
        IfwEntry ifwEntry = new IfwEntry();
        for (BackupEntry entry : list) {
            Map<String, Set<String>> componentMap = getComponentMapInIfw(entry.cType, ifwEntry);
            Set<String> componentSet = componentMap.get(packageName);
            if (componentSet == null) {
                componentSet = new HashSet<>();
                componentMap.put(packageName, componentSet);
            }
            componentSet.add(entry.className);
        }
        saveIfwEntryToLocalFile(packageName, ifwEntry, tempDir);
    }

    private static void buildIfwEntryToString(StringBuilder sBuilder, @NonNull String packageName, @NonNull IfwEntry ifwEntry) {
        buildIfwEntryToString(sBuilder, packageName, ifwEntry.serviceMap, "service");
        buildIfwEntryToString(sBuilder, packageName, ifwEntry.activityMap, "activity");
        buildIfwEntryToString(sBuilder, packageName, ifwEntry.receiverMap, "broadcast");
    }

    /**
     * @param sBuilder
     * @param packageName
     * @param componentMap
     * @param tagName      service,activity,broadcast
     */
    private static void buildIfwEntryToString(StringBuilder sBuilder, @NonNull String packageName, Map<String, Set<String>> componentMap, String tagName) {
        if (componentMap != null && componentMap.size() > 0) {
            Set<String> set = componentMap.get(packageName);
            if (set != null && set.size() > 0) {
                sBuilder.append("  <").append(tagName).append(" block=\"true\" log=\"false\">\n");
                for (String className : set) {
                    String cls = className;
                    if (className.startsWith(packageName)) {
                        cls = "." + className.substring(packageName.length() + 1);
                    }
                    sBuilder.append("    <component-filter name=\"")
                            .append(packageName)
                            .append("/")
                            .append(cls)
                            .append("\" />\n");
                }
                sBuilder.append("  </").append(tagName).append(">\n");
            }
        }
    }

    /**
     * 保存IfwEntry中指定packageName的数据到系统文件
     * @param localTempDir 可以为空；为空时使用 getExternalCacheDir()
     */
    private static boolean saveIfwEntryToSystemFile(Context context, @NonNull String packageName, @NonNull IfwEntry ifwEntry,@Nullable File localTempDir) {
        StringBuilder sBuilder = new StringBuilder("<rules>\n");
        buildIfwEntryToString(sBuilder, packageName, ifwEntry);
        sBuilder.append("</rules>\n");

        return saveIfwStringToSystemFile(context, sBuilder.toString(), packageName, localTempDir);
    }

    /**
     * 保存IfwEntry到我自定义后缀的系统文件
     */
    public static boolean saveIfwEntryToMySystemFile(Context context, @NonNull IfwEntry ifwEntry) {
        Set<String> packageNameSet = new HashSet<>(ifwEntry.serviceMap.keySet());
        packageNameSet.addAll(ifwEntry.receiverMap.keySet());
        packageNameSet.addAll(ifwEntry.activityMap.keySet());
        Utils.debug(TAG, "saveIfwEntryToMySystemFile:" + packageNameSet.toString()+","+ifwEntry.serviceMap.keySet().toString()+","
            +ifwEntry.receiverMap.keySet()+","+ifwEntry.activityMap.keySet());
        for (String pkg : packageNameSet) {
            boolean result = saveIfwEntryToSystemFile(context, pkg, ifwEntry, null);
            if (!result) {
                return false;
            }
        }
        return true;
    }


    /**
     * 保存IfwEntry到本地文件
     */
    private static void saveIfwEntryToLocalFile(
            @NonNull String packageName,
            @NonNull IfwEntry ifwEntry,
            String destDir) throws IOException {
        StringBuilder sBuilder = new StringBuilder("<rules>\n");
        buildIfwEntryToString(sBuilder, packageName, ifwEntry);
        sBuilder.append("</rules>\n");

        saveIfwStringToLocalFile(sBuilder.toString(), packageName, destDir);
    }

    /**
     * 保存指定包名的ifw内容到系统文件，文件名格式为：packageName$.xml
     *
     * @param content ifw内容
     * @param pkgName app包名
     */
    private static boolean saveIfwStringToSystemFile(
            Context context,
            String content,
            @NonNull String pkgName,
            @Nullable File localTempDir
    ) {
        String tempRelativePath = getLocalRelativeTempIfwPath(context, pkgName, localTempDir);
        File tempFile = new File(Environment.getExternalStorageDirectory(), tempRelativePath);
        String destPath = getSystemIfwDir() + File.separator + pkgName + IfwUtil.BACKUP_SYSTEM_FILE_EXT_OF_MINE;
        FileWriter writer = null;
        try {
            writer = new FileWriter(tempFile, false);
            writer.write(content);
            writer.flush();
            writer.close();
            boolean result = copy(tempRelativePath, destPath);
            if (result) {
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "saveIfwStringToSystemFile error", e);
            Utils.err(TAG, "saveIfwStringToSystemFile error", e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        //复制到/data/system/ifw下失败，得把临时文件删了
        if (tempFile.exists()) {
            tempFile.delete();
        }
        return false;
    }

    /**
     * 保存指定包名的ifw内容到本地文件，文件名格式为：packageName$.xml
     *
     * @param content ifw内容
     * @param pkgName app包名
     * @param destDir 存放ifw文件的目录
     */
    private static void saveIfwStringToLocalFile(String content, @NonNull String pkgName, String destDir) throws IOException {
        String tempPath = destDir + File.separator + pkgName + IfwUtil.BACKUP_SYSTEM_FILE_EXT_OF_MINE;
        FileWriter writer = null;
        try {
            writer = new FileWriter(tempPath, false);
            writer.write(content);
            writer.flush();
            writer.close();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * file path to file path
     * @param srcRelativePath 相对/sdcard路径
     */
    private static boolean copy(String srcRelativePath, String destPath) {
        boolean cpResult = Utils.runRootCommand("cp -f $EXTERNAL_STORAGE/" + srcRelativePath + " " + destPath + " \n")
                || Utils.runRootCommand("cat $EXTERNAL_STORAGE/" + srcRelativePath + " > " + destPath + " \n");
        if (!cpResult) {
            return false;
        }
        return Utils.runRootCommand("chmod 644 " + destPath);
    }

    /**
     * all files in srsDir to destDir
     * <br>必须用 cp -R src/. dest 才能复制目录下面的所有文件
     */
    public static boolean copySysIfwToTempDir(File destDir) {
        String srcDir = IfwUtil.getSystemIfwDir();
        String tempRelativePath = getLocalRelativeTempIfwPath(destDir);
        return Utils.runRootCommand("cp -fR " + srcDir + "/. $EXTERNAL_STORAGE/" + tempRelativePath);
    }

    /**
     * all files in srsDir to destDir
     * <br>必须用 cp -R src/. dest 才能复制目录下面的所有文件
     */
    public static boolean copyToIfwDir(File srcDir) {
        String destDir = IfwUtil.getSystemIfwDir();
        String tempRelativePath = getLocalRelativeTempIfwPath(srcDir);
        if (!Utils.runRootCommand("cp -fR $EXTERNAL_STORAGE/" + tempRelativePath + "/. " + destDir)) {
            return false;
        }
        String chmodCmd = String.format("find %s -name '*%s'|xargs chmod 644",
                destDir, IfwUtil.BACKUP_SYSTEM_FILE_EXT_OF_MINE);
        return Utils.runRootCommand(chmodCmd);
    }

    private static boolean isEncryptedFilesystemEnabled() {
        try {
            return (boolean) Class.forName("android.os.SystemProperties")
                    .getMethod("getBoolean", String.class, boolean.class)
                    .invoke(null, SYSTEM_PROPERTY_EFS_ENABLED, false);
        } catch (Exception e) {
            Log.e(TAG, "isEncryptedFilesystemEnabled error", e);
            Utils.err(TAG, "isEncryptedFilesystemEnabled error", e);
            return false;
        }
    }

    private static String getDirectoryPath(String variableName, String defaultPath) {
        String path = System.getenv(variableName);
        return path == null ? defaultPath : path;
    }

    public static String getSystemIfwDir() {
        String baseDir;
        if (isEncryptedFilesystemEnabled()) {
            baseDir = getDirectoryPath("ANDROID_SECURE_DATA", "/data/secure");
        } else {
            baseDir = getDirectoryPath("ANDROID_DATA", "/data");
        }
        return baseDir + File.separator + "system" + File.separator + "ifw";
    }

    /**
     * 加载指定包名的ifw文件
     *
     * @param cType 暂时都是all
     * @param loadLocalFirst 之前已加载到本地了，可以先加载本地文件
     */
    public static IfwEntry loadIfwFileForPkg(Context context, @NonNull String packageName, int cType, boolean loadLocalFirst) throws Exception {
        String tempRelativePath = getLocalRelativeTempIfwPath(context, packageName);
        if (!loadLocalFirst) {
//            String command = String.format("ls %s | grep '%s%s'", getSystemIfwDir(), packageName, "\\" + IfwUtil.BACKUP_SYSTEM_FILE_EXT_OF_MINE);
//            List<String> files = Utils.runRootCommandForResult(command);
//            if (files == null || files.size() == 0) {
//                return IfwEntry.EMPTY;
//            }
//            String sourcePath = getSystemIfwDir() + File.separator + files.get(0);
//            String catCmd = String.format("cat %s > $EXTERNAL_STORAGE/%s", sourcePath, tempRelativePath);
//            if (!Utils.runRootCommand(catCmd)) {
//                return IfwEntry.ROOT_ERROR;
//            }

            //下面第一个命令有问题，文件不存在时不会终止命令
//            String command = String.format("cat `find %s -name '%s'|head -1` > $EXTERNAL_STORAGE/%s",
            String command = String.format("find %s -name '%s' | xargs cat > $EXTERNAL_STORAGE/%s",
                    getSystemIfwDir(),
                    packageName + "\\" + IfwUtil.BACKUP_SYSTEM_FILE_EXT_OF_MINE,
                    tempRelativePath);
            if (!Utils.runRootCommand(command)) {
                return IfwEntry.ROOT_ERROR;
            }
        }

        return parseIfwFile(cType, packageName, new File(Environment.getExternalStorageDirectory(),tempRelativePath));
    }

    /**
     * 加载所有ifw文件
     *
     * @param loadLocalFirst 之前已加载到本地了，可以先加载本地文件
     */
    public static IfwEntry loadAllIfwFile(Context context, int cType, File ifwTempDir,boolean loadLocalFirst) throws Exception {
        //临时目录
        if (!ifwTempDir.exists()) {
            ifwTempDir.mkdirs();
        }

        if (!loadLocalFirst) {
            if(!copySysIfwToTempDir(ifwTempDir)){
                return IfwEntry.ROOT_ERROR;
            }
        }

        IfwEntry ifw = new IfwEntry();
        for (File f : ifwTempDir.listFiles()) {
            if (f.isDirectory()) {
                f.delete();
                continue;
            }

            if (!loadLocalFirst) {
                if (!f.getName().endsWith(IfwUtil.BACKUP_SYSTEM_FILE_EXT_OF_MINE)) {
                    f.delete();
                    continue;
                } else {
                    //复制全部系统ifw文件时没法去掉$号，只能这时候去掉
                    String name = f.getName().substring(0, f.getName().length() - BACKUP_SYSTEM_FILE_EXT_OF_MINE.length()) + BACKUP_LOCAL_FILE_EXT;
                    File newF = new File(f.getParent(), name);
                    if (newF.exists()) {
                        newF.delete();
                    }
                    f.renameTo(newF);
                    parseIfwFile(ifw, cType, null, newF);
                }
            } else {
                if (!f.getName().endsWith(BACKUP_LOCAL_FILE_EXT)) {
                    f.delete();
                    continue;
                } else {
                    parseIfwFile(ifw, cType, null, f);
                }
            }

        }

        return ifw;
    }

    private static Map<String, Set<String>> getComponentMapInIfw(int cType, IfwEntry ifwEntry) {
        Map<String, Set<String>> componentMap = null;
        if (cType == COMPONENT_FLAG_SERVICE) {
            componentMap = ifwEntry.serviceMap;
        } else if (cType == COMPONENT_FLAG_ACTIVITY) {
            componentMap = ifwEntry.activityMap;
        } else if (cType == COMPONENT_FLAG_RECEIVER) {
            componentMap = ifwEntry.receiverMap;
        }
        return componentMap;
    }

    public static boolean isComponentInIfw(@NonNull String packageName, String className, int cType, IfwEntry ifwEntry) {
        if (ifwEntry == null || ifwEntry.status < 0) {
            return false;
        }

        Map<String, Set<String>> componentMap = getComponentMapInIfw(cType, ifwEntry);

        return componentMap != null
                && componentMap.get(packageName) != null
                && componentMap.get(packageName).contains(className);
    }

    /**
     * cache目录下packageName.ifw文件
     * @see #getLocalRelativeTempIfwPath(Context, String)
     */
//    private static String getLocalTempIfwPath(Context context, @NonNull String packageName) {
//        return context.getExternalCacheDir() + File.separator + packageName + BACKUP_LOCAL_FILE_EXT;
//    }

    /**
     * 外置cache目录下packageName.ifw文件的相对/sdcard路径
     * shell中获取的/sdcard路径可能和api获取的不一致，且互相不可见：http://www.cloudchou.com/android/post-689.html
     * /storage/emulated/legacy vs  /storage/emulated/0
     */
    private static String getLocalRelativeTempIfwPath(Context context, @NonNull String packageName) {
        return getLocalRelativeTempIfwPath(context, packageName, null);
    }

    /**
     * 外置cache目录下（或者指定临时目录下）packageName.ifw文件的相对/sdcard路径
     * shell中获取的/sdcard路径可能和api获取的不一致，且互相不可见：http://www.cloudchou.com/android/post-689.html
     * /storage/emulated/legacy vs  /storage/emulated/0
     */
    private static String getLocalRelativeTempIfwPath(Context context, @NonNull String packageName,@Nullable File localTempDir) {
        File dir = localTempDir;
        if (localTempDir == null) {
            dir = context.getExternalCacheDir();
        }
        File full = new File(dir, packageName + BACKUP_LOCAL_FILE_EXT);
        return getLocalRelativeTempIfwPath(full);
    }

    /**
     * cache目录下packageName.ifw文件的相对/sdcard路径
     * shell中获取的/sdcard路径可能和api获取的不一致，且互相不可见：http://www.cloudchou.com/android/post-689.html
     * /storage/emulated/legacy vs  /storage/emulated/0
     */
    private static String getLocalRelativeTempIfwPath(File file) {
        return Environment.getExternalStorageDirectory().toURI().relativize(file.toURI()).getPath();
    }

    /**
     * 重构方法，省略IfwEntry的传入
     * @see #parseIfwFile(IfwEntry, int, String, File)
     */
    public static IfwEntry parseIfwFile(int cType,@Nullable String packageName, File tempPath) throws Exception {
        return parseIfwFile(null, cType, packageName, tempPath);
    }

    /**
     * 从ifw文件(tempPath)中读数据到entry中，可以不限包名
     * @param ifw 可以为空，最后会返回最新的IfwEntry
     */
    public static IfwEntry parseIfwFile(@Nullable IfwEntry ifw, int cType, @Nullable String packageName,@NonNull File tempPath) throws Exception {
        if (ifw == null) {
            ifw = new IfwEntry();
        }
        XmlPullParser parser = Xml.newPullParser();
        FileInputStream in = new FileInputStream(tempPath);
        parser.setInput(in, "UTF-8");
        int type;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (type == XmlPullParser.START_TAG && !"rules".equals(parser.getName())) {
                if (((cType & COMPONENT_FLAG_ACTIVITY) > 0) && ("activity".equals(parser.getName()))) {
                    parseComponent(parser, packageName, ifw.activityMap);
                } else if (((cType & COMPONENT_FLAG_RECEIVER) > 0) && ("broadcast".equals(parser.getName()))) {
                    parseComponent(parser, packageName, ifw.receiverMap);
                } else if (((cType & COMPONENT_FLAG_SERVICE) > 0) && ("service".equals(parser.getName()))) {
                    parseComponent(parser, packageName, ifw.serviceMap);
                }
            }
        }
        in.close();
        return ifw;
    }

    /**
     * @param packageName 限定解析的App包名
     * @param map         packagename -> Set<className>
     */
    private static void parseComponent(XmlPullParser parser,
                                       @Nullable String packageName,
                                       @NonNull Map<String, Set<String>> map) {
        if (!"true".equals(parser.getAttributeValue(null, "block"))) {
            return;
        }
        int depth = parser.getDepth();
        int type;
        try {
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT &&
                    (type != XmlPullParser.END_TAG || parser.getDepth() > depth)) {
                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                    continue;
                }
                if ("component-filter".equals(parser.getName())) {
                    String cName = parser.getAttributeValue(null, "name");
                    ComponentName c = ComponentName.unflattenFromString(cName);
                    if (c != null && (TextUtils.isEmpty(packageName)
                            || (TextUtils.equals(c.getPackageName(), packageName)))) {
                        Set<String> set = map.get(c.getPackageName());
                        if (set == null) {
                            set = new HashSet<>();
                            map.put(c.getPackageName(), set);
                        }
                        set.add(c.getClassName());
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static class IfwEntry {
        public Map<String, Set<String>> activityMap = new ArrayMap<>();
        public Map<String, Set<String>> receiverMap = new ArrayMap<>();
        public Map<String, Set<String>> serviceMap = new ArrayMap<>();
        public int status;

        public final static IfwEntry ROOT_ERROR, EMPTY;

        static {
            ROOT_ERROR = new IfwEntry();
            ROOT_ERROR.status = -1;

            EMPTY = new IfwEntry();
            EMPTY.status = 0;
        }

        public void clear() {
            if (activityMap != null) {
                activityMap.clear();
            }
            if (receiverMap != null) {
                receiverMap.clear();
            }
            if (serviceMap != null) {
                serviceMap.clear();
            }
        }

    }

}
