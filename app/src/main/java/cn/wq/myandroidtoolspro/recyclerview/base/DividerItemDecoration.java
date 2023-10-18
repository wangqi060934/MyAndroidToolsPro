package cn.wq.myandroidtoolspro.recyclerview.base;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class DividerItemDecoration extends RecyclerView.ItemDecoration{
    private static final int[] ATTRS = new int[]{
            android.R.attr.listDivider
    };
    private Drawable mDivider;

    public DividerItemDecoration(Context context) {
        super();
        TypedArray typedArray=context.obtainStyledAttributes(ATTRS);
        mDivider=typedArray.getDrawable(0);
        typedArray.recycle();
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDraw(c, parent, state);
        int left=parent.getPaddingLeft();
        int right=parent.getWidth()-parent.getPaddingRight();
        for(int i=0,len=parent.getChildCount();i<len;i++) {
            View child = parent.getChildAt(i);
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
            int top=child.getBottom()+params.bottomMargin;
            mDivider.setBounds(left,top,right,top+mDivider.getIntrinsicHeight());
            mDivider.draw(c);
        }
    }
}
