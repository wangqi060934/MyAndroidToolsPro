package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.MenuItemCompat.OnActionExpandListener;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filter.FilterListener;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.wq.myandroidtoolspro.MainActivity;
import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.recyclerview.base.RecyclerListFragment;
import cn.wq.myandroidtoolspro.recyclerview.toolbar.SearchWithToolbarRecyclerFragment;

public class LogFragment extends SearchWithToolbarRecyclerFragment implements FilterListener {
    private LogAdapter mAdapter;
    private LoadLogTask mTask;

    /**
     * 匹配level/tag/pid区,不包括time和log内容
     * <p>
     * 示例：
     * "06-17 07:31:14.172 I/Vold    (   98): Vold 2.1 (the revenge) firing up"
     */
    private Pattern pattern = Pattern
            .compile("(\\w)/([^\\(]+)\\(\\s*(\\d+)\\):");
    // level tag pid
    private final static int TIME_LENGTH = 19;

    private final static String Verbose = "V";
    private final static String Debug = "D";
    private final static String Info = "I";
    private final static String Warn = "W";
    private final static String Error = "E";
    private final static String Assert = "A";

    private static Map<String, Integer> LogLevelMap = new HashMap<>();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        mAdapter = new LogAdapter(getContext());
        setAdapter(mAdapter);
        mTask = new LoadLogTask();
        mTask.execute();

        initActionbar(0, getString(R.string.logcat));

        LogLevelMap.clear();
        LogLevelMap.put(Verbose, 0);
        LogLevelMap.put(Debug, 1);
        LogLevelMap.put(Info, 2);
        LogLevelMap.put(Warn, 3);
        LogLevelMap.put(Error, 4);
        LogLevelMap.put(Assert, 5);
    }

    private String loadLastLine() {
        Process process = null;
        DataOutputStream out = null;
        BufferedReader reader = null;
        try {
            process = Runtime.getRuntime().exec("su");
            out = new DataOutputStream(process.getOutputStream());
            out.writeBytes("logcat -v time -d\n");
            out.writeBytes("exit\n");
            out.flush();

            reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream()), 8192);
            String result = null;
            String line;
            while ((line = reader.readLine()) != null) {
                result = line;
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (reader != null) {
                    reader.close();
                }
                if (process != null) {
                    process.destroy();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private class LoadLogTask extends AsyncTask<Void, LogEntry, String> {
        private String lastLine;
        private boolean flag_lastLine_received, flag_listview_showed;
        private boolean mPause;
        private boolean hasPaused;

        void setPause(boolean isPaused) {
            mPause = isPaused;
            if (isPaused) {
                hasPaused = true;
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            lastLine = loadLastLine();
            if (lastLine == null) {
                return null;
            }
            
            Process process = null;
            DataOutputStream out = null;
            BufferedReader reader = null;
            try {
                process = Runtime.getRuntime().exec("su");
                out = new DataOutputStream(process.getOutputStream());
                out.writeBytes("logcat -v time\n");
                out.writeBytes("exit\n");
                out.flush();

                // process.waitFor();
                // System.out.println(process.exitValue());
                reader = new BufferedReader(new InputStreamReader(
                        process.getInputStream()), 8192);
                String result;
                while ((result = reader.readLine()) != null) {
                    if (isCancelled()) {
                        return null;
                    }

                    if (!Character.isDigit(result.charAt(0))
                            || result.length() <= TIME_LENGTH) {
                        continue;
                    }

                    LogEntry logEntry = new LogEntry();
                    // if (Character.isDigit(result.charAt(0))
                    // && result.length() > TIME_LENGTH) {
                    logEntry.time = result.substring(0, TIME_LENGTH - 1);
                    // }
                    Matcher matcher = pattern.matcher(result);
                    if (matcher.find(TIME_LENGTH)) {
                        logEntry.level = matcher.group(1);
                        logEntry.tag = matcher.group(2);
                        logEntry.pid = matcher.group(3);
                        logEntry.log = result.substring(matcher.end());
                    }

                    if (TextUtils.isEmpty(logEntry.pid)) {
                        continue;
                    }

                    if (result.equals(lastLine)) {
                        flag_lastLine_received = true;
                    }

                    publishProgress(logEntry);
                }
                // return result;
            } catch (IOException e) {
                e.printStackTrace();
                // } catch (InterruptedException e) {
                // e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                    if (reader != null) {
                        reader.close();
                    }
                    if (process != null) {
                        process.destroy();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result == null) {
                Toast.makeText(getContext(), R.string.failed_to_gain_root,
                        Toast.LENGTH_SHORT).show();
                setListShown(true, isResumed());
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setListShown(false, true);
        }

        private void scrollToBottom() {
            getRecyclerListView().post(new Runnable() {
                @Override
                public void run() {
                    scrollToPosition(mAdapter.getItemCount() - 1);
                }
            });
        }

        @Override
        protected void onProgressUpdate(LogEntry... values) {
            super.onProgressUpdate(values);
            if (mPause) {
                mAdapter.justAdd(values[0]);
                return;
            }

            if (flag_lastLine_received) {
                if (!flag_listview_showed) {
                    setListShown(true, true);
//					listView.setFastScrollEnabled(true);
                    flag_listview_showed = true;
                    scrollToBottom();
                } else {
                    if (hasPaused) {
                        hasPaused = false;
                        scrollToBottom();
                    } else {
                        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) getRecyclerListView().getLayoutManager();
                        if (linearLayoutManager.findLastVisibleItemPosition() == mAdapter.getItemCount() - 1) {
                            scrollToBottom();
                        }
                    }
                }

                mAdapter.add(values[0]);
            } else {
                mAdapter.justAdd(values[0]);
            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        mTask.setPause(false);
    }

    @Override
    public void onStop() {
        super.onStop();
        mTask.setPause(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.logcat, menu);

        setSearchHint(getString(R.string.hint_logcat_search));
    }

    @Override
    public void onFilterComplete(int count) {
        scrollToPosition(count - 1);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear:
                mAdapter.clear();
                break;
            case R.id.save:
                if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    Toast.makeText(getContext(), R.string.need_external_storage, Toast.LENGTH_LONG).show();
                    break;
                }

                try {
                    SimpleDateFormat format = new SimpleDateFormat(
                            "yyyy-MM-dd-HH_mm_ss", Locale.getDefault());
                    String path = Environment.getExternalStorageDirectory()
                            + File.separator + "log_" + format.format(new Date()) + ".txt";
                    FileWriter writer = new FileWriter(path);
                    final int count = mAdapter.getItemCount();
                    StringBuilder builder = new StringBuilder();
                    for (int i = 0; i < count; i++) {
                        LogEntry entry = (LogEntry) mAdapter.getItem(i);
                        builder.append(entry.time);
                        builder.append(": ");
                        builder.append(entry.level);
                        builder.append("/");
                        builder.append(entry.tag);
                        builder.append("(");
                        builder.append(entry.pid);
                        builder.append("): ");
                        builder.append(entry.log);
                        builder.append("\n");
                    }
                    writer.write(builder.toString());
                    writer.flush();
                    writer.close();
                    Toast.makeText(getContext(), getString(R.string.format_save_path, path),
                            Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.level_default:
                mAdapter.setLevel(0);
                item.setChecked(true);
                scrollToPosition(mAdapter.getItemCount() - 1);
                break;
            case R.id.level_debug:
                mAdapter.setLevel(1);
                item.setChecked(true);
                scrollToPosition(mAdapter.getItemCount() - 1);
                break;
            case R.id.level_info:
                mAdapter.setLevel(2);
                item.setChecked(true);
                scrollToPosition(mAdapter.getItemCount() - 1);
                break;
            case R.id.level_warn:
                mAdapter.setLevel(3);
                item.setChecked(true);
                scrollToPosition(mAdapter.getItemCount() - 1);
                break;
            case R.id.level_error:
                mAdapter.setLevel(4);
                item.setChecked(true);
                scrollToPosition(mAdapter.getItemCount() - 1);
                break;
            case R.id.level_assert:
                mAdapter.setLevel(5);
                item.setChecked(true);
                scrollToPosition(mAdapter.getItemCount() - 1);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mTask.cancel(true);
    }

    private class LogAdapter extends RecyclerView.Adapter<VHolder> implements Filterable {
        private LinkedList<LogEntry> list;
        private Context context;
        private final static int MAX_LINES = 10000;
        // private Object object = new Object();
        private LinkedList<LogEntry> originalData;
        private int mLevel;
        private CharSequence query;
        private final Object mLock = new Object();

        public LogAdapter(Context context) {
            super();
            this.context = context;
            list = new LinkedList<>();
            originalData = new LinkedList<>();
        }

        public void setLevel(int level) {
            if (level == mLevel) {
                return;
            }
            mLevel = level;
            list.clear();
            for (LogEntry entry : originalData) {
                if (checkLevel(entry) && checkSearch(entry, query)) {
                    list.add(entry);
                }
            }

            notifyDataSetChanged();
        }

        public void clear() {
            list.clear();
            originalData.clear();
            notifyDataSetChanged();
        }

        public void add(LogEntry entry) {
            justAdd(entry);
            notifyDataSetChanged();
        }

        public void justAdd(LogEntry entry) {
            synchronized (mLock) {
                originalData.add(entry);
                if (originalData.size() > MAX_LINES) {
                    originalData.removeFirst();
                }
            }

            if (checkSearch(entry, query) && checkLevel(entry)) {
                list.add(entry);
                if (list.size() > MAX_LINES) {
                    list.removeFirst();
                }
            }
        }

        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public VHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new VHolder(LayoutInflater.from(context).inflate(
                    R.layout.item_logcat_list, parent, false));
        }

        @Override
        public void onBindViewHolder(VHolder holder, int position) {
            LogEntry entry = list.get(position);
            holder.tag.setText(entry.tag);
            holder.pid.setText(entry.pid);
            holder.time.setText(entry.time);
            holder.level.setText(entry.level);
            holder.log.setText(entry.log);
            holder.level.setBackgroundColor(getLogColorForLevel(context, entry.level));

            if (entry.tag != null) {
                holder.tag.setTextColor(entry.tag.hashCode() | 0xff222222);
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        private boolean checkSearch(LogEntry entry, CharSequence constraint) {
            if (TextUtils.isEmpty(constraint)) {
                return true;
            }
            return (!TextUtils.isEmpty(entry.pid) && entry.pid.contains(constraint))
                    || (!TextUtils.isEmpty(entry.log) && entry.log.contains(constraint))
                    || (!TextUtils.isEmpty(entry.tag) && entry.tag.contains(constraint));
        }

        private boolean checkLevel(LogEntry entry) {
            if (mLevel == 0) {
                return true;
            }
            Integer level = LogLevelMap.get(entry.level);
            return level != null && level >= mLevel;
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected void publishResults(CharSequence constraint,
                                              FilterResults result) {
                    list = (LinkedList<LogEntry>) result.values;
                    notifyDataSetChanged();
                }

                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    query = constraint;

                    final FilterResults results = new FilterResults();
                    if (TextUtils.isEmpty(constraint)) {
                        final LinkedList<LogEntry> tempList = new LinkedList<>();
                        synchronized (mLock) {
                            for (LogEntry entry : originalData) {
                                if (checkLevel(entry)) {
                                    tempList.add(entry);
                                }
                            }
                        }

                        results.values = tempList;
                        results.count = tempList.size();
                    } else {
                        final LinkedList<LogEntry> newValues = new LinkedList<>();
                        //TODO:java.util.ConcurrentModificationException
                        //	at java.util.LinkedList$LinkIterator.next(LinkedList.java:124)
                        synchronized (mLock) {
                            for (LogEntry entry : originalData) {
                                if (checkSearch(entry, constraint)
                                        && checkLevel(entry)) {
                                    newValues.add(entry);
                                }
                            }
                        }

                        results.values = newValues;
                        results.count = newValues.size();
                    }
                    return results;
                }
            };
        }


    }

    private class VHolder extends RecyclerView.ViewHolder {
        TextView time, level, tag, pid, log;

        public VHolder(View itemView) {
            super(itemView);
            tag = (TextView) itemView.findViewById(R.id.tag);
            pid = (TextView) itemView.findViewById(R.id.pid);
            time = (TextView) itemView.findViewById(R.id.time);
            level = (TextView) itemView.findViewById(R.id.level);
            log = (TextView) itemView.findViewById(R.id.log);

            itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    MyDialogFragment dialog = new MyDialogFragment();

                    LogEntry entry = (LogEntry) mAdapter.getItem(getLayoutPosition());
                    Bundle args = new Bundle();
                    args.putString("tag", entry.tag);
                    args.putString("level", entry.level);
                    args.putString("pid", entry.pid);
                    args.putString("application", entry.level);
                    args.putString("time", entry.time);
                    args.putString("debug", entry.log);
                    //		args.putInt("position", position);
                    dialog.setArguments(args);
                    dialog.show(getChildFragmentManager(), "dialog");
                }
            });
        }
    }

    private static int getLogColorForLevel(Context context, String level) {
        final int colorId;
        if (Debug.equals(level)) {
            colorId = R.color.log_debug;
        } else if (Error.equals(level)) {
            colorId = R.color.log_error;
        } else if (Warn.equals(level)) {
            colorId = R.color.log_warn;
        } else if (Info.equals(level)) {
            colorId = R.color.log_info;
        } else if (Assert.equals(level)) {
            colorId = R.color.log_error;
        } else {
            colorId = android.R.color.black;
        }
        return ContextCompat.getColor(context, colorId);
    }

    private static class LogEntry {
        String time, level, tag, pid, log;
    }

    public static class MyDialogFragment extends DialogFragment {
        private LogEntry entry;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            entry = new LogEntry();

            Bundle args = getArguments();
            entry.tag = args.getString("tag");
            entry.level = args.getString("level");
            entry.pid = args.getString("pid");
            entry.time = args.getString("time");
            entry.log = args.getString("debug");

//			setStyle(STYLE_NO_TITLE, 0);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            getDialog().setTitle(R.string.detail);

            View rootView = inflater.inflate(R.layout.dialog_logcat, container, false);
            TextView tag = (TextView) rootView.findViewById(R.id.tag);
            TextView level = (TextView) rootView.findViewById(R.id.level);
            TextView pid = (TextView) rootView.findViewById(R.id.pid);
            TextView application = (TextView) rootView.findViewById(R.id.application);
            TextView time = (TextView) rootView.findViewById(R.id.time);
            TextView log = (TextView) rootView.findViewById(R.id.log);

//			final int position=getArguments().getInt("position");
//			LogEntry entry=(LogEntry) mAdapter.getItem(position);
            tag.setText(entry.tag);
//			tag.setTextColor(entry.tag.hashCode() | 0xff555555);

            Integer index = LogLevelMap.get(entry.level);
            if (index != null) {
                String[] levels = getResources().getStringArray(R.array.log_level);
                level.setText(levels[index]);
                level.setTextColor(getLogColorForLevel(getContext(), entry.level));
            }

            pid.setText("PID:" + entry.pid);
            time.setText(getString(R.string.pre_time, entry.time));

            ActivityManager am = (ActivityManager) getContext().getSystemService(Activity.ACTIVITY_SERVICE);
            List<RunningAppProcessInfo> processInfos = am.getRunningAppProcesses();
            for (RunningAppProcessInfo pInfo : processInfos) {
                if (pInfo.pid == Integer.valueOf(entry.pid)) {
                    application.setText(pInfo.processName);
                }
            }

            log.setText(entry.log);
            rootView.findViewById(R.id.button).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    dismiss();
                }
            });
            return rootView;
        }

    }


}
