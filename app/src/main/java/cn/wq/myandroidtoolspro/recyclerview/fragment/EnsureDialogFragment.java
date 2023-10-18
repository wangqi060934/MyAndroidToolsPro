package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import cn.wq.myandroidtoolspro.R;


public class EnsureDialogFragment extends DialogFragment {

    public static EnsureDialogFragment newInstance(String title,String message) {
        Bundle bundle = new Bundle();
        bundle.putString("title",title);
        bundle.putString("message",message);

        EnsureDialogFragment f = new EnsureDialogFragment();
        f.setArguments(bundle);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle bundle=getArguments();

        return new AlertDialog.Builder(getContext())
                .setTitle(bundle.getString("title"))
                .setMessage(bundle.getString("message"))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, null);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, null);
                    }
                })
                .create();
    }

}
