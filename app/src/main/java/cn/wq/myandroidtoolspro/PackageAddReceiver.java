package cn.wq.myandroidtoolspro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class PackageAddReceiver extends BroadcastReceiver {
	private static final String TAG = "PackageAddReceiver";

	@Override
	public void onReceive(Context context, Intent data) {
		handleAppAdd(context, data);
	}

	public static void handleAppAdd(Context context, Intent data) {
		String action = data.getAction();
		if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
			final String str = data.getDataString();
			if (str != null && str.length() > 7) {
				String packageName = data.getDataString().substring(8);

				ApplicationInfo aInfo;
				String label = packageName;
				try {
					PackageManager pm = context.getPackageManager();
					aInfo = pm.getApplicationInfo(packageName, 0);
					label = (String) aInfo.loadLabel(pm);
				} catch (NameNotFoundException e) {
					e.printStackTrace();
				}
				Intent intent = new Intent(context, AppInfoActivity.class);
				intent.putExtra("packageName", packageName);
				intent.putExtra("title", label);
				intent.putExtra("part", true);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
				context.startActivity(intent);
			} else {
				com.tencent.mars.xlog.Log.e(TAG, "PackageAddReceiver data not match:" + data.getDataString());
			}
		}
	}

}
