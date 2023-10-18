package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.IfwUtil;
import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.model.ComponentModel;
import cn.wq.myandroidtoolspro.model.ServiceEntry;
import cn.wq.myandroidtoolspro.recyclerview.adapter.AbstractComponentAdapter;
import cn.wq.myandroidtoolspro.recyclerview.multi.MultiSelectableViewHolder;
import cn.wq.myandroidtoolspro.recyclerview.multi.MultiSelectionUtils;
import cn.wq.myandroidtoolspro.recyclerview.toolbar.MultiSectionWithToolbarRecyclerFragment;

public class ServiceRecyclerListFragment extends MultiSectionWithToolbarRecyclerFragment {
    private ServiceAdapter adapter;
    private String packageName;
    private Context mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    public static ServiceRecyclerListFragment newInstance(Bundle bundle) {
        ServiceRecyclerListFragment f = new ServiceRecyclerListFragment();
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
    protected AbstractComponentAdapter<ServiceEntry> generateAdapter() {
        adapter = new ServiceAdapter(mContext);
        return adapter;
    }

    @Override
    protected void reloadData(Integer... checkedItemPositions) {
        adapter.setData(loadData());
    }

    @Override
    protected boolean disableByIfw(Integer... positions) {
        return IfwUtil.saveComponentIfw(mContext, packageName, mIfwEntry, adapter, IfwUtil.COMPONENT_FLAG_SERVICE, useParentIfw, positions);
    }

    @Override
    protected boolean isSupportIfw() {
        return true;
    }

    @Override
    protected List<ServiceEntry> loadData() {
        boolean isIfw = Utils.isPmByIfw(mContext);
        if (isIfw) {
            loadDataForIfw(packageName);
        }

        List<ServiceEntry> result = new ArrayList<>();
        PackageManager pm = mContext.getPackageManager();
        List<ComponentModel> models = Utils.getComponentModels(mContext, packageName, 0);
        List<ActivityManager.RunningServiceInfo> runningServiceInfos = Utils
                .getRunningServiceInfos(mContext, packageName);
        for (ComponentModel model : models) {
            ServiceEntry entry = new ServiceEntry();
            entry.className = model.className;
            entry.packageName = model.packageName;
            entry.isRunning = Utils.isRunning(model.packageName, model.className,
                    runningServiceInfos);
            if (isIfw) {
                entry.isIfwed = IfwUtil.isComponentInIfw(packageName, model.className, IfwUtil.COMPONENT_FLAG_SERVICE, mIfwEntry);
            } else {
                entry.enabled = Utils.isComponentEnabled(model, pm);
            }
            result.add(entry);
        }

        Collections.sort(result, comparator);

        return result;
    }

    private class ServiceAdapter extends AbstractComponentAdapter<ServiceEntry> {
        private int runningTextColor;

        ServiceAdapter(Context context) {
            super(context);
            runningTextColor = ContextCompat.getColor(context, R.color.actionbar_color);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new BaseComponentViewHolder(LayoutInflater.from(mContext)
                    .inflate(R.layout.item_component_list, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            BaseComponentViewHolder vHolder = (BaseComponentViewHolder) holder;
            ServiceEntry entry = getItem(position);

            if (getIsFullName()) {
                vHolder.name.setText(entry.className);
            } else {
                vHolder.name.setText(entry.className.substring(entry.className
                        .lastIndexOf(".") + 1));
            }

            vHolder.checkBox.setChecked(entry.enabled);
            vHolder.setSelected(getMultiController().isSelectedAtPosition(position));

            if (Utils.isPmByIfw(mContext)) {
                vHolder.checkBox.setVisibility(View.GONE);
                vHolder.wall.setVisibility(entry.isIfwed ? View.VISIBLE : View.INVISIBLE);
                vHolder.name.setTextColor(entry.isRunning ? runningTextColor : primaryTextColor);
            } else {
                vHolder.checkBox.setVisibility(View.VISIBLE);
                vHolder.wall.setVisibility(View.GONE);
                vHolder.name.setTextColor(entry.isRunning ? runningTextColor : entry.enabled ? primaryTextColor : redTextColor);
            }
//            vHolder.name.setTextColor(entry.isRunning ? runningTextColor : entry.enabled ? primaryTextColor : redTextColor);
        }

    }

    private class BaseComponentViewHolder extends MultiSelectableViewHolder {
        TextView name;
        SwitchCompat checkBox;
        ImageView wall;

        BaseComponentViewHolder(View itemView) {
            super(itemView);

            name = (TextView) itemView.findViewById(R.id.name);
            checkBox = (SwitchCompat) itemView.findViewById(R.id.checkbox);
            wall = itemView.findViewById(R.id.wall);
        }

        @Override
        public MultiSelectionUtils.Controller loadMultiController() {
            return getMultiController();
        }
    }

}
