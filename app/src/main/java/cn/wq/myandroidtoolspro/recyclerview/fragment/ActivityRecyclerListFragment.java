package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.IfwUtil;
import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.model.ComponentEntry;
import cn.wq.myandroidtoolspro.model.ComponentModel;
import cn.wq.myandroidtoolspro.recyclerview.adapter.AbstractComponentAdapter;
import cn.wq.myandroidtoolspro.recyclerview.toolbar.MultiSectionWithToolbarRecyclerFragment;
import cn.wq.myandroidtoolspro.recyclerview.multi.MultiSelectableViewHolder;
import cn.wq.myandroidtoolspro.recyclerview.multi.MultiSelectionUtils;

public class ActivityRecyclerListFragment extends MultiSectionWithToolbarRecyclerFragment{
    private ActivityAdapter mAdapter;
    private String packageName;
    private String launchActivityName;
    private Context mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    public static ActivityRecyclerListFragment newInstance(Bundle bundle) {
        ActivityRecyclerListFragment f = new ActivityRecyclerListFragment();
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

        launchActivityName=getLaunchActivityName();
    }

    private String getLaunchActivityName() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage(packageName);
        ResolveInfo rInfo=mContext.getPackageManager().resolveActivity(intent, 0);
        if (rInfo != null) {
            return rInfo.activityInfo.name;
        }
        return null;
    }

    @Override
    protected AbstractComponentAdapter<ComponentEntry> generateAdapter() {
        mAdapter = new ActivityAdapter(mContext);
        return mAdapter;
    }

    @Override
    protected void reloadData(Integer... checkedItemPositions) {
        mAdapter.setData(loadData());
    }

    @Override
    protected boolean isSupportIfw() {
        return true;
    }

    @Override
    protected boolean disableByIfw(Integer... positions) {
        return IfwUtil.saveComponentIfw(mContext, packageName, mIfwEntry, mAdapter, IfwUtil.COMPONENT_FLAG_ACTIVITY, useParentIfw, positions);
    }

    @Override
    protected List<ComponentEntry> loadData() {
        boolean isIfw = Utils.isPmByIfw(mContext);
        if (isIfw) {
            loadDataForIfw(packageName);
        }

        List<ComponentEntry> result = new ArrayList<>();

        PackageManager pm = mContext.getPackageManager();
        List<ComponentModel> models = Utils.getComponentModels(mContext, packageName, 2);
        for(ComponentModel model:models){
            if(model==null){
                continue;
            }
            ComponentEntry entry=new ComponentEntry();
            entry.packageName=model.packageName;
            entry.className=model.className;
            if (isIfw) {
                entry.isIfwed = IfwUtil.isComponentInIfw(packageName, model.className, IfwUtil.COMPONENT_FLAG_ACTIVITY, mIfwEntry);
            } else {
                entry.enabled= Utils.isComponentEnabled(model, pm);
            }
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

    private class ActivityAdapter extends AbstractComponentAdapter<ComponentEntry> {
        ActivityAdapter(Context context) {
            super(context);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
           return new BaseComponentViewHolder(LayoutInflater.from(mContext)
                    .inflate(R.layout.item_activity_list, parent, false));
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

            if(TextUtils.equals(launchActivityName,entry.className)){
                vHolder.homeImg.setVisibility(View.VISIBLE);
            }else{
                vHolder.homeImg.setVisibility(View.GONE);
            }
            vHolder.setSelected(getMultiController().isSelectedAtPosition(position));

            if (Utils.isPmByIfw(mContext)) {
                vHolder.checkBox.setVisibility(View.GONE);
                vHolder.wall.setVisibility(entry.isIfwed ? View.VISIBLE : View.INVISIBLE);
            } else {
                vHolder.checkBox.setVisibility(View.VISIBLE);
                vHolder.wall.setVisibility(View.GONE);
                vHolder.name.setTextColor(entry.enabled ? primaryTextColor : redTextColor);
            }
        }

    }

    private class BaseComponentViewHolder extends MultiSelectableViewHolder {
        TextView name;
        SwitchCompat checkBox;
        ImageView homeImg;
        ImageView wall;

        BaseComponentViewHolder(View itemView) {
            super(itemView);

            name = (TextView) itemView.findViewById(R.id.name);
            checkBox=(SwitchCompat)itemView.findViewById(R.id.checkbox);
            homeImg=(ImageView)itemView.findViewById(R.id.home);
            wall = itemView.findViewById(R.id.wall);
        }

        @Override
        public MultiSelectionUtils.Controller loadMultiController() {
            return getMultiController();
        }
    }


}
