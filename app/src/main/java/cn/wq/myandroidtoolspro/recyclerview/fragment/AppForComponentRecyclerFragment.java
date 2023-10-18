package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cn.wq.myandroidtoolspro.BuildConfig;
import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.DBHelper;
import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.model.AppEntry;
import cn.wq.myandroidtoolspro.recyclerview.base.RecyclerListView;
import cn.wq.myandroidtoolspro.recyclerview.base.SearchRecyclerListFragment;

public class AppForComponentRecyclerFragment extends SearchRecyclerListFragment
        implements RecyclerListView.OnRecyclerItemClickListener {
    private boolean isSystem;
    private int type;
    private ComponentAdapter mAdapter;
    private LoadComponentTask mTask;
    private SortChangeReceiver mChangeReceiver;
    private int clicked_pos = -1;
    private static int mReceiverFlag = 0;
    private AsyncTask<Integer, Void, Integer> loadServiceTask;

    private Context mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    public static AppForComponentRecyclerFragment newInstance(boolean isSystem, int type) {
        AppForComponentRecyclerFragment f = new AppForComponentRecyclerFragment();

        Bundle bundle = new Bundle();
        bundle.putBoolean("isSystem", isSystem);
        bundle.putInt("type", type);
        f.setArguments(bundle);
        return f;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Utils.debug(getClass().getSimpleName() + " >> " + isSystem + " , " + type + " , " + (mAdapter == null));
        if (mAdapter == null) {
            Bundle bundle = getArguments();
            isSystem = bundle.getBoolean("isSystem");
            type = bundle.getInt("type");

            mAdapter = new ComponentAdapter(mContext);
            setAdapter(mAdapter);

            executeTask();
        } else {
            if (mAdapter.getItemCount() == 0) {
                executeTask();
            } else if (clicked_pos >= 0) {
                mAdapter.updateLine(clicked_pos, type);
            }
        }
    }

    private void executeTask() {
        if (mTask != null) {
            mTask.cancel(true);
            mTask = null;
        }
        mTask = new LoadComponentTask();
        if (Build.VERSION.SDK_INT >= 11) {
            mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else
            mTask.execute();
    }

    private class LoadComponentTask extends AsyncTask<Void, Void, List<AppEntry>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setListShown(false, true);
        }

        @Override
        protected void onPostExecute(List<AppEntry> result) {
            super.onPostExecute(result);
            if (isCancelled()) {
                return;
            }
            mAdapter.setData(result);
            scrollToPosition(0);
            if (isResumed()) {
                setListShown(true, true);
            } else {
                setListShown(true, false);
            }

            if (type == 1) {
                mReceiverFlag |= (isSystem ? 0x1 : 0x2);
                if (mReceiverFlag == 0x3) {
                    mReceiverFlag = 0;
                    LocalBroadcastManager.getInstance(getActivity())
                            .sendBroadcast(new Intent(Utils.ACTION_RECEIVER_FINISH));
                }
            }
        }

        @Override
        protected List<AppEntry> doInBackground(Void... params) {
            //这段代码会卡主线程
            if (type == 1) {
                synchronized (AppForComponentRecyclerFragment.this) {
                    if (mReceiverFlag == 0) {
                        SQLiteDatabase db = DBHelper.getInstance(mContext).getWritableDatabase();
                        db.delete(DBHelper.APPS_TABLE_NAME, null, null);
                    }
                }
            }

            return Utils.getAppsWithType(mContext, isSystem, type);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (type == 0) {
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mChangeReceiver);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (type == 0) {
            if (mChangeReceiver == null) {
                mChangeReceiver = new SortChangeReceiver();
            }

            LocalBroadcastManager.getInstance(mContext).registerReceiver(mChangeReceiver,
                    new IntentFilter(Utils.ACTION_SORT_CHANGE));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mTask != null) {
            mTask.cancel(true);
            mTask = null;
        }
        if (loadServiceTask != null) {
            loadServiceTask.cancel(true);
        }
    }

    private class SortChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Utils.ACTION_SORT_CHANGE.equals(intent.getAction())) {
                executeTask();
            }
        }
    }

    //参考AppForManageRecyclerFragment
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (!TextUtils.isEmpty(queryBefore)) {
            expandSearchView();
            searchView.setQuery(queryBefore, false);
        }
    }

    private CharSequence queryBefore;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (type == 0) {
            MenuItem menuItem = menu.add(0, R.id.menu_sort_id, 0,
                    R.string.sort_by);
            menuItem.setIcon(R.drawable.ic_sort_white);
            MenuItemCompat
                    .setShowAsAction(
                            menuItem,
                            MenuItemCompat.SHOW_AS_ACTION_IF_ROOM
                                    | MenuItemCompat.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        }

        if (searchView != null && !TextUtils.isEmpty(searchView.getQuery())) {
            queryBefore = searchView.getQuery();
        }

        super.onCreateOptionsMenu(menu, inflater);
        if (!BuildConfig.isFree) {
            menu.findItem(R.id.search_component_in_all_apps).setVisible(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sort_id:
                SortDialogFragment dialog = new SortDialogFragment();
                dialog.show(getChildFragmentManager(), "sort");
                break;
            case R.id.search_component_in_all_apps:
                Fragment fragment = SearchComponentInAllFragment.newInstance(type);
                FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.content, fragment);
                ft.setTransition(FragmentTransaction.TRANSIT_NONE);
                ft.addToBackStack(null);
                ft.commit();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SortDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final SharedPreferences sp = PreferenceManager
                    .getDefaultSharedPreferences(getActivity());
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.sort_by)
                    .setSingleChoiceItems(R.array.sort_service,
                            sp.getInt("sort_service", 0),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    sp.edit().putInt("sort_service", which).apply();
                                    LocalBroadcastManager.getInstance(getActivity())
                                            .sendBroadcast(
                                                    new Intent(
                                                            Utils.ACTION_SORT_CHANGE));
                                    dialog.dismiss();
                                }
                            })
                    .setNegativeButton(R.string.cancel, null).create();
        }

    }

    @Override
    public void onItemClick(int position, View v) {
        if (searchView != null && !TextUtils.isEmpty(searchView.getQuery())) {
            searchView.clearFocus();    //bug:20170711 关闭输入法，避免进入详细仍然显示输入法
        }

        // FIXME: 2019-04-23 trick，没办法啊
        if (searchView != null) {
            queryBefore = searchView.getQuery();
        }

        AppEntry entry = (AppEntry) mAdapter.getItem(position);
        try {
            mContext.getPackageManager().getPackageInfo(
                    entry.packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(),
                    R.string.app_has_been_uninstalled, Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        Bundle args = new Bundle();
        args.putString("packageName", entry.packageName);
        args.putString("title", entry.label);
        clicked_pos = position;

        Fragment fragment;
        switch (type) {
            case 0:
                fragment = ServiceRecyclerListFragment.newInstance(args);
                break;
            case 1:
                fragment = ReceiverRecyclerListFragment.newInstance(args);
                break;
            case 2:
                fragment = ActivityRecyclerListFragment.newInstance(args);
                break;
            default:
                fragment = ProviderRecyclerListFragment.newInstance(args);
                break;
        }

        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.content, fragment);
        ft.setTransition(FragmentTransaction.TRANSIT_NONE);
        ft.addToBackStack(null);
        ft.commit();
    }

    private class VHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView nameText;
        TextView totalNum;
        TextView disableNum;
        TextView runningNum;

        public VHolder(View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(R.id.icon);
            nameText = (TextView) itemView
                    .findViewById(R.id.name);
            totalNum = (TextView) itemView
                    .findViewById(R.id.total_num);
            disableNum = (TextView) itemView
                    .findViewById(R.id.disabled_num);
            runningNum = (TextView) itemView
                    .findViewById(R.id.running_num);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onItemClick(getLayoutPosition(), view);
                }
            });
        }
    }

    private class ComponentAdapter extends RecyclerView.Adapter<VHolder> implements Filterable {
        private Context context;
        private List<AppEntry> list;
        private List<AppEntry> originalData;
        private final Object mLock = new Object();
        private AppFilter mFilter;

        public ComponentAdapter(Context context) {
            super();
            this.context = context;
            list = new ArrayList<>();
        }

        public void setData(List<AppEntry> list) {
            this.list.clear();
            if (list != null) {
                this.list.addAll(list);
            }

            if (originalData != null) {
                originalData.clear();
                originalData = null;
            }
            notifyDataSetChanged();
        }

        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public VHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new VHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_list_for_component, parent, false));
        }

        @Override
        public void onBindViewHolder(VHolder holder, int position) {
            AppEntry entry = list.get(position);
            Utils.loadApkIcon(AppForComponentRecyclerFragment.this, entry.packageName, holder.icon);
            holder.nameText.setText(entry.label);
            holder.totalNum.setText("" + entry.totalNum);

            if (entry.disabledNum == 0) {
                holder.disableNum.setVisibility(View.INVISIBLE);
            } else {
                holder.disableNum.setVisibility(View.VISIBLE);
                holder.disableNum.setText("" + entry.disabledNum);
            }

            int runningNum = entry.runningNum;
            if (runningNum == 0) {
                holder.runningNum.setVisibility(View.GONE);
            } else {
                holder.runningNum.setVisibility(View.VISIBLE);
                holder.runningNum.setText("" + runningNum);
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public void updateLine(final int position, final int type) {
            if (position < 0 || list == null || position > list.size() - 1) {
                return;
            }

            if (Utils.isPmByIfw(mContext)) {
                return;
            }

            if (loadServiceTask != null) {
                loadServiceTask.cancel(true);
            }
            loadServiceTask = new AsyncTask<Integer, Void, Integer>() {
//                    private CustomProgressDialogFragment dialog;

                private boolean checkPositionValid(int position) {
                    return position >= 0 && position < getItemCount();
                }

                @Override
                protected Integer doInBackground(Integer... params) {
                    final int position = params[0];
                    if (!checkPositionValid(position)) {
                        return -1;
                    }
                    AppEntry entry = (AppEntry) getItem(position);
                    entry.disabledNum = Utils.getDisabledNum(type, entry.packageName, context);
                    // 只有service才需要
                    if (type == 0) {
                        entry.runningNum = Utils.getRunningNum(entry.packageName,
                                Utils.getRunningServiceInfos(context, entry.packageName));
                    }
                    //此处为异步处理，可能执行到这里时list已经变了
                    if (!checkPositionValid(position)) {
                        return -1;
                    }
                    list.set(position, entry);
                    return position;
                }

//                    @Override
//                    protected void onPreExecute() {
//                        super.onPreExecute();
//                        dialog = new CustomProgressDialogFragment();
//                        dialog.show(getChildFragmentManager(), "service_for_o");
//                    }

//                    @Override
//                    protected void onCancelled() {
//                        super.onCancelled();
//                        if (dialog != null && dialog.isAdded()) {
//                            dialog.dismissAllowingStateLoss();
//                        }
//                    }

                @Override
                protected void onPostExecute(Integer pos) {
                    super.onPostExecute(pos);
//                        if (dialog != null && dialog.isAdded()) {
//                            dialog.dismissAllowingStateLoss();
//                        }

                    if (pos >= 0) {
                        notifyItemChanged(pos);
                    }
                }
            };
            loadServiceTask.execute(position);
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
                                          Filter.FilterResults results) {
                list = (List<AppEntry>) results.values;
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

                List<AppEntry> tempList;
                if (TextUtils.isEmpty(constraint)) {
                    synchronized (mLock) {
                        tempList = new ArrayList<>(originalData);
                    }
                    results.values = tempList;
                    results.count = tempList.size();
                } else {
                    synchronized (mLock) {
                        tempList = new ArrayList<>(originalData);
                    }

                    final List<AppEntry> newValues = new ArrayList<>();
                    String lowercaseQuery = constraint.toString().toLowerCase();
                    for (AppEntry entry : tempList) {
                        if (entry.label.toLowerCase(Locale.getDefault())
                                .contains(lowercaseQuery)) {
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
}
