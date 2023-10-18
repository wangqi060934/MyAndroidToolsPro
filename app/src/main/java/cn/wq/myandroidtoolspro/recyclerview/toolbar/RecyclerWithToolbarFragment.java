package cn.wq.myandroidtoolspro.recyclerview.toolbar;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;

import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.recyclerview.base.RecyclerListFragment;

public class RecyclerWithToolbarFragment extends RecyclerListFragment {
    private ActionBar mActionBar;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.recycler_with_toolbar_list_fragment,container,false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args=getArguments();
        if (args != null && args.getBoolean("ignoreToolbar")) {
            return;
        }

        ViewStub viewStub= (ViewStub) view.findViewById(R.id.view_stub);
        viewStub.inflate();

        Toolbar toolbar=(Toolbar) view.findViewById(R.id.toolbar);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(toolbar);
        mActionBar=activity.getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    protected void initActionbar(int iconType,String title){
        if (mActionBar == null) {
            return;
        }
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
                    if (child != null && child instanceof ImageView){
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
