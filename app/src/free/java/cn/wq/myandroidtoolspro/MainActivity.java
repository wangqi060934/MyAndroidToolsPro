package cn.wq.myandroidtoolspro;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.LinearLayout;

import com.tencent.mars.xlog.Log;
import com.tencent.mars.xlog.Xlog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cn.wq.myandroidtoolspro.helper.AdHelperInterface;
import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.recyclerview.fragment.about.AboutFragment;
import cn.wq.myandroidtoolspro.recyclerview.fragment.AppListForDataFragment;
import cn.wq.myandroidtoolspro.recyclerview.fragment.AppManageParentFragment;
import cn.wq.myandroidtoolspro.recyclerview.fragment.ComponentParentFragment;
import cn.wq.myandroidtoolspro.recyclerview.fragment.LogFragment;
import cn.wq.myandroidtoolspro.recyclerview.fragment.ProcessFragment;
import cn.wq.myandroidtoolspro.recyclerview.fragment.ReceiverParentFragment;
import cn.wq.myandroidtoolspro.recyclerview.fragment.UidFragment;
import cn.wq.myandroidtoolspro.recyclerview.fragment.current.CurrentFragment;
import cn.wq.navigationview.GridNavigationView;

public class MainActivity extends BaseActivity implements GridNavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainActivity";
    private static final String TAG_AD = "MatAd";
    private DrawerLayout mDrawerLayout;
    private GridNavigationView mNavigationView;
    private int mSavedMenuItemId;
    private static final String NAV_ITEM_ID = "navItemId";
    private static final int REQUEST_CODE_PERMISSION = 2;
    private boolean needKillSelf;
//    private BroadcastReceiver pkgAddReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final int theme = PreferenceManager.getDefaultSharedPreferences(this).getInt(PREFERENCE_THEME, 0);
        if (theme == 2) {
            setTheme(R.style.AppThemeBlack);
        } else if (theme == 1) {
            setTheme(R.style.AppThemeDark);
        } else {
            setTheme(R.style.AppTheme);
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationView = (GridNavigationView) findViewById(R.id.navigationView);
        mNavigationView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {
            mSavedMenuItemId = R.id.about;

            //wq:bug on 2.3,https://code.google.com/p/android/issues/detail?id=81083
//            if(Build.VERSION.SDK_INT<11){
//                try {
//                    Class.forName("android.os.AsyncTask");
//                } catch (ClassNotFoundException e) {
//                    e.printStackTrace();
//                }
//            }

            Intent intent = getIntent();
            if (intent == null || !intent.getBooleanExtra("restart", false)) {
                mDrawerLayout.openDrawer(mNavigationView);
            }
            needKillSelf = intent != null && intent.getBooleanExtra("needKillSelf", false);
            changeContainerFragment(R.id.about);
        } else {
            mSavedMenuItemId = savedInstanceState.getInt(NAV_ITEM_ID, R.id.about);
        }

//        if (Build.VERSION.SDK_INT < 11 && !checkAdbEnabled()) {
//            Toast.makeText(this, R.string.toast_adb, Toast.LENGTH_SHORT).show();
//        }

        boolean permGranted = checkPermission();

        if (theme == 2 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager.TaskDescription description = new ActivityManager.TaskDescription(
                    null,
                    null,
                    ContextCompat.getColor(this, R.color.dark_black));
            setTaskDescription(description);
        }

        initXlog();

        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            StrictMode.setThreadPolicy(
                    new StrictMode.ThreadPolicy.Builder()
                            .detectAll().penaltyLog().build());
            StrictMode.setVmPolicy(
                    new StrictMode.VmPolicy.Builder().
                            detectAll().penaltyLog().build());
        }

//        registerPkgAddReceiver();
    }

//    private void registerPkgAddReceiver() {
//        if (pkgAddReceiver == null) {
//            pkgAddReceiver = new BroadcastReceiver() {
//                @Override
//                public void onReceive(Context context, Intent intent) {
//                    PackageAddReceiver.handleAppAdd(context, intent);
//                }
//            };
//            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
//            intentFilter.addDataScheme("package");
//            registerReceiver(pkgAddReceiver, intentFilter);
//        }
//    }

    @Override
    public LinearLayout getAdContainer() {
        return findViewById(R.id.content_container);
    }

    /**
     * @return whethe the permissions has granted
     */
    private boolean checkPermission() {
        boolean sdPermFB = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED;

        if (sdPermFB) {
            try {
                List<String> permList = new ArrayList<>(2);
                if (sdPermFB) {
                    permList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
                ActivityCompat.requestPermissions(this, permList.toArray(new String[0])
                        , REQUEST_CODE_PERMISSION);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }

    private void initXlog() {
        final String logPath = getExternalFilesDir(null) + "/xlog";

        // this is necessary, or may cash for SIGBUS
        final String cachePath = this.getFilesDir() + "/xlog";

        //init xlog
        if (BuildConfig.DEBUG) {
            Xlog.appenderOpen(Xlog.LEVEL_DEBUG, Xlog.AppednerModeAsync, cachePath, logPath, "Mat", null);
            Xlog.setConsoleLogOpen(true);
        } else {
            Xlog.appenderOpen(Xlog.LEVEL_INFO, Xlog.AppednerModeAsync, cachePath, logPath, "Mat", null);
            Xlog.setConsoleLogOpen(false);
        }

        com.tencent.mars.xlog.Log.setLogImp(new Xlog());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        mDrawerLayout.closeDrawer(mNavigationView);

        final int id = menuItem.getItemId();
//        if(id==mSavedMenuItemId){
//            return  true;
//        }
        menuItem.setChecked(true);

        changeContainerFragment(id);

        mSavedMenuItemId = id;
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mSavedMenuItemId == R.id.logcat && keyCode == KeyEvent.KEYCODE_MENU) {
            if (mDrawerLayout.isDrawerOpen(mNavigationView)) {
                mDrawerLayout.closeDrawer(mNavigationView);
            } else {
                mDrawerLayout.openDrawer(mNavigationView);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(NAV_ITEM_ID, mSavedMenuItemId);
    }

//    @Override
//    public void onConfigurationChanged(Configuration newConfig) {
//        super.onConfigurationChanged(newConfig);
//        mDrawerToggle.onConfigurationChanged(newConfig);
//    }
//
//    @Override
//    protected void onPostCreate(Bundle savedInstanceState) {
//        super.onPostCreate(savedInstanceState);
//        mDrawerToggle.syncState();
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                    if (mDrawerLayout.isDrawerOpen(mNavigationView)) {
                        mDrawerLayout.closeDrawer(mNavigationView);
                    } else
                        mDrawerLayout.openDrawer(mNavigationView);
                } else {
                    onBackPressed();
                }
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (needKillSelf) { //不然的话刚换主题后 马上从launcher icon永远启动会在new task中新建activity
            finish();
//            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
//            if (am != null) {
//                am.killBackgroundProcesses(getPackageName());
//            }
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        com.tencent.mars.xlog.Log.appenderClose();

        File tempDB = new File(Utils.getTempDBPath(this));
        File tempDBwal = new File(Utils.getTempDBWalPath(this));
        File tempSPrefs = new File(Utils.getTempSPfrefsPath(this));

        if (tempDB.exists()) {
            tempDB.delete();
        }
        if (tempDBwal.exists()) {
            tempDBwal.delete();
        }
        if (tempSPrefs.exists()) {
            tempSPrefs.delete();
        }

//        if (pkgAddReceiver != null) {
//            try {
//                unregisterReceiver(pkgAddReceiver);
//            } catch (Exception e) {
//                Log.e(TAG, "unregisterReceiver pkgAddReceiver", e);
//                Utils.err(TAG, "unregisterReceiver pkgAddReceiver", e);
//            }
//        }
    }

    private void changeContainerFragment(int itemId) {
        Fragment f;
        switch (itemId) {
            case R.id.service:
                f = ComponentParentFragment.newInstance(0);
                break;
            case R.id.receiver:
                f = new ReceiverParentFragment();
                break;
            case R.id.activity:
                f = ComponentParentFragment.newInstance(2);
                break;
            case R.id.provider:
                f = ComponentParentFragment.newInstance(3);
                break;
            case R.id.preferece:
                f = AppListForDataFragment.newInstance(0);
                break;
            case R.id.database:
                f = AppListForDataFragment.newInstance(1);
                break;
            case R.id.process:
                f = new ProcessFragment();
                break;
            case R.id.logcat:
                f = new LogFragment();
                break;
            case R.id.uid:
                f = new UidFragment();
                break;
            case R.id.current:
                f = new CurrentFragment();
                break;
            case R.id.apps:
                f = new AppManageParentFragment();
                break;
            default:
                f = new AboutFragment();
                break;
        }

        if (getSupportFragmentManager() == null) {
            Log.e(TAG, "getSupportFragmentManager is null when change to fragment:" + f.toString());
            return;
        }
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.content, f);
//        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        ft.commit();
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        if (onBackListener != null) {
            onBackListener.onBack();
        }

        super.onBackPressed();
    }

    public interface OnBackListener{
        void onBack();
    }
    private OnBackListener onBackListener;
    public void addOnBackListener(OnBackListener l) {
        onBackListener = l ;
    }
    public void removeOnBackListener() {
        onBackListener = null;
    }

}