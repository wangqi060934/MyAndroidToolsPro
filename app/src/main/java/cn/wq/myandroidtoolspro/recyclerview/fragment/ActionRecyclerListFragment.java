package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.DBHelper;
import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.model.ActionEntry;
import cn.wq.myandroidtoolspro.model.ReceiverEntry;
import cn.wq.myandroidtoolspro.recyclerview.base.SearchRecyclerListFragment;

public class ActionRecyclerListFragment extends SearchRecyclerListFragment
        implements LoaderManager.LoaderCallbacks<List<ActionEntry>>{
    public static final String ANDROID_RESOURCES
            = "http://schemas.android.com/apk/res/android";

    private ActionAdapter actionAdapter;
    private int clicked_pos = -1;
    private FinishReceiver mReceiver;
    private static boolean isAllShown;
    //需要等两外两个加载完成才能开始加载数据库
    private boolean isPrepared;
    private Context mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    private class FinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent data) {
            if (Utils.ACTION_RECEIVER_FINISH.equals(data.getAction())) {
                isPrepared=true;
                init();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mReceiver == null) {
            mReceiver = new FinishReceiver();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(Utils.ACTION_RECEIVER_FINISH);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, filter);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        if(actionAdapter==null){
            actionAdapter = new ActionAdapter(mContext);
            setAdapter(actionAdapter);
            setListShown(false,true);
        }else if (clicked_pos >= 0) {
            ActionEntry entry=(ActionEntry) actionAdapter.getItem(clicked_pos);
            SQLiteDatabase db= DBHelper.getInstance(getActivity()).getReadableDatabase();
            StringBuilder sb=new StringBuilder();
            sb.append("select sum(enabled) from receiver where _id in ");
            sb.append("(select receiver_id from a_r where action_id= ");
            sb.append("(select _id from action where action_name='");
            sb.append(entry.actionName);
            sb.append("'))");
            Cursor cursor=db.rawQuery(sb.toString(), null);
            if(cursor.moveToFirst()){
                entry.disabledNum=entry.totalNum-cursor.getInt(0);
                actionAdapter.updateLine(clicked_pos, entry);
            }
            cursor.close();
        }
    }

    private void init() {
        //java.lang.IllegalStateException: Content view not yet created
        if (isRemoving() || !isPrepared) {
            return;
        }
        setListShown(false,true);
        if (getLoaderManager().getLoader(0) == null) {
            getLoaderManager().initLoader(0, null, this);
        } else {
            getLoaderManager().restartLoader(0, null, this);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
//        inflater.inflate(R.menu.search, menu);

        final MenuItem searchMenuItem=menu.findItem(R.id.search);
        searchMenuItem.setVisible(isAllShown);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
        searchView.setQueryHint(getString(R.string.search_by_action_name));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (actionAdapter != null) {
                    actionAdapter.getFilter().filter(newText);
                }
                return true;
            }
        });

        MenuItem item=menu.add(0, 0, 1, R.string.see_all_actions);
        MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        MenuItemCompat.setActionView(item, R.layout.toggle_name);

        TextView textView=(TextView) MenuItemCompat.getActionView(item);
        textView.setText(R.string.see_all_actions);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                isAllShown = !isAllShown;
                searchMenuItem.setVisible(isAllShown);
                init();
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<List<ActionEntry>> onCreateLoader(int arg0, Bundle arg1) {
        return new ActionsLoader(mContext);
    }

    @Override
    public void onLoadFinished(Loader<List<ActionEntry>> arg0,
                               List<ActionEntry> list) {
        actionAdapter.setList(list,isAllShown);
        if (isResumed()) {
            setListShown(true,true);
        } else {
            setListShown(true,false);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<ActionEntry>> arg0) {
        actionAdapter.setList(null,isAllShown);
    }

     private static class ActionsLoader extends AsyncTaskLoader<List<ActionEntry>> {
        private List<ActionEntry> mActions;
        private final String[] ALL_ACTIONS = new String[] {
                Intent.ACTION_BOOT_COMPLETED,
                ConnectivityManager.CONNECTIVITY_ACTION,
                WifiManager.NETWORK_STATE_CHANGED_ACTION,
                WifiManager.WIFI_STATE_CHANGED_ACTION,
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REMOVED,
                Intent.ACTION_SCREEN_ON, Intent.ACTION_SCREEN_OFF,
                Intent.ACTION_USER_PRESENT };

        public ActionsLoader(Context context) {
            super(context);
        }

        @Override
        public List<ActionEntry> loadInBackground() {
            SQLiteDatabase db = DBHelper.getInstance(getContext()).getWritableDatabase();
            db.beginTransaction();

            db.delete(DBHelper.RECEIVER_TABLE_NAME, null, null);
            db.delete(DBHelper.ACTION_TABLE_NAME, null, null);
            db.delete(DBHelper.A_R_TABLE_NAME, null, null);

            Cursor cursor = db.query(DBHelper.APPS_TABLE_NAME,
                    DBHelper.APPS_TABLE_COLUMNS, null, null, null, null,
                    null);
            while (cursor.moveToNext()) {
                String appName=cursor.getString(cursor
                        .getColumnIndex(DBHelper.APPS_TABLE_COLUMNS[1]));
                String packageName = cursor.getString(cursor
                        .getColumnIndex(DBHelper.APPS_TABLE_COLUMNS[2]));

                PackageManager pm = getContext().getPackageManager();

                try {
                    Context targetContent = getContext().createPackageContext(packageName, 0);
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
                            if (!entry.className.contains(".")) {
                                entry.className=packageName+"."+entry.className;
                            }else if(entry.className.startsWith(".")){
                                entry.className=packageName+entry.className;
                            }
                            entry.enabledInManifest = parser.getAttributeBooleanValue(ANDROID_RESOURCES, "enabled", true);
                            parseAction(parser, resources, db,appName,entry,pm);
                        }
                    }

                } catch (PackageManager.NameNotFoundException | NoSuchMethodException | IllegalArgumentException | InvocationTargetException | IOException | XmlPullParserException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            cursor.close();

            List<ActionEntry> result=new ArrayList<>();
            if(isAllShown){
                StringBuilder sb=new StringBuilder();
                sb.append("select count(receiver._id) as total,sum(receiver.enabled) as enable_num,action.action_name ");
                sb.append("from receiver,action,a_r where receiver._id=a_r.receiver_id and a_r.action_id=action._id ");
                sb.append("group by a_r.action_id order by total desc,total-enable_num desc");
                Cursor cursor2=db.rawQuery(sb.toString(), null);
                while(cursor2.moveToNext()){
                    ActionEntry entry=new ActionEntry();
                    entry.totalNum=cursor2.getInt(0);
                    entry.disabledNum=cursor2.getInt(0)-cursor2.getInt(1);
                    entry.actionName=cursor2.getString(2);
                    result.add(entry);
                }
                cursor2.close();

            }else {
//                final int count = ALL_ACTIONS.length;
                for (String action : ALL_ACTIONS) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("select count(_id),sum(enabled) from receiver where _id in");
                    sb.append("(select receiver_id from a_r where action_id =");
                    sb.append("(select _id from action where action_name='");
                    sb.append(action);
                    sb.append("'))");
                    Cursor cursor3 = db.rawQuery(sb.toString(), null);
                    if (cursor3.moveToFirst()) {
                        ActionEntry entry = new ActionEntry();
                        entry.actionName = action;
                        entry.totalNum = cursor3.getInt(0);
                        entry.disabledNum = cursor3.getInt(0) - cursor3.getInt(1);
                        result.add(entry);
                    }
                    cursor3.close();
                }
            }

            db.setTransactionSuccessful();
            db.endTransaction();
            return result;
        }

        private void parseAction(XmlResourceParser parser,Resources resources,SQLiteDatabase db,String appName,ReceiverEntry receiverEntry,PackageManager pm) throws IOException, XmlPullParserException {
            List<Long> actionIds=new ArrayList<>();
            StringBuilder sBuilder=new StringBuilder();

            final int innerDepth=parser.getDepth();
            int type;
            while((type=parser.next())!=XmlPullParser.END_DOCUMENT &&
                    (type!=XmlPullParser.END_TAG||parser.getDepth()>innerDepth)){
                if(type==XmlPullParser.END_TAG||type==XmlPullParser.TEXT){
                    continue;
                }
                if("action".equals(parser.getName())){
                    String name = Utils.getAttributeValueByName(parser, resources, "name");

                    if(name==null){
                        continue;
                    }

                    //TODO:2.3bug
                    if(Build.VERSION.SDK_INT<11){
                        long action_id;
                        Cursor cr = db.rawQuery("select _id from " + DBHelper.ACTION_TABLE_NAME + " where action_name=?", new String[]{name});
                        if(cr.moveToFirst()){
                            action_id=cr.getInt(cr.getColumnIndex("_id"));
                        }else {
                            ContentValues cv=new ContentValues();
                            cv.put("action_name", name);
                            action_id=db.insert(DBHelper.ACTION_TABLE_NAME, null, cv);
                        }
                        cr.close();
                        actionIds.add(action_id);
                    }else {
                        ContentValues cv=new ContentValues();
                        cv.put("action_name", name);
                        long action_id=db.insert(DBHelper.ACTION_TABLE_NAME, null, cv);
                        //insertWithOnConflict有bug，只能查询
                        //2.3重复添加返回值有bug
                        if(action_id<0){
                            Cursor cr=db.rawQuery("select _id from " + DBHelper.ACTION_TABLE_NAME + " where action_name=?", new String[]{name});
                            if(cr.moveToFirst()){
                                action_id=cr.getInt(cr.getColumnIndex("_id"));
                            }
                            cr.close();
                        }
                        actionIds.add(action_id);
                    }

                    sBuilder.append(name);
                    sBuilder.append("\n");
                }
            }

            if (sBuilder.length() > 0) {
                sBuilder.deleteCharAt(sBuilder.length() - 1);
            }

            ContentValues values = new ContentValues();
            values.put(DBHelper.RECEIVER_TABLE_COLUMNS[1], appName);
            values.put(DBHelper.RECEIVER_TABLE_COLUMNS[2], receiverEntry.packageName);
            values.put(DBHelper.RECEIVER_TABLE_COLUMNS[3], receiverEntry.className);
            values.put(DBHelper.RECEIVER_TABLE_COLUMNS[4], sBuilder.toString());
            values.put(DBHelper.RECEIVER_TABLE_COLUMNS[5], Utils.isComponentEnabled(receiverEntry, pm));
            long receiver_id=db.insert(DBHelper.RECEIVER_TABLE_NAME, null, values);
            for(long action_id:actionIds){
                ContentValues cv=new ContentValues();
                cv.put(DBHelper.A_R_TABLE_COLUMNS[1], action_id);
                cv.put(DBHelper.A_R_TABLE_COLUMNS[2], receiver_id);
                db.insert(DBHelper.A_R_TABLE_NAME, null, cv);
            }
            actionIds.clear();

        }

        @Override
        public void deliverResult(List<ActionEntry> actions) {
            // super.deliverResult(apps);
            if (isReset()) {
                if (actions != null) {
                    onReleaseResources(actions);
                }
            }
            List<ActionEntry> oldActions = actions;
            mActions = actions;

            if (isStarted()) {
                super.deliverResult(actions);
            }

            if (oldActions != null) {
                onReleaseResources(oldActions);
            }
        }

        @Override
        protected void onStartLoading() {
            super.onStartLoading();
            if (mActions != null) {
                deliverResult(mActions);
            }

            if (mActions == null || takeContentChanged()) {
                forceLoad();
            }
        }

        @Override
        public void onCanceled(List<ActionEntry> data) {
            super.onCanceled(data);
            onReleaseResources(data);
        }

        @Override
        protected void onStopLoading() {
            super.onStopLoading();
            cancelLoad();
        }

        @Override
        protected void onReset() {
            super.onReset();
            onStopLoading();

            if (mActions != null) {
                onReleaseResources(mActions);
                mActions = null;
            }

        }

        protected void onReleaseResources(List<ActionEntry> apps) {
        }
    }


    private class ActionAdapter extends RecyclerView.Adapter<VHolder> implements Filterable {
        private List<ActionEntry> list;
        private String[] actions;
        private boolean isAllShown;
        private List<ActionEntry> originalData;
        private final Object mLock = new Object();
        private ActionFilter mFilter;

        public ActionAdapter(Context context) {
            super();
            list = new ArrayList<>();
            actions = context.getResources().getStringArray(
                    R.array.ALL_ACTIONS_SUMMARY);
            originalData = new ArrayList<>();
        }

        @Override
        public Filter getFilter() {
            if (mFilter == null) {
                mFilter = new ActionFilter();
            }
            return mFilter;
        }

        private class ActionFilter extends Filter{
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                final FilterResults results = new FilterResults();
                List<ActionEntry> tempList;

                if (TextUtils.isEmpty(constraint)) {
                    synchronized (mLock) {
                        tempList = new ArrayList<>(originalData);
                    }
                    results.values = tempList;
                    results.count = tempList.size();
                }else{
                    synchronized (mLock) {
                        tempList = new ArrayList<>(originalData);
                    }
                    final List<ActionEntry> newValues = new ArrayList<>();
                    String lowercaseQuery = constraint.toString().toLowerCase();
                    for (ActionEntry entry : tempList) {
                        if (entry.actionName.toLowerCase().contains(lowercaseQuery)) {
                            newValues.add(entry);
                        }
                    }

                    results.values = newValues;
                    results.count = newValues.size();
                }

                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                list = (List<ActionEntry>) results.values;
                notifyDataSetChanged();
            }
        }

        public void setList(List<ActionEntry> list,boolean isAllShown) {
            this.isAllShown=isAllShown;

            this.list.clear();
            if (list != null) {
                this.list.addAll(list);
            }

            synchronized (mLock) {
                originalData.clear();
                if (list != null) {
                    originalData.addAll(list);
                }
            }
            notifyDataSetChanged();
        }

        public void updateLine(int position,ActionEntry entry){
            list.set(position, entry);
            for (ActionEntry actionEntry : originalData) {
                if (TextUtils.equals(entry.actionName, actionEntry.actionName)) {
                    actionEntry.disabledNum=entry.disabledNum;
                }
            }
            notifyItemChanged(position);
        }
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public void onBindViewHolder(VHolder holder, int position) {
            ActionEntry entry = list.get(position);

            holder.name.setText(isAllShown?entry.actionName:actions[position]);
            holder.name.setTextSize(isAllShown?14:16);

            holder.totalNum.setText(Integer.toString(entry.totalNum));

            if (entry.disabledNum == 0) {
                holder.disableNum.setVisibility(View.INVISIBLE);
            } else {
                holder.disableNum.setVisibility(View.VISIBLE);
                holder.disableNum.setText(Integer.toString(entry.disabledNum));
            }
        }

        @Override
        public VHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new VHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_receiver_action_list,parent,false));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

    }

    private class VHolder extends RecyclerView.ViewHolder{
        TextView name;
        TextView totalNum;
        TextView disableNum;

        public VHolder(View itemView) {
            super(itemView);

            name = (TextView) itemView.findViewById(R.id.name);
            totalNum = (TextView) itemView
                    .findViewById(R.id.total_num);
            disableNum = (TextView) itemView
                    .findViewById(R.id.disabled_num);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position=getLayoutPosition();
                    ActionEntry action = (ActionEntry) actionAdapter.getItem(position);
                    if (action.totalNum == 0) {
                        return;
                    }

                    clicked_pos = position;

                    FragmentTransaction ft = getActivity().getSupportFragmentManager()
                            .beginTransaction();

                    ft.replace(R.id.content,
                            ReceiverWithActionParentFragment.newInstance(action.actionName));
                    ft.setTransition(FragmentTransaction.TRANSIT_NONE);
                    ft.addToBackStack(null);
                    ft.commit();
                }
            });

        }
    }

}
