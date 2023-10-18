package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.database.sqlite.SQLiteDatabase;
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

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.DBHelper;
import cn.wq.myandroidtoolspro.helper.IfwUtil;
import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.model.ComponentEntry;
import cn.wq.myandroidtoolspro.model.ReceiverEntry;
import cn.wq.myandroidtoolspro.recyclerview.adapter.AbstractComponentAdapter;
import cn.wq.myandroidtoolspro.recyclerview.multi.MultiSelectableViewHolder;
import cn.wq.myandroidtoolspro.recyclerview.multi.MultiSelectionUtils;
import cn.wq.myandroidtoolspro.recyclerview.toolbar.MultiSectionWithToolbarRecyclerFragment;

public class ReceiverRecyclerListFragment extends MultiSectionWithToolbarRecyclerFragment{

    private ReceiverAdapter adapter;
    private String packageName;
    private static final String ANDROID_NAMESPACE
            = "http://schemas.android.com/apk/res/android";

    private Context mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    public static ReceiverRecyclerListFragment newInstance(Bundle bundle) {
        ReceiverRecyclerListFragment f = new ReceiverRecyclerListFragment();
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
    protected AbstractComponentAdapter<ReceiverEntry> generateAdapter() {
        adapter = new ReceiverAdapter(mContext);
        return adapter;
    }

    @Override
    protected void reloadData(Integer... checkedItemPositions) {
        SQLiteDatabase db = DBHelper.getInstance(mContext).getWritableDatabase();
        db.beginTransaction();
        for (int position : checkedItemPositions) {
            ComponentEntry entry= adapter.getItem(position);
            entry.enabled=!entry.enabled;
            DBHelper.updateReceiver(entry, db);
        }
        db.setTransactionSuccessful();
        db.endTransaction();

        adapter.setData(loadData());
    }

    @Override
    protected boolean disableByIfw(Integer... positions) {
        return IfwUtil.saveComponentIfw(mContext, packageName, mIfwEntry, adapter, IfwUtil.COMPONENT_FLAG_RECEIVER, useParentIfw, positions);
    }

    @Override
    protected boolean isSupportIfw() {
        return true;
    }

    @Override
    protected List<ReceiverEntry> loadData() {
        boolean isIfw = Utils.isPmByIfw(mContext);
        if (isIfw) {
            loadDataForIfw(packageName);
        }

        PackageManager pm = mContext.getPackageManager();
        List<ReceiverEntry> result = new ArrayList<>();

        try {
            Context targetContent = mContext.createPackageContext(packageName, 0);
            AssetManager assetManager = targetContent.getAssets();
            String sourceDir = pm.getApplicationInfo(packageName, 0).sourceDir;

            Method addAssetPath = AssetManager.class.getMethod("addAssetPath",
                    String.class);
            int cookie = (Integer) addAssetPath.invoke(assetManager, sourceDir);
            XmlResourceParser parser = assetManager.openXmlResourceParser(
                    cookie, "AndroidManifest.xml");
            Resources resources=targetContent.getResources();
            final int innerDepth=parser.getDepth();
            int type;
            while((type=parser.next())!= XmlPullParser.END_DOCUMENT &&
                    (type!=XmlPullParser.END_TAG||parser.getDepth()>innerDepth)){
                if(type==XmlPullParser.END_TAG||type==XmlPullParser.TEXT){
                    continue;
                }

                if("receiver".equals(parser.getName())){
                    ReceiverEntry entry=new ReceiverEntry();
                    entry.packageName=packageName;
                    entry.className = Utils.getAttributeValueByName(parser, resources, "name");

                    if(entry.className==null){
                        continue;
                    }

                    if(!entry.className.contains(".")){
                        entry.className=entry.packageName+"."+entry.className;
                    }else if(entry.className.startsWith(".")){
                        entry.className=entry.packageName+entry.className;
                    }

                    entry.enabledInManifest = parser.getAttributeBooleanValue(ANDROID_NAMESPACE, "enabled",true);
//                    entry.enabled=pm.getComponentEnabledSetting(new ComponentName(
//                            entry.packageName, entry.className)) <= 1;
                    if (isIfw) {
                        entry.isIfwed = IfwUtil.isComponentInIfw(packageName, entry.className, IfwUtil.COMPONENT_FLAG_RECEIVER, mIfwEntry);
                    } else {
                        entry.enabled = Utils.isComponentEnabled(entry, pm);
                    }

                    entry.actions=parseActions(parser);
                    result.add(entry);
                }
            }

            Collections.sort(result, new Comparator<ReceiverEntry>() {
                @Override
                public int compare(ReceiverEntry lhs, ReceiverEntry rhs) {
                    String l = lhs.className.substring(lhs.className
                            .lastIndexOf(".") + 1);
                    String r = rhs.className.substring(rhs.className
                            .lastIndexOf(".") + 1);
                    return l.compareTo(r);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private String parseActions(XmlPullParser parser) throws XmlPullParserException, IOException{
        StringBuilder builder=new StringBuilder();
        final int innerDepth=parser.getDepth();
        int type;
        while((type=parser.next())!=XmlPullParser.END_DOCUMENT &&
                (type!=XmlPullParser.END_TAG||parser.getDepth()>innerDepth)){
            if(type==XmlPullParser.END_TAG||type==XmlPullParser.TEXT){
                continue;
            }
            if("action".equals(parser.getName())){
                builder.append(parser.getAttributeValue(ANDROID_NAMESPACE, "name"));
                builder.append("\n");
            }
        }

        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    private class ReceiverAdapter extends AbstractComponentAdapter<ReceiverEntry> {
        public ReceiverAdapter(Context context) {
            super(context);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ReceiverViewHolder(LayoutInflater.from(mContext)
                    .inflate(R.layout.item_receiver_list, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ReceiverViewHolder vHolder= (ReceiverViewHolder) holder;
            ReceiverEntry entry = getItem(position);

            if (getIsFullName()) {
                vHolder.name.setText(entry.className);
            } else {
                vHolder.name.setText(entry.className.substring(entry.className
                        .lastIndexOf(".") + 1));
            }

            vHolder.checkBox.setChecked(entry.enabled);

            if (TextUtils.isEmpty(entry.actions)) {
                vHolder.actions.setVisibility(View.GONE);
            } else {
                vHolder.actions.setVisibility(View.VISIBLE);
                vHolder.actions.setText(entry.actions);
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

        private  class ReceiverViewHolder extends MultiSelectableViewHolder{
            TextView actions;
            TextView name;
            SwitchCompat checkBox;
            ImageView wall;
            ReceiverViewHolder(View itemView) {
                super(itemView);
                actions= (TextView) itemView.findViewById(R.id.actions);
                name = (TextView) itemView.findViewById(R.id.name);
                checkBox=(SwitchCompat)itemView.findViewById(R.id.checkbox);
                wall = itemView.findViewById(R.id.wall);
            }

            @Override
            public MultiSelectionUtils.Controller loadMultiController() {
                return getMultiController();
            }
        }

    }


}
