package com.ikun656.lanfile;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.LocaleList;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.Locale;

/** 简单的偏好存储：主题（明/暗）与语言（zh/en）。 */
public final class AppPrefs {
    private static final String NAME = "lanfile_prefs";
    private static final String KEY_THEME = "theme";      // 0=跟随系统 1=亮 2=暗
    private static final String KEY_LANG = "lang";        // ""=跟随系统 "zh" "en"

    private AppPrefs() {}

    private static SharedPreferences sp(Context ctx) {
        return ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static int getThemeMode(Context ctx) {
        return sp(ctx).getInt(KEY_THEME, 0);
    }

    public static void setThemeMode(Context ctx, int mode) {
        sp(ctx).edit().putInt(KEY_THEME, mode).apply();
        AppCompatDelegate.setDefaultNightMode(toNightMode(mode));
    }

    private static int toNightMode(int mode) {
        switch (mode) {
            case 1: return AppCompatDelegate.MODE_NIGHT_NO;
            case 2: return AppCompatDelegate.MODE_NIGHT_YES;
            default: return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }

    public static String getLang(Context ctx) {
        return sp(ctx).getString(KEY_LANG, "");
    }

    public static void setLang(Context ctx, String lang) {
        sp(ctx).edit().putString(KEY_LANG, lang).apply();
    }

    /** 在 attachBaseContext 调用，按存储切换语言。 */
    public static Context applyLocale(Context ctx) {
        String lang = getLang(ctx);
        if (lang.isEmpty()) return ctx;
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration cfg = new Configuration(ctx.getResources().getConfiguration());
        cfg.setLocale(locale);
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            cfg.setLocales(new LocaleList(locale));
        }
        return ctx.createConfigurationContext(cfg);
    }
}
