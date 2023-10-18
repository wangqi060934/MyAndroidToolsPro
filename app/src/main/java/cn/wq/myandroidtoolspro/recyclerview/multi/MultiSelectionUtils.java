package cn.wq.myandroidtoolspro.recyclerview.multi;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;

import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.recyclerview.base.RecyclerListView;

public class MultiSelectionUtils {
    public static Controller attach(MultiSelectionRecyclerListFragment fragment) {
        return Controller.attach(fragment);
    }

    public static class Controller implements ActionMode.Callback,RecyclerListView.OnRecyclerItemClickListener{
        private RecyclerListView mRecyclerListView;
        private AppCompatActivity mActivity;
        private ActionMode mActionMode;
        private ArrayList<Integer> mItemsSelected;
        private MultiSelectionRecyclerListFragment mFragment;

        private Controller() {
        }

        private static Controller attach(MultiSelectionRecyclerListFragment fragment) {
            Controller controller = new Controller();
            controller.mActivity= (AppCompatActivity) fragment.getActivity();
            controller.mRecyclerListView=fragment.getRecyclerListView();
            controller.mFragment=fragment;
            return controller;
        }

        public boolean isInActionMode() {
            return mActionMode != null;
        }

        public void startActionMode() {
            if (mActionMode == null) {
                mItemsSelected = new ArrayList<>();
                mActionMode=mActivity.startSupportActionMode(this);
            }
        }

        public void finish() {
            if (mActionMode != null) {
                mActionMode.finish();
                mActionMode = null;
            }
        }

        private String getStateKey() {
            return MultiSelectionUtils.class.getSimpleName() + "_" + mRecyclerListView.getId();
        }
        public void saveInstanceState(Bundle outState) {
            if (mActionMode != null) {
                outState.putIntegerArrayList(getStateKey(),mItemsSelected);
            }
        }

        public void restoreInstanceState(Bundle savedInstanceState) {
            mItemsSelected=null;
            if (savedInstanceState != null) {
                mItemsSelected = savedInstanceState.getIntegerArrayList(getStateKey());
            }

            RecyclerView.Adapter adapter=mRecyclerListView.getAdapter();
            if (mItemsSelected == null||adapter==null) {
                return;
            }
            startActionMode();
        }

        public void onStateChanged(int position,boolean selected) {
            int index=mItemsSelected.indexOf(position);
            if(selected && index<0){
                mItemsSelected.add(position);
            }else if(!selected && index>=0){
                mItemsSelected.remove(index);
                if (mItemsSelected.size() == 0) {
                    finish();
                }
            }
        }

        public ArrayList<Integer> getSelectedItemsPosition() {
            return mItemsSelected;
        }

        public boolean isSelectedAtPosition(int position) {
            return mItemsSelected != null && mItemsSelected.contains(position);
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            if (Build.VERSION.SDK_INT >= 21) {
                mActivity.getWindow().setStatusBarColor(ContextCompat.getColor(mActivity, R.color.blue_grey_700));
            }
            if(mFragment!=null && mFragment.onCreateActionMode(mode,menu)){
                if (mItemsSelected != null && mItemsSelected.size() > 0) {
                    for(int i=0;i<mItemsSelected.size();i++) {
                        MultiSelectableViewHolder viewHolder= (MultiSelectableViewHolder) mRecyclerListView.findViewHolderForLayoutPosition(mItemsSelected.get(i));
                        viewHolder.setSelected(true);
                    }
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return mFragment!=null && mFragment.onPrepareActionMode(mode, menu);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return mFragment != null && mFragment.onActionItemClicked(mode, item);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            if (Build.VERSION.SDK_INT >= 21) {
                mActivity.getWindow().setStatusBarColor(ContextCompat.getColor(mActivity, android.R.color.transparent));
            }
            if (mFragment != null) {
                mFragment.onDestroyActionMode(mode);
            }
            if (mItemsSelected != null && mItemsSelected.size() > 0) {
                for(int i=0;i<mItemsSelected.size();i++) {
                    MultiSelectableViewHolder viewHolder= (MultiSelectableViewHolder) mRecyclerListView.findViewHolderForLayoutPosition(mItemsSelected.get(i));
                    if (viewHolder != null) {
                        viewHolder.setSelected(false);
                    }
                }
                mItemsSelected.clear();
            }
            mActionMode=null;
        }

        @Override
        public void onItemClick(int position, View v) {
            if (mFragment != null) {
                mFragment.onItemClick(position,v);
            }
        }
    }


}
