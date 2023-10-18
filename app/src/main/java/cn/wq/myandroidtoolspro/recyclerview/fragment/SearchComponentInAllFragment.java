package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.IfwUtil;
import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.model.ComponentEntry;
import cn.wq.myandroidtoolspro.model.ComponentModel;
import cn.wq.myandroidtoolspro.recyclerview.adapter.AbstractComponentAdapter;
import cn.wq.myandroidtoolspro.recyclerview.multi.MultiSelectableViewHolder;
import cn.wq.myandroidtoolspro.recyclerview.multi.MultiSelectionUtils;
import cn.wq.myandroidtoolspro.recyclerview.toolbar.MultiSectionWithToolbarRecyclerFragment;

public class SearchComponentInAllFragment extends MultiSectionWithToolbarRecyclerFragment {
    private int type, cType;
    private CnAdpater mAdpater;
    private Context mContext;
    private File ifwTempDir;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    /**
     * @see Utils#getComponentModels
     */
    public static SearchComponentInAllFragment newInstance(int type) {
        Bundle args = new Bundle();
        args.putInt("type",type);
        SearchComponentInAllFragment f=new SearchComponentInAllFragment();
        f.setArguments(args);
        return  f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle data = getArguments();
        type = data.getInt("type");
        ifwTempDir = new File(mContext.getExternalCacheDir(), UUID.randomUUID().toString());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (ifwTempDir != null && ifwTempDir.exists()) {
            ifwTempDir.delete();
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        String title;
        if (type == 1) {
            title=getString(R.string.search_in_all_receiver);
            cType = IfwUtil.COMPONENT_FLAG_RECEIVER;
        } else if (type == 2) {
            title=getString(R.string.search_in_all_activity);
            cType = IfwUtil.COMPONENT_FLAG_ACTIVITY;
        }else if (type == 3) {
            title=getString(R.string.search_in_all_provider);
        }else {
            title=getString(R.string.search_in_all_services);
            cType = IfwUtil.COMPONENT_FLAG_SERVICE;
        }
        initActionbar(1,title);
    }

    @Override
    protected AbstractComponentAdapter generateAdapter() {
        mAdpater = new CnAdpater(mContext);
        return mAdpater;
    }

    @Override
    protected void reloadData(Integer... checkedItemPositions) {
        mAdpater.setData(loadData());
    }

    @Override
    protected boolean isSupportIfw() {
        return cType > 0;
    }

    @Override
    protected boolean disableByIfw(Integer... positions) {
        if (positions == null || positions.length == 0) {
            return true;
        }
        String pkg = null;
        List<Integer> prePkgPos = new ArrayList<>();
        for (Integer pos : positions) {
            CnEntry entry = mAdpater.getItem(pos);
            if (entry == null || entry.packageName == null) {
                continue;
            }
            if (pkg == null) {
                pkg = entry.packageName;
                prePkgPos.clear();
                prePkgPos.add(pos);
                continue;
            }
            if (!TextUtils.equals(pkg, entry.packageName)) {
                boolean section = IfwUtil.saveComponentIfw(mContext, pkg, mIfwEntry, mAdpater, cType,
                        useParentIfw, ifwTempDir, prePkgPos.toArray(new Integer[0]));
                if (!section) {
                    return false;
                }
                pkg = entry.packageName;
                prePkgPos.clear();
            }
            prePkgPos.add(pos);
        }
        if (prePkgPos.size() > 0) {
            boolean section = IfwUtil.saveComponentIfw(mContext, pkg, mIfwEntry, mAdpater, cType,
                    useParentIfw, ifwTempDir, prePkgPos.toArray(new Integer[0]));
            if (!section) {
                return false;
            }
            prePkgPos.clear();
        }

        return true;
    }

    @Override
    protected List<CnEntry> loadData(){
        boolean isIfw = isSupportIfw() && Utils.isPmByIfw(mContext);
        if (isIfw) {
            loadAllMyIfw(ifwTempDir);
        }

        List<CnEntry> result = new ArrayList<>();
        PackageManager pm=mContext.getPackageManager();
        List<ApplicationInfo> aInfos = pm.getInstalledApplications(0);
        if(aInfos==null){
            return result;
        }
        for(ApplicationInfo aInfo:aInfos){
            if (aInfo == null) {
                continue;
            }
            if (isIfw && (aInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0) {
                continue;
            }
            List<ComponentModel> models=Utils.getComponentModels(mContext,aInfo.packageName,type);
            CnEntry entry;
            for (ComponentModel model : models) {
                entry=new CnEntry();
                entry.isSystem=(aInfo.flags&ApplicationInfo.FLAG_SYSTEM)>0;
                entry.packageName=aInfo.packageName;
                entry.className=model.className;
                entry.label = Utils.getAppLabel(pm, aInfo);
                if (isIfw) {
                    entry.isIfwed = IfwUtil.isComponentInIfw(entry.packageName, model.className, cType, mIfwEntry);
                } else {
                    entry.enabled = Utils.isComponentEnabled(model, pm);
                }

                result.add(entry);
            }
        }
            Collections.sort(result, new Comparator<CnEntry>() {
                @Override
                public int compare(CnEntry lhs, CnEntry rhs) {
                    int result = lhs.packageName.compareTo(rhs.packageName);
                    if (result == 0) {
                        String l = lhs.className
                                .substring(lhs.className.lastIndexOf(".") + 1);
                        String r = rhs.className
                                .substring(rhs.className.lastIndexOf(".") + 1);
                        result = l.compareTo(r);
                    }
                    return result;
                }
            });
        return result;
    }

    private class CnEntry extends ComponentEntry{
        public boolean isSystem;
        public String label;
    }
    private class CnAdpater extends AbstractComponentAdapter<CnEntry>{
        CnAdpater(Context context){
            super(context);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new VHolder(LayoutInflater.from(mContext).inflate(
                    R.layout.item_component_in_search_all, parent,false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            VHolder vHolder = (VHolder) holder;
            CnEntry entry = getItem(position);

            if (getIsFullName()) {
                vHolder.className.setText(entry.className);
            } else {
                vHolder.className.setText(entry.className
                        .substring(entry.className.lastIndexOf(".") + 1));
            }

//            holder.className.setTextColor(entry.enabled ? textColorPrimary : context.getResources()
//                    .getColor(R.color.holo_red_light));

            vHolder.label.setText(entry.label);
            vHolder.checkBox.setChecked(entry.enabled);
            Utils.loadApkIcon(SearchComponentInAllFragment.this, entry.packageName, vHolder.icon);

            if (isSupportIfw() && Utils.isPmByIfw(mContext)) {
                vHolder.checkBox.setVisibility(View.GONE);
                vHolder.wall.setVisibility(entry.isIfwed ? View.VISIBLE : View.INVISIBLE);
                vHolder.className.setTextColor(primaryTextColor);
            } else {
                vHolder.checkBox.setVisibility(View.VISIBLE);
                vHolder.wall.setVisibility(View.GONE);
                vHolder.className.setTextColor(entry.enabled ? primaryTextColor : redTextColor);
            }

            vHolder.setSelected(getMultiController().isSelectedAtPosition(position));
        }

    }

    private class VHolder extends MultiSelectableViewHolder{
        SwitchCompat checkBox;
        TextView className,label;
        ImageView icon,wall;

        public VHolder(View itemView) {
            super(itemView);

            checkBox = (SwitchCompat) itemView
                    .findViewById(R.id.checkbox);
            className = (TextView) itemView.findViewById(R.id.className);
            label = (TextView) itemView.findViewById(R.id.label);
            icon = (ImageView) itemView.findViewById(R.id.icon);
            wall = itemView.findViewById(R.id.wall);
        }

        @Override
        public MultiSelectionUtils.Controller loadMultiController() {
            return getMultiController();
        }
    }
}
