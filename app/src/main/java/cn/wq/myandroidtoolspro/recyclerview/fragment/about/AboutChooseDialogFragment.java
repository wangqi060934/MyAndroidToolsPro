package cn.wq.myandroidtoolspro.recyclerview.fragment.about;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import cn.wq.myandroidtoolspro.BuildConfig;
import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.IfwUtil;
import cn.wq.myandroidtoolspro.helper.Utils;
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
 * 备份、还原 选择弹出框
 */
public class AboutChooseDialogFragment extends DialogFragment {
    private boolean[] checkedItems = new boolean[]{true, true, true, true};
    private boolean isOnly3rd = true;
    private int checkedItem;    //free
    private int type;
    private ListView listView;
    private ProgressBar progressBar;
    private CompositeDisposable compositeDisposable;
    private ArrayAdapter<String> proAdapter;
    private Button deleteBtn, okBtn, editBtn;
    private Context mContext;
    private static final int REQUEST_CODE_TO_EDIT = 123;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    private List<String> getAllBackupFileNames() {
        final List<String> result = new ArrayList<>();
        File oldFile = new File(Environment.getExternalStorageDirectory() + "/myandroidtoolspro_backup.txt");
        if (oldFile.exists()) {
            result.add(oldFile.getAbsolutePath());
        }
        String dirString = Environment.getExternalStorageDirectory() + "/MyAndroidTools/";
        File dir = new File(dirString);
        if (!dir.exists()) {
            return result;
        }
//        File[] files = dir.listFiles(new FileFilter() {
//            @Override
//            public boolean accept(File f) {
//                if (!f.isFile()) {
//                    return false;
//                }
//                if (Utils.isPmByIfw(mContext)) {
//                    return f.getName().endsWith(IfwUtil.BACKUP_LOCAL_FILE_EXT);
//                } else {
//                    return !f.getName().endsWith(IfwUtil.BACKUP_LOCAL_FILE_EXT);
//                }
//            }
//        });
//        if (files != null && files.length > 0) {
//            Arrays.sort(files, new Comparator<File>() {
//                @Override
//                public int compare(File lhs, File rhs) {
//                    return (int) (rhs.lastModified() - lhs.lastModified());
//                }
//            });
//            for (File f : files) {
//                result.add(f.getName());
//            }
//        }

        File[] files = dir.listFiles();
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                return (rhs.lastModified() < lhs.lastModified()) ? -1 : ((rhs.lastModified() == lhs.lastModified()) ? 0 : 1);
            }
        });
        for (File f : files) {
            if (!f.isFile()) {
                continue;
            }
            boolean isPmByIfw = Utils.isPmByIfw(mContext);
            boolean isIfwFile = f.getName().endsWith(IfwUtil.BACKUP_LOCAL_FILE_EXT);
            if (isPmByIfw && !isIfwFile) {
                continue;
            } else if (!isPmByIfw && isIfwFile) {
                continue;
            }
            result.add(f.getName());
        }
        return result;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //onSaveIns..无效，只能在这里存
        if (type == 1 && !BuildConfig.isFree && listView != null) {
            if (getArguments() == null) {
                Bundle args = new Bundle();
                setArguments(args);
            }
            getArguments().putInt("checkedPos", listView.getCheckedItemPosition());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        clearRx();
    }

    private void clearRx() {
        if (compositeDisposable != null) {
            compositeDisposable.clear();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // type     0:backup    1:restore
        type = getArguments().getInt("type");
        if (type == 0) {
            isOnly3rd = PreferenceManager.getDefaultSharedPreferences(mContext)
                    .getBoolean("backupOnly3rd", false);

            View titleView = LayoutInflater.from(mContext).inflate(R.layout.dialog_backup_title, null);
            SwitchCompat switchCompat = (SwitchCompat) titleView.findViewById(R.id.checkbox);
            switchCompat.setChecked(isOnly3rd);
            switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    isOnly3rd = b;
                    PreferenceManager.getDefaultSharedPreferences(mContext).edit()
                            .putBoolean("backupOnly3rd", isOnly3rd)
                            .apply();
                }
            });

            return new AlertDialog.Builder(mContext)
//						.setTitle(R.string.backup)
                    .setCustomTitle(titleView)
                    .setMultiChoiceItems(R.array.backup_choose,
                            checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which, boolean isChecked) {
                                    checkedItems[which] = isChecked;
                                }
                            })
                    .setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    Intent data = new Intent();
                                    data.putExtra("checkedItems",
                                            checkedItems);
                                    data.putExtra("isOnly3rd", isOnly3rd);
                                    getTargetFragment().onActivityResult(
                                            getTargetRequestCode(),
                                            Activity.RESULT_OK, data);
                                    dismiss();
                                }
                            })
                    .setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    dismiss();
                                }
                            }).create();
        } else {
            if (!BuildConfig.isFree) {
                return createProDialog();
            } else {
                return createFreeDialog();
            }
        }

    }

    private void loadDataForPro() {
        Disposable disposable = Observable.create(new ObservableOnSubscribe<List<String>>() {
            @Override
            public void subscribe(ObservableEmitter<List<String>> emitter) throws Exception {
                List<String> result = getAllBackupFileNames();
                emitter.onNext(result);
                emitter.onComplete();
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) {
                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        progressBar.setVisibility(View.GONE);
                        listView.setVisibility(View.VISIBLE);

                        editBtn.setEnabled(true);
                        okBtn.setEnabled(true);
                        deleteBtn.setEnabled(true);
                    }
                })
                .subscribe(new Consumer<List<String>>() {
                    @Override
                    public void accept(List<String> result) throws Exception {
                        initListviewForPro(result);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });

        if (compositeDisposable == null) {
            compositeDisposable = new CompositeDisposable();
        }
        compositeDisposable.add(disposable);

    }

    private void initListviewForPro(List<String> files) {
        proAdapter = new ArrayAdapter<>(mContext,
                android.R.layout.simple_list_item_single_choice,
                files);
        listView.setAdapter(proAdapter);

        final int pos = getArguments().getInt("checkedPos", -1);
        if (pos >= 0 && pos < proAdapter.getCount()) {
            listView.setItemChecked(pos, true);
        }
    }

    private Dialog createProDialog() {
        View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_restore_view, null);
        final RadioButton disableBtn = (RadioButton) view.findViewById(R.id.disable);
        disableBtn.setChecked(true);
        if (Utils.isPmByIfw(mContext)) {
            RadioGroup disableSetting = view.findViewById(R.id.disable_setting);
            disableSetting.setVisibility(View.GONE);
        }

//                RadioButton enableBtn = (RadioButton) view.findViewById(R.id.enable);
        listView = view.findViewById(R.id.listview);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        progressBar = view.findViewById(R.id.progress_bar);

//        final ArrayAdapter<String> adapter = new ArrayAdapter<>(mContext,
//                android.R.layout.simple_list_item_single_choice,
//                getAllBackupFileNames());
//        listView.setAdapter(adapter);
//
//        final int pos = getArguments().getInt("checkedPos", -1);
//        if (pos >= 0 && pos < adapter.getCount()) {
//            listView.setItemChecked(pos, true);
//        }

        loadDataForPro();

        view.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearRx();
                getDialog().dismiss();
            }
        });
        deleteBtn = view.findViewById(R.id.delete);
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = listView.getCheckedItemPosition();
                if (position >= 0) {
                    String path = proAdapter.getItem(position);
                    if (path != null) {
                        File file = new File(Environment.getExternalStorageDirectory() + "/MyAndroidTools/" + path);
                        if (file.exists()) {
                            file.delete();
                            proAdapter.remove(path);
                            listView.clearChoices();
                            return;
                        }
                    }
                }

                Toast toast = Toast.makeText(mContext, R.string.backup_delete_failed, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.TOP, 0, 0);
                toast.show();
            }
        });
        okBtn = view.findViewById(R.id.ok);
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = listView.getCheckedItemPosition();
                if (position >= 0) {
                    String path = proAdapter.getItem(position);
                    if (path != null) {
                        Intent data = new Intent();
                        data.putExtra("checkedItem", disableBtn.isChecked() ? 0 : 1);
                        data.putExtra("path", Environment.getExternalStorageDirectory() + "/MyAndroidTools/" + proAdapter.getItem(position));
                        getTargetFragment().onActivityResult(
                                getTargetRequestCode(),
                                Activity.RESULT_OK, data);
                        getDialog().dismiss();
                        return;
                    }
                }

                Toast toast = Toast.makeText(mContext, R.string.backup_delete_failed, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.TOP, 0, 0);
                toast.show();
            }
        });
        editBtn = view.findViewById(R.id.edit);
        editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO:wq 2019/4/16 快速点击多次
                int position = listView.getCheckedItemPosition();
                if (position >= 0) {
                    String path = proAdapter.getItem(position);
                    if (path != null) {
//                        dismissAllowingStateLoss();
                        getDialog().hide();

                        String fileName = proAdapter.getItem(position);
                        Fragment f = new BackupEditFragment();
                        Bundle args = new Bundle();
                        args.putString("path", Environment.getExternalStorageDirectory() + "/MyAndroidTools/" + fileName);
                        f.setArguments(args);
                        f.setTargetFragment(AboutChooseDialogFragment.this, REQUEST_CODE_TO_EDIT);

                        FragmentTransaction ft = getActivity().getSupportFragmentManager()
                                .beginTransaction();
                        ft.replace(R.id.content, f);
//                            ft.setTransition(FragmentTransaction.TRANSIT_NONE);
                        ft.addToBackStack(null);
                        ft.commit();
                        return;
                    }
                }

                Toast toast = Toast.makeText(mContext, R.string.backup_delete_failed, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.TOP, 0, 0);
                toast.show();
            }
        });

        return new AlertDialog.Builder(mContext)
                .setTitle(R.string.restore)
                .setView(view)
                .create();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_TO_EDIT) {
            getDialog().show();
            if (resultCode != Activity.RESULT_CANCELED) {
                //需要刷新数据
                progressBar.setVisibility(View.VISIBLE);
                listView.setVisibility(View.INVISIBLE);
                //删除了文件 清除选中状态
                if (resultCode == Activity.RESULT_FIRST_USER) {
                    listView.clearChoices();
                }
                editBtn.setEnabled(false);
                okBtn.setEnabled(false);
                deleteBtn.setEnabled(false);

                loadDataForPro();
            }
        }
    }

    private Dialog createFreeDialog() {
        return new AlertDialog.Builder(mContext)
                .setTitle(R.string.restore)
                .setSingleChoiceItems(R.array.restore_choose, 0,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0,
                                                int which) {
                                checkedItem = which;
                            }
                        })
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                Intent data = new Intent();
                                data.putExtra("checkedItem",
                                        checkedItem);
                                getTargetFragment().onActivityResult(
                                        getTargetRequestCode(),
                                        Activity.RESULT_OK, data);
                                dismiss();
                            }
                        })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                dismiss();
                            }
                        }).create();
    }
}
