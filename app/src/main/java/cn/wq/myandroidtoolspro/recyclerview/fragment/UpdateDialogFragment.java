package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import cn.wq.myandroidtoolspro.R;

public class UpdateDialogFragment extends DialogFragment {

    public static UpdateDialogFragment create(String version, String info, String url) {
        Bundle args = new Bundle();
        args.putString("version", version);
        args.putString("info", info);
        args.putString("url", url);

        UpdateDialogFragment dialog = new UpdateDialogFragment();
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.dialog_update_check, null);
        TextView versionTv = root.findViewById(R.id.version);
        final TextView infoTv = root.findViewById(R.id.info);

        versionTv.setText(getArguments().getString("version"));
        infoTv.setText(getArguments().getString("info"));


        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.has_new_version)
                .setView(root)
                .setPositiveButton(R.string.download, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getArguments().getString("url")));
                        startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
    }

}
