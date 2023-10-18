package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.recyclerview.toolbar.SearchWithToolbarRecyclerFragment;

public class UidFragment extends SearchWithToolbarRecyclerFragment {
	private Pattern pattern=Pattern.compile("[\\d,]+,i,uid,(\\d+),([\\w\\.]+)");
	private PackageManager pm;
	private LoadBatteryTask mTask;
	private UidAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		pm=getActivity().getPackageManager();

		mAdapter=new UidAdapter(getActivity());
		setAdapter(mAdapter);
		
		mTask=new LoadBatteryTask();
		mTask.execute();
		
        initActionbar(0, getString(R.string.uid));
	}

    class LoadBatteryTask extends AsyncTask<Void, Void, List<UidEntry> >{
		@Override
		protected List<UidEntry> doInBackground(Void... arg0) {
			String sCommand;
			if(Build.VERSION.SDK_INT>=19){
				sCommand="dumpsys batterystats --checkin\n";
			}else {
				sCommand="dumpsys batteryinfo --checkin\n";
			}
			
			//方法2
//			try {
//				Shell shell=Shell.startRootShell();
//				final SparseArray<List<UidEntry>> result=new SparseArray<List<UidEntry>>();
//				Command command=new Command(sCommand) {
//					private int lastUid=-1;
//					private List<UidEntry> list;
//					
//					@Override
//					public void output(int id, String line) {
//						Matcher matcher=pattern.matcher(line);
//						if(matcher.matches()){
//							int uid=Integer.valueOf(matcher.group(1));
//							UidEntry entry=new UidEntry();
//							entry.packageName=matcher.group(2);
//							try {
//								entry.icon=pm.getApplicationIcon(entry.packageName);
//							} catch (NameNotFoundException e) {
//								e.printStackTrace();
//							}
//							
//							if(lastUid!=uid){
//								if(lastUid==-1){
//									list=new ArrayList<UidEntry>();
//								}else {
//									result.put(lastUid, list);
//									list=new ArrayList<UidEntry>();
//								}
//							}
//							lastUid=uid;
//							list.add(entry);
//							
//						}else {
//							if(list!=null){
//								result.put(lastUid, list);
//								list=null;
//							}
//						}
//					}
//					
//					@Override
//					public void afterExecution(int id, int exitCode) {
//						
//					}
//				};
//				shell.add(command).waitForFinish();
//				shell.close();
//				return result;
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
			
			List<String> suResult= Utils.runRootCommandForResult(sCommand);
            if(suResult==null){
				return null;
			}

			List<UidEntry> result = new ArrayList<>();
			for(String line:suResult){
				Matcher matcher=pattern.matcher(line);
				if(matcher.matches()){
					UidEntry entry=new UidEntry();
					entry.packageName=matcher.group(2);
                    entry.uid=Integer.valueOf(matcher.group(1));
                    result.add(entry);
				}
			}
			
			return result;
		}

		@Override
		protected void onPostExecute(List<UidEntry> result) {
			super.onPostExecute(result);
            if (result == null && getActivity() != null) {
                Toast.makeText(getActivity(), R.string.failed_to_gain_root, Toast.LENGTH_SHORT).show();
            } else {
                mAdapter.setData(result);
                setListShown(true, isResumed());
            }
        }

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			setListShown(false,true);
		}
	}
	
	private class UidEntry{
		int uid;
		String packageName;
	}

	private class UidAdapter extends RecyclerView.Adapter<VHolder> implements Filterable{
		private Context context;
        private List<UidEntry> mList;
        private List<UidEntry> originalData;
        private final Object mLock = new Object();
        private UidFilter mFilter;

		public UidAdapter(Context context) {
			super();
			this.context=context;
            mList = new ArrayList<>();
        }

        public void setData(List<UidEntry> list){
            mList.clear();
            if(list!=null){
                mList.addAll(list);
            }
            notifyDataSetChanged();
        }

		public Object getItem(int position) {
			return  mList.get(position);
		}

        @Override
        public VHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new VHolder(LayoutInflater.from(context).inflate(R.layout.item_uid_list, parent, false));
        }

        @Override
        public void onBindViewHolder(VHolder holder, int position) {
            UidEntry entry=(UidEntry) getItem(position);

            if(position==0){
                holder.uid.setText(entry.uid+"");
                holder.uid.setVisibility(View.VISIBLE);
            }else{
                UidEntry previous=(UidEntry) getItem(position-1);
                if(previous.uid==entry.uid){
                    holder.uid.setVisibility(View.GONE);
                }else{
                    holder.uid.setVisibility(View.VISIBLE);
                    holder.uid.setText(entry.uid+"");
                }
            }

            Utils.loadApkIcon(UidFragment.this, entry.packageName, holder.icon);
            holder.name.setText(entry.packageName);
        }

        @Override
        public int getItemCount() {
            return mList.size();
        }

        private class UidFilter extends Filter {
            @Override
            protected void publishResults(CharSequence constraint,
                                          FilterResults results) {
                mList = (List<UidEntry>) results.values;
                notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                final FilterResults results = new FilterResults();
                if (originalData == null) {
                    synchronized (mLock) {
                        originalData = new ArrayList<>(mList);
                    }
                }

                List<UidEntry> list;
                if (TextUtils.isEmpty(constraint)) {
                    synchronized (mLock) {
                        list = new ArrayList<>(originalData);
                    }
                    results.values = list;
                    results.count = list.size();
                } else {
                    synchronized (mLock) {
                        list = new ArrayList<>(originalData);
                    }

                    final List<UidEntry> newValues = new ArrayList<>();
                    for (UidEntry entry : list) {
                        if (entry.packageName.contains(constraint)
                                ||Integer.toString(entry.uid).contains(constraint)) {
                            newValues.add(entry);
                        }
                    }

                    results.values = newValues;
                    results.count = newValues.size();
                }

                return results;
            }
        }

        @Override
        public Filter getFilter() {
            if (mFilter == null) {
                mFilter = new UidFilter();
            }
            return mFilter;
        }

	}

    private class VHolder extends RecyclerView.ViewHolder{
        ImageView icon;
        TextView name,uid;

        public VHolder(View itemView) {
            super(itemView);
            icon=(ImageView) itemView.findViewById(R.id.icon);
            name=(TextView)itemView.findViewById(R.id.name);
            uid=(TextView)itemView.findViewById(R.id.uid);
        }
    }

	@Override
	public void onDestroy() {
		super.onDestroy();
        if(mTask!=null){
		    mTask.cancel(true);
        }
	}
	
}
