package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.DBHelper;
import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.recyclerview.base.RecyclerListFragment;

public class UninstalledRecyclerListFragment extends RecyclerListFragment
        implements AppManageParentFragment.FragmentSelectListener {
    private UninstallAdapter mAdapter;
    private LoadUninstalledTask mTask;
    private Context mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onSelected() {
        mAdapter = new UninstallAdapter(getContext());
        setAdapter(mAdapter);

        if (mTask != null) {
            mTask.cancel(true);
        }
        mTask = new LoadUninstalledTask();
        mTask.execute();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setEmptyText(getString(R.string.empty));
    }

    private class LoadUninstalledTask extends AsyncTask<Void, Void, List<UninstallEntry>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setListShown(false,true);
        }

        @Override
        protected void onPostExecute(List<UninstallEntry> localAppEntries) {
            super.onPostExecute(localAppEntries);
            mAdapter.addData(localAppEntries);

            setListShown(true,isResumed());
        }

        @Override
        protected List<UninstallEntry> doInBackground(Void... params) {
            SQLiteDatabase db = DBHelper.getInstance(mContext).getReadableDatabase();
            Cursor cursor = db.query("uninstalled", null, null, null, null, null, null);
            List<UninstallEntry> result = new ArrayList<>();
            if (cursor != null) {
                UninstallEntry entry;
                while (cursor.moveToNext()) {
                    entry = new UninstallEntry();
                    entry.label = cursor.getString(cursor.getColumnIndex("appName"));
                    entry.packageName = cursor.getString(cursor.getColumnIndex("packageName"));
                    entry.sourcePath = cursor.getString(cursor.getColumnIndex("sourcePath"));
                    entry.backupPath = cursor.getString(cursor.getColumnIndex("backupPath"));
                    byte[] bytes = cursor.getBlob(cursor.getColumnIndex("icon"));
                    if (bytes != null) {
                        entry.bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    }

                    result.add(entry);
                }
                cursor.close();
            }
            return result;
        }
    }


    private class UninstallAdapter extends RecyclerView.Adapter<VHolder> {
        private List<UninstallEntry> mList;
        private Context mContext;

        public UninstallAdapter(Context context) {
            super();
            mList = new ArrayList<>();
            mContext = context;
        }

        public void delete(int position) {
            UninstallEntry entry = mList.remove(position);

            SQLiteDatabase db = DBHelper.getInstance(mContext).getWritableDatabase();
            db.delete("uninstalled", "packageName=?", new String[]{entry.packageName});

            notifyItemRemoved(position);
        }

        public void addData(List<UninstallEntry> list) {
            if (mList.size() > 0) {
                mList.clear();
            }
            mList.addAll(list);
            notifyDataSetChanged();
        }

        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public VHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new VHolder(LayoutInflater.from(mContext).inflate(
                    R.layout.item_app_manage_list, parent, false));
        }

        @Override
        public void onBindViewHolder(VHolder holder, int position) {
            final UninstallEntry entry = mList.get(position);
            holder.icon.setImageBitmap(entry.bitmap);
            holder.label.setText(entry.label);
            holder.packageName.setText(entry.packageName);
        }

        @Override
        public int getItemCount() {
            return mList.size();
        }
    }

    private class VHolder extends RecyclerView.ViewHolder{
        ImageView icon;
        TextView label, packageName;

        public VHolder(View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(R.id.icon);
            label = (TextView) itemView.findViewById(R.id.label);
            packageName = (TextView) itemView
                    .findViewById(R.id.packageName);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final int position=getLayoutPosition();
                    final UninstallEntry entry = (UninstallEntry) mAdapter.getItem(position);

                    PopupMenu popupMenu = new PopupMenu(getContext(), view, Gravity.BOTTOM | Gravity.CENTER);
                    popupMenu.inflate(R.menu.uninstall);
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.uninstall_delete:
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            File file = new File(entry.backupPath);
                                            if (file.exists()) {
                                                file.delete();
                                            }

                                            final String apkDir;
                                            final int splashPos = entry.sourcePath.lastIndexOf("/");
                                            if (splashPos != -1) {
                                                apkDir = entry.sourcePath.substring(0, splashPos);
                                            } else {
                                                apkDir = entry.sourcePath;
                                            }

                                            String unmountCommand = Utils.getUnmountSystemCommand(mContext);

                                            Utils.runRootCommand(
//                                                    "mount -o remount,rw /system",
                                                    "rm -rf " + apkDir,
                                                    "rm -rf " + "/data/data/" + entry.packageName);
//                                            Utils.runRootCommand("mount -o remount,ro /system");
                                            Utils.runRootCommand(unmountCommand);
                                        }
                                    }).start();

                                    mAdapter.delete(position);
                                    break;
                                case R.id.uninstall_restore:
                                    new AsyncTask<Void, Void, Boolean>() {
                                        @Override
                                        protected void onPreExecute() {
                                            super.onPreExecute();
                                        }

                                        @Override
                                        protected void onPostExecute(Boolean result) {
                                            super.onPostExecute(result);
                                            if (result) {
                                                mAdapter.delete(position);
                                                Toast toast=Toast.makeText(mContext, R.string.uninstall_restore_success_toast, Toast.LENGTH_SHORT);
                                                toast.setGravity(Gravity.CENTER,0,0);
                                                toast.show();
                                            } else {
                                                Toast.makeText(mContext, R.string.operation_failed, Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                        @Override
                                        protected Boolean doInBackground(Void... params) {
                                            String unmountCommand = Utils.getUnmountSystemCommand(mContext);
                                            if (unmountCommand == null) {
                                                return false;
                                            }

                                            boolean result = Utils.runRootCommand(
//                                                    mountCommand,
                                                    "mkdir -p '" + entry.sourcePath.substring(0, entry.sourcePath.lastIndexOf("/")) + "'",
//                                            "mv -f " + entry.backupPath + " " + entry.sourcePath,
                                                    "cp " + entry.backupPath + " " + entry.sourcePath,
                                                    "chmod 644 " + entry.sourcePath);
                                            if (result) {
                                                File backup = new File(entry.backupPath);
                                                if (backup.exists()) {
                                                    backup.delete();
                                                }
                                            }

//                                            if (Build.VERSION.SDK_INT < 24) {
//                                                Utils.runRootCommand("mount -o remount,ro /system");
//                                            }
                                            Utils.runRootCommand(unmountCommand);
                                            return result;
                                        }
                                    }.execute();
                                    break;
                            }
                            return true;
                        }
                    });
                    popupMenu.show();

                }
            });

        }
    }

    private class UninstallEntry {
        String label, packageName, sourcePath, backupPath;
        Bitmap bitmap;
    }


}



