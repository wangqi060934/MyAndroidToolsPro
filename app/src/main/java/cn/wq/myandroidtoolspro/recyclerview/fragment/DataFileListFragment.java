package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.wq.myandroidtoolspro.BuildConfig;
import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.recyclerview.toolbar.RecyclerWithToolbarFragment;

public class DataFileListFragment extends RecyclerWithToolbarFragment {
    private String packageName,directoryPath;
    private FileAdapter mAdapter;
    private LoadFilesTask mTask;
    private int type;
//    private final static String ACTION_DIALOG_CLICK = "action_dialog_click";
    private final static int REQUEST_CODE_DIALOG_CLICK = 1000;

    public static DataFileListFragment newInstance(Bundle args) {
        DataFileListFragment fragment = new DataFileListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new FileAdapter(getContext());
        setAdapter(mAdapter);

//        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mBroadcastReceiver,new IntentFilter(ACTION_DIALOG_CLICK));

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(false);

        Bundle data = getArguments();
        packageName = data.getString("packageName");
        directoryPath = data.getString("directoryPath");
        type = data.getInt("type");

        initActionbar(1, data.getString("title"));
        setToolbarLogo(packageName);


        if (mTask == null) {
            mTask = new LoadFilesTask();
            mTask.execute();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTask != null) {
            mTask.cancel(true);
        }
//        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_DIALOG_CLICK) {
            handleDelete(data);
        }
    }

    private void handleDelete(Intent intent) {
        int position = intent.getIntExtra("position", -1);
            if (position < 0) {
                return;
            }
            FileEntry fileEntry = (FileEntry) mAdapter.getItem(position);
            final String cmd = "rm " + fileEntry.fileDir+"/"+fileEntry.name;
            new AsyncTask<String,Void,Boolean>(){
                @Override
                protected Boolean doInBackground(String... param) {
                    return Utils.runRootCommandForResult(param[0]) != null;
                }

                @Override
                protected void onPostExecute(Boolean aBoolean) {
                    super.onPostExecute(aBoolean);

                    if (aBoolean) {
                        mTask = new LoadFilesTask();
                        mTask.execute();
                    }
                }
            }.execute(cmd);
    }

//    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            int position = intent.getIntExtra("position", -1);
//            if (position < 0) {
//                return;
//            }
//            FileEntry fileEntry = (FileEntry) mAdapter.getItem(position);
//
//            if (Utils.runRootCommandForResult("rm " + fileEntry.fileDir+"/"+fileEntry.name) != null) {
//                mTask = new LoadFilesTask();
//                mTask.execute();
//            }
//        }
//    };

    private class LoadFilesTask extends AsyncTask<Void, Void, List<FileEntry>> {
        private String rootPath;
        private final static int PARSE_STYLE_OLD = 1;
        private final static int PARSE_STYLE_NEW = 2;

        //加载File和Dir
        private List<FileEntry> loadAllFile(String dir,@NonNull Set<String> walFileNames) {
            List<FileEntry> result = new ArrayList<>();
            //nexus 5支持ls -F，不一定所有手机支持.
            //标准ls -F是文件夹以 / 结尾
            List<String> fileList = Utils.runRootCommandForResult("ls -F " + dir + "\n");
            if (fileList != null) {
                int parseStyle = -1;
                for (String line : fileList) {
                    String[] array = null;
                    if (parseStyle < 0) {
                        array = line.split(" ", 2);
                        if (array.length == 2) {
                            parseStyle = PARSE_STYLE_OLD;
                        } else {
                            parseStyle = PARSE_STYLE_NEW;
                        }
                    }

                    if (parseStyle == PARSE_STYLE_OLD) {
                        if (array == null) {
                            array = line.split(" ", 2);
                        }
                        if ("-".equals(array[0])) {
//                        FileEntry fileEntry = parseLine(dir, array[1]);
                            FileEntry fileEntry = parseLine(dir, array[1], walFileNames);
                            if (fileEntry != null) {
//                            result.add(parseLine(dir, array[1]));
                                result.add(fileEntry);
                            }
                        } else if ("d".equals(array[0])) {
                            //是目录的话就递归
                            List<FileEntry> entries = loadAllFile(dir + "/" + array[1],walFileNames);
                            if (entries != null) {
                                result.addAll(entries);
                            }
                        }
                    } else { //PARSE_STYLE_NEW
                        if (line.endsWith("/")) {
                            List<FileEntry> entries = loadAllFile(dir + "/" + line, walFileNames);
                            if (entries != null) {
                                result.addAll(entries);
                            }
                        } else {
                            FileEntry fileEntry = parseLine(dir, line, walFileNames);
                            if (fileEntry != null) {
                                result.add(fileEntry);
                            }
                        }
                    }
                }

                for (FileEntry fileEntry : result) {
                    if (walFileNames.contains(fileEntry.fileDir + "/" + fileEntry.name + "-wal")) {
                        fileEntry.hasWal = true;
                    }
                }
            }
            return result;
        }

        //只加载文件
        private List<FileEntry> loadJustFiles() {
            List<String> suResult = Utils.runRootCommandForResult("ls " + rootPath + "\n");
            if (suResult == null) {
                return null;
            }

            Set<String> walFileNames = new HashSet<>();
            List<FileEntry> result = new ArrayList<>();
            for (String line : suResult) {
//                FileEntry fileEntry = parseLine(rootPath, line);
                FileEntry fileEntry = parseLine(rootPath, line, walFileNames);
                if (fileEntry != null) {
//                    result.add(parseLine(rootPath, line));
                    result.add(fileEntry);
                }
            }
            for (FileEntry fileEntry : result) {
                if (walFileNames.contains(fileEntry.fileDir + "/" + fileEntry.name + "-wal")) {
                    fileEntry.hasWal = true;
                }
            }
            return result;
        }

        //2018/5/15 Room中数据都在wal文件里面，必须一起打开
        /**
         * @param walFileNames  完整路径
         */
        private FileEntry parseLine(String dir, String fileName,@NonNull Set<String> walFileNames) {
            if (type == 1) {
                if (/*fileName.endsWith(".db-journal")*/
                        fileName.endsWith("-journal")
                        || fileName.endsWith("-shm")) {
                    return null;
                } else if (fileName.endsWith("-wal")) {
                    walFileNames.add(dir +"/" +fileName);
                    return null;
                }
            }
            FileEntry file = new FileEntry();
            // file.name=line.substring(0, line.lastIndexOf('.'));
            file.name = fileName;
//            file.fileDir = dir + "/" + fileName;
            file.fileDir = dir;
            return file;
        }

        @Override
        protected List<FileEntry> doInBackground(Void... params) {
//            rootPath = "/data/data/" + packageName
//                    + (type == 0 ? "/shared_prefs" : "/databases");
            rootPath = directoryPath;
            Set<String> walFileNames = new HashSet<>();
            List<FileEntry> entries = loadAllFile(rootPath,walFileNames);
            walFileNames.clear();
            if (entries == null) {
                entries = loadJustFiles();
            }
            return entries;
        }

        @Override
        protected void onPostExecute(List<FileEntry> result) {
            super.onPostExecute(result);
            setListShown(true, isResumed());

            if (result == null) {
                setEmptyText(getString(R.string.failed_to_gain_root));
                return;
            }
            mAdapter.setList(result);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setListShown(false, true);
        }

    }


    private class FileEntry {
        String fileDir, name;
        boolean hasWal; //db-wal文件
    }

    private class FileAdapter extends RecyclerView.Adapter<VHolder> {
        private List<FileEntry> list;
        private Context context;

        FileAdapter(Context context) {
            super();
            this.context = context;
            list = new ArrayList<>();
        }

        public void setList(List<FileEntry> list) {
            this.list.clear();
            if (list != null) {
                this.list.addAll(list);
            }
            notifyDataSetChanged();
        }

        public Object getItem(int arg0) {
            return list.get(arg0);
        }

        @Override
        public VHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new VHolder(LayoutInflater.from(context).inflate(
                    R.layout.item_data_in_app_list, parent, false));
        }

        @Override
        public void onBindViewHolder(VHolder holder, int position) {
            holder.textView.setText(list.get(position).name);
            holder.descView.setText(list.get(position).fileDir);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

    }

    private class VHolder extends RecyclerView.ViewHolder {
        TextView textView,descView;

        public VHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(R.id.name);
            descView = (TextView) itemView.findViewById(R.id.desc);
            ImageView icon = (ImageView) itemView.findViewById(R.id.icon);
            if (type == 1) {
                icon.setImageResource(R.drawable.ic_sqlite);
            } else {
                icon.setImageResource(R.drawable.ic_description_white_24dp);
                icon.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP);
            }

//            if (type == 0) {
////                Drawable drawable = ContextCompat.getDrawable(getContext(),R.drawable.ic_file);
//                Drawable drawable = AppCompatResources.getDrawable(getContext(), R.drawable.ic_description_white_24dp);
//                drawable.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP);
//                textView.setCompoundDrawablesWithIntrinsicBounds(
//                        drawable, null, null, null);
//            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getLayoutPosition();
                    final FileEntry file = (FileEntry) mAdapter.getItem(position);

                    Bundle args = new Bundle();
                    args.putString("packageName", packageName);
                    args.putString("name", file.name);
                    args.putString("dir", file.fileDir);
                    args.putBoolean("hasWal", file.hasWal);

                    AppCompatActivity activity = (AppCompatActivity) getActivity();
                    FragmentTransaction ft = activity.getSupportFragmentManager()
                            .beginTransaction();
                    ft.replace(R.id.content,
                            type == 0 ? SPreferenceFragment.newInstance(args)
                                    : TableListFragment.newInstance(args));
                    ft.setTransition(FragmentTransaction.TRANSIT_NONE);
                    ft.addToBackStack(null);
                    ft.commit();
                }
            });

            if (!BuildConfig.isFree) {
                itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
//                    DialogFragment dialogFragment=new DialogFragment(){
//                        @NonNull
//                        @Override
//                        public Dialog onCreateDialog(Bundle savedInstanceState) {
//
//                            return new AlertDialog.Builder(getActivity()).setItems(new String[]{getString(R.string.delete)}, new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    if(which==0){
//                                        FileEntry fileEntry= (FileEntry) mAdapter.getItem(getLayoutPosition());
//                                        if(Utils.runRootCommandForResult("rm "+fileEntry.fileDir)!=null){
//                                            mTask = new LoadFilesTask();
//                                            mTask.execute();
//                                        }
//                                    }
//                                }
//                            }).create();
//
//                        }
//                    };
                        DialogFragment dialogFragment = InnerDialogFragment.newInstance(getLayoutPosition());
                        dialogFragment.setTargetFragment(DataFileListFragment.this, REQUEST_CODE_DIALOG_CLICK);
                        dialogFragment.show(getFragmentManager(), null);
                        return true;
                    }
                });

            }


        }
    }

    public static class InnerDialogFragment extends DialogFragment {
        public static InnerDialogFragment newInstance(int position) {
            InnerDialogFragment f = new InnerDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("position", position);
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
//                        Intent intent = new Intent(ACTION_DIALOG_CLICK);
                        Intent intent = new Intent();
                        intent.putExtra("position", getArguments().getInt("position", -1));
//                        getActivity().sendBroadcast(intent);
//                        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
                        getTargetFragment().onActivityResult(REQUEST_CODE_DIALOG_CLICK, Activity.RESULT_OK,intent);
                    }
                }
            }).create();

        }
    }

}
