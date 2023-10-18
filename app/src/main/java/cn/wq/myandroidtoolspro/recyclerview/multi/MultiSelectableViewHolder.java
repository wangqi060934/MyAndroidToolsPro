package cn.wq.myandroidtoolspro.recyclerview.multi;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.StateSet;
import android.view.View;

import cn.wq.myandroidtoolspro.R;

public abstract class MultiSelectableViewHolder
        extends RecyclerView.ViewHolder
        implements View.OnLongClickListener, View.OnClickListener{
    private boolean mIsSelected;
    private Drawable mDefaultBackground,mSelectedBackground;

    public MultiSelectableViewHolder(View itemView) {
        super(itemView);

        if(!itemView.isClickable()){
            itemView.setClickable(true);
        }
        if (!itemView.isLongClickable()) {
            itemView.setLongClickable(true);
        }
        itemView.setOnClickListener(this);
        itemView.setOnLongClickListener(this);

        mDefaultBackground = itemView.getBackground();
        setSelectionnModeDrawable(R.color.actionbar_color_light);
    }

    public abstract MultiSelectionUtils.Controller loadMultiController();

    public void setSelectionnModeDrawable(@ColorRes int colorRes) {
        //导致默认的RippleDrawable无效
//        StateListDrawable stateListDrawable = new StateListDrawable();
//        stateListDrawable.addState(StateSet.WILD_CARD,null);
//
//        ColorDrawable drawable = new ColorDrawable(ContextCompat.getColor(itemView.getContext(),colorRes));
//        stateListDrawable.addState(new int[]{android.R.attr.state_selected},drawable);
//        itemView.setBackgroundDrawable(stateListDrawable);

        mSelectedBackground=new ColorDrawable(ContextCompat.getColor(itemView.getContext(),colorRes));
    }

    public void setSelected(boolean selected){
        if (mIsSelected != selected) {
            mIsSelected=selected;
//            itemView.setSelected(selected);
            itemView.setBackgroundDrawable(selected?mSelectedBackground:mDefaultBackground);
        }
    }

    public boolean isSelected() {
        return mIsSelected;
    }


    @Override
    public boolean onLongClick(View view) {
        if (!loadMultiController().isInActionMode()) {
            loadMultiController().startActionMode();
            setSelected(true);
            loadMultiController().onStateChanged(getLayoutPosition(),true);
        }else{
            onClick(view);
        }

        return true;
    }

    @Override
    public void onClick(View view) {
        if (loadMultiController().isInActionMode()) {
            boolean isSelected=isSelected();
            setSelected(!isSelected);
            loadMultiController().onStateChanged(getLayoutPosition(),!isSelected);
            return;
        }
        loadMultiController().onItemClick(getLayoutPosition(),view);
    }

}
