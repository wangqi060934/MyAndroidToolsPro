package cn.wq.myandroidtoolspro.helper;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

import cn.wq.myandroidtoolspro.BaseActivity;

public class LanguageUtils {
    public static int getChoosedLanguage(SharedPreferences sharedPreferences) {
        int defaultLanguage = sharedPreferences.getInt(BaseActivity.PREFERENCE_LANGUAGE, -1);
        if (defaultLanguage < 0) {
            boolean usingEnglish = sharedPreferences.getBoolean(BaseActivity.PREFERENCE_ENGLISH, false);
            if (usingEnglish) {
                defaultLanguage = 0;
                sharedPreferences.edit().remove(BaseActivity.PREFERENCE_ENGLISH).apply();
            } else {
                String locale = getSystemLocale().getLanguage();
                if ("zh".equals(locale)) {
                    defaultLanguage = 1;
                } else if ("ru".equals(locale)) {
                    defaultLanguage = 2;
                } else {
                    defaultLanguage = 0;
                }
            }
        }
        return defaultLanguage;
    }

    public static Locale getChoosedLocale(SharedPreferences sharedPreferences) {
        int language = getChoosedLanguage(sharedPreferences);
        Locale locale;
        if (language == 0) {
            locale = Locale.ENGLISH;
        } else if (language == 1) {
            locale = Locale.CHINA;
        } else if (language == 2) {
            locale = new Locale("ru", "RU");
        } else {
            locale = getSystemLocale();
        }
        return locale;
    }

    private static Locale getSystemLocale() {
        if (Build.VERSION.SDK_INT >= 24) {
            return Resources.getSystem().getConfiguration().getLocales().get(0);
        } else {
            return Resources.getSystem().getConfiguration().locale;
        }
    }

}
