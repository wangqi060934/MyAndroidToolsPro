package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.tencent.mars.xlog.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cn.wq.myandroidtoolspro.AppInfoActivity;
import cn.wq.myandroidtoolspro.BuildConfig;
import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.ViewManifestActivity2;
import cn.wq.myandroidtoolspro.helper.IfwUtil;
import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.model.ComponentModel;
import cn.wq.myandroidtoolspro.recyclerview.fragment.about.RenameDialogFragment;
import cn.wq.myandroidtoolspro.recyclerview.multi.MultiSelectionRecyclerListFragment;
import cn.wq.myandroidtoolspro.recyclerview.toolbar.BaseFragment;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class AppInfoForManageFragment2 extends BaseFragment {
    private static final String TAG = "AppInfoForManageFrag";
    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private String packageName;
    private CustomProgressDialogFragment dialog;
    private Context mContext;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    public static IfwUtil.IfwEntry mIfwEntry;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    public static AppInfoForManageFragment2 newInstance(Bundle args){
        AppInfoForManageFragment2 f=new AppInfoForManageFragment2();
		f.setArguments(args);
		return f;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Bundle data=getArguments();
//        packageName = data.getString("packageName");

        initActionbar(2,data.getString("title"));
        setToolbarLogo(packageName);
	}

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (compositeDisposable != null) {
            compositeDisposable.clear();
        }
        if (mIfwEntry != null) {
            mIfwEntry.clear();
        }
    }

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        Bundle data=getArguments();
        if (data != null) {
            packageName = data.getString("packageName");
        }

        final View root = inflater.inflate(R.layout.fragment_component_parent, container, false);
        if (!Utils.isPmByIfw(mContext)) {
            initViews(root);
        } else {
            Disposable disposable = Observable.create(new ObservableOnSubscribe<IfwUtil.IfwEntry>() {
                @Override
                public void subscribe(ObservableEmitter<IfwUtil.IfwEntry> emitter) throws Exception {
                    IfwUtil.IfwEntry ifwEntry = IfwUtil.loadIfwFileForPkg(mContext, packageName, IfwUtil.COMPONENT_FLAG_ALL, false);
                    emitter.onNext(ifwEntry);
                    emitter.onComplete();
                }
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe(new Consumer<Disposable>() {
                        @Override
                        public void accept(Disposable disposable) {
                            // TODO:wq 2019/4/17 最好是在布局中增加progressbar
                            CustomProgressDialogFragment.showProgressDialog("loadIfw", getFragmentManager());
                        }
                    })
                    .doFinally(new Action() {
                        @Override
                        public void run() throws Exception {
                            CustomProgressDialogFragment.hideProgressDialog("loadIfw", getFragmentManager());
                        }
                    })
                    .subscribe(new Consumer<IfwUtil.IfwEntry>() {
                        @Override
                        public void accept(IfwUtil.IfwEntry ifwEntry) throws Exception {
                            mIfwEntry = ifwEntry;
                            if (mIfwEntry == null) {
                                Toast.makeText(mContext, R.string.load_ifw_by_root_failed, Toast.LENGTH_SHORT).show();
                                return;
                            } else if (mIfwEntry == IfwUtil.IfwEntry.ROOT_ERROR) {
                                Toast.makeText(mContext, R.string.failed_to_gain_root, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            initViews(root);
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) {
                            throwable.printStackTrace();
                            Log.e(TAG, "loadIfwFileForPkg error", throwable);
                            Toast.makeText(mContext, R.string.load_ifw_by_root_failed, Toast.LENGTH_SHORT).show();
                        }
                    });

            compositeDisposable.add(disposable);
        }
		return root;
	}

    private void initViews(View root) {
        mViewPager = (ViewPager) root.findViewById(R.id.view_pager);
        mViewPager.setAdapter(new MyPagerAdapter(this, getArguments()));
        mTabLayout = (TabLayout) root.findViewById(R.id.tab_layout);
        mTabLayout.setupWithViewPager(mViewPager);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            private int pre_pos;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if(pre_pos==position){
                    return;
                }
                MultiSelectionRecyclerListFragment fragment=getFragment(pre_pos);
                if(fragment!=null){
                    fragment.closeActionMode();
                }
                pre_pos = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        if (!BuildConfig.isFree) {
            MenuItem manifest=menu.add(0, R.id.view_manifest, 0, R.string.view_manifest);
            MenuItemCompat.setShowAsAction(manifest,MenuItemCompat.SHOW_AS_ACTION_NEVER);

            MenuItem backup = menu.add(0, R.id.backup, 1, R.string.backup_disabled_of_an_app);
            MenuItemCompat.setShowAsAction(backup,MenuItemCompat.SHOW_AS_ACTION_NEVER);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.view_manifest:
                Intent intent = new Intent(getContext(), ViewManifestActivity2.class);
//                Bundle args=getArguments();
                intent.putExtra("packageName", packageName);
//                intent.putExtra("title",args.getString("title"));
                startActivity(intent);
                return true;
            case R.id.backup:
                backup();
                return true;
        }
        return false;
    }

    private void backup() {
        setProgressDialogVisibility(true);

        StringBuilder sb = new StringBuilder();
        PackageManager pm = mContext.getPackageManager();

        List<ComponentModel> services= Utils.getComponentModels(mContext,packageName,4);
        for (ComponentModel s : services) {
            if (!Utils.isComponentEnabled(s, pm)) {
                sb.append(s.packageName)
                        .append("/")
                        .append(s.className)
                        .append("\n");
            }
        }

        String dirString= Environment.getExternalStorageDirectory() + "/MyAndroidTools/";
        File dir = new File(dirString);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmm_SSS", Locale.getDefault());
        String string= dirString+"backup_" + dateFormat.format(new Date());
        try {
            FileWriter writer = new FileWriter(new File(string));
            writer.write(sb.toString());
            writer.flush();
            writer.close();
//            Toast.makeText(mActivity, getString(R.string.backup_done, string), Toast.LENGTH_LONG).show();

            setProgressDialogVisibility(false);

            Bundle args = new Bundle();
            args.putString("path",string);
            RenameDialogFragment renameDialog = new RenameDialogFragment();
            renameDialog.setArguments(args);
            renameDialog.show(getActivity().getSupportFragmentManager(),"rename");

        } catch (IOException e) {
            e.printStackTrace();
            setProgressDialogVisibility(false);
            Toast.makeText(mContext, R.string.operation_failed, Toast.LENGTH_SHORT).show();
        }

    }

        private void setProgressDialogVisibility(boolean visible) {
        if (visible) {
            if (dialog == null) {
                dialog = new CustomProgressDialogFragment();
                dialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
                dialog.setCancelable(false);
            }

            if (!dialog.isVisible() && getActivity() != null && !getActivity().isFinishing()) {
                try {
                    dialog.show(getActivity().getSupportFragmentManager(), "dialog");
                } catch (Exception e) {
                }
            }
        } else {
            if (dialog != null && getFragmentManager()!=null) {
                dialog.dismissAllowingStateLoss();
            }
        }
    }

    private MultiSelectionRecyclerListFragment getFragment(int position){
       return  (MultiSelectionRecyclerListFragment) getChildFragmentManager().findFragmentByTag("android:switcher:" + mViewPager.getId() + ":"
                + position);
    }
	
	private class MyPagerAdapter extends FragmentPagerAdapter {
		private String[] titles;
		private Bundle args;
        private int actionModeBackground = -1;
        private int defaultTabBackground = -1;
		
        MyPagerAdapter(Fragment fragment,Bundle bundle) {
			super(fragment.getChildFragmentManager());
			args=bundle;
            titles=new String[]{fragment.getString(R.string.service),
                fragment.getString(R.string.broadcast_receiver),
                fragment.getString(R.string.activity),
                fragment.getString(R.string.content_provider)};

            actionModeBackground = Utils.getColorFromAttr(mContext,R.attr.actionModeBackground);
            defaultTabBackground = Utils.getColorFromAttr(mContext, R.attr.colorPrimary);
        }

		@Override
		public Fragment getItem(int position) {
            args.putBoolean("ignoreToolbar",true);
            args.putBoolean("useParentIfw", true);

            MultiSelectionRecyclerListFragment f;
			switch (position) {
			case 0:
				f = ServiceRecyclerListFragment.newInstance(args);
                break;
			case 1:
                f = ReceiverRecyclerListFragment.newInstance(args);
                break;
			case 2:
                f = ActivityRecyclerListFragment.newInstance(args);
                break;
			default:
                f = ProviderRecyclerListFragment.newInstance(args);
                break;
			}
            f.addActionModeLefecycleCallback(mActionModeLefecycleCallback);
            return f;
		}

        private MultiSelectionRecyclerListFragment.ActionModeLefecycleCallback mActionModeLefecycleCallback
                =new MultiSelectionRecyclerListFragment.ActionModeLefecycleCallback() {
            @Override
            public void onModeCreated() {
//                mTabLayout.setBackgroundColor(ContextCompat.getColor(getActivity(),R.color.blue_grey_500));
                mTabLayout.setBackgroundColor(actionModeBackground);
            }

            /**
             * onDestroyActionMode 将 statusbar 颜色置为透明了，在 AppInfoActivity 中会显示成灰白
             * @see cn.wq.myandroidtoolspro.recyclerview.multi.MultiSelectionUtils.Controller#onDestroyActionMode(ActionMode)
             */
            @Override
            public void onModeDestroyed() {
//                mTabLayout.setBackgroundColor(ContextCompat.getColor(getActivity(),R.color.actionbar_color));
                mTabLayout.setBackgroundColor(defaultTabBackground);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                        && getActivity() != null
                        && getActivity() instanceof AppInfoActivity) {
                    getActivity().getWindow().setStatusBarColor(ContextCompat.getColor(getContext(),R.color.actionbar_color_dark));
                }
            }
        };

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
