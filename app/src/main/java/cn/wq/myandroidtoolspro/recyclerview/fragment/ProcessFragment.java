package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug.MemoryInfo;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.model.ProcessEntry;
import cn.wq.myandroidtoolspro.recyclerview.toolbar.SearchWithToolbarRecyclerFragment;

public class ProcessFragment extends SearchWithToolbarRecyclerFragment implements
        LoaderManager.LoaderCallbacks<List<ProcessEntry>> {
    private static final String TAG = "ProcessFragment";
    private ProcessAdapter adapter;
    private static boolean isUseRoot;
    private SharedPreferences sharedPreferences;
    private final static String PROCESS_KEY = "proceess_root_default";

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        adapter = new ProcessAdapter(getActivity());
        setAdapter(adapter);

        setListShown(false, true);
        getLoaderManager().initLoader(0, null, this);

        initActionbar(0, getString(R.string.process));

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        isUseRoot = sharedPreferences.getBoolean(PROCESS_KEY, true);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        setSearchHint(getString(R.string.hint_process_search));

        MenuItem menuItem = menu.add(0, R.id.process_root_default, 1, R.string.process_root_default);
        MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_NEVER);
        menuItem.setCheckable(true);
        menuItem.setChecked(isUseRoot);

        MenuItem refreshItem=menu.add(0,R.id.process_refresh,2,R.string.refresh);
        MenuItemCompat.setShowAsAction(refreshItem, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        refreshItem.setIcon(R.drawable.ic_refresh_white);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.process_refresh:
                scrollToPosition(0);

                getRecyclerListView().post(new Runnable() {
                    @Override
                    public void run() {
                        setListShown(false, isResumed());
                        getLoaderManager().restartLoader(0, null, ProcessFragment.this);
                    }
                });
                break;
            case R.id.process_root_default:
                isUseRoot = !item.isChecked();
                sharedPreferences.edit().putBoolean(PROCESS_KEY, isUseRoot).apply();;
                item.setChecked(isUseRoot);
                getRecyclerListView().post(new Runnable() {
                    @Override
                    public void run() {
                        setListShown(false, isResumed());
                        getLoaderManager().restartLoader(0, null, ProcessFragment.this);
                    }
                });
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private class ProcessAdapter extends RecyclerView.Adapter<VHolder> implements Filterable {
        private Context context;
        private List<ProcessEntry> data;
        private List<ProcessEntry> originalData;
        private final Object mLock = new Object();
        private ProcessFilter mFilter;
        private boolean isLower21;

        @Override
        public VHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new VHolder(LayoutInflater.from(context).inflate(
                    R.layout.item_process_list, parent, false));
        }

        @Override
        public void onBindViewHolder(VHolder holder, int position) {
            final ProcessEntry entry = data.get(position);
            holder.pid.setText(context.getString(R.string.pre_pid, entry.pid));
            // holder.importance.setText(context.getString(R.string.pre_importance,entry.importance));
            if (entry.uid < 0) {
                holder.uid.setVisibility(View.GONE);
            } else {
                holder.uid.setVisibility(View.VISIBLE);
                holder.uid.setText(context.getString(R.string.pre_uid, entry.uid));
            }

            holder.icon.setImageDrawable(entry.icon);
            holder.processName.setText(entry.processName);

            if (isLower21) {
                holder.memory.setText(context.getString(R.string.pre_pss_memory,
                        (float) entry.memory / 1024));
                holder.pkgList.setText(entry.pkgList);
            } else {
                if (entry.memory < 0) {
                    holder.memory.setVisibility(View.GONE);
                } else {
                    holder.memory.setText(context.getString(R.string.pre_rss_memory,
                            (float) entry.memory / 1024));
                    holder.memory.setVisibility(View.VISIBLE);
                }
            }

            holder.stop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    new AsyncTask<Void, Void, Boolean>() {
                        @Override
                        protected Boolean doInBackground(Void... arg0) {
//							return Utils.runRootCommand3("kill "+entry.pid)!=null;
                            return Utils.runRootCommand("kill " + entry.pid);
                        }

                        @Override
                        protected void onPostExecute(Boolean result) {
                            super.onPostExecute(result);
                            if (result) {
                                setListShown(false, isResumed());
                                getLoaderManager().restartLoader(0, null, ProcessFragment.this);
                            } else {
                                Toast.makeText(context, R.string.failed_to_gain_root, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }.execute();
                }
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        private class ProcessFilter extends Filter {
            @Override
            protected void publishResults(CharSequence constraint,
                                          FilterResults results) {
                data = (List<ProcessEntry>) results.values;
                notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                final FilterResults results = new FilterResults();
                if (originalData == null) {
                    synchronized (mLock) {
                        originalData = new ArrayList<>(data);
                    }
                }

                List<ProcessEntry> list;
                if (TextUtils.isEmpty(constraint)) {
                    synchronized (mLock) {
                        list = new ArrayList<>(originalData);
                    }
                    results.values = list;
                    results.count = list.size();
                } else {
                    synchronized (mLock) {
                        list = new ArrayList<>(originalData);
                    }

                    final List<ProcessEntry> newValues = new ArrayList<>();
                    for (ProcessEntry entry : list) {
                        if (Integer.toString(entry.pid).contains(constraint)
                                || Integer.toString(entry.uid).contains(constraint)) {
                            newValues.add(entry);
                        }
                    }

                    results.values = newValues;
                    results.count = newValues.size();
                }

                return results;
            }
        }

        @Override
        public Filter getFilter() {
            if (mFilter == null) {
                mFilter = new ProcessFilter();
            }
            return mFilter;
        }

        ProcessAdapter(Context context) {
            super();
            this.context = context;
            this.data = new ArrayList<>();
            isLower21 = Build.VERSION.SDK_INT < 21;
        }

        public void setData(List<ProcessEntry> list) {
            this.data.clear();
            if (list != null) {
                this.data.addAll(list);
            }
            notifyDataSetChanged();
        }

        public Object getItem(int position) {
            return data.get(position);
        }

    }

    private class VHolder extends RecyclerView.ViewHolder {
        TextView processName;
        TextView pid;
        TextView pkgList, pre_pkgList;
        // TextView importance;
        TextView uid;
        TextView memory;
        ImageView icon;
        Button stop;

        public VHolder(View itemView) {
            super(itemView);

            processName = (TextView) itemView
                    .findViewById(R.id.processName);
            pid = (TextView) itemView.findViewById(R.id.pid);
            pkgList = (TextView) itemView
                    .findViewById(R.id.pkgList);
            pre_pkgList = (TextView) itemView.findViewById(R.id.pre_pkgList);
            // holder.importance=(TextView)convertView.findViewById(R.id.importance);
            uid = (TextView) itemView.findViewById(R.id.uid);
            memory = (TextView) itemView
                    .findViewById(R.id.memory);
            icon = (ImageView) itemView.findViewById(R.id.icon);
            stop = (Button) itemView.findViewById(R.id.stop);
            if (Build.VERSION.SDK_INT >= 21) {
                pkgList.setVisibility(View.GONE);
                pre_pkgList.setVisibility(View.GONE);
            }
        }
    }

    private static class ProcessLoader extends AsyncTaskLoader<List<ProcessEntry>> {
        private List<ProcessEntry> mProcesses;
        private Context mContext;

        ProcessLoader(Context context) {
            super(context);
            mContext = context;
        }

        @Override
        public List<ProcessEntry> loadInBackground() {
            if (Build.VERSION.SDK_INT < 21) {
                return loadAllProcessLower21();
            } else {
                return loadAllProcessUpper21(isUseRoot);
            }
        }

        private List<ProcessEntry> loadAllProcessUpper21(boolean isRoot) {
            PackageManager pm = mContext.getPackageManager();
            List<ProcessEntry> result = new ArrayList<>();
            List<String> stdout = isRoot ? Utils.runRootCommandForResult("ps") : Utils.runCommandForResult("ps");
            if (stdout == null || stdout.size() == 0) {
                return result;
            }
            final int FIRST_APP_USER_ID = 10000;
            //http://stackoverflow.com/questions/30619349/android-5-1-1-and-above-getrunningappprocesses-returns-my-application-packag
            //uid: http://blog.csdn.net/annkie/article/details/8111842
            Pattern pattern;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                //                              u0_a53    706   247   876068 10592 __skb_recv 00000000 S com.mipay.wallet
                //                                USER       PID      PPID  VSIZE   RSS   WCHAN          PC   (S)   NAME
                pattern = Pattern.compile("u\\d+_a(\\d+) +(\\d+) +(?:-?\\d+ +)?\\d+ +\\d+ +(\\d+) +[\\w:./]+ +\\w+ +\\w +([\\w:./]+)");
                //特殊情况
                //USER      PID   ADJ   PPID  VSIZE  RSS   WCHAN              PC  NAME
                //u0_a92    1909  200   796   1937456 141844 SyS_epoll_ 7f9a1da9b4 S com.touchtype.swiftkey
            } else {
                pattern = Pattern.compile("app_(\\d+) +(\\d+) +\\d+ +\\d+ +(\\d+) +[\\w:./]+ +\\w+ +\\w +([\\w:./]+)");
            }

            for (String line : stdout) {
                Matcher matcher = pattern.matcher(line);
                if (!matcher.matches()) {
                    continue;
                }
                ProcessEntry entry = new ProcessEntry();
                entry.uid = Integer.parseInt(matcher.group(1)) + FIRST_APP_USER_ID;
                entry.pid = Integer.parseInt(matcher.group(2));
                entry.memory = Integer.parseInt(matcher.group(3));
                entry.processName = matcher.group(4);

                String pkgName;
                final int quoteIndex = entry.processName.indexOf(":");
                if (quoteIndex < 0) {
                    pkgName = entry.processName;
                } else {
                    pkgName = entry.processName.substring(0, quoteIndex);
                }
                try {
                    entry.icon = pm.getApplicationIcon(pkgName);
                } catch (NameNotFoundException e) {
                    entry.icon = mContext.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
                }

                result.add(entry);
            }

            if (result.size() > 0) {
                return result;
            }

            //试试busybox解析
            // 1688 1000       0:00 {otion.superuser} com.genymotion.superuser
            pattern = Pattern.compile(" ?(\\d+) +(\\d+) +\\d+:\\d+ +\\{[\\w:./]+\\} +([\\w:./]+) ?");

            for (String line : stdout) {
                Matcher matcher = pattern.matcher(line);
                if (!matcher.matches()) {
                    continue;
                }
                int userid = Integer.parseInt(matcher.group(2));
                if (userid <= 0) {
                    continue;
                }
                ProcessEntry entry = new ProcessEntry();
                entry.pid = Integer.parseInt(matcher.group(1));
                entry.processName = matcher.group(3);
                entry.uid = -100;
                entry.memory = -100;

                String pkgName;
                final int quoteIndex = entry.processName.indexOf(":");
                if (quoteIndex < 0) {
                    pkgName = entry.processName;
                } else {
                    pkgName = entry.processName.substring(0, quoteIndex);
                }
                try {
                    entry.icon = pm.getApplicationIcon(pkgName);
                } catch (NameNotFoundException e) {
                    entry.icon = mContext.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
                }
                result.add(entry);
            }

            //busybox的解析方式也没有解析成功，保存结果到文件
            if (result.size() == 0) {
                String path = mContext.getExternalCacheDir() + File.separator + "process_result";
                try {
                    FileWriter writer = new FileWriter(path);
                    for (String line : stdout) {
                        writer.write(line);
                        writer.write("\r\n");
                    }
                    writer.flush();
                    writer.close();
                    com.tencent.mars.xlog.Log.e(TAG, "process result parse empty,saved at " + path);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return result;
        }

        private List<ProcessEntry> loadAllProcessLower21() {
            ActivityManager am = (ActivityManager) mContext
                    .getSystemService(Activity.ACTIVITY_SERVICE);
            PackageManager pm = mContext.getPackageManager();
            List<RunningAppProcessInfo> processInfos = am
                    .getRunningAppProcesses();
            List<ProcessEntry> result = new ArrayList<>();
            int[] pids = new int[processInfos.size()];
            int count = 0;
            for (RunningAppProcessInfo processInfo : processInfos) {
                ProcessEntry entry = new ProcessEntry();
                entry.processName = processInfo.processName;
                // entry.importance=processInfo.importance;
                entry.pid = processInfo.pid;
                entry.uid = processInfo.uid;
                StringBuilder builder = new StringBuilder();
                for (String s : processInfo.pkgList) {
                    builder.append(s);
                    builder.append("\n");

                    if (entry.icon == null) {
                        try {
                            entry.icon = pm.getApplicationIcon(s);
                        } catch (NameNotFoundException e) {
                            e.printStackTrace();
                            entry.icon = mContext.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
                        }
                    }
                }
                builder.deleteCharAt(builder.length() - 1);
                entry.pkgList = builder.toString();
                result.add(entry);

                pids[count++] = processInfo.pid;
            }

            MemoryInfo[] mems = am.getProcessMemoryInfo(pids);
            for (int i = 0; i < count; i++) {
                result.get(i).memory = mems[i].getTotalPss();
            }

            Collections.sort(result, new Comparator<ProcessEntry>() {
                @Override
                public int compare(ProcessEntry lhs, ProcessEntry rhs) {
                    return rhs.memory - lhs.memory;
                }
            });

            return result;
        }

        @Override
        public void deliverResult(List<ProcessEntry> processes) {
            // super.deliverResult(apps);
            if (isReset()) {
                if (processes != null) {
                    onReleaseResources(processes);
                }
            }
            List<ProcessEntry> oldProcesses = processes;
            mProcesses = processes;

            if (isStarted()) {
                super.deliverResult(processes);
            }

            if (oldProcesses != null) {
                onReleaseResources(oldProcesses);
            }
        }

        @Override
        protected void onStartLoading() {
            super.onStartLoading();
            if (mProcesses != null) {
                deliverResult(mProcesses);
            }

            if (mProcesses == null || takeContentChanged()) {
                forceLoad();
            }
        }

        @Override
        public void onCanceled(List<ProcessEntry> data) {
            super.onCanceled(data);
            onReleaseResources(data);
        }

        @Override
        protected void onStopLoading() {
            super.onStopLoading();
            cancelLoad();
        }

        @Override
        protected void onReset() {
            super.onReset();
            onStopLoading();

            if (mProcesses != null) {
                onReleaseResources(mProcesses);
                mProcesses = null;
            }

        }

        protected void onReleaseResources(List<ProcessEntry> apps) {
        }
    }

    @Override
    public Loader<List<ProcessEntry>> onCreateLoader(int arg0, Bundle arg1) {
        return new ProcessLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<ProcessEntry>> arg0,
                               List<ProcessEntry> list) {
        adapter.setData(list);
        setListShown(true, isResumed());
        setHasOptionsMenu(true);
    }

    @Override
    public void onLoaderReset(Loader<List<ProcessEntry>> arg0) {
        adapter.setData(null);
    }

}
