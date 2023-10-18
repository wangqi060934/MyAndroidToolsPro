package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.webkit.WebView;

import cn.wq.myandroidtoolspro.R;

/**
 * Created by wangqi on 2017/8/17.
 */

public class LicenseDialogFragment extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        WebView webView = new WebView(getContext());
        webView.loadUrl("file:///android_asset/licenses.html");
        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.open_lincense)
                .setView(webView)
                .create();
    }
}
