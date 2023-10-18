package cn.wq.myandroidtoolspro.recyclerview.fragment.current;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import cn.wq.myandroidtoolspro.R;

/**
 * Created by wangqi on 2017/7/12.
 */

public class AccessibilityDialogFragment extends DialogFragment {

    public static AccessibilityDialogFragment newInstance(boolean enable) {
        Bundle args = new Bundle();
        args.putBoolean("enable",enable);

        AccessibilityDialogFragment f = new AccessibilityDialogFragment();
        f.setArguments(args);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        boolean enable = getArguments().getBoolean("enable", false);

        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.accessibility_dialog_title)
                .setMessage(enable ? R.string.accessibility_dialog_message_enable : R.string.accessibility_dialog_message_disable)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    }
                }).show();
    }
}
