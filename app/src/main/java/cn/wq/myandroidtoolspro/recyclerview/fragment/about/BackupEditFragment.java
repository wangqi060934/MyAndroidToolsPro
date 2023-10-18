package cn.wq.myandroidtoolspro.recyclerview.fragment.about;


import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.mars.xlog.Log;

import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import cn.wq.myandroidtoolspro.MainActivity;
import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.IfwUtil;
import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.helper.ZipUtil;
import cn.wq.myandroidtoolspro.model.BackupEntry;
import cn.wq.myandroidtoolspro.recyclerview.fragment.CustomProgressDialogFragment;
import cn.wq.myandroidtoolspro.recyclerview.toolbar.RecyclerWithToolbarFragment;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

// TODO: 2019/4/12
// 6.save as ifw

// 4.备份弹出重命名框太慢，其实不慢
// 5.编辑完后还原dialog
// 7.back with component type

public class BackupEditFragment extends RecyclerWithToolbarFragment implements MainActivity.OnBackListener {
    private static final String TAG = "BackupEditFragment";
    private BackupAdapter mAdapter;
    private String path;
    private MenuItem toggleMenuItem;
    private final static int REQUEST_CODE_RENAME = 88;
    private Context mContext;
    private CompositeDisposable compositeDisposable;
    private boolean isIfwFile;
    private boolean isNewFormat;//是否是包含组件类型的新格式
    private boolean hasChanged; //上个页面的列表需要刷新（改名、删除）
    private boolean hasDelete;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        if (context instanceof MainActivity) {
            ((MainActivity) context).addOnBackListener(this);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        ((MainActivity) mContext).removeOnBackListener();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);
        path = getArguments().getString("path");
        mAdapter = new BackupAdapter(mContext);
        setAdapter(mAdapter);
        setEmptyText(getString(R.string.empty));
        initActionbar(1, path.substring(path.lastIndexOf("/") + 1));

        if (compositeDisposable != null) {
            compositeDisposable.clear();
        }
        isIfwFile = path.endsWith(IfwUtil.BACKUP_LOCAL_FILE_EXT);
        loadData();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.backup_edit, menu);

        toggleMenuItem = menu.findItem(R.id.toggle_name);

        if (!isIfwFile) {
            MenuItem ifw = menu.findItem(R.id.save_as_ifw);
            ifw.setVisible(true);
        }
    }

    @Override
    public void onBack() {
        if (getTargetFragment() != null) {
            getTargetFragment().onActivityResult(
                    getTargetRequestCode(),
                    hasChanged ? Activity.RESULT_OK : (hasDelete ? Activity.RESULT_FIRST_USER : Activity.RESULT_CANCELED),
                    null
            );
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // TODO: 2019/4/14
            //调用的居然是 返回按钮事件，可以在toolbar.setNavigationOnClickListener中处理
//            case android.R.id.home:
//                getActivity().onBackPressed();
//                break;
            case R.id.toggle_name:
                boolean isFullMode = mAdapter.toggleName();
                toggleMenuItem.setIcon(isFullMode ? R.drawable.ic_short_name : R.drawable.ic_full_name_white);
                break;
            case R.id.save:
                save();
                break;
            case R.id.rename:
                RenameDialogFragment renameDialog = RenameDialogFragment.newInstance(isIfwFile, path);
                renameDialog.setTargetFragment(this, REQUEST_CODE_RENAME);
                renameDialog.show(getActivity().getSupportFragmentManager(), "rename");
                break;
            case R.id.save_as_ifw:
                //从menifest中获取类型
                saveAsIfw();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadData() {
        if (isIfwFile) {
            loadDataIfw();
        } else {
            loadDataNotIfw();
        }
    }

    private void saveAsIfw() {
        final int count = mAdapter.getItemCount();
        if (count == 0) {
            Toast.makeText(mContext, R.string.backup_content_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        Disposable disposable = Observable.create(new ObservableOnSubscribe<String>() {
            private void addComs2Pkg(Map<String, List<BackupEntry>> pkg2Coms,BackupEntry entry) {
                List<BackupEntry> coms = pkg2Coms.get(entry.packageName);
                if (coms == null) {
                    coms = new ArrayList<>();
                    pkg2Coms.put(entry.packageName, coms);
                }
                coms.add(entry);
            }

            private void parseComType(Map<String, List<BackupEntry>> pkg2Coms) {
                Map<String, BackupEntry> name2Com = new HashMap<>();
                for (Map.Entry<String, List<BackupEntry>> mapEntry : pkg2Coms.entrySet()) {
                    String packageName = mapEntry.getKey();
                    List<BackupEntry> coms = mapEntry.getValue();
                    name2Com.clear();
                    if (coms != null) {
                        for (BackupEntry entry : coms) {
                            if (entry.className.startsWith(".")) {
                                entry.className = entry.packageName + entry.className;
                            }
                            name2Com.put(entry.className, entry);
                        }
                    }

                    try {
                        Context targetContent = getContext().createPackageContext(packageName, 0);
                        AssetManager assetManager = targetContent.getAssets();
                        String sourceDir = mContext.getPackageManager().getApplicationInfo(packageName, 0).sourceDir;

                        Method addAssetPath = AssetManager.class.getMethod("addAssetPath",
                                String.class);
                        int cookie = (Integer) addAssetPath.invoke(assetManager, sourceDir);
                        XmlResourceParser parser = assetManager.openXmlResourceParser(
                                cookie, "AndroidManifest.xml");
                        Resources resources=targetContent.getResources();

                        final int innerDepth=parser.getDepth();
                        int type;
                        while((type=parser.next())!= XmlPullParser.END_DOCUMENT &&
                                (type!=XmlPullParser.END_TAG||parser.getDepth()>innerDepth)){
                            if(type==XmlPullParser.END_TAG||type==XmlPullParser.TEXT){
                                continue;
                            }

                            String typeName = parser.getName();
                            if ("receiver".equals(typeName)
                                    || "service".equals(typeName)
//                                    || "provider".equals(typeName)
                                    || "activity".equals(typeName) || "activity-alias".equals(typeName)) {

                                String className = Utils.getAttributeValueByName(parser, resources, "name");
                                BackupEntry entry = name2Com.get(className);
                                if (entry == null) {
                                    continue;
                                }
                                switch (typeName.charAt(0)){
                                    case 'a':
                                        entry.cType = IfwUtil.COMPONENT_FLAG_ACTIVITY;
                                        break;
                                    case 'r':
                                        entry.cType = IfwUtil.COMPONENT_FLAG_RECEIVER;
                                        break;
                                    case 's':
                                        entry.cType = IfwUtil.COMPONENT_FLAG_SERVICE;
                                        break;
                                }
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "parseComType error", e);
                    }
                }
            }

            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                if (!isNewFormat) {
                    Map<String, List<BackupEntry>> pkg2Coms = new HashMap<>();
                    PackageManager pm = mContext.getPackageManager();
                    String prePkgName = null;
                    //类似saveToIfwFormat
                    for (int i = 0; i < count; i++) {
                        BackupEntry entry = mAdapter.getItem(i);
                        if (i > 1 && TextUtils.equals(prePkgName, entry.packageName)) {
                            BackupEntry pre = mAdapter.getItem(i - 1);
                            entry.isSystem = pre.isSystem;
                            addComs2Pkg(pkg2Coms, entry);
                        } else {
                            try {
                                ApplicationInfo aInfo = pm.getApplicationInfo(entry.packageName, 0);
                                entry.isSystem = (aInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0;
                            } catch (Exception e) {
                                //loadDataNotIfw时已忽略本机不存在的app
                                continue;
                            }
                            prePkgName = entry.packageName;
                            addComs2Pkg(pkg2Coms, entry);
                        }
                    }

                    parseComType(pkg2Coms);
                    pkg2Coms.clear();
                }

                String newPath = path + IfwUtil.BACKUP_LOCAL_FILE_EXT;
                saveToIfwFormat(newPath);
                emitter.onNext(newPath);
                emitter.onComplete();
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) {
                        CustomProgressDialogFragment.showProgressDialog("saveToIfw",getFragmentManager());
                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        CustomProgressDialogFragment.hideProgressDialog("saveToIfw",getFragmentManager());
                    }
                })
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String result) throws Exception {
                        Toast.makeText(mContext, getString(R.string.format_save_path, result), Toast.LENGTH_SHORT).show();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        throwable.printStackTrace();
                        Toast.makeText(mContext, R.string.operation_failed, Toast.LENGTH_SHORT).show();
                    }
                });

        if (compositeDisposable == null) {
            compositeDisposable = new CompositeDisposable();
        }
        compositeDisposable.add(disposable);
    }

    /**
     * 非ifw模式
     */
    private void loadDataNotIfw() {
        Disposable disposable = Observable.create(new ObservableOnSubscribe<List<BackupEntry>>() {
            @Override
            public void subscribe(ObservableEmitter<List<BackupEntry>> emitter) throws Exception {
                List<BackupEntry> result = new ArrayList<>();
                FileReader fileReader = new FileReader(path);
                BufferedReader reader = new BufferedReader(fileReader);
                String s;
                BackupEntry entry;
                while ((s = reader.readLine()) != null) {
                    String[] strs = s.split(" ");
                    if (strs.length > 1) {
                        s = strs[0].trim();
                    }
                    int index = s.indexOf("/");
                    if (index < 0) {
                        continue;
                    }
                    entry = new BackupEntry();

                    if (strs.length > 1) {
                        String types = strs[1].trim();
                        if (types.length() > 1) {
                            if (!isNewFormat) {
                                isNewFormat = true;
                            }
                            entry.isSystem = types.charAt(0) == 's';
                            switch (types.charAt(1)){
                                case 'a':
                                    entry.cType = IfwUtil.COMPONENT_FLAG_ACTIVITY;
                                    break;
                                case 'r':
                                    entry.cType = IfwUtil.COMPONENT_FLAG_RECEIVER;
                                    break;
                                case 's':
                                    entry.cType = IfwUtil.COMPONENT_FLAG_SERVICE;
                                    break;
                            }
                        }
                    }

                    entry.packageName = s.substring(0, index);
                    entry.className = s.substring(index + 1);
                    if (entry.className.startsWith(".")) {
                        entry.className = entry.packageName + entry.className;
                    }
                    PackageManager pm = mContext.getPackageManager();
                    try {
                        ApplicationInfo aInfo = pm.getApplicationInfo(entry.packageName, 0);
                        CharSequence c = aInfo.loadLabel(pm);
                        if (TextUtils.isEmpty(c)) {
                            entry.appName = entry.packageName;
                        } else {
                            entry.appName = c.toString();
                        }
                        result.add(entry);
                    } catch (PackageManager.NameNotFoundException e) {
//                        e.printStackTrace();
                    }
                }
                fileReader.close();

                emitter.onNext(result);
                emitter.onComplete();
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) {
                        setListShown(false, true);
                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        setListShown(true, true);
                    }
                })
                .subscribe(new Consumer<List<BackupEntry>>() {
                    @Override
                    public void accept(List<BackupEntry> result) throws Exception {
                        mAdapter.addData(result);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        throwable.printStackTrace();
                        Toast.makeText(mContext, R.string.failed_to_load_backup_file, Toast.LENGTH_SHORT).show();
                    }
                });

        if (compositeDisposable == null) {
            compositeDisposable = new CompositeDisposable();
        }
        compositeDisposable.add(disposable);
    }

    private void loadDataIfw() {
        Disposable disposable = Observable.create(new ObservableOnSubscribe<List<BackupEntry>>() {
            private void parseComponent(@NonNull List<BackupEntry> result, Map<String, Set<String>> componentMap,
                                        CharSequence appName, String pkgName,
                                        int cType) {
                if (componentMap == null) {
                    return;
                }
                Set<String> componentSet = componentMap.get(pkgName);
                if (componentSet == null) {
                    return;
                }
                if (cType != IfwUtil.COMPONENT_FLAG_ACTIVITY
                        && cType != IfwUtil.COMPONENT_FLAG_RECEIVER
                        && cType != IfwUtil.COMPONENT_FLAG_SERVICE) {
                    return;
                }
                BackupEntry entry;
                for (String service : componentSet) {
                    entry = new BackupEntry();
                    entry.appName = appName == null ? "" : appName.toString();
                    entry.className = service;
                    entry.packageName = pkgName;
                    entry.cType = cType;
                    result.add(entry);
                }

            }

            @Override
            public void subscribe(ObservableEmitter<List<BackupEntry>> emitter) throws Exception {
                //临时目录
                File dir = new File(mContext.getExternalCacheDir(), UUID.randomUUID().toString());
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                ZipUtil.unzip(path, dir);

                File[] files = dir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.endsWith(IfwUtil.BACKUP_SYSTEM_FILE_EXT_OF_MINE);
                    }
                });

                List<BackupEntry> result = new ArrayList<>();
                PackageManager pm = mContext.getPackageManager();
                for (File f : files) {
                    String pkgName = f.getName().substring(0, f.getName().length() - 5);
                    ApplicationInfo aInfo = null;
                    try {
                        aInfo = pm.getApplicationInfo(pkgName, 0);
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                    if (aInfo == null) {
                        f.delete();
                        continue;
                    }
                    CharSequence appName = aInfo.loadLabel(pm);
                    IfwUtil.IfwEntry ifwEntry = IfwUtil.parseIfwFile(IfwUtil.COMPONENT_FLAG_ALL, pkgName, f);
                    parseComponent(result, ifwEntry.serviceMap, appName, pkgName, IfwUtil.COMPONENT_FLAG_SERVICE);
                    parseComponent(result, ifwEntry.receiverMap, appName, pkgName, IfwUtil.COMPONENT_FLAG_RECEIVER);
                    parseComponent(result, ifwEntry.activityMap, appName, pkgName, IfwUtil.COMPONENT_FLAG_ACTIVITY);
                }
                //临时目录可以删除了
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
                        setListShown(false, true);
                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        setListShown(true, true);
                    }
                })
                .subscribe(new Consumer<List<BackupEntry>>() {
                    @Override
                    public void accept(List<BackupEntry> result) throws Exception {
                        mAdapter.addData(result);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        throwable.printStackTrace();
                        Toast.makeText(mContext, R.string.failed_to_load_backup_file, Toast.LENGTH_SHORT).show();
                    }
                });

        if (compositeDisposable == null) {
            compositeDisposable = new CompositeDisposable();
        }
        compositeDisposable.add(disposable);

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_RENAME) {
            initActionbar(1, data.getStringExtra("path"));
            hasChanged = true;
        }
    }

    private void save() {
        Disposable disposable = Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> emitter) throws Exception {
                if (isIfwFile) {
                    saveToIfwFormat();
                } else {
                    saveNoIfw();
                }
                emitter.onNext(true);
                emitter.onComplete();
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) {
                        CustomProgressDialogFragment.showProgressDialog("save",getFragmentManager());
                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        CustomProgressDialogFragment. hideProgressDialog("save",getFragmentManager());
                    }
                })
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean result) throws Exception {
                        Toast.makeText(mContext, R.string.operation_done, Toast.LENGTH_SHORT).show();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        throwable.printStackTrace();
                        Log.e(TAG, "save error", throwable);
                        Toast.makeText(mContext, R.string.operation_failed, Toast.LENGTH_SHORT).show();
                    }
                });

        if (compositeDisposable == null) {
            compositeDisposable = new CompositeDisposable();
        }
        compositeDisposable.add(disposable);
    }

    private void saveToIfwFormat() throws Exception {
        saveToIfwFormat(path);
    }

    private void saveToIfwFormat(String destIfwPath) throws Exception {
        List<BackupEntry> list = new ArrayList<>();
        String prePkgName = null;
        //临时目录
        File dir = new File(mContext.getExternalCacheDir(), UUID.randomUUID().toString());
        if (!dir.exists()) {
            dir.mkdirs();
        }

        int count = mAdapter.getItemCount();
        for (int i = 0; i < count; i++) {
            BackupEntry entry = mAdapter.getItem(i);
            if (entry.packageName == null) {
                continue;
            }
            if (!TextUtils.equals(prePkgName, entry.packageName)) {
                if (prePkgName == null) {
                    prePkgName = entry.packageName;
                } else {
                    if (list.size() > 0) {
                        //保存上一个app的ifw
                        IfwUtil.saveComponentIfwToLocal(prePkgName, list, dir.getAbsolutePath());
                        list.clear();
                    }
                    prePkgName = entry.packageName;
                }
            }
            list.add(entry);
        }

        if (list.size() > 0 && prePkgName != null) {
            //最后一个app的ifw
            IfwUtil.saveComponentIfwToLocal(prePkgName, list, dir.getAbsolutePath());
            list.clear();
        }

        ZipUtil.zip(dir.listFiles(), destIfwPath);
        //临时目录可以删除了
        if (dir.exists()) {
            dir.delete();
        }
    }

    private void saveNoIfw() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mAdapter.getItemCount(); i++) {
            BackupEntry entry = mAdapter.getItem(i);
            sb.append(entry.packageName)
                    .append("/")
                    //推迟到还原的时候
//                    .append(Matcher.quoteReplacement(entry.className))
                    .append(entry.className)
                    .append("\n");
        }
        FileWriter writer = new FileWriter(path);
        writer.write(sb.toString());
        writer.flush();
        writer.close();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (compositeDisposable != null) {
            compositeDisposable.clear();
        }
    }

    private void handleEmptyBackupFile() {
        new AlertDialog.Builder(mContext)
                .setTitle(R.string.hint_first_time)
                .setMessage(R.string.hint_delete_empty_backup_file)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        File file = new File(path);
                        if (file.exists()) {
                            file.delete();
                        }
                        hasDelete = true;
//                        onBack();
                        getActivity().onBackPressed();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private class BackupAdapter extends RecyclerView.Adapter {
        private Context mContext;
        private List<BackupEntry> data;
        private boolean isFullName;

        BackupAdapter(Context context) {
            mContext = context;
            data = new ArrayList<>();
        }

        void addData(List<BackupEntry> list) {
            data.clear();
            if (list != null) {
                data.addAll(list);
            }
            notifyDataSetChanged();

            if (data.size() == 0) {
                handleEmptyBackupFile();
            }
        }

        public List<BackupEntry> getData() {
            return data;
        }

        boolean toggleName() {
            isFullName = !isFullName;
            notifyDataSetChanged();
            return isFullName;
        }

        BackupEntry getItem(int position) {
            return data.get(position);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new BackupViewHolder(LayoutInflater.from(mContext)
                    .inflate(R.layout.item_backup_component_list, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            BackupViewHolder vHolder = (BackupViewHolder) holder;
            BackupEntry entry = data.get(position);

            if (isFullName) {
                vHolder.className.setText(entry.className);
            } else {
                vHolder.className.setText(entry.className.substring(entry.className
                        .lastIndexOf(".") + 1));
            }

            vHolder.appName.setText(entry.appName);

            Utils.loadApkIcon(BackupEditFragment.this, entry.packageName, vHolder.icon);

            vHolder.close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    data.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, getItemCount());
                }
            });

        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        private class BackupViewHolder extends RecyclerView.ViewHolder {
            TextView appName, className;
            ImageButton close;
            ImageView icon;

            BackupViewHolder(View itemView) {
                super(itemView);
                appName = (TextView) itemView.findViewById(R.id.app_name);
                className = (TextView) itemView.findViewById(R.id.class_name);
                close = (ImageButton) itemView.findViewById(R.id.close);
                icon = (ImageView) itemView.findViewById(R.id.icon);

            }

        }

    }

}
