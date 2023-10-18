package cn.wq.myandroidtoolspro;

import android.os.Bundle;
import android.preference.PreferenceManager;

import cn.wq.myandroidtoolspro.recyclerview.fragment.AppInfoForManageFragment2;

public class AppInfoActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final int theme = PreferenceManager.getDefaultSharedPreferences(this).getInt(PREFERENCE_THEME, 0);
        if (theme == 2) {
            setTheme(R.style.AppThemeBlack_AppInfoTheme);
        } else if (theme == 1) {
            setTheme(R.style.AppThemeDark_AppInfoTheme);
        } else {
            setTheme(R.style.AppTheme_AppInfoTheme);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_info);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(
                    R.id.content,
                    AppInfoForManageFragment2.newInstance(getIntent().getExtras()))
                    .commit();
        }
    }

}
