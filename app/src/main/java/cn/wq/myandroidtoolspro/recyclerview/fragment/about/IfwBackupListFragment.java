package cn.wq.myandroidtoolspro.recyclerview.fragment.about;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.IfwUtil;
import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.helper.ZipUtil;
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

/**
 * ifw备份文件编辑显示app列表
 */
@Deprecated
public class IfwBackupListFragment  extends RecyclerWithToolbarFragment {
    private BackupAdapter mAdapter;
    private MenuItem toggleMenuItem;
    private final static int REQUEST_CODE_RENAME = 88;
    private String path;
    private Context mContext;
    private CompositeDisposable compositeDisposable;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
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
        loadData();
    }

    private void loadData() {
        Disposable disposable = Observable.create(new ObservableOnSubscribe<List<BackupEntry>>() {
            @Override
            public void subscribe(ObservableEmitter<List<BackupEntry>> emitter) throws Exception {
                //临时目录
                File dir = new File(getContext().getExternalCacheDir(), UUID.randomUUID().toString());
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                ZipUtil.unzip(path, dir);

                String[] files = dir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.endsWith(IfwUtil.BACKUP_SYSTEM_FILE_EXT_OF_MINE);
                    }
                });

                List<BackupEntry> result = new ArrayList<>();
                PackageManager pm = mContext.getPackageManager();
                for (String f : files) {
                    String pkgName = f.substring(0, f.length() - 5);
                    try {
                        ApplicationInfo aInfo = pm.getApplicationInfo(pkgName, 0);
                        CharSequence appName = aInfo.loadLabel(pm);
                        result.add(new BackupEntry(appName == null ? "" : appName.toString(), aInfo.packageName, f));
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                        new File(f).delete();
                    }

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
    public void onDestroy() {
        super.onDestroy();
        if (compositeDisposable != null) {
            compositeDisposable.clear();
        }
    }

    private class BackupEntry{
        String appName,packageName,filePath;

        public BackupEntry(String appName, String packageName, String filePath) {
            this.appName = appName;
            this.packageName = packageName;
            this.filePath = filePath;
        }
    }

    private class BackupAdapter extends RecyclerView.Adapter {
        private Context mContext;
        private List<BackupEntry> data;

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
            final BackupEntry entry = data.get(position);

            vHolder.appName.setText(entry.appName);
            vHolder.className.setText(entry.packageName);

            Utils.loadApkIcon(IfwBackupListFragment.this, entry.packageName, vHolder.icon);

            vHolder.close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    data.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, getItemCount());
                    new File(entry.filePath).delete();
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
