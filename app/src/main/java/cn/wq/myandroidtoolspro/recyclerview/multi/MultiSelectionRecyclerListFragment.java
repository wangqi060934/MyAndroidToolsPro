package cn.wq.myandroidtoolspro.recyclerview.multi;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.ClipboardManager;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.tencent.mars.xlog.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

import cn.wq.myandroidtoolspro.BaseActivity;
import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.IfwUtil;
import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.model.ComponentEntry;
import cn.wq.myandroidtoolspro.model.ServiceEntry;
import cn.wq.myandroidtoolspro.recyclerview.adapter.AbstractComponentAdapter;
import cn.wq.myandroidtoolspro.recyclerview.base.RecyclerListFragment;
import cn.wq.myandroidtoolspro.recyclerview.base.RecyclerListView;
import cn.wq.myandroidtoolspro.recyclerview.fragment.AppInfoForManageFragment2;
import cn.wq.myandroidtoolspro.recyclerview.fragment.CustomProgressDialogFragment;
import cn.wq.myandroidtoolspro.recyclerview.fragment.ServiceRecyclerListFragment;
import eu.chainfire.libsuperuser.Shell;
import moe.shizuku.api.ShizukuClient;
import moe.shizuku.api.ShizukuPackageManagerV26;

public abstract class MultiSelectionRecyclerListFragment<T extends ComponentEntry>
        extends RecyclerListFragment
        implements ActionMode.Callback, SearchView.OnQueryTextListener, RecyclerListView.OnRecyclerItemClickListener {
    private static final String TAG = "MultiSelRecyclerLF";
    private MultiSelectionUtils.Controller mController;
    private AbstractComponentAdapter<T> mAdapter;

    private SearchView searchView;
    private MenuItem searchMenuItem, toggleMenuItem;
    private Integer[] mSelectedItems;
    private List<ParallellDisableTask> mDisableTasks;
    private AtomicInteger parallelCount;
//    private CustomProgressDialogFragment dialog;
    private LoadDataTask mLoadDataTask;
    private ActionModeLefecycleCallback mActionModeCallBack;
    private Context mContext;
//    private final static String ADMOB_ACTIVITY_NAME = "com.google.android.gms.ads.AdActivity";

    private final static int PARAL_THRETHOLD = 8;//禁用数量较少时不用多任务同时处理
    protected IfwUtil.IfwEntry mIfwEntry;
    protected boolean hasLoadIfw, useParentIfw;

    public interface ActionModeLefecycleCallback {
        void onModeCreated();

        void onModeDestroyed();
    }

    public void addActionModeLefecycleCallback(ActionModeLefecycleCallback callback) {
        mActionModeCallBack = callback;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);
        mAdapter = generateAdapter();
        setAdapter(mAdapter);
        setEmptyText(getString(R.string.empty));

        Bundle data = getArguments();
        if (data != null) {
            useParentIfw = data.getBoolean("useParentIfw", false);
        }
        //是否是子fragment,父fragment设置title就行了
//        if (data != null && !data.getBoolean("part")) {
//            ((MainActivity) getActivity()).resetActionbar(true, data.getString("title"));
//        }

        // 此前必须setAdapter
        mController = MultiSelectionUtils.attach(this);
        mController.restoreInstanceState(savedInstanceState);

        if (mLoadDataTask != null) {
            mLoadDataTask.cancel(true);
        }
        mLoadDataTask = new LoadDataTask();
        mLoadDataTask.execute();
    }

    public MultiSelectionUtils.Controller getMultiController() {
        return mController;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mController != null) {
            mController.saveInstanceState(outState);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.component, menu);

        searchMenuItem = menu.findItem(R.id.search_component);
        searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
        searchView.setQueryHint(getString(R.string.hint_app_search));
        searchView.setOnQueryTextListener(this);

        toggleMenuItem = menu.findItem(R.id.toggle_name);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().onBackPressed();
                break;
            case R.id.toggle_name:
                boolean isFullMode = mAdapter.toggleName();
                toggleMenuItem.setIcon(isFullMode ? R.drawable.ic_short_name : R.drawable.ic_full_name_white);
                break;
            case R.id.start_multi_select:
                mController.startActionMode();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        toggleMenuItem.setIcon(mAdapter.getIsFullName() ? R.drawable.ic_short_name : R.drawable.ic_full_name_white);
    }

    public void closeActionMode() {
        if (mController != null) {
            mController.finish();
        }
    }

    private class LoadDataTask extends AsyncTask<Void, Void, List<T>> {
        @Override
        protected List<T> doInBackground(Void... params) {
            return loadData();
        }

        @Override
        protected void onPostExecute(List<T> list) {
            super.onPostExecute(list);

            setListShown(true, true);
            mAdapter.setData(list);

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            //不加isAdded() 可能getChildFragmentManager() 会报错：Fragment has not been attached yet.
            if (!sharedPreferences.getBoolean("first_toast", false) && isAdded()
                    && getActivity() != null && !getActivity().isFinishing()) {

                FirstToastDialog dialog = new FirstToastDialog();
                try {
                    dialog.show(getChildFragmentManager(), "first");
                } catch (Exception e) {
                }

                sharedPreferences.edit().putBoolean("first_toast", true).apply();
            }

            if (Utils.isPmByIfw(mContext) && !hasLoadIfw && mIfwEntry != null && mIfwEntry.status < 0) {
                Toast.makeText(mContext, R.string.load_ifw_by_root_failed, Toast.LENGTH_LONG).show();
            }

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setListShown(false, true);
        }
    }

    /**
     * 在loadData之前加载ifw规则
     * @param packageName
     */
    protected void loadDataForIfw(String packageName) {
        if (useParentIfw) {
            mIfwEntry = AppInfoForManageFragment2.mIfwEntry;
        } else {
            try {
                mIfwEntry = IfwUtil.loadIfwFileForPkg(mContext, packageName, IfwUtil.COMPONENT_FLAG_ALL, hasLoadIfw);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "loadDataForIfw error", e);
            }
            if (mIfwEntry != null && mIfwEntry != IfwUtil.IfwEntry.ROOT_ERROR) {
                hasLoadIfw = true;
            }
        }
    }

    /**
     * 加载本app的所有ifw，后缀为{@link IfwUtil#BACKUP_SYSTEM_FILE_EXT_OF_MINE}
     */
    protected void loadAllMyIfw(File ifwTempDir) {
        //外层检测是否是ifw模式
        try {
            mIfwEntry = IfwUtil.loadAllIfwFile(mContext,IfwUtil.COMPONENT_FLAG_ALL, ifwTempDir,hasLoadIfw);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "loadAllMyIfw error", e);
        }
        if (mIfwEntry != null && mIfwEntry != IfwUtil.IfwEntry.ROOT_ERROR) {
            hasLoadIfw = true;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mIfwEntry != null) {
            mIfwEntry.clear();
        }
    }

    abstract protected List<T> loadData();

    abstract protected AbstractComponentAdapter<T> generateAdapter();

    /**
     * disable/enable后update下
     *
     * @param checkedItemPositions 选中的positions
     */
    abstract protected void reloadData(Integer... checkedItemPositions);

    /**
     * Service、Activity、Provider需要处理ifw模式下的禁用
     * @param positions
     */
    abstract protected boolean disableByIfw(Integer... positions);

//        private boolean canUseIfw() {
//            MultiSelectionRecyclerListFragment thisInst = MultiSelectionRecyclerListFragment.this;
//            return thisInst instanceof ServiceRecyclerListFragment
//                    || thisInst instanceof ActivityRecyclerListFragment
//                    || thisInst instanceof ReceiverRecyclerListFragment
//                    //组件全局搜索
//                    || thisInst instanceof SearchComponentInAllFragment;
//        }
    /**
     * 是否支持ifw禁用；provider不支持，另外组件全局模式下也要判断是否是provider
     */
    protected boolean isSupportIfw() {
        return false;
    }


    //--------------------------
    //---------ActionMode-------
    //--------------------------
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.setTitle(R.string.multi_select);
        mode.getMenuInflater().inflate(R.menu.actionmode, menu);
        if (mActionModeCallBack != null) {
            mActionModeCallBack.onModeCreated();
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        if (mActionModeCallBack != null) {
            mActionModeCallBack.onModeDestroyed();
        }
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ok: {
                ArrayList<Integer> selectedPos = mController.getSelectedItemsPosition();
                int total = selectedPos.size();
                if (total == 0) {
                    mode.finish();
                    break;
                }

                mSelectedItems = new Integer[total];
                for (int i = 0; i < total; i++) {
                    mSelectedItems[i] = selectedPos.get(i);
                }

                setProgressDialogVisibility(true);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    final int CORE_POOL_SIZE;
                    //ifw模式不需要并行处理
                    if (total <= PARAL_THRETHOLD || Utils.isPmByIfw(mContext)) {
                        CORE_POOL_SIZE = 1;
                    } else {
                        CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors() + 1;
                    }
                    final int chunckSize = (int) Math.ceil(total / (float) CORE_POOL_SIZE);
                    mDisableTasks = new ArrayList<>(CORE_POOL_SIZE);
                    parallelCount = new AtomicInteger(CORE_POOL_SIZE);

//                mStartTime=System.currentTimeMillis();
                    for (int i = 0; i < CORE_POOL_SIZE; i++) {
                        if (i == CORE_POOL_SIZE - 1) {
                            int length = total - i * chunckSize;
                            if (length <= 0) {
                                parallelCount.decrementAndGet();
                                continue;
                            }
                            mDisableTasks.add(new ParallellDisableTask(mSelectedItems.length));
                            Integer[] dest = new Integer[length];
                            System.arraycopy(mSelectedItems, i * chunckSize, dest, 0, length);
                            mDisableTasks.get(i).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, dest);
                        } else {
                            if (i * chunckSize > total) {
                                parallelCount.decrementAndGet();
                                continue;
                            }
                            int length = chunckSize;
                            if (i * chunckSize + length > total) {
                                length = total - i * chunckSize;
                            }
                            mDisableTasks.add(new ParallellDisableTask(mSelectedItems.length));
                            Integer[] dest = new Integer[length];
                            System.arraycopy(mSelectedItems, i * chunckSize, dest, 0, length);
//                            mTasks[i].executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,dest);
                            mDisableTasks.get(i).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, dest);
                        }
                    }
                } else {
                    parallelCount = new AtomicInteger(1);
                    mDisableTasks = new ArrayList<>(1);
                    mDisableTasks.add(new ParallellDisableTask(mSelectedItems.length));
                    mDisableTasks.get(0).execute(mSelectedItems);
                }
                mode.finish();
            }
            break;
            case R.id.copy: {
                ArrayList<Integer> selectedPos = mController.getSelectedItemsPosition();
                StringBuilder sb = new StringBuilder();
                for (int checkedItemId : selectedPos) {
                    ComponentEntry entry = mAdapter.getItem(checkedItemId);
                    if (mAdapter.getIsFullName()) {
                        sb.append(entry.packageName).append("/").append(entry.className);
                    } else {
                        sb.append(entry.className.substring(entry.className
                                .lastIndexOf(".") + 1));
                    }
                    sb.append(" ");
                }

                ClipboardManager clipboardManager = (ClipboardManager) mContext
                        .getSystemService(Activity.CLIPBOARD_SERVICE);
                clipboardManager.setText(sb.toString());

                Toast.makeText(mContext, R.string.copy_toast,
                        Toast.LENGTH_SHORT).show();
            }
            break;
            case R.id.selectAll: {
                ArrayList<Integer> selectedPos = mController.getSelectedItemsPosition();
                RecyclerView recyclerView = getRecyclerListView();
                if (selectedPos.size() == mAdapter.getItemCount()) {
                    for (int i = 0; i < mAdapter.getItemCount(); i++) {
                        MultiSelectableViewHolder holder = (MultiSelectableViewHolder) recyclerView.findViewHolderForLayoutPosition(i);
                        mController.onStateChanged(i, false);
                        if (holder != null) {
                            holder.setSelected(false);
                        }

                    }
                    mode.finish();
                } else {
                    for (int i = 0; i < mAdapter.getItemCount(); i++) {
                        MultiSelectableViewHolder holder = (MultiSelectableViewHolder) recyclerView.findViewHolderForLayoutPosition(i);
                        mController.onStateChanged(i, true);
                        if (holder != null) {
                            holder.setSelected(true);
                        }
                    }
                }
            }
            break;
            default:
                break;
        }
        return true;
    }

    //--------------------------
    //--------------------------

    public static class FirstToastDialog extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Dialog dialog = new AlertDialog.Builder(getContext())
                    .setTitle(R.string.hint_first_time)
                    .setView(R.layout.dialog_first_toast)
                    .setNegativeButton(R.string.ok, null)
                    .create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setWindowAnimations(R.style.DialogAnimation);
            }
            return dialog;
        }

//        @Override
//        public void onActivityCreated(Bundle savedInstanceState) {
//            super.onActivityCreated(savedInstanceState);
//            getDialog().getWindow().setWindowAnimations(R.style.DialogAnimation);
//        }
    }

    protected Comparator<T> comparator = new Comparator<T>() {
        @Override
        public int compare(T lhs, T rhs) {
            if (lhs instanceof ServiceEntry) {
                ServiceEntry le = (ServiceEntry) lhs;
                ServiceEntry re = (ServiceEntry) rhs;
                if (le.isRunning && !re.isRunning) {
                    return -1;
                } else if (!le.isRunning && re.isRunning) {
                    return 1;
                }
            }

            if (!lhs.enabled && rhs.enabled) {
                return -1;
            } else if (lhs.enabled && !rhs.enabled) {
                return 1;
            }

            String l = lhs.className
                    .substring(lhs.className.lastIndexOf(".") + 1);
            String r = rhs.className
                    .substring(rhs.className.lastIndexOf(".") + 1);
            return l.compareTo(r);
        }
    };

    // -----------------------------
    // ------------searchview-------
    // -----------------------------
    @Override
    public boolean onQueryTextChange(String query) {
        if (mAdapter != null) {
            mAdapter.getFilter().filter(query);
        }
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }
    // -----------------------------

    /**
     * Android 8.0+获取running service需要用root命令，所以返回loadData()，其它返回boolean
     */
    private class ParallellDisableTask extends AsyncTask<Integer, Void, Object> {
        private boolean isGetServiceFor26up;    //Android O(26)开始获取 service 特殊要处理
        private int errorType;  //1.不允许禁用admob 2.组件实际不存在 3.shizuku失败 4.ifw失败
        private String errorMessage;

        private int total;  //总数

        public ParallellDisableTask(int total) {
            this.total = total;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Utils.debug(this + "||onPreExecute");
        }

        @Override
        protected void onPostExecute(Object result) {
            super.onPostExecute(result);
            int currentPendings = parallelCount.decrementAndGet();

//            System.out.println(result+" : "+parallelCount.get());
            Utils.debug(this + "||onPostExecute,result=[" + result + "],currentPendings:[" + currentPendings + "],isGetServiceFor26up:" + isGetServiceFor26up + ",errorType:" + errorType);

            if (result instanceof Boolean) {    //isGetServiceFor26up 可能返回 false
                boolean r = (boolean) result;
                if (r || errorType == 2) { //禁用了不存在的组件 可以认为成功了
                    if (currentPendings <= 0) {
                        reloadData(mSelectedItems);
                        if (MenuItemCompat.isActionViewExpanded(searchMenuItem)) {
                            mAdapter.getFilter().filter(searchView.getQuery());
                        }
                        setProgressDialogVisibility(false);
                    }

                    if (errorType == 2) {
                        if (total == 1) {
                            showToastInCenter(R.string.component_not_exist);
                        } else {
                            showToastInCenter(R.string.a_component_not_exist);
                        }
                    }
                } else {
                    setProgressDialogVisibility(false);
                    for (ParallellDisableTask mTask : mDisableTasks) {
                        if (mTask != null) {
                            mTask.cancel(true);
                        }
                    }

                    if (errorType == 1) {
                        showToastInCenter(R.string.disallow_disable_admob);
                    } else if (errorType == 3) {
                        showToastInCenter(getString(R.string.shizuku_run_command_failed, errorMessage));
                    } else if (errorType == 4) {
                        showToastInCenter(R.string.ifw_save_failed);
                    } else {
                        showToastInCenter(R.string.failed_to_gain_root);
                    }

                }
                return;
            }

            if (isGetServiceFor26up) {    //android o 8.0 的 service 特殊处理
                mAdapter.setData((List<T>) result);
                if (MenuItemCompat.isActionViewExpanded(searchMenuItem)) {
                    mAdapter.getFilter().filter(searchView.getQuery());
                }
                setProgressDialogVisibility(false);
            }


            Utils.debug(this + ",3333");
        }

        private void showToastInCenter(@StringRes int res) {
            Toast toast = Toast.makeText(mContext, res,
                    Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }

        private void showToastInCenter(String res) {
            Toast toast = Toast.makeText(mContext, res,
                    Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }

        @Override
        protected Object doInBackground(Integer... params) {
            if (isCancelled()) {
                return false;
            }
            isGetServiceFor26up = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && MultiSelectionRecyclerListFragment.this instanceof ServiceRecyclerListFragment;

            int pmChannel = PreferenceManager.getDefaultSharedPreferences(getContext())
                    .getInt(BaseActivity.PREFERENCE_PM_CHANNER,BaseActivity.PM_CHANNEL_ROOT_COMMAND);
            boolean result;
            if (pmChannel == BaseActivity.PM_CHANNEL_SHIZUKU) {
                result = disableByShizuku(params);
            } else if(pmChannel == BaseActivity.PM_CHANNEL_IFW && isSupportIfw()){
                result = disableByIfw(params);
                if (!result) {
                    errorType = 4;
                }
            }else {
                result = disableByPm(params);
            }

            if (result && isGetServiceFor26up) {
                Utils.debug(this + ",ffff");
                // TODO: 2019/3/31 考虑不要重复加载ifw
                return loadData();
            }
            return result;

        }

        private boolean disableByShizuku(Integer... params) {
            PackageManager pm = mContext.getPackageManager();
            ComponentName cName;
            ComponentEntry entry;
            for (int i : params) {
                if (isCancelled()) {
                    return false;
                }
                if (i >= mAdapter.getItemCount()) {
                    continue;
                }
                entry = mAdapter.getItem(i);
                if (entry == null) {
                    continue;
                }
                cName = new ComponentName(entry.packageName, entry.className);
                int newState;
                if (pm.getComponentEnabledSetting(cName) <= PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    //https://blog.csdn.net/linliang815/article/details/76179405
                    //报错shell cannot change component state for */* to 2
                    //api会识别成shell_uid
                    newState = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
                } else {
                    newState = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
                }
                try {
                    Utils.debug(TAG,"shizuku isAuthorized:"+ ShizukuClient.getState().isAuthorized());
                    ShizukuPackageManagerV26.setComponentEnabledSetting(
                            cName,
                            newState,
//                            PackageManager.DONT_KILL_APP,
                            0,
                            0
                    );
                } catch (Exception e) {
                    Log.e(TAG, "shizuku disable error", e);
                    Utils.err(TAG,"shizuku disable error", e);
                    if (e.getMessage() != null) {
                        errorMessage = e.getMessage();
                    }
                    errorType = 3;
                    return false;
                }
            }
            return true;
        }

        private boolean disableByPm(Integer... params) {
            StringBuilder builder = new StringBuilder();
            PackageManager pm = mContext.getPackageManager();
            ComponentName cName;
            ComponentEntry entry;
            for (int i : params) {
                if (isCancelled()) {
                    return false;
                }
                if (i >= mAdapter.getItemCount()) {
                    continue;
                }
                entry = mAdapter.getItem(i);
                if (entry == null) {
                    continue;
                }
                cName = new ComponentName(entry.packageName, entry.className);

                if (pm.getComponentEnabledSetting(cName) <= PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    //free版限制不允许禁止admob
//                    if (BuildConfig.isFree && entry.className.equals(ADMOB_ACTIVITY_NAME)) {
//                        errorType = 1;
//                        return false;
//                    }
                    builder.append("pm disable ");
                } else {
                    builder.append("pm enable ");
                }
                builder.append(entry.packageName);
                builder.append("/");
                builder.append(Matcher.quoteReplacement(entry.className));
                builder.append("\n");
            }
//            return Utils.runRootCommand(builder.toString());
//            return Shell.SU.run(builder.toString()) != null;

            Utils.debug(this + ",4444");

            boolean result;
            if (Shell.SU.available()) {
//                final List<String> ss = Shell.SU.runGetOnlyErr(builder.toString());
//                final Pattern pattern = Pattern.compile("[\\w\\W]+Component class [^ ]+ does not exist in [^ ]+");
//                //禁用多个时 只有最后一个不存在时会影响process.exitValue()返回1
//                if (ss != null && ss.size() > 0 && pattern.matcher(ss.get(ss.size() - 1)).matches()) {
//                    errorType = 2;
//                    result = false;
//                } else {
//                    result = ss != null;
//                }
                int r = Utils.runMultiPmDisableCommand(builder.toString());
                if (r == -2) {
                    errorType = 2;
                }
                result = r > 0;
            } else {
                result = Shell.SU.run(builder.toString()) != null;
            }
            return result;
        }

    }

    private void setProgressDialogVisibility(boolean visible) {
        Utils.debug("setProgressDialogVisibility：" + visible);
        CustomProgressDialogFragment dialog = (CustomProgressDialogFragment) getActivity().getSupportFragmentManager().findFragmentByTag("dialog");
        if (visible) {
            if (dialog == null) {
                dialog = new CustomProgressDialogFragment();
                dialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
                dialog.setCancelable(false);
                Utils.debug("setProgressDialogVisibility：" + 5555);
            }

            //Can not perform this action after onSaveInstanceState
            if (!dialog.isVisible() && getActivity() != null && !getActivity().isFinishing()) {
                try {
                    dialog.show(getActivity().getSupportFragmentManager(), "dialog");
                } catch (Exception e) {
                }
                Utils.debug("setProgressDialogVisibility：" + 6666);
            }
            Utils.debug("setProgressDialogVisibility：" + 7777);
        } else {
            //https://stackoverflow.com/a/19980877/1263423
            //或者判断 dialog.isResumed()
            // TODO: 2019/2/21 如果在后台可能dialog一直不会消失
            if (dialog != null && getFragmentManager()!=null) {
                dialog.dismissAllowingStateLoss();
                Utils.debug("setProgressDialogVisibility：" + 8888);
            }
            Utils.debug("setProgressDialogVisibility：" + 9999);
        }
    }

    @Override
    public void onItemClick(int position, View v) {
        setProgressDialogVisibility(true);
        parallelCount = new AtomicInteger(1);

        mSelectedItems = new Integer[]{position};
        mDisableTasks = new ArrayList<>(1);
        mDisableTasks.add(new ParallellDisableTask(1));
        mDisableTasks.get(0).execute(position);
    }

}
