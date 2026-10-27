package tm.kaza.namaz.ag.util;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

import tm.kaza.namaz.ag.data.DataStore;

/**
 * Управление темой оформления: системная, светлая или тёмная.
 */
public class ThemeUtil {

    public static void apply(Context context) {
        String mode = DataStore.get(context).getThemeMode();
        int night;
        switch (mode) {
            case DataStore.THEME_LIGHT:
                night = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case DataStore.THEME_DARK:
                night = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            default:
                night = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }
        if (AppCompatDelegate.getDefaultNightMode() != night) {
            AppCompatDelegate.setDefaultNightMode(night);
        }
    }

    public static String modeLabel(Context context, String mode) {
        switch (mode) {
            case DataStore.THEME_LIGHT:
                return context.getString(tm.kaza.namaz.ag.R.string.theme_light);
            case DataStore.THEME_DARK:
                return context.getString(tm.kaza.namaz.ag.R.string.theme_dark);
            default:
                return context.getString(tm.kaza.namaz.ag.R.string.theme_auto);
        }
    }
}
