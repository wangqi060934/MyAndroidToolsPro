package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.tencent.mars.xlog.Log;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.recyclerview.fragment.FileChooseDialog.FileChooseDialogFragment;
import cn.wq.myandroidtoolspro.recyclerview.toolbar.SearchWithToolbarRecyclerFragment;

public class AppListForDataFragment extends SearchWithToolbarRecyclerFragment {
	private static final String TAG = "AppListForDataFragment";
	private AppAdapter mAdapter;
	private final static String slash = "/";
	private LoadDirTask mTask;
	private int type;
    private final static int REQUEST_CODE_FILE_CHOOSE=88;
	private Context mContext;
	private SharedPreferences sharedPreferences;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		mContext = context;
	}

    /**
	 * @param type
	 *            0:SharedPref 1:Databases
	 */
	public static AppListForDataFragment newInstance(int type) {
        AppListForDataFragment fragment = new AppListForDataFragment();
		Bundle args = new Bundle();
		args.putInt("type", type);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAdapter = new AppAdapter(mContext);
		setAdapter(mAdapter);

		type = getArguments().getInt("type");
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (mTask == null) {
			mTask = new LoadDirTask();
			//-d : return only directory
//			String cmd = type == 0 ? "ls -d /data/data/*/shared_prefs\n"
//					: "ls -d /data/data/*/databases\n";
			//可以递归获取
			String cmd = type == 0 ? "find /data/data/ /data/user_de/ -type d|grep '/shared_prefs'"
					: "find /data/data/ /data/user_de/ -type d|grep '/databases'|grep -v '/databases/'";
			mTask.execute(cmd);

		}

        initActionbar(0,getString(type == 0 ? R.string.sharedPreferences
                : R.string.database));
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
	}

	//参考AppForManageRecyclerFragment
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		if (!TextUtils.isEmpty(queryBefore)) {
			expandSearchView();
			searchView.setQuery(queryBefore, false);
		}
	}

	private CharSequence queryBefore;
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if (searchView != null && !TextUtils.isEmpty(searchView.getQuery())) {
			queryBefore = searchView.getQuery();
		}

        super.onCreateOptionsMenu(menu, inflater);

        if (type == 1
//				&& !BuildConfig.isFree
				) {
            MenuItem menuItem=menu.add(0, R.id.open_database, 0, R.string.open_database_in_sdcard);
            menuItem.setIcon(R.drawable.ic_folder_open_white_24dp);
            MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.open_database) {
            if (Utils.checkSDcard(getContext())) {
                chooseFile();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void chooseFile() {
		String lastPath = sharedPreferences.getString("last_db_dir", Environment.getExternalStorageDirectory().getAbsolutePath());

		FileChooseDialogFragment dialog = new FileChooseDialogFragment();
        Bundle args = new Bundle();
        args.putString(FileChooseDialogFragment.PATH_COME_IN, lastPath);
        dialog.setArguments(args);
        dialog.setTargetFragment(this, REQUEST_CODE_FILE_CHOOSE);
//        dialog.show(getActivity().getSupportFragmentManager(),"file_choose");
//		dialog.show(getChildFragmentManager(),"file_choose");
		dialog.show(getFragmentManager(),"file_choose");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_CODE_FILE_CHOOSE) {
			String path = data.getStringExtra(FileChooseDialogFragment.PATH_SEND_BACK);
			if (path == null) {
				com.tencent.mars.xlog.Log.e(TAG, "no path returned!");
				return;
			}
			if (resultCode == Activity.RESULT_OK) {
				final int slash = path.lastIndexOf("/");
				final String dir,name;
				if (slash >= 0) {
					dir = path.substring(0, slash);
					sharedPreferences.edit().putString("last_db_dir", dir).apply();;
					name = path.substring(slash + 1);
				}else{
					dir = path;
					name = "";
				}
//				toLocalFileTables(path,path.substring(slash + 1));
				toLocalFileTables(dir,name);
			} else if (resultCode == Activity.RESULT_CANCELED) {
				sharedPreferences.edit().putString("last_db_dir", path).apply();;
			}

		}
    }

    private void toLocalFileTables(String dir,String name) {
		Bundle args = new Bundle();
		args.putString("dir", dir);
		args.putString("name", name);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        FragmentTransaction ft = activity.getSupportFragmentManager()
                .beginTransaction();
        ft.replace(R.id.content,TableListFragment.newInstance(args));
        ft.setTransition(FragmentTransaction.TRANSIT_NONE);
        ft.addToBackStack(null);
        ft.commit();
    }

    @Override
	public void onDestroy() {
		super.onDestroy();
		if(mTask!=null){
			mTask.cancel(true);
		}
	}

	private class LoadDirTask extends AsyncTask<String, Void, List<AppItem>> {

		@Override
		protected List<AppItem> doInBackground(String... params) {
			List<String> suResult=Utils.runRootCommandForResult(params[0]);
			if(suResult==null){
				return null;
			}
			List<AppItem> result = new ArrayList<>();
            PackageManager pm = mContext.getPackageManager();
			for(String line:suResult){
                if(line.length()>11) {
					String pkgStr = null;
					if (line.startsWith("/data/data/")) {	// 从/data/data/开始算起；
						// /data/data/cn.wq.myandroidtools/databases
						// /data/data/dkplugin.cqh.tax/virtual/data/user/0/cn.wq.myandroidtools/databases
//					final int slashIndex = line.lastIndexOf(slash);
						final int slashIndex = line.indexOf(slash, 11);	//"/data/data/".length+1
						if (slashIndex < 0 || slashIndex < 11) {
							continue;
						}
						pkgStr = line.substring(11, slashIndex);
					} else if (line.startsWith("/data/user_de/")) {
						// 支持 Context.createDeviceProtectedStorageContext()
						// /data/user_de/0/com.android.settings/shared_prefs
						final int startSlashIndex = line.indexOf(slash, 14);    // "/data/user_de/".length+1
						if (startSlashIndex < 0) {
							Log.d(TAG, "line not match slash:" + line);
							continue;
						}
						final int endSlashIndex = line.indexOf(slash, startSlashIndex + 1);
						if (endSlashIndex < 0) {
							continue;
						}
						pkgStr = line.substring(startSlashIndex + 1, endSlashIndex);
					}
					if (TextUtils.isEmpty(pkgStr)) {
						continue;
					}

//					String string = line.substring(11, slashIndex);
                    AppItem item = new AppItem();
					// 多开分身 重复
					// /data/data/dkplugin.cqh.tax/virtual/data/user/0/cn.wq.myandroidtools/shared_prefs
					// /data/data/dkplugin.cqh.tax/shared_prefs
                    try {
                        ApplicationInfo aInfo = pm.getApplicationInfo(pkgStr, 0);
                        item.label = aInfo.loadLabel(pm).toString();
                        item.packageName = pkgStr;
                        item.directoryPath = line;
                        result.add(item);
                    } catch (NameNotFoundException e) {
                        e.printStackTrace();
                    }
                }
			}

            Collections.sort(result, new Comparator<AppItem>() {
                @Override
                public int compare(AppItem lhs, AppItem rhs) {
                    return Collator.getInstance(Locale.getDefault()).compare(
                            Utils.trimFor160(lhs.label),
                            Utils.trimFor160(rhs.label));
                }
            });
			return result;

			//方法3：
//			try {
//				final List<String> result = new ArrayList<String>();
//				Shell shell=Shell.startRootShell();
//				Command command=new Command(params[0]){
//
//					@Override
//					public void output(int id, String line) {
//						result.add(line.substring(11, line.lastIndexOf(slash)));
//					}
//
//					@Override
//					public void afterExecution(int id, int exitCode) {
//					}
//
//				};
//				shell.add(command).waitForFinish();
//				shell.close();
//				return result;
//			} catch (Exception e1) {
//				e1.printStackTrace();
//			}
//			return null;

			//方法1：
//			Process process = null;
//			DataOutputStream out = null;
//			BufferedReader reader = null;
//			try {
//				process = Runtime.getRuntime().exec("su");
//				out = new DataOutputStream(process.getOutputStream());
//				out.writeBytes(params[0]);
//				// out.writeBytes("ls /data/data\n");
//				out.writeBytes("exit\n");
//				out.flush();
//
//				reader = new BufferedReader(new InputStreamReader(
//						process.getInputStream()), 8192);
//				List<String> result = new ArrayList<String>();
//				String line;
//				while ((line = reader.readLine()) != null) {
//					result.add(line.substring(11, line.lastIndexOf(slash)));
//				}
//				return result;
//			} catch (IOException e) {
//				e.printStackTrace();
//			} finally {
//				try {
//					if (out != null) {
//						out.close();
//					}
//					if (reader != null) {
//						reader.close();
//					}
//					if (process != null) {
//						process.destroy();
//					}
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//			return null;
		}

		@Override
		protected void onPostExecute(List<AppItem> result) {
			super.onPostExecute(result);
			setListShown(true,true);
			if (result == null) {
				setEmptyText(getString(R.string.failed_to_gain_root));
				return;
			}

			mAdapter.setList(result);
			setHasOptionsMenu(true);
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			setListShown(false,true);
		}

	}

	private class AppItem {
		String label;
		String packageName;
		String directoryPath;
	}

	private class AppAdapter extends RecyclerView.Adapter<VHolder> implements Filterable {
		private List<AppItem> list;
		private Context context;
		private List<AppItem> originalData;
		private final Object mLock = new Object();
		private AppFilter mFilter;

        AppAdapter(Context context) {
			super();
			this.context = context;
			list = new ArrayList<>();
        }

		public void setList(List<AppItem> list) {
			this.list.clear();
			if (list != null) {
				this.list.addAll(list);
			}
			notifyDataSetChanged();
		}

		public Object getItem(int arg0) {
			return list.get(arg0);
		}

        @Override
        public VHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new VHolder(LayoutInflater.from(context).inflate(
                    R.layout.item_data_in_app_list, parent,false));
        }

        @Override
        public void onBindViewHolder(VHolder holder, int position) {
            AppItem item = list.get(position);

            holder.label.setText(item.label);
            holder.desc.setText(item.directoryPath);
			Utils.loadApkIcon(AppListForDataFragment.this, item.packageName, holder.icon);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

		@Override
		public Filter getFilter() {
			if (mFilter == null) {
				mFilter = new AppFilter();
			}
			return mFilter;
		}

		private class AppFilter extends Filter {
			@Override
			protected void publishResults(CharSequence constraint,
					FilterResults results) {
				list = (List<AppItem>) results.values;
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

				List<AppItem> tempList;
				if (TextUtils.isEmpty(constraint)) {
					synchronized (mLock) {
						tempList = new ArrayList<>(originalData);
					}
					results.values = tempList;
					results.count = tempList.size();
				} else {
					synchronized (mLock) {
						tempList = new ArrayList<>(originalData);
					}

					final List<AppItem> newValues = new ArrayList<>();
                    String lowercaseQuery = constraint.toString().toLowerCase();
					for (AppItem entry : tempList) {
						if (entry.label.toLowerCase(Locale.getDefault())
								.contains(lowercaseQuery)) {
							newValues.add(entry);
						}
					}

					results.values = newValues;
					results.count = newValues.size();
				}

				return results;
			}
		}

	}

    private class VHolder extends RecyclerView.ViewHolder{
        ImageView icon;
        TextView label,desc;

        public VHolder(View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(R.id.icon);
            label = (TextView) itemView.findViewById(R.id.name);
			desc = (TextView) itemView.findViewById(R.id.desc);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
					InputMethodManager inputMethodManager = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
					if (inputMethodManager != null) {
						inputMethodManager.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
					}

					// FIXME: 2019-04-23 trick，没办法啊
					if (searchView != null) {
						queryBefore = searchView.getQuery();
					}

					int position=getLayoutPosition();
                    final AppItem item = (AppItem) mAdapter.getItem(position);
                    Bundle args = new Bundle();
                    args.putString("packageName", item.packageName);
                    args.putString("title", item.label);
                    args.putInt("type", type);
                    args.putString("directoryPath", item.directoryPath);

                    AppCompatActivity activity = (AppCompatActivity) mContext;
                    FragmentTransaction ft = activity.getSupportFragmentManager()
                            .beginTransaction();
                    ft.replace(R.id.content,
                            DataFileListFragment.newInstance(args));
            		ft.setTransition(FragmentTransaction.TRANSIT_NONE);
                    ft.addToBackStack(null);
                    ft.commit();
                }
            });
        }
    }

}
