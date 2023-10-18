package cn.wq.myandroidtoolspro;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.LinearLayout;

import cn.wq.myandroidtoolspro.helper.LanguageUtils;

public class BaseActivity extends AppCompatActivity{
//    public final static String PREFERENCE_DARK_THEME="darkTheme";
    public final static String PREFERENCE_THEME="theme";
    public final static String PREFERENCE_ENGLISH="english";
    public final static String PREFERENCE_LANGUAGE="language";
    public final static String PREFERENCE_PACKAGE_RECEIVED="packageReceived";
    public final static String PREFERENCE_PM_CHANNER="pm_channel";
    public static final String PREFERENCE_CLOSE_BUGLY = "close_bugly";

    //    private CharSequence savedTitle;
    public final static int PM_CHANNEL_ROOT_COMMAND = 0;
    public final static int PM_CHANNEL_SHIZUKU = 1;
    public final static int PM_CHANNEL_IFW = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences=PreferenceManager.getDefaultSharedPreferences(this);

        Resources resources=getBaseContext().getResources();
        Configuration cfg=resources.getConfiguration();
        cfg.locale= LanguageUtils.getChoosedLocale(sharedPreferences);
        resources.updateConfiguration(cfg,resources.getDisplayMetrics());
        super.onCreate(savedInstanceState);
    }

//    /**
//     * @param showUp whether show up icon
//     * @param title
//     */
//    public void resetActionbar(boolean showUp,String title){
//        ActionBar mActionBar=getSupportActionBar();
//        if(title==null){
//            mActionBar.setTitle(savedTitle);
//        }else{
//            savedTitle=mActionBar.getTitle();
//            mActionBar.setTitle(title);
//        }
//
//        if(showUp){
//            mActionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);
//        }else{
//            mActionBar.setHomeAsUpIndicator(R.drawable.ic_menu_white);
//        }
//    }

    public LinearLayout getAdContainer(){
        return null;
    }
}
