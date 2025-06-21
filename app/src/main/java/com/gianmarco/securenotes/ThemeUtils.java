package com.gianmarco.securenotes;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeUtils {
    private static final String PREFS_NAME = "secure_notes_prefs";
    private static final String KEY_SKIP_AUTH_ON_THEME = "skip_auth_on_theme";

    public static void setTheme(Context context, int mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_SKIP_AUTH_ON_THEME, true).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static boolean shouldSkipAuth(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean skip = prefs.getBoolean(KEY_SKIP_AUTH_ON_THEME, false);
        if (skip) {
            prefs.edit().putBoolean(KEY_SKIP_AUTH_ON_THEME, false).apply();
        }
        return skip;
    }
} 