package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.util.List;

import cn.wq.myandroidtoolspro.BuildConfig;
import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.DBHelper;
import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.recyclerview.toolbar.BaseFragment;

public class AppManageParentFragment extends BaseFragment {

    private Context mContext;
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    public interface FragmentSelectListener {
        void onSelected();
    }

    private AsyncTask<Void, Void, Void> mLoadTask;
    private boolean mDataLoaded;
    private ViewPager mViewPager;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initActionbar(0, getString(R.string.title_apps));

        if (mLoadTask == null) {
            mLoadTask = new AsyncTask<Void, Void, Void>() {
//                private MultiSelectionListFragment.ProgressDialogFragment mDialogFragment;

                @Override
                protected Void doInBackground(Void... params) {
                    PackageManager pm = mContext.getPackageManager();
                    List<ApplicationInfo> aInfos = pm.getInstalledApplications(0);

                    SQLiteDatabase db = DBHelper.getInstance(mContext).getReadableDatabase();
                    db.beginTransaction();
                    db.delete("app_manage", null, null);
                    for (ApplicationInfo aInfo : aInfos) {
                        if (isCancelled()) {
                            return null;
                        }
                        File file = new File(aInfo.sourceDir);
                        if (!file.exists()) {
                            continue;
                        }
                        ContentValues cv = new ContentValues();
                        cv.put("packageName", aInfo.packageName);
                        cv.put("appName", Utils.getAppLabel(pm, aInfo));
                        cv.put("sourcePath", aInfo.sourceDir);
                        cv.put("time", file.lastModified());
                        cv.put("enabled",
                                pm.getApplicationEnabledSetting(aInfo.packageName) <= PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
                        db.insert("app_manage", null, cv);
                    }
                    db.setTransactionSuccessful();
                    db.endTransaction();
                    return null;
                }

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
//                    mDialogFragment = new MultiSelectionListFragment.ProgressDialogFragment();
//                    mDialogFragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
//                    mDialogFragment.setCancelable(false);
//                    mDialogFragment.show(getActivity().getSupportFragmentManager(), "dialog");
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
//                    mDialogFragment.dismissAllowingStateLoss();
                    mDataLoaded=true;

                    selectChildFragment(mViewPager.getCurrentItem());
                }

                @Override
                protected void onCancelled() {
                    super.onCancelled();
                    mDataLoaded=false;
                }
            };
            mLoadTask.execute();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLoadTask != null) {
            mLoadTask.cancel(true);
            mLoadTask = null;
        }
    }

    private void initViews(View rootView) {
        mViewPager = (ViewPager) rootView.findViewById(R.id.view_pager);
        mViewPager.setAdapter(new MyPagerAdapter(AppManageParentFragment.this));
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
               selectChildFragment(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        TabLayout tabLayout = (TabLayout) rootView.findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(mViewPager);
    }

    private void selectChildFragment(int position) {
        if (!mDataLoaded) {
            return;
        }
        Fragment f = getChildFragmentManager().findFragmentByTag("android:switcher:" + mViewPager.getId() + ":" + position);
        if (f instanceof FragmentSelectListener) {
            ((FragmentSelectListener) f).onSelected();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_component_parent, container, false);
        initViews(rootView);
        return rootView;
    }

    private static class MyPagerAdapter extends FragmentPagerAdapter {
        private String[] titles;

        MyPagerAdapter(Fragment fragment) {
            super(fragment.getChildFragmentManager());
            titles = fragment.getResources().getStringArray(
                    R.array.app_manage_title);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return AppForManageRecyclerFragment.newInstance(false);
                case 1:
                    return AppForManageRecyclerFragment.newInstance(true);
                default:
                    return new UninstalledRecyclerListFragment();
            }
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
