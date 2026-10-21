package tm.kaza.namaz.ag.util;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import tm.kaza.namaz.ag.R;

/**
 * Единая настройка status bar / navigation bar для edge-to-edge (Android 15+).
 *
 * Начиная с targetSdk 35 Android принудительно делает приложения edge-to-edge:
 * цвета statusBarColor/navigationBarColor игнорируются, а layout рисуется
 * под системными барами. Поэтому мы сами включаем edge-to-edge, рисуем фон
 * под барами (градиентная шапка уходит под статус-бар) и добавляем отступ
 * сверху контенту шапки.
 *
 * Нижняя навигация (BottomNavigationView) применяет нижний отступ сама,
 * поэтому её дополнительно паддить не нужно.
 */
public final class SystemBarsHelper {

    private SystemBarsHelper() {
    }

    /** Полная настройка окна под edge-to-edge. */
    public static void edgeToEdge(Activity activity) {
        if (activity == null) return;
        Window window = activity.getWindow();
        if (window == null) return;

        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= 29) {
            window.setNavigationBarContrastEnforced(false);
            window.setStatusBarContrastEnforced(false);
        }

        boolean lightStatus = activity.getResources().getBoolean(R.bool.light_status_bar);
        boolean lightNav = activity.getResources().getBoolean(R.bool.light_nav_bar);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(lightStatus);
        controller.setAppearanceLightNavigationBars(lightNav);
    }

    /** Добавить отступ сверху (под статус-бар), сохраняя исходные padding. */
    public static void padTop(View view) {
        pad(view, false, true, false, false);
    }

    /** Добавить отступ снизу (под навигационную панель), сохраняя исходные padding. */
    public static void padBottom(View view) {
        pad(view, false, false, false, true);
    }

    private static void pad(View view, boolean left, boolean top, boolean right, boolean bottom) {
        if (view == null) return;
        final int initialLeft = view.getPaddingLeft();
        final int initialTop = view.getPaddingTop();
        final int initialRight = view.getPaddingRight();
        final int initialBottom = view.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    initialLeft + (left ? bars.left : 0),
                    initialTop + (top ? bars.top : 0),
                    initialRight + (right ? bars.right : 0),
                    initialBottom + (bottom ? bars.bottom : 0)
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }
}
