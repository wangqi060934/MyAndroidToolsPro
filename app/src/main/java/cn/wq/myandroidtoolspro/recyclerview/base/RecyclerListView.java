package cn.wq.myandroidtoolspro.recyclerview.base;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

public class RecyclerListView extends RecyclerView{
    private View mEmptyView;
    private AdapterDataObserver mDataObserver=new AdapterDataObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            Adapter adapter=getAdapter();
            if (adapter != null && adapter.getItemCount() > 0) {
                setVisibility(View.VISIBLE);
                if (mEmptyView != null) {
                    mEmptyView.setVisibility(View.GONE);
                }
            }else{
                setVisibility(View.GONE);
                if (mEmptyView != null) {
                    mEmptyView.setVisibility(View.VISIBLE);
                }
            }
        }
    };

    public RecyclerListView(Context context) {
        this(context,null);
    }

    public RecyclerListView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public RecyclerListView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setEmptyView(View v) {
        mEmptyView=v;
    }

    @Override
    public void setAdapter(Adapter adapter) {
        super.setAdapter(adapter);

        if (adapter != null) {
            adapter.registerAdapterDataObserver(mDataObserver);
        }
        mDataObserver.onChanged();
    }

    public interface OnRecyclerItemClickListener {
        void onItemClick(int position,View v);
    }

}
