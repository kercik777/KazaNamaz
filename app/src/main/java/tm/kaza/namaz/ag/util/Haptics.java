package tm.kaza.namaz.ag.util;

import android.content.Context;
import android.os.Build;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.View;

/**
 * Единая точка тактильного отклика.
 *
 * <p>Прямой вызов Vibrator используется вместо {@code View.performHapticFeedback}:
 * на Android 12+ системный отклик вида часто подавляется, если у окна нет
 * собственного тактильного канала, поэтому нажатия ощущались «немыми».
 * Разрешение {@code android.permission.VIBRATE} объявлено в манифесте — это
 * обычное разрешение, диалог пользователю не показывается.</p>
 */
public final class Haptics {

    /** Длительность короткого отклика нажатия в миллисекундах. */
    private static final long CLICK_DURATION_MS = 24;

    private Haptics() {
    }

    public static void click(View source) {
        if (source == null) return;
        Context context = source.getContext().getApplicationContext();
        Vibrator vibrator = vibrator(context);
        if (vibrator == null || !vibrator.hasVibrator()) {
            source.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            return;
        }
        try {
            VibrationEffect effect = VibrationEffect.createOneShot(
                    CLICK_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && systemTouchFeedbackEnabled(context)) {
                vibrator.vibrate(effect, new VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_TOUCH)
                        .build());
            } else {
                vibrator.vibrate(effect);
            }
        } catch (RuntimeException e) {
            source.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
        }
    }

    private static boolean systemTouchFeedbackEnabled(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0;
    }

    private static Vibrator vibrator(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager manager =
                    (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            return manager == null ? null : manager.getDefaultVibrator();
        }
        return (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }
}
