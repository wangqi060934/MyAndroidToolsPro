package cn.wq.myandroidtoolspro;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.Toast;

import com.tencent.mars.xlog.Xlog;

import java.io.File;

import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.recyclerview.fragment.AboutFragment;
import cn.wq.myandroidtoolspro.recyclerview.fragment.AppListForDataFragment;
import cn.wq.myandroidtoolspro.recyclerview.fragment.AppManageParentFragment;
import cn.wq.myandroidtoolspro.recyclerview.fragment.ComponentParentFragment;
import cn.wq.myandroidtoolspro.recyclerview.fragment.current.CurrentFragment;
import cn.wq.myandroidtoolspro.recyclerview.fragment.LogFragment;
import cn.wq.myandroidtoolspro.recyclerview.fragment.ProcessFragment;
import cn.wq.myandroidtoolspro.recyclerview.fragment.ReceiverParentFragment;
import cn.wq.myandroidtoolspro.recyclerview.fragment.UidFragment;
import cn.wq.navigationview.GridNavigationView;

public class MainActivity extends BaseActivity implements GridNavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout mDrawerLayout;
    private GridNavigationView mNavigationView;
    private int mSavedMenuItemId;
    private static final String NAV_ITEM_ID="navItemId";
    private static final int REQUEST_CODE_PERMISSION=2;
    private boolean needKillSelf;

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

//        SharedPreferences sharedPreferences=PreferenceManager.getDefaultSharedPreferences(this);
//        if (sharedPreferences.getBoolean(PREFERENCE_DARK_THEME, false)) {
//            setTheme(R.style.AppThemeDark);
//        } else {
//            setTheme(R.style.AppTheme);
//        }

        super.onCreate(savedInstanceState);

//        if(!checkWhetherAuthorised()){
//            Toast.makeText(this,R.string.toast_piracy,Toast.LENGTH_LONG).show();
//            finish();
//            return;
//        }

        setContentView(R.layout.activity_main);
//        Toolbar toolbar=(Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

//        ActionBar actionBar=getSupportActionBar();
//        actionBar.setDisplayHomeAsUpEnabled(true);
//        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_white);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationView= (GridNavigationView) findViewById(R.id.navigationView);
        mNavigationView.setNavigationItemSelectedListener(this);

        if(savedInstanceState==null){
            mSavedMenuItemId=R.id.about;

            //wq:bug on 2.3,https://code.google.com/p/android/issues/detail?id=81083
            if(Build.VERSION.SDK_INT<11){
                try {
                    Class.forName("android.os.AsyncTask");
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            Intent intent=getIntent();
            if(intent==null||!intent.getBooleanExtra("restart",false)){
                mDrawerLayout.openDrawer(mNavigationView);
            }

            needKillSelf = intent != null && intent.getBooleanExtra("needKillSelf", false);
            changeContainerFragment(R.id.about);
        }else{
            mSavedMenuItemId=savedInstanceState.getInt(NAV_ITEM_ID,R.id.about);
        }

        if (Build.VERSION.SDK_INT < 11 && !checkAdbEnabled()) {
            Toast.makeText(this, R.string.toast_adb, Toast.LENGTH_SHORT).show();
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                !=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}
                    , REQUEST_CODE_PERMISSION);
        }

//        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
//            StrictMode.setThreadPolicy(
//                    new StrictMode.ThreadPolicy.Builder()
//                            .detectAll().penaltyLog().build());
//            StrictMode.setVmPolicy(
//                    new StrictMode.VmPolicy.Builder().
//                            detectAll().penaltyLog().build());
//        }

        //修改 Recent Activity 中 toolbar 的背景颜色
        if (theme == 2 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager.TaskDescription description = new ActivityManager.TaskDescription(
                    null,
                    BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher_dark),
                    ContextCompat.getColor(this, R.color.dark_black));
            setTaskDescription(description);
        }

        initXlog();
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

    /**
     * 1.google play: "com.android.vending"
     * 2.amazon: "com.amazon.venezia"
     */
    /**
     private boolean checkWhetherAuthorised() {
     //        String storePkgName="com.android.vending";
     //        try {
     //            getPackageManager().getPackageInfo(storePkgName, 0);
     //            String installer=getPackageManager().getInstallerPackageName(getPackageName());
     //            return TextUtils.equals(installer, storePkgName);
     //        } catch (PackageManager.NameNotFoundException e) {
     //            return false;
     //        }

     try {
     PackageInfo pInfo=getPackageManager().getPackageInfo("com.android.vending", 0);
     if (pInfo != null) {
     return true;
     }
     } catch (PackageManager.NameNotFoundException e) {
     //            e.printStackTrace();
     }

     try {
     PackageInfo pInfo=getPackageManager().getPackageInfo("com.amazon.venezia", 0);
     return pInfo!=null;
     } catch (PackageManager.NameNotFoundException e) {
     //            e.printStackTrace();
     }
     return false;
     }
     */

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==REQUEST_CODE_PERMISSION){
//            if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
//            }
        }
    }

    private boolean checkAdbEnabled() {
        return Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ADB_ENABLED, 0) > 0;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        mDrawerLayout.closeDrawer(mNavigationView);

        final int id=menuItem.getItemId();

        menuItem.setChecked(true);

        changeContainerFragment(id);

        mSavedMenuItemId=id;
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(mSavedMenuItemId==R.id.logcat&&keyCode==KeyEvent.KEYCODE_MENU){
            if(mDrawerLayout.isDrawerOpen(mNavigationView)){
                mDrawerLayout.closeDrawer(mNavigationView);
            }else{
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
        switch (item.getItemId()){
            case android.R.id.home:
                if(getSupportFragmentManager().getBackStackEntryCount()==0){
                    if(mDrawerLayout.isDrawerOpen(mNavigationView)){
                        mDrawerLayout.closeDrawer(mNavigationView);
                    }else
                        mDrawerLayout.openDrawer(mNavigationView);
                }else{
                    onBackPressed();
                }
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        super.onBackPressed();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        com.tencent.mars.xlog.Log.appenderClose();

        File tempDB=new File(Utils.getTempDBPath(this));
        File tempDBwal=new File(Utils.getTempDBWalPath(this));
        File tempSPrefs=new File(Utils.getTempSPfrefsPath(this));

        if(tempDB.exists()){
            tempDB.delete();
        }
        if (tempDBwal.exists()) {
            tempDBwal.delete();
        }
        if(tempSPrefs.exists()){
            tempSPrefs.delete();
        }

    }

    private void changeContainerFragment(int itemId){
        Fragment f;
        switch (itemId){
            case R.id.service:
                f = ComponentParentFragment.newInstance(0);
                break;
            case R.id.receiver:
                f= new ReceiverParentFragment();
                break;
            case R.id.activity:
                f = ComponentParentFragment.newInstance(2);
                break;
            case R.id.provider:
                f = ComponentParentFragment.newInstance(3);
                break;
            case R.id.preferece:
                f= AppListForDataFragment.newInstance(0);
                break;
            case R.id.database:
                f = AppListForDataFragment.newInstance(1);
                break;
            case R.id.process:
                f=new ProcessFragment();
                break;
            case R.id.logcat:
                f=new LogFragment();
                break;
            case R.id.uid:
                f=new UidFragment();
                break;
            case R.id.current:
                f = new CurrentFragment();
                break;
            case R.id.apps:
                f=new AppManageParentFragment();
                break;
            default:
                f = new AboutFragment();
                break;
        }

        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        FragmentTransaction ft=getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.content, f);
        ft.setTransition(FragmentTransaction.TRANSIT_NONE);
        ft.commit();
    }

}
