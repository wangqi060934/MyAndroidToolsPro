package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import cn.wq.myandroidtoolspro.MainActivity;
import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.recyclerview.multi.MultiSelectionRecyclerListFragment;
import cn.wq.myandroidtoolspro.recyclerview.toolbar.BaseFragment;

public class ReceiverWithActionParentFragment extends BaseFragment {
	private String action;
    private ViewPager mViewPager;
    private TabLayout mTabLayout;

	public static ReceiverWithActionParentFragment newInstance(String action){
        ReceiverWithActionParentFragment f=new ReceiverWithActionParentFragment();
		
		Bundle data=new Bundle();
		data.putString("action", action);
		f.setArguments(data);
		return f;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
        initActionbar(1,action);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		action=getArguments().getString("action");
		
        View rootView=inflater.inflate(R.layout.fragment_component_parent, container, false);
        mViewPager = (ViewPager) rootView.findViewById(R.id.view_pager);
        mViewPager.setAdapter(new MyPagerAdapter(this, action));

        mTabLayout = (TabLayout) rootView.findViewById(R.id.tab_layout);
        mTabLayout.setupWithViewPager(mViewPager);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            private int pre_pos;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (pre_pos == position) {
                    return;
                }
                MultiSelectionRecyclerListFragment fragment = getFragment(pre_pos);
                if (fragment != null) {
                    fragment.closeActionMode();
                }
                pre_pos = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
		return rootView;
	}

    private MultiSelectionRecyclerListFragment getFragment(int position){
        return  (MultiSelectionRecyclerListFragment) getChildFragmentManager().findFragmentByTag("android:switcher:" + mViewPager.getId() + ":"
                + position);
    }

	private class MyPagerAdapter extends FragmentPagerAdapter {
		private String[] titles;
		private String action;

		public MyPagerAdapter(Fragment fragment,String action) {
			super(fragment.getChildFragmentManager());
			titles = fragment.getResources().getStringArray(
					R.array.component_listfragment_title);
			this.action=action;
		}

		@Override
		public Fragment getItem(int position) {
			MultiSelectionRecyclerListFragment f = ReceiverWithActionListFragment.newInstance(position==1,action);
            f.addActionModeLefecycleCallback(mActionModeLefecycleCallback);
            return f;
		}

        private MultiSelectionRecyclerListFragment.ActionModeLefecycleCallback mActionModeLefecycleCallback=new MultiSelectionRecyclerListFragment.ActionModeLefecycleCallback() {
            @Override
            public void onModeCreated() {
                mTabLayout.setBackgroundColor(ContextCompat.getColor(getContext(),R.color.blue_grey_500));
            }

            @Override
            public void onModeDestroyed() {
                mTabLayout.setBackgroundColor(ContextCompat.getColor(getContext(),R.color.actionbar_color));
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
