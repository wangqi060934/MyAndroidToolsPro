package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import cn.wq.myandroidtoolspro.BaseActivity;
import cn.wq.myandroidtoolspro.BuildConfig;
import cn.wq.myandroidtoolspro.R;

public class CustomProgressDialogFragment extends DialogFragment {
    private static final String ARGS_MESSAGE = "MESSAGE";

    public static void showProgressDialog(@NonNull String tag, FragmentManager fm, String message) {
        if (fm == null) {
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString(ARGS_MESSAGE, message);

        CustomProgressDialogFragment f = new CustomProgressDialogFragment();
        f.setArguments(bundle);
        f.show(fm, tag);
    }

    public static void showProgressDialog(@NonNull String tag, FragmentManager fm) {
        if (fm == null) {
            return;
        }
        CustomProgressDialogFragment f = new CustomProgressDialogFragment();
        f.show(fm, tag);
    }

    public static void hideProgressDialog(@NonNull String tag, FragmentManager fm) {
        if (fm == null) {
            return;
        }
        CustomProgressDialogFragment f =
                (CustomProgressDialogFragment) fm.findFragmentByTag(tag);
        if (f != null) {
            f.dismissAllowingStateLoss();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_progress, container, false);
        if (getArguments() != null && getArguments().containsKey(ARGS_MESSAGE)) {
            TextView msgTv = v.findViewById(R.id.message);
            msgTv.setText(getArguments().getString(ARGS_MESSAGE));
        }
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!BuildConfig.isFree
                && PreferenceManager.getDefaultSharedPreferences(getContext()).getInt(BaseActivity.PREFERENCE_THEME, 0) > 0) {
            ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
            progressBar.setIndeterminateDrawable(ContextCompat.getDrawable(getContext(), R.drawable.progress_dark));
        }
    }
}
