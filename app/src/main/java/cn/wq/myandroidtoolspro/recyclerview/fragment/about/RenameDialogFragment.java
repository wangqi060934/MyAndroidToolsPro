package cn.wq.myandroidtoolspro.recyclerview.fragment.about;


import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;

import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.IfwUtil;

public class RenameDialogFragment extends DialogFragment {
    private EditText mEditText;
    private String path;

    public static RenameDialogFragment newInstance(boolean isIfw,String backupFilePath) {
        Bundle args = new Bundle();
        args.putBoolean("isIfw", isIfw);
        args.putString("path", backupFilePath);
        RenameDialogFragment f = new RenameDialogFragment();
        f.setArguments(args);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_rename, null);
        mEditText = (EditText) view.findViewById(R.id.edittext);
        path = getArguments().getString("path");
        if (path != null) {
            String fileName = path.substring(path.lastIndexOf("/") + 1);
            mEditText.setText(fileName);
            mEditText.setSelection(fileName.length());
        }
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setView(view)
                .setTitle(R.string.backup_rename_title)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
//                        Toast.makeText(getActivity(), getString(R.string.backup_done, path), Toast.LENGTH_LONG).show();
                    }
                })
                .create();
        return dialog;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
//        Toast.makeText(getActivity(), getString(R.string.backup_done, path), Toast.LENGTH_LONG).show();

    }

    @Override
    public void onStart() {
        super.onStart();

        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog == null) {
            return;
        }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(mEditText.getText())) {
                    mEditText.setError(getString(R.string.backup_file_name_empty));
                    return;
                }
                if (getArguments() != null && getArguments().getBoolean("isIfw")) {
                    if (!mEditText.getText().toString().trim().endsWith(IfwUtil.BACKUP_LOCAL_FILE_EXT)) {
                        mEditText.setError(getString(R.string.backup_file_ext_wrong));
                        return;
                    }
                }
                //判断是否修改了文件名
                File file = new File(Environment.getExternalStorageDirectory() + "/MyAndroidTools/" + mEditText.getText().toString());
                if (!TextUtils.equals(file.getAbsolutePath(), path)) {
                    if (file.exists()) {
                        mEditText.setError(getString(R.string.backup_file_exists));
                        return;
                    }
                }

                if (path != null) {
                    if (new File(path).renameTo(file)) {
                        Toast.makeText(getActivity(), getString(R.string.backup_done, file.getAbsolutePath()), Toast.LENGTH_LONG).show();

                        Fragment fragment = getTargetFragment();
                        if (fragment != null) {
                            Intent data = new Intent();
                            data.putExtra("path", mEditText.getText().toString());
                            fragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, data);
                        }
                        dismiss();
                    } else {
                        Toast.makeText(getActivity(), R.string.backup_file_rename_failed, Toast.LENGTH_LONG).show();
                    }
                }

            }
        });

    }
}
