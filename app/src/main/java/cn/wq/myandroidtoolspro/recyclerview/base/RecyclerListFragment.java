package cn.wq.myandroidtoolspro.recyclerview.base;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;

import cn.wq.myandroidtoolspro.BaseActivity;
import cn.wq.myandroidtoolspro.R;

public class RecyclerListFragment extends Fragment {
    private View mProgressContainer;
    private ProgressBar mProgressBar;
    private View mListContainer;
    private TextView mEmptyTv;
    private RecyclerListView mList;
    protected RecyclerView.Adapter mAdapter;

    private boolean mListShown;
    private Context mContext;
    private boolean isDark;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isDark = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getInt(BaseActivity.PREFERENCE_THEME, 0) > 0;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.recycler_list_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //进入其它页面的时候会调用onDestroyView，mList=null，返回时mAdapter!=null,不能直接设置各个子View，需要重新setListShown
        ensureList();
    }

    private void ensureList() {
        if (mList != null) {
            return;
        }
        View view = getView();
        if (view == null) {
            return;
        }
        mProgressContainer = view.findViewById(R.id.progressContainer);
        mListContainer = view.findViewById(R.id.listContainer);
        mEmptyTv = view.findViewById(android.R.id.empty);
        mList = view.findViewById(android.R.id.list);
        mProgressBar = view.findViewById(R.id.progress_bar);
        if (mProgressBar != null && isDark) {
            mProgressBar.setIndeterminateDrawable(ContextCompat.getDrawable(view.getContext(), R.drawable.progress_dark));
        }

        mList.setLayoutManager(new LinearLayoutManager(view.getContext(), LinearLayoutManager.VERTICAL, false));
        mList.addItemDecoration(new DividerItemDecoration(view.getContext()));

        //和FastScoller冲突
//        mList.setScrollbarFadingEnabled(false);
//        mList.setVerticalScrollBarEnabled(true);

        if (mEmptyTv != null) {
            mList.setEmptyView(mEmptyTv);
        }

        if (mAdapter != null) {
            RecyclerView.Adapter adapter = mAdapter;
            //mAdapter等于空的时候下一步里面才会setListShown(true
            mAdapter = null;
            setAdapter(adapter);
        } else {
            setListShown(false, false);
        }
    }

    @Override
    public void onDestroyView() {
        mListContainer = mProgressContainer = mEmptyTv = null;
        mProgressBar = null;
        mList = null;
        mListShown = false;
        super.onDestroyView();
    }

    public void setAdapter(RecyclerView.Adapter adapter) {
        boolean hasAdapter = mAdapter != null;
        mAdapter = adapter;
        if (mList != null) {
            mList.setAdapter(adapter);
            if (!mListShown && !hasAdapter) {
                setListShown(true, getView().getWindowToken() != null);
            }
        }
    }

    public void setListShown(boolean shown, boolean animate) {
        ensureList();
        if (shown == mListShown || mListContainer == null) {
            return;
        }
        mListShown = shown;
        if (shown) {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(mContext, android.R.anim.fade_out));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(mContext, android.R.anim.fade_in));
            } else {
                mProgressContainer.clearAnimation();
                mListContainer.clearAnimation();
            }
            mProgressContainer.setVisibility(View.GONE);
            mListContainer.setVisibility(View.VISIBLE);
        } else {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(mContext, android.R.anim.fade_in));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(mContext, android.R.anim.fade_out));
            } else {
                mProgressContainer.clearAnimation();
                mListContainer.clearAnimation();
            }
            mProgressContainer.setVisibility(View.VISIBLE);
            mListContainer.setVisibility(View.GONE);
        }
    }

    public void scrollToPosition(int position) {
        if (mList != null) {
            mList.scrollToPosition(position);
        }
    }

    public RecyclerListView getRecyclerListView() {
        return mList;
    }

    public RecyclerView.Adapter getRecyclerAdapter() {
        return mAdapter;
    }

    public void setEmptyText(CharSequence text) {
        if (mEmptyTv != null) {
            mEmptyTv.setText(text);
        }
    }

    public TextView getEmptyView(){
        return mEmptyTv;
    }

}
