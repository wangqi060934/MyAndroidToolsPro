package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.DBHelper;
import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.model.ReceiverWithActionEntry;
import cn.wq.myandroidtoolspro.recyclerview.adapter.AbstractComponentAdapter;
import cn.wq.myandroidtoolspro.recyclerview.multi.MultiSelectableViewHolder;
import cn.wq.myandroidtoolspro.recyclerview.multi.MultiSelectionRecyclerListFragment;
import cn.wq.myandroidtoolspro.recyclerview.multi.MultiSelectionUtils;

public class ReceiverWithActionListFragment extends MultiSelectionRecyclerListFragment {
	private ReceiverInActionAdapter adapter;
    private String mAction;
    private boolean mIsSystem;
	
	public static ReceiverWithActionListFragment newInstance(boolean isSystem, String action){
		Bundle data=new Bundle();
		data.putBoolean("isSystem", isSystem);
		data.putString("action", action);
		data.putBoolean("part", true);
		
		ReceiverWithActionListFragment f=new ReceiverWithActionListFragment();
		f.setArguments(data);
		return f;
	}
	
	@Override
	protected AbstractComponentAdapter<ReceiverWithActionEntry> generateAdapter() {
		adapter=new ReceiverInActionAdapter(getContext());
		return adapter;
	}

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Bundle data=getArguments();
        mAction=data.getString("action");
        mIsSystem=data.getBoolean("isSystem");

        super.onActivityCreated(savedInstanceState);
    }

    @Override
	protected List<ReceiverWithActionEntry> loadData(){
		List<ReceiverWithActionEntry> receivers=new ArrayList<>();

		SQLiteDatabase db= DBHelper.getInstance(getContext()).getReadableDatabase();
		StringBuilder sb=new StringBuilder();
		sb.append("select receiver.* from receiver,apps where receiver._id in ");
		sb.append("(select receiver_id from a_r where action_id =");
		sb.append("(select _id from action where action_name='");
		sb.append(mAction);
		sb.append("')) and receiver.package_name=apps.package_name and apps.is_system=");
		sb.append(mIsSystem?1:0);
		sb.append(" order by app_name");
		
		Cursor cursor=db.rawQuery(sb.toString(), null);
		while(cursor.moveToNext()){
			ReceiverWithActionEntry entry=new ReceiverWithActionEntry();
			entry.appName=cursor.getString(cursor.getColumnIndex(DBHelper.RECEIVER_TABLE_COLUMNS[1]));
			entry.packageName=cursor.getString(cursor.getColumnIndex(DBHelper.RECEIVER_TABLE_COLUMNS[2]));
			entry.className=cursor.getString(cursor.getColumnIndex(DBHelper.RECEIVER_TABLE_COLUMNS[3]));
			entry.actions=cursor.getString(cursor.getColumnIndex(DBHelper.RECEIVER_TABLE_COLUMNS[4]));
			entry.enabled=cursor.getInt(cursor.getColumnIndex(DBHelper.RECEIVER_TABLE_COLUMNS[5]))==1;
			receivers.add(entry);
		}
		cursor.close();
		return receivers;
	}

    @Override
    protected boolean disableByIfw(Integer... positions) {
        return false;
    }

    @Override
	protected void reloadData(Integer... checkedItemPositions) {
		adapter.toggleLines(checkedItemPositions);
	}

	private class ReceiverInActionAdapter extends AbstractComponentAdapter<ReceiverWithActionEntry> {
        private int foundTextColor;

        ReceiverInActionAdapter(Context context) {
			super(context);

			foundTextColor = ContextCompat.getColor(mContext,R.color.teal_500);
        }

        void toggleLines(Integer... checkedItemPositions){
			SQLiteDatabase db = DBHelper.getInstance(getContext()).getWritableDatabase();
			db.beginTransaction();
			
			for(int position:checkedItemPositions){
				ReceiverWithActionEntry entry=  getItem(position);
				entry.enabled=!entry.enabled;
				
				DBHelper.updateReceiver(entry, db);

                adapter.notifyItemChanged(position);
			}
			db.setTransactionSuccessful();
			db.endTransaction();

//			adapter.notifyDataSetChanged();
		}
		
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new VHolder(LayoutInflater.from(mContext).inflate(
                    R.layout.item_receiver_with_action_list, parent,false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            VHolder vHolder= (VHolder) holder;
            ReceiverWithActionEntry entry =  getItem(position);
            vHolder.appName.setText(entry.appName);

            if (getIsFullName()) {
                vHolder.receiverName.setText(entry.className);
            } else {
                vHolder.receiverName.setText(entry.className
                        .substring(entry.className.lastIndexOf(".") + 1));
            }

//            vHolder.receiverName.setTextColor(
//                    ContextCompat.getColor(context,entry.enabled ?
//                        textColorPrimary: R.color.holo_red_light));
            vHolder.receiverName.setTextColor(entry.enabled ? primaryTextColor : redTextColor);


            vHolder.checkBox.setChecked(entry.enabled);
            Utils.loadApkIcon(ReceiverWithActionListFragment.this, entry.packageName, vHolder.icon);

            if(TextUtils.isEmpty(entry.actions)){
                vHolder.actions.setVisibility(View.GONE);
            }else {
                vHolder.actions.setVisibility(View.VISIBLE);
                int index = entry.actions.indexOf(mAction);
                if (index >= 0) {
                    Spannable spannable = new SpannableString(entry.actions);
                    spannable.setSpan(
                            new ForegroundColorSpan(foundTextColor),
                            index,
                            index+mAction.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            );
                    vHolder.actions.setText(spannable);
                }else{
                    vHolder.actions.setText(entry.actions);
                }

            }
        }

	}

    private class VHolder extends MultiSelectableViewHolder{
        SwitchCompat checkBox;
        TextView appName,receiverName,actions;
        ImageView icon;

        public VHolder(View itemView) {
            super(itemView);

            checkBox=(SwitchCompat) itemView.findViewById(R.id.checkbox);
            actions=(TextView)itemView.findViewById(R.id.actions);
            receiverName=(TextView)itemView.findViewById(R.id.receiver_name);
            appName=(TextView)itemView.findViewById(R.id.app_name);
            icon=(ImageView)itemView.findViewById(R.id.icon);
        }


        @Override
        public MultiSelectionUtils.Controller loadMultiController() {
            return getMultiController();
        }
    }

}
