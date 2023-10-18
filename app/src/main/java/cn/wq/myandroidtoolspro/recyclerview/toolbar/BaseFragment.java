package cn.wq.myandroidtoolspro.recyclerview.toolbar;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import cn.wq.myandroidtoolspro.BaseActivity;
import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.Utils;

public class BaseFragment extends Fragment{
    private ActionBar mActionBar;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar=(Toolbar) view.findViewById(R.id.toolbar);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(toolbar);
        mActionBar=activity.getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }

        final boolean isDark = PreferenceManager.getDefaultSharedPreferences(getContext()).getInt(BaseActivity.PREFERENCE_THEME, 0) > 0;
        if (isDark) {
            toolbar.setPopupTheme(R.style.ThemeOverlay_AppCompat_Dark);
        }
    }

    /**
     * @param iconType 0:normal 1:back 2:close
     */
    protected void initActionbar(int iconType,String title){
        if(title==null){
            mActionBar.setTitle(R.string.app_name);
        }else{
            mActionBar.setTitle(title);
        }

        switch (iconType) {
            case 1:
                mActionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);
                break;
            case 2:
                mActionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
                break;
            default:
                mActionBar.setHomeAsUpIndicator(R.drawable.ic_menu_white);
                break;
        }
    }

    protected void setToolbarLogo(String packageName) {
        if (mActionBar == null || TextUtils.isEmpty(packageName)) {
            return;
        }
        try {
            int length = (int) (getResources().getDisplayMetrics().density * 24);
            Drawable icon=getContext().getPackageManager().getApplicationIcon(packageName);
            View view=getView();
            if (view != null) {
                Toolbar toolbar= (Toolbar) view.findViewById(R.id.toolbar);
                toolbar.setLogo(icon);
                for (int i = 0; i < toolbar.getChildCount(); i++) {
                    View child = toolbar.getChildAt(i);
                    if (child instanceof ImageView){
                        ImageView iv = (ImageView) child;
                        if ( iv.getDrawable() == icon ) {
                            ViewGroup.LayoutParams params=iv.getLayoutParams();
                            params.width=params.height=length;
                        }
                    }
                }
            }

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

}
