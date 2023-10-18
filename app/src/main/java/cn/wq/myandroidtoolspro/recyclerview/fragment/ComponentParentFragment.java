package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.recyclerview.toolbar.BaseFragment;
import eu.chainfire.libsuperuser.Shell;
import moe.shizuku.api.ShizukuClient;

public class ComponentParentFragment extends BaseFragment{
    private int type;
    private AsyncTask<Void,Void,Boolean> checkRootTask;
    private boolean hasCheckRoot;

    public static ComponentParentFragment newInstance(int type){
        ComponentParentFragment f=new ComponentParentFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("type",type);
        f.setArguments(bundle);
        return f;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String title;
        switch (type){
            case  0:
                title=getString(R.string.service);
                break;
            case 1:
                title=getString(R.string.broadcast_receiver);
                break;
            case 2:
                title=getString(R.string.activity);
                break;
            default:
                title=getString(R.string.content_provider);
        }

        initActionbar(0,title);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        type=getArguments().getInt("type");

        final View rootView=inflater.inflate(R.layout.fragment_component_parent,container,false);

        if (type == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !hasCheckRoot) { //8.0以上获取runningService特殊处理
            checkRootTask = new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... voids) {
                    if (Utils.isPmByShizuku(getContext()) && ShizukuClient.getState().isAuthorized()) {
                        return true;
                    }
                    return Shell.SU.available();
                }

                @Override
                protected void onPostExecute(Boolean aBoolean) {
                    super.onPostExecute(aBoolean);

                    if (!aBoolean) {
                        Toast.makeText(getContext(), R.string.root_failed_to_get_running_service, Toast.LENGTH_LONG).show();
                    }
                    hasCheckRoot = true;

                    initViews(rootView); //先不管检测结果
                }
            };
            checkRootTask.execute();
        }else{
            initViews(rootView);
        }

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (checkRootTask != null) {
            checkRootTask.cancel(true);
        }
    }

    private void initViews(View rootView) {
        ViewPager viewPager = (ViewPager) rootView.findViewById(R.id.view_pager);
        viewPager.setAdapter(new MyPagerAdapter(this,type));

//        <style name="Base.Widget.Design.TabLayout" parent="android:Widget">
//        <item name="tabMaxWidth">@dimen/design_tab_max_width</item>
//        <item name="tabIndicatorColor">?attr/colorAccent</item>
//        <item name="tabIndicatorHeight">2dp</item>
//        <item name="tabPaddingStart">12dp</item>
//        <item name="tabPaddingEnd">12dp</item>
//        <item name="tabBackground">?attr/selectableItemBackground</item>
//        <item name="tabTextAppearance">@style/TextAppearance.Design.Tab</item>
//        <item name="tabSelectedTextColor">?android:textColorPrimary</item>
//        </style>
        TabLayout tabLayout = (TabLayout) rootView.findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);
    }

    private static class MyPagerAdapter extends FragmentPagerAdapter {
        private String[] titles;
        private int type;

        MyPagerAdapter(Fragment fragment, int type) {
            super(fragment.getChildFragmentManager());
            titles = fragment.getResources().getStringArray(
                    R.array.component_listfragment_title);
            this.type = type;
        }

        @Override
        public Fragment getItem(int position) {
            return AppForComponentRecyclerFragment.newInstance(position == 1, type);
        }

        @Override
        public int getCount() {
            return titles.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }
    }
}
