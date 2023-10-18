package cn.wq.myandroidtoolspro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class AppInfoReceiver extends BroadcastReceiver {
    private static final String ACTION_SHOW_APP_INFO = BuildConfig.isFree ? "cn.wq.myandroidtools.SHOW_APP_INFO" : "cn.wq.myandroidtoolspro.SHOW_APP_INFO";
    private static final String TAG = BuildConfig.isFree ? "MyAndroidTools" : "MyAndroidToolsPro";

    @Override
    public void onReceive(Context context, Intent data) {
        String action = data.getAction();

        if (ACTION_SHOW_APP_INFO.equals(action)) {
            Bundle bundle = data.getExtras();
            if (bundle == null) {
                com.tencent.mars.xlog.Log.e(TAG, "The intent to MyAndroidTools must have a string data named \"packageName\".");
                return;
            }
            String packageName = bundle.getString("packageName");
            if (packageName == null) {
                com.tencent.mars.xlog.Log.e(TAG, "The intent to MyAndroidTools must have a string data named \"packageName\".");
                return;
            }

            Intent intent = new Intent(context, AppInfoActivity.class);
            intent.putExtra("packageName", packageName);
            intent.putExtra("part", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            context.startActivity(intent);
        }
    }

}
