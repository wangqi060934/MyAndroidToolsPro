package cn.wq.myandroidtoolspro.recyclerview.fragment.FileChooseDialog;


import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;

import cn.wq.myandroidtoolspro.R;

public class FileChooseDialogFragment extends DialogFragment{
    public final static String PATH_COME_IN = "PATH_COME_IN";
    public final static String PATH_SEND_BACK = "PATH_SEND_BACK";
    private String rootPath,currentPath;
    private FileAdapter mFileAdapter;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rootPath = Environment.getExternalStorageDirectory().getAbsolutePath();

        currentPath = getArguments().getString(PATH_COME_IN);
        if (TextUtils.isEmpty(currentPath) || (!new File(currentPath).exists())) {
            currentPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        }

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //手动设置 scrollbar 无效
//        int screenHeight=getResources().getDisplayMetrics().heightPixels;
//        mRecyclerView = new RecyclerView(getActivity());
//        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
//        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,screenHeight/2);
//        mRecyclerView.setLayoutParams(params);

        View view = LayoutInflater.from(getContext()).inflate(R.layout.fragment_recyclerview, null);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mFileAdapter = new FileAdapter();
        mRecyclerView.setAdapter(mFileAdapter);

        AlertDialog dialog =  new AlertDialog.Builder(getActivity())
                .setTitle(R.string.choose_db_from_sdcard)
                .setView(view)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();   //取消的时候也保存当前目录
                        intent.putExtra(PATH_SEND_BACK, currentPath);
                        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED,intent);
                        dismiss();
                    }
                })
                .setPositiveButton(R.string.parent_directory, null)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                final Button positive = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                if (positive != null) {
                    positive.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (!TextUtils.equals(currentPath, rootPath)) {
                                File curFile = new File(currentPath);
                                currentPath = curFile.getParent();
                                if (!checkCurrentPath()) {
                                    Toast.makeText(getContext(), "The file '" + (currentPath == null ? "" : currentPath) + "' not exists!", Toast.LENGTH_SHORT).show();
                                    currentPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                                }
                                mFileAdapter.changeCurrentPath();
                                mFileAdapter.toUpperPath();

                                final boolean enable = !currentPath.equals(rootPath);
                                if (positive.isEnabled() != enable) {
                                    positive.setEnabled(enable);
                                }
                            }
                        }
                    });
                }

            }
        });
        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mFileAdapter.changeCurrentPath();
    }

    private boolean checkCurrentPath() {
        return !TextUtils.isEmpty(currentPath) && new File(currentPath).exists();
    }

    private class FileAdapter extends RecyclerView.Adapter<FileHolder>{
        private List<FileEntry> data;
        private Stack<Integer> lastTopIndexStack;
        private Stack<Integer> lastTopPosStack;

        FileAdapter() {
            this.data = new ArrayList<>();
            lastTopIndexStack = new Stack<>();
            lastTopPosStack = new Stack<>();
        }

        void changeCurrentPath() {
            loadFiles();
        }

        private Comparator<File> mFileComparator=new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                if (lhs.isDirectory() ^ rhs.isDirectory()) {
                    return lhs.isDirectory() ? -1 : 1;
                }else{
                    if (lhs.getName().startsWith(".") && !rhs.getName().startsWith(".")) {
                        return 1;
                    } else if (!lhs.getName().startsWith(".") && rhs.getName().startsWith(".")) {
                        return -1;
                    }else{
                        return lhs.getName().compareToIgnoreCase(rhs.getName());
                    }
                }
            }
        };

        private void loadFiles() {
            if (!checkCurrentPath()) {
                Toast.makeText(getContext(), "The file '" + (currentPath == null ? "" : currentPath) + "' not exists!", Toast.LENGTH_SHORT).show();
                currentPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                loadFiles();
                return;
            }
            File file = new File(currentPath);
            File[] files = file.listFiles();
            if (files != null) {
                Arrays.sort(files, mFileComparator);
            }
            data.clear();

            final Button positive = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
            final boolean enable = !currentPath.equals(rootPath);
            if (positive !=null && positive.isEnabled() != enable) {
                positive.setEnabled(enable);
            }
            if (enable) {
                data.add(new FileEntry(file.getParent(), FileEntry.TYPE_PARENT));

                String title = currentPath.substring(currentPath.lastIndexOf("/") + 1);
                getDialog().setTitle(title);
            }else{
                getDialog().setTitle(getString(R.string.choose_db_from_sdcard));
            }

            FileEntry entry;
            if (files != null) {
                for (File f : files) {
                    entry = new FileEntry(f.getAbsolutePath(), f.isDirectory() ? FileEntry.TYPE_DIRECTORY : FileEntry.TYPE_FILE);
                    data.add(entry);
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public FileHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new FileHolder(LayoutInflater.from(getActivity()).inflate(R.layout.item_file_choose, parent, false));
        }

        @Override
        public void onBindViewHolder(FileHolder holder, int position) {
            final FileEntry entry = data.get(position);
            if (entry.type == FileEntry.TYPE_FILE) {
                holder.icon.setImageResource(R.drawable.ic_description_white_24dp);
            } else {
                holder.icon.setImageResource(R.drawable.ic_folder_open_white_24dp);
            }

            if (entry.type == FileEntry.TYPE_PARENT) {
                holder.name.setText(" ·· ");
            }else{
                String fileName = entry.path.substring(entry.path.lastIndexOf("/") + 1);
                holder.name.setText(fileName);
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (entry.type == FileEntry.TYPE_FILE) {
                        Intent intent = new Intent();
                        intent.putExtra(PATH_SEND_BACK, entry.path);
                        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK,intent);
                        dismiss();

                        lastTopPosStack.clear();
                        lastTopIndexStack.clear();
                        return;
                    }

                    currentPath = entry.path;

                    final int index = mLayoutManager.findFirstVisibleItemPosition();
                    int top = 0;
                    if (mLayoutManager.getItemCount() > 0) {
                        top = mLayoutManager.getChildAt(0).getTop() - mRecyclerView.getPaddingTop();
                    }

                    changeCurrentPath();
                    if (entry.type == FileEntry.TYPE_PARENT) {
                        toUpperPath();
                    }else{
                        //TYPE_DIRECTORY
                        lastTopPosStack.push(top);
                        lastTopIndexStack.push(index);
                        mLayoutManager.scrollToPositionWithOffset(0,0);
                    }

                }
            });

        }

        void toUpperPath(){
            if (!lastTopIndexStack.isEmpty()) {
                final int lastTopIndex = lastTopIndexStack.pop();
                if (lastTopIndex >= 0 && lastTopIndex < getItemCount()) {
                    if (!lastTopPosStack.isEmpty()) {
                        mLayoutManager.scrollToPositionWithOffset(lastTopIndex, lastTopPosStack.pop());
                        return;
                    }
                }else{
                    //未知原因
                    lastTopIndexStack.clear();
                    lastTopPosStack.clear();
                }
            }
            mLayoutManager.scrollToPositionWithOffset(0,0);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    private class FileHolder extends RecyclerView.ViewHolder{
        ImageView icon;
        TextView name;
         FileHolder(View itemView) {
            super(itemView);
             icon = (ImageView) itemView.findViewById(R.id.icon);
             name = (TextView) itemView.findViewById(R.id.name);
        }
    }

    private class FileEntry{
        final static int TYPE_FILE=0;
        final static int TYPE_DIRECTORY=1;
        final static int TYPE_PARENT=2;

        String path;
        int type;

        FileEntry(String path, int type) {
            this.path = path;
            this.type = type;
        }
    }
}
