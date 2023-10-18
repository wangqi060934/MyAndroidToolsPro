package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cn.wq.myandroidtoolspro.BuildConfig;
import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.recyclerview.toolbar.RecyclerWithToolbarFragment;

/**
 * packageName为空的时候说明打开的是sdcard上的db文件
 */
public class TableListFragment extends RecyclerWithToolbarFragment {
    private TableAdapter mAdapter;
    private String packageName, name, source_dir;
    private boolean hasWal;
    private AsyncTask<Boolean, Void, List<String>> mLoadTableNameTask;
    private Context mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    public static TableListFragment newInstance(Bundle args) {
        TableListFragment fragment = new TableListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new TableAdapter(getActivity());
        setAdapter(mAdapter);

        setHasOptionsMenu(true);

        if (!Utils.checkSDcard(mContext)) {
            return;
        }

        Bundle data = getArguments();
        packageName = data.getString("packageName");
        name = data.getString("name");
        source_dir = data.getString("dir");
        hasWal = data.getBoolean("hasWal");

//		if (Utils.runRootCommand3("cat " + source_dir + " > " + Utils.tempDBPath
//				+ "\n")) {
//			try {
//				SQLiteDatabase db = SQLiteDatabase.openDatabase(Utils.tempDBPath,
//						null, 0);
//				Cursor cursor = db.rawQuery(
//						"SELECT name FROM sqlite_master WHERE type='table'", null);
//				while (cursor.moveToNext()) {
//					mAdapter.add(cursor.getString(0));
//				}
//				cursor.close();
//				db.close();
//			} catch (SQLiteException e) {
//				getFragmentManager().popBackStack();
//				Toast.makeText(getActivity(), "It's not a valid database file.", Toast.LENGTH_SHORT).show();
//			}
//		}

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mBroadcastReceiver, new IntentFilter(ACTION_DIALOG_ACTION));


    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initActionbar(1,name);
        setToolbarLogo(packageName);

        if (mLoadTableNameTask == null) {
            mLoadTableNameTask = new LoadTableNameTask();
            mLoadTableNameTask.execute(true);
        }

    }

    private BroadcastReceiver mBroadcastReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int position = intent.getIntExtra("position", -1);
            if (position < 0) {
                return;
            }
            String tableName = (String) mAdapter.getItem(position);
            if ("sqlite_sequence".equals(tableName)) {
                return;
            }
            SQLiteDatabase db = SQLiteDatabase.openDatabase(Utils.getTempDBPath(getContext()),
                    null, 0);
            try {
                db.execSQL("DROP TABLE `" + tableName+"`");
            } catch (Exception e) {
                Toast.makeText(getContext(), e.toString(), Toast.LENGTH_LONG).show();
            }
            saveToSourceData();
            mLoadTableNameTask = new LoadTableNameTask();
            mLoadTableNameTask.execute(false);
        }
    };

    private class LoadTableNameTask extends AsyncTask<Boolean, Void, List<String>> {
        int errorType;

        private boolean copyDBToTemp(){
            final String pkg = mContext.getPackageName();
            final String fullPath =source_dir + "/" + name;
            if (!hasWal) {
                return Utils.runRootCommand("cp -f "+fullPath+" /sdcard/Android/data/"+ pkg +"/cache/temp2.db \n")
                        || Utils.runRootCommand("cat " + fullPath + " > " + Utils.getTempDBPath(mContext) + "\n")
                        || Utils.runRootCommand("cat " + fullPath + " > " + System.getenv("EXTERNAL_STORAGE") + "/Android/data/"+ pkg +"/cache/temp2.db \n");
            }else{
                return Utils.runRootCommand("cp -f " + fullPath + " /sdcard/Android/data/" + pkg + "/cache/temp2.db \n", "cp -f " + fullPath + "-wal /sdcard/Android/data/" + pkg + "/cache/temp2.db-wal \n")
                        || Utils.runRootCommand("cat " + fullPath + " > " + Utils.getTempDBPath(mContext) + "\n", "cat " + fullPath + "-wal > " + Utils.getTempDBWalPath(mContext) + "\n")
                        || Utils.runRootCommand("cat " + fullPath + " > " + System.getenv("EXTERNAL_STORAGE") + "/Android/data/" + pkg + "/cache/temp2.db \n", "cat " + fullPath + "-wal > " + System.getenv("EXTERNAL_STORAGE") + "/Android/data/" + pkg + "/cache/temp2.db-wal \n");
            }
        }


        @Override
        protected List<String> doInBackground(Boolean... params) {
            //删除的时候不要重新cat
            boolean isFirstTime = false;
            if (params != null && params.length > 0) {
                isFirstTime = params[0];
            }

            List<String> result = new ArrayList<>();

//            final String pkg = mContext.getPackageName();
//            if (!isFirstTime
//                    || Utils.runRootCommand("cp -f "+source_dir+" /sdcard/Android/data/"+ pkg +"/cache/temp2.db \n")
//                    || Utils.runRootCommand("cat " + source_dir + " > " + Utils.getTempDBPath(mContext) + "\n")
//                    || Utils.runRootCommand("cat " + source_dir + " > " + System.getenv("EXTERNAL_STORAGE") + "/Android/data/"+ pkg +"/cache/temp2.db \n")) {
            if(!isFirstTime || copyDBToTemp()){
                try {
                    SQLiteDatabase db = SQLiteDatabase.openDatabase(Utils.getTempDBPath(mContext),
                            null, 0);
                    //collate nocase 让大小写字母相等
                    Cursor cursor = db.rawQuery(
                            "SELECT name FROM sqlite_master WHERE type='table' order by name collate nocase", null);
                    while (cursor.moveToNext()) {
                        result.add(cursor.getString(0));
                    }
                    cursor.close();
                    db.close();
                    return result;
                } catch (SQLiteException e) {
                    errorType = 1;
                    e.printStackTrace();
                    return null;
                }
            } else {
                errorType = 2;
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<String> result) {
            super.onPostExecute(result);
            setListShown(true,isResumed());
            if (result == null) {
                if (errorType == 1) {
                    getFragmentManager().popBackStack();
                    Toast.makeText(mContext, R.string.invalid_db_file, Toast.LENGTH_SHORT).show();
                } else if (errorType == 2) {
                    Toast.makeText(mContext, R.string.failed_to_gain_root, Toast.LENGTH_SHORT).show();
                }
            } else {
                mAdapter.setData(result);
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setListShown(false,true);
        }
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLoadTableNameTask != null) {
            mLoadTableNameTask.cancel(true);
        }
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mBroadcastReceiver);

        //退出此页面就删除db-wal临时文件，可能没有被覆盖
        File tempDBwal=new File(Utils.getTempDBWalPath(getContext()));
        if (tempDBwal.exists()) {
            tempDBwal.delete();
        }
    }

    private class TableAdapter extends RecyclerView.Adapter<VHolder> {
        private List<String> list;
        private Context context;

        TableAdapter(Context context) {
            super();
            this.context = context;
            list = new ArrayList<>();
        }

        public void setData(List<String> list) {
            if (this.list != null) {
                this.list.clear();
                if (list != null) {
                    this.list.addAll(list);
                }
            }
            notifyDataSetChanged();
        }

        public Object getItem(int arg0) {
            return list.get(arg0);
        }

        @Override
        public VHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new VHolder(LayoutInflater.from(context).inflate(
                    R.layout.item_file_list, parent, false));
        }

        @Override
        public void onBindViewHolder(VHolder holder, int position) {
            TextView textView = (TextView) holder.itemView;
            textView.setText(list.get(position));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

    }

    private class VHolder extends RecyclerView.ViewHolder {
        public VHolder(View itemView) {
            super(itemView);

            TextView textView = (TextView) itemView;
            textView.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_sql_table, 0, 0, 0);

            itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Bundle args = new Bundle();
                    args.putString("packageName", packageName);
                    args.putString("table", (String) (mAdapter.getItem(getLayoutPosition())));
                    args.putString("fullPath", source_dir+"/"+name);
                    args.putBoolean("hasWal", hasWal);

                    AppCompatActivity activity = (AppCompatActivity) getActivity();
                    FragmentTransaction ft = activity.getSupportFragmentManager()
                            .beginTransaction();
                    ft.replace(R.id.content,
                            TableSingleFragment.newInstance(args));
            		ft.setTransition(FragmentTransaction.TRANSIT_NONE);
                    ft.addToBackStack(null);
                    ft.commit();
                }
            });

            if (!BuildConfig.isFree) {
                itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
//                    final DialogFragment dialogFragment = new DialogFragment() {
//                        @NonNull
//                        @Override
//                        public Dialog onCreateDialog(Bundle savedInstanceState) {
//                            return new AlertDialog.Builder(getActivity()).setItems(new String[]{getString(R.string.delete)}, new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    if (which == 0) {
//                                        String tableName = (String) mAdapter.getItem(getLayoutPosition());
//                                        if ("sqlite_sequence".equals(tableName)) {
//                                            return;
//                                        }
//                                        SQLiteDatabase db = SQLiteDatabase.openDatabase(Utils.getTempDBPath(getActivity()),
//                                                null, 0);
//                                        try {
//                                            db.execSQL("DROP TABLE " + tableName);
//                                        } catch (Exception e) {
//                                            Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_LONG).show();
//                                        }
//                                        saveToSourceData();
//                                        mLoadTableNameTask = new LoadTableNameTask();
//                                        mLoadTableNameTask.execute(false);
//                                    }
//                                }
//                            }).create();
//                        }
//                    };
                        DialogFragment dialogFragment = InnerDialogFragment.newInstance(getLayoutPosition());
                        dialogFragment.show(getChildFragmentManager(), "delete");
                        return true;
                    }
                });

            }

        }
    }

    public static class InnerDialogFragment extends DialogFragment{

        public static InnerDialogFragment newInstance(int position) {
            Bundle bundle = new Bundle();
            bundle.putInt("position", position);
            InnerDialogFragment f = new InnerDialogFragment();
            f.setArguments(bundle);
            return f;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getContext()).setItems(new String[]{getString(R.string.delete)}, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        int position = getArguments().getInt("position", -1);
                        Intent intent = new Intent(ACTION_DIALOG_ACTION);
                        intent.putExtra("position", position);
                        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
                    }
                }
            }).create();
        }
    }

    private final static String ACTION_DIALOG_ACTION = "action_dialog_action";

    private void saveToSourceData() {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... arg0) {
//                return Utils.runRootCommand("cat /sdcard/Android/data/cn.wq.myandroidtoolspro/cache/temp2.db > "+source_dir+"\n")
//                        || Utils.runRootCommand("cat " + Utils.getTempDBPath(mContext) + " > " + source_dir + "\n")
//                        || Utils.runRootCommand("cat " + System.getenv("EXTERNAL_STORAGE") + "/Android/data/cn.wq.myandroidtoolspro/cache/temp2.db > " + source_dir + "\n");

                final String pkg = mContext.getPackageName();
                final String fullPath = source_dir + "/" + name;
                if (!hasWal) {
                    return Utils.runRootCommand("cat /sdcard/Android/data/"+ pkg +"/cache/temp2.db > "+fullPath+"\n")
                            || Utils.runRootCommand("cat " + Utils.getTempDBPath(mContext) + " > " + fullPath + "\n")
                            || Utils.runRootCommand("cat " + System.getenv("EXTERNAL_STORAGE") + "/Android/data/"+ pkg +"/cache/temp2.db > " + fullPath + "\n");
                }else{
                    return Utils.runRootCommand("cat /sdcard/Android/data/"+ pkg +"/cache/temp2.db > "+fullPath+"\n","cat /sdcard/Android/data/"+ pkg +"/cache/temp2.db-wal > "+fullPath+"-wal\n")
                            || Utils.runRootCommand("cat " + Utils.getTempDBPath(mContext) + " > " + fullPath + "\n","cat " + Utils.getTempDBWalPath(mContext) + " > " + fullPath + "-wal\n")
                            || Utils.runRootCommand("cat " + System.getenv("EXTERNAL_STORAGE") + "/Android/data/"+ pkg +"/cache/temp2.db > " + fullPath + "\n","cat " + System.getenv("EXTERNAL_STORAGE") + "/Android/data/"+ pkg +"/cache/temp2.db-wal > " + fullPath + "-wal\n");
                }

            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if (!result) {
                    Toast.makeText(getActivity(), "Save Failed", Toast.LENGTH_SHORT).show();
                }
            }

        }.execute();
    }





}
