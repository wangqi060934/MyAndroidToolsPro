package cn.wq.myandroidtoolspro.recyclerview.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Filter;
import android.widget.Filterable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.model.ComponentEntry;
import cn.wq.myandroidtoolspro.model.ReceiverWithActionEntry;

public abstract class AbstractComponentAdapter<T extends ComponentEntry>
        extends RecyclerView.Adapter
        implements Filterable{
    private List<T> list;
    private boolean isFullName;
    private ComponentFilter mFilter;
    private List<T> originalData;
    private final Object mLock = new Object();
    protected Context mContext;
    protected int primaryTextColor;
    protected int redTextColor;

//    public AbstractComponentAdapter() {
//        super();
//        this.list = new ArrayList<>();
//    }

    public AbstractComponentAdapter(Context context) {
        this.list = new ArrayList<>();
        mContext = context;
        primaryTextColor = Utils.getColorFromAttr(context, android.R.attr.textColorPrimary);
        redTextColor = ContextCompat.getColor(mContext, R.color.holo_red_light);
    }

    public T getItem(int position) {
        return list.get(position);
    }

    public void setData(List<T> list) {
        this.list.clear();
        if (list != null) {
            this.list.addAll(list);
        }

        //wq:搜索后禁用 刷新状态
        if(originalData!=null){
            originalData.clear();
            originalData=null;
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public boolean toggleName() {
        isFullName = !isFullName;
        notifyDataSetChanged();
        return isFullName;
    }

    public boolean getIsFullName() {
        return isFullName;
    }

    @Override
    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new ComponentFilter();
        }
        return mFilter;
    }

    private class ComponentFilter extends Filter {
        @Override
        protected void publishResults(CharSequence constraint,
                                      FilterResults results) {
            list = (List<T>) results.values;
            notifyDataSetChanged();
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            final FilterResults results = new FilterResults();
            if (originalData == null) {
                synchronized (mLock) {
                    originalData = new ArrayList<>(list);
                }
            }

            List<T> tempList;
            if (TextUtils.isEmpty(constraint)||constraint.toString().trim().length()==0) {
                synchronized (mLock) {
                    tempList = new ArrayList<>(originalData);
                }
                results.values = tempList;
                results.count = tempList.size();
            } else {
                synchronized (mLock) {
                    tempList = new ArrayList<>(originalData);
                }

                final List<T> newValues = new ArrayList<>();
                int type=-1;
                for (T entry : tempList) {
                    if(type<0){
                        type=0;
                        if(entry instanceof ReceiverWithActionEntry){
                            type=1;
                        }
                    }

                    //最好trim一下
                    String query=constraint.toString().trim().toLowerCase(Locale.getDefault());
                    String lowerName=entry.className.toLowerCase(Locale.getDefault());
                    if ((isFullName && lowerName.contains (query)
                            || (!isFullName && lowerName.substring(lowerName.lastIndexOf(".")+1).contains(query)))){
                        newValues.add(entry);
                    }else if(type>0){
                        ReceiverWithActionEntry receiverEntry= (ReceiverWithActionEntry) entry;
                        if(receiverEntry.appName.toLowerCase(Locale.getDefault()).contains(query)){
                            newValues.add(entry);
                        }
                    }
                }

                results.values = newValues;
                results.count = newValues.size();
            }

            return results;
        }
    }

}
