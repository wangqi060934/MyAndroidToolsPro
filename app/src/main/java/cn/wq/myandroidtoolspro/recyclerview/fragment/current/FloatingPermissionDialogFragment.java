package cn.wq.myandroidtoolspro.recyclerview.fragment.current;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.tencent.mars.xlog.Log;

import cn.wq.myandroidtoolspro.R;

/**
 * Created by wangqi on 2017/7/16.
 */

public class FloatingPermissionDialogFragment extends DialogFragment {
    private static final String KEY_MESSAGE = "KEY_MESSAGE";
    private static final String KEY_INTENT = "KEY_INTENT";
    private static final String TAG = "FloatingPermissionDialo";

    public static FloatingPermissionDialogFragment newInstance(String message,Intent intent) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_MESSAGE, message);
        bundle.putParcelable(KEY_INTENT, intent);

        FloatingPermissionDialogFragment f = new FloatingPermissionDialogFragment();
        f.setArguments(bundle);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        String msg = getArguments().getString(KEY_MESSAGE);
        final Intent intent = getArguments().getParcelable(KEY_INTENT);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.accessibility_dialog_title)
                .setMessage(msg)
                .setNegativeButton(R.string.cancel, null)
                //无效，需要设置调用 DialogFragment 的相同方法
//                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
//                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
//                        intent.setData(Uri.parse("package:" + getContext().getPackageName()));
//                        startActivity(intent);
                        if(intent!=null){
                            try {
                                startActivity(intent);
                            } catch (Exception e) {
                                Log.e(TAG, "not found activity to handle intent:" + intent.toString());
                                Toast.makeText(getContext(), R.string.operation_failed, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }).create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

}
