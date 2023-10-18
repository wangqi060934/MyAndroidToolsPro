package cn.wq.myandroidtoolspro.recyclerview.fragment.about;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import cn.wq.myandroidtoolspro.R;

public class LoadAllIfwDialog extends DialogFragment {
    private Context mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(mContext)
                .setTitle(R.string.hint_first_time)
                .setView(R.layout.dialog_load_all_ifw)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getTargetFragment().onActivityResult(
                                getTargetRequestCode(),
                                Activity.RESULT_OK, null);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel,null)
                .create();
    }

}
