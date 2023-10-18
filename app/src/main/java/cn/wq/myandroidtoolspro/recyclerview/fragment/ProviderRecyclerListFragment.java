package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.model.ComponentEntry;
import cn.wq.myandroidtoolspro.model.ComponentModel;
import cn.wq.myandroidtoolspro.recyclerview.adapter.AbstractComponentAdapter;
import cn.wq.myandroidtoolspro.recyclerview.toolbar.MultiSectionWithToolbarRecyclerFragment;
import cn.wq.myandroidtoolspro.recyclerview.multi.MultiSelectableViewHolder;
import cn.wq.myandroidtoolspro.recyclerview.multi.MultiSelectionUtils;

public class ProviderRecyclerListFragment extends MultiSectionWithToolbarRecyclerFragment {
    private ProviderAdapter mAdapter;
    private String packageName;
    private Context mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    public static ProviderRecyclerListFragment newInstance(Bundle bundle) {
        ProviderRecyclerListFragment f = new ProviderRecyclerListFragment();
        f.setArguments(bundle);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle data = getArguments();
        packageName = data.getString("packageName");
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setToolbarLogo(packageName);
    }

    @Override
    protected AbstractComponentAdapter<ComponentEntry> generateAdapter() {
        mAdapter =  new ProviderAdapter(mContext);
        return  mAdapter;
    }

    @Override
    protected void reloadData(Integer... checkedItemPositions) {
        mAdapter.setData(loadData());
    }

    @Override
    protected boolean disableByIfw(Integer... positions) {
        return false;
    }

    @Override
    protected List<ComponentEntry> loadData() {
        List<ComponentEntry> result = new ArrayList<>();

        PackageManager pm = mContext.getPackageManager();
        List<ComponentModel> models=Utils.getComponentModels(mContext,packageName,3);
        for(ComponentModel model:models){
            if(model==null){
                continue;
            }
            ComponentEntry entry=new ComponentEntry();
            entry.packageName=model.packageName;
            entry.className=model.className;
            entry.enabled=Utils.isComponentEnabled(model, pm);
            result.add(entry);
        }

        Collections.sort(result, new Comparator<ComponentEntry>() {
            @Override
            public int compare(ComponentEntry lhs, ComponentEntry rhs) {
                String l = lhs.className.substring(
                        lhs.className.lastIndexOf(".") + 1);
                String r =
                        rhs.className.substring(
                                rhs.className.lastIndexOf(".") + 1);
                return l.compareTo(r);
            }
        });
        return result;
    }

    private class ProviderAdapter extends AbstractComponentAdapter<ComponentEntry> {
        ProviderAdapter(Context context) {
            super(context);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new BaseComponentViewHolder(LayoutInflater.from(mContext)
                    .inflate(R.layout.item_component_list, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            BaseComponentViewHolder vHolder= (BaseComponentViewHolder) holder;
            ComponentEntry entry = getItem(position);

            if (getIsFullName()) {
                vHolder.name.setText(entry.className);
            } else {
                vHolder.name.setText(entry.className.substring(entry.className
                        .lastIndexOf(".") + 1));
            }

            vHolder.checkBox.setChecked(entry.enabled);
            vHolder.name.setTextColor(entry.enabled ? primaryTextColor : redTextColor);
            vHolder.setSelected(getMultiController().isSelectedAtPosition(position));
        }

    }

    private class BaseComponentViewHolder extends MultiSelectableViewHolder {
        TextView name;
        SwitchCompat checkBox;

        BaseComponentViewHolder(View itemView) {
            super(itemView);

            name = (TextView) itemView.findViewById(R.id.name);
            checkBox=(SwitchCompat)itemView.findViewById(R.id.checkbox);
        }

        @Override
        public MultiSelectionUtils.Controller loadMultiController() {
            return getMultiController();
        }
    }

}
