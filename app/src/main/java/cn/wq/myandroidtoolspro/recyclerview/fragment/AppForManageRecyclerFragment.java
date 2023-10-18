package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.DBHelper;
import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.recyclerview.base.SearchRecyclerListFragment;
import cn.wq.myandroidtoolspro.recyclerview.fragment.current.FloatingPermissionDialogFragment;

public class AppForManageRecyclerFragment extends SearchRecyclerListFragment
        implements AppManageParentFragment.FragmentSelectListener {
    private static final String TAG = "AppForManageRecyclerFra";
    private AppAdapter mAdapter;
    private boolean isDisabled;
    private int clicked_pos;
    private LoadAppsTask mTask;
    private ChangeReceiver mChangeReceiver;
    private Context mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        isDisabled = args.getBoolean("isDisabled");
    }

    @Override
    public void onSelected() {
//        Bundle args = getArguments();
//        isDisabled = args.getBoolean("isDisabled");

        queryBefore = null;
        mAdapter = new AppAdapter(mContext);
        setAdapter(mAdapter);

        if (mTask != null) {
            mTask.cancel(true);
        }
        mTask = new LoadAppsTask();
        mTask.execute();
    }

    public static AppForManageRecyclerFragment newInstance(boolean isDisabled) {
        AppForManageRecyclerFragment fragment = new AppForManageRecyclerFragment();

        Bundle args = new Bundle();
        args.putBoolean("isDisabled", isDisabled);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//        if (mAdapter == null) {
        //加载完再显示menu
//            setHasOptionsMenu(false);
//            onSelected();
//        }
    }

    @Override
    protected boolean initOptionsMenuOnCreate() {
        return false;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        //onCreateOptionsMenu和onPrepareOptionsMenu返回时会调用两次，只有放在onPrepareOptionsMenu里才能展开
        //onCreateOptionsMenu第二次searchView.getQuery()为空
        if (!TextUtils.isEmpty(queryBefore)) {
//            searchView.onActionViewExpanded();
//            searchView.setIconified(false);
            expandSearchView();         //fixme:放在onCreateOptionsMenu里没用
            searchView.setQuery(queryBefore, false);
        }
    }

    private CharSequence queryBefore;   //解决输入框问题

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem menuItem = menu.add(0, R.id.menu_sort_id, 0, R.string.sort_by);
        menuItem.setIcon(R.drawable.ic_sort_white);
        MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM
                | MenuItemCompat.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

        if (searchView != null && !TextUtils.isEmpty(searchView.getQuery())) {
//        if (searchView != null) {
            queryBefore = searchView.getQuery();
        }
        super.onCreateOptionsMenu(menu, inflater);
        setSearchHint(getString(R.string.hint_app_manage_search));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_sort_id) {
            searchView.setQuery("", false);
            searchView.clearFocus();

            SortDialogFragment dialog = new SortDialogFragment();
            dialog.show(getChildFragmentManager(), "sort");
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SortDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
            return new AlertDialog.Builder(getContext())
                    .setTitle(R.string.sort_by)
                    .setSingleChoiceItems(R.array.sort_app, sp.getInt("sort_app", 2),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    sp.edit().putInt("sort_app", which).apply();
                                    LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(Utils.ACTION_APP_SORT));
                                    dialog.dismiss();
                                }
                            })
                    .setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {
                                }
                            })
                    .create();
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) { //disable or enable
//                    Intent intent=new Intent(Utils.ACTION_APP_CHANGE);
//                    intent.putExtra("isDisabled", isDisabled);
//                    intent.putExtra("app", (LocalAppEntry) mAdapter.getItem(clicked_pos));
//                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                mAdapter.delete(clicked_pos);

                if (data != null && data.getBooleanExtra("isDisabled", false)) {
                    Intent startIntent = mContext.getPackageManager().getLaunchIntentForPackage(data.getStringExtra("packageName"));
                    if (startIntent != null) {
                        startActivity(startIntent);
                    }
                }
            } else if (resultCode == Activity.RESULT_FIRST_USER) { //uninstall
                mAdapter.delete(clicked_pos);
            }
        }
    }

    private class LoadAppsTask extends AsyncTask<Void, Void, List<LocalAppEntry>> {
        @Override
        protected List<LocalAppEntry> doInBackground(Void... arg0) {
//				List<String> list=Utils.runRootCommandForResult("pm list packages " + (isDisabled ? "-d" : "-e"));
//                if(list==null){
//					return null;
//				}

            SQLiteDatabase db = DBHelper.getInstance(getContext()).getReadableDatabase();
            db.beginTransaction();
//                Cursor cursor = db.query("uninstalled", new String[]{"packageName"}, null, null, null, null, null);
//                List<String> uninstallApps = new ArrayList<>();
//                if (cursor != null) {
//                    while (cursor.moveToNext()) {
//                        uninstallApps.add(cursor.getString(0));
//                    }
//                    cursor.close();
//                }

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
            final int sortType = sp.getInt("sort_app", 1);
            List<LocalAppEntry> result = new ArrayList<>();
            if (sortType == 0) {
//                    Cursor c = db.query("app_manage", null, "enabled=?", new String[]{isDisabled ? "0" : "1"}, null, null, "appName asc");
                StringBuilder sb = new StringBuilder("SELECT a.appName,a.packageName,a.time from app_manage as a WHERE a.enabled=")
                        .append(isDisabled ? "0" : "1")
                        .append(" AND a.packageName not in (SELECT b.packageName FROM uninstalled as b) ORDER BY appName asc");

                Cursor c = db.rawQuery(sb.toString(), null);
                if (c != null) {
                    while (c.moveToNext()) {
                        String packageName = c.getString(c.getColumnIndex("packageName"));
//                            if (uninstallApps.contains(packageName)) {
//                                continue;
//                            }
                        LocalAppEntry entry = new LocalAppEntry();
                        entry.label = c.getString(c.getColumnIndex("appName"));
                        entry.packageName = packageName;
                        entry.time = c.getLong(c.getColumnIndex("time"));

                        result.add(entry);
                    }
                    c.close();
                }
            } else if (sortType == 2) {
                StringBuilder sb = new StringBuilder("SELECT a.appName,a.packageName,a.time from app_manage AS a LEFT JOIN app_history AS b ON a.packageName=b.packageName ");
                sb.append(" WHERE a.packageName not in (SELECT c.packageName FROM uninstalled as c) AND ");
                if (isDisabled) {
                    sb.append("a.enabled=0 ORDER BY b.d_time DESC,a.time DESC");
                } else {
                    sb.append("a.enabled=1 ORDER BY b.e_time DESC,a.time DESC");
                }
                Cursor c = db.rawQuery(sb.toString(), null);
                if (c != null) {
                    while (c.moveToNext()) {
                        String packageName = c.getString(c.getColumnIndex("packageName"));
//                            if (uninstallApps.contains(packageName)) {
//                                continue;
//                            }
                        LocalAppEntry entry = new LocalAppEntry();
                        entry.label = c.getString(c.getColumnIndex("appName"));
                        entry.packageName = packageName;
                        entry.time = c.getLong(c.getColumnIndex("time"));

                        result.add(entry);
                    }
                    c.close();
                }
            } else {
                StringBuilder sb = new StringBuilder("SELECT a.appName,a.packageName,a.time from app_manage a ");
                sb.append(" WHERE a.packageName not in (SELECT c.packageName FROM uninstalled as c) AND ");
                if (isDisabled) {
                    sb.append("a.enabled=0 ORDER BY a.time DESC");
                } else {
                    sb.append("a.enabled=1 ORDER BY a.time DESC");
                }
                Cursor c = db.rawQuery(sb.toString(), null);
                if (c != null) {
                    while (c.moveToNext()) {
                        String packageName = c.getString(c.getColumnIndex("packageName"));
//                            if (uninstallApps.contains(packageName)) {
//                                continue;
//                            }
                        LocalAppEntry entry = new LocalAppEntry();
                        entry.label = c.getString(c.getColumnIndex("appName"));
                        entry.packageName = packageName;
                        entry.time = c.getLong(c.getColumnIndex("time"));

                        result.add(entry);
                    }
                    c.close();
                }
            }

            db.setTransactionSuccessful();
            db.endTransaction();
            return result;
        }

        @Override
        protected void onPostExecute(List<LocalAppEntry> result) {
            super.onPostExecute(result);

            if (result == null) {
                setEmptyText(getString(R.string.failed_to_gain_root));
            }

            mAdapter.setList(result);
            setListShown(true, isResumed());
            scrollToPosition(0);

            // fixme:加载和搜索冲突，必须等到加载完才能开始搜索
            setHasOptionsMenu(true);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setHasOptionsMenu(false);   //禁用页面搜搜然后点击查看所有组件信息，再返回结果为空
            setListShown(false, true);
        }

//			private void sortApps(List<LocalAppEntry> list){
//				SharedPreferences sp=PreferenceManager.getDefaultSharedPreferences(getActivity());
//				final int sortType=sp.getInt("sort_app", 1);
//
//				final Collator collator = Collator.getInstance(Locale.getDefault());
//				Collections.sort(list, new Comparator<LocalAppEntry>() {
//                    @Override
//                    public int compare(LocalAppEntry lhs, LocalAppEntry rhs) {
//                        if (sortType == 0) {
//                            return collator.compare(
//                                    Utils.trimFor160(lhs.label), Utils.trimFor160(rhs.label));
//                        } else {
//                            return lhs.time > rhs.time ? -1 : (lhs.time == rhs.time ? 0 : 1);
//                        }
//                    }
//                });
//			}

    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mChangeReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mChangeReceiver == null) {
            mChangeReceiver = new ChangeReceiver();
        }

        IntentFilter filter = new IntentFilter();
//			filter.addAction(Utils.ACTION_APP_CHANGE);
        filter.addAction(Utils.ACTION_APP_SORT);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mChangeReceiver, filter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mTask != null) {
            mTask.cancel(true);
        }
    }

    public void clearSearchViewFocus() {
        if (!TextUtils.isEmpty(searchView.getQuery())) {
            searchView.clearFocus();    //bug:20170625 关闭输入法，避免进入详细仍然显示输入法
        }
    }

    private class ChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
//				if(Utils.ACTION_APP_CHANGE.equals(action)
//						&&intent.getExtras().getBoolean("isDisabled")!=isDisabled){
//					mAdapter.addApp((LocalAppEntry) intent.getParcelableExtra("app"));
//				}else
            if (Utils.ACTION_APP_SORT.equals(action)) {
                if (mAdapter == null) { // 注意
                    return;
                }
                if (mTask != null) {
                    mTask.cancel(true);
                }
                mTask = new LoadAppsTask();
                mTask.execute();
            }

        }
    }

    private class AppAdapter extends RecyclerView.Adapter<VHolder> implements Filterable {
        private Context context;
        private List<LocalAppEntry> list;
        private List<LocalAppEntry> originalData;
        private final Object mLock = new Object();
        private AppFilter mFilter;

        AppAdapter(Context context) {
            super();
            this.context = context;
            list = new ArrayList<>();
        }

        public void setList(List<LocalAppEntry> list) {
            this.list.clear();
            if (list != null) {
                this.list.addAll(list);
            }

            if (originalData != null) {
                synchronized (mLock) {
                    originalData.clear();
                    originalData = null;
                }
            }
            notifyDataSetChanged();
        }

        public void delete(int position) {
            if (list != null) {
                if (position < 0 || position > list.size() - 1) {
                    com.tencent.mars.xlog.Log.e(TAG , "AppForMangeRecyclerFragment delete indexoutOfBounds:" + position + ",total:" + list.size());
                    return;
                }
                LocalAppEntry entry = list.remove(position);
                notifyItemRemoved(position);

                if (originalData != null) {
                    final int index = originalData.indexOf(entry);
                    if (index != -1) {
                        originalData.remove(index);
                    }
                }
            }
        }

        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public VHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new VHolder(LayoutInflater.from(context).inflate(
                    R.layout.item_app_manage_list, parent, false));
        }

        @Override
        public void onBindViewHolder(VHolder holder, int position) {
            final LocalAppEntry entry = list.get(position);
            Utils.loadApkIcon(AppForManageRecyclerFragment.this, entry.packageName, holder.icon);

            holder.label.setText(entry.label);
            holder.packageName.setText(entry.packageName);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        @Override
        public Filter getFilter() {
            if (mFilter == null) {
                mFilter = new AppFilter();
            }
            return mFilter;
        }

        private class AppFilter extends Filter {
            @Override
            protected void publishResults(CharSequence constraint,
                                          FilterResults results) {
                list = (List<LocalAppEntry>) results.values;
                notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                final FilterResults results = new FilterResults();
                if (originalData == null) {
                    synchronized (mLock) {
                        originalData = new ArrayList<>(list);
                    }
                }

                List<LocalAppEntry> tempList = new ArrayList<>(originalData);
                if (TextUtils.isEmpty(constraint)) {
                    results.values = tempList;
                    results.count = tempList.size();
                } else {
                    final List<LocalAppEntry> newValues = new ArrayList<>();
                    String lowercaseQuery = constraint.toString().toLowerCase();
                    for (LocalAppEntry entry : tempList) {
                        if (entry.label.toLowerCase(Locale.getDefault())
                                .contains(lowercaseQuery)
                                ||
                                entry.packageName.toLowerCase(Locale.getDefault()).contains(lowercaseQuery)) {
                            newValues.add(entry);
                        }
                    }

                    results.values = newValues;
                    results.count = newValues.size();
                }

                return results;
            }
        }
    }

    private class VHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView label;
        TextView packageName;

        public VHolder(View itemView) {
            super(itemView);

            icon = (ImageView) itemView.findViewById(R.id.icon);
            label = (TextView) itemView.findViewById(R.id.label);
            packageName = (TextView) itemView
                    .findViewById(R.id.packageName);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (!checkUsageStatForO()) {
//                            Toast.makeText(getContext(), "You need to grant usage access to this app!", Toast.LENGTH_SHORT).show();
                            FloatingPermissionDialogFragment.newInstance(
                                    getString(R.string.usage_permission_hint),
                                    new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                    .show(getChildFragmentManager(), "permission");
                            return;
                        }
                    }

                    int position = clicked_pos = getLayoutPosition();
                    LocalAppEntry entry = (LocalAppEntry) mAdapter.getItem(position);

                    // FIXME: 2019-04-23 trick，没办法啊
                    if (searchView != null) {
                        queryBefore = searchView.getQuery();
                    }

                    HandleDialogFragment dialog = new HandleDialogFragment();
                    dialog.setTargetFragment(AppForManageRecyclerFragment.this, 0);
                    Bundle args = new Bundle();
                    args.putString("label", entry.label);
                    args.putString("packageName", entry.packageName);
                    args.putLong("time", entry.time);
                    args.putBoolean("isDisabled", isDisabled);
                    dialog.setArguments(args);
//                    dialog.show(getChildFragmentManager(), "dialog");
                    try {
                        if (getActivity() != null && !getActivity().isFinishing()) {
                            dialog.show(getFragmentManager(), "dialog");
                        }
                    } catch (Exception e) {
                    }
                }
            });

        }
    }

    //android 8.0
    @TargetApi(Build.VERSION_CODES.O)
    private boolean checkUsageStatForO() {
        AppOpsManager appOpsManager = (AppOpsManager) getContext().getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getContext().getPackageName());
        if (mode == AppOpsManager.MODE_DEFAULT) {
            return getContext().checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED;
        } else {
            return mode == AppOpsManager.MODE_ALLOWED;
        }
    }

    private class LocalAppEntry {
        String label, packageName;
        long time;
    }

}
