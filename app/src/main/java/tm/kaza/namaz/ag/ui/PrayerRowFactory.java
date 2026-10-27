package tm.kaza.namaz.ag.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import tm.kaza.namaz.ag.R;
import tm.kaza.namaz.ag.data.PrayerType;

/**
 * Создаёт строки намазов: имя и две явные кнопки «Выполнен» / «Пропущен».
 *
 * Состояния намаза:
 *  0 — не отмечено (нейтрально, по умолчанию),
 *  1 — пропущен (каза),
 *  2 — выполнен.
 *
 * Кнопки работают как переключатели: повторное нажатие на активную
 * кнопку возвращает намаз в нейтральное состояние.
 */
public class PrayerRowFactory {

    public interface OnToggle {
        void onToggle(int prayerIndex, int newState);
    }

    public static final int[] PRAYER_MAIN = {
            R.color.prayer_fajr, R.color.prayer_zuhr, R.color.prayer_asr,
            R.color.prayer_maghrib, R.color.prayer_isha, R.color.prayer_witr
    };

    public static final int[] PRAYER_SOFT = {
            R.color.prayer_fajr_soft, R.color.prayer_zuhr_soft, R.color.prayer_asr_soft,
            R.color.prayer_maghrib_soft, R.color.prayer_isha_soft, R.color.prayer_witr_soft
    };

    public static View build(Context ctx, ViewGroup parent, PrayerType prayer,
                             int[] flags, final OnToggle listener) {
        LayoutInflater inflater = LayoutInflater.from(ctx);
        View row = inflater.inflate(R.layout.item_prayer, parent, false);

        TextView name = row.findViewById(R.id.tvPrayerName);
        ImageView icon = row.findViewById(R.id.ivPrayerIcon);
        FrameLayout iconBg = row.findViewById(R.id.prayerIconBg);

        LinearLayout btnDone = row.findViewById(R.id.btnDone);
        LinearLayout btnMissed = row.findViewById(R.id.btnMissed);
        ImageView ivDone = row.findViewById(R.id.ivDoneIcon);
        ImageView ivMissed = row.findViewById(R.id.ivMissedIcon);
        TextView tvDone = row.findViewById(R.id.tvDoneLabel);
        TextView tvMissed = row.findViewById(R.id.tvMissedLabel);

        int idx = prayer.getIndex();
        name.setText(prayer.getName(ctx));
        icon.setColorFilter(ContextCompat.getColor(ctx, PRAYER_MAIN[idx]));
        iconBg.getBackground().setTint(ContextCompat.getColor(ctx, PRAYER_SOFT[idx]));

        final int state = flags[idx];
        applyPills(ctx, btnDone, ivDone, tvDone, btnMissed, ivMissed, tvMissed, state);

        btnDone.setOnClickListener(v -> {
            int newState = (state == 2) ? 0 : 2;
            applyPills(ctx, btnDone, ivDone, tvDone, btnMissed, ivMissed, tvMissed, newState);
            if (listener != null) listener.onToggle(idx, newState);
        });

        btnMissed.setOnClickListener(v -> {
            int newState = (state == 1) ? 0 : 1;
            applyPills(ctx, btnDone, ivDone, tvDone, btnMissed, ivMissed, tvMissed, newState);
            if (listener != null) listener.onToggle(idx, newState);
        });

        return row;
    }

    /**
     * Обновляет вид кнопок: активная кнопка заливается цветом,
     * остальные — нейтральный контур.
     *
     * @param state 0 — не отмечено, 1 — пропущен, 2 — выполнен.
     */
    private static void applyPills(Context ctx,
                                   LinearLayout btnDone, ImageView ivDone, TextView tvDone,
                                   LinearLayout btnMissed, ImageView ivMissed, TextView tvMissed,
                                   int state) {
        int onFill = ContextCompat.getColor(ctx, R.color.text_on_primary);
        int neutralText = ContextCompat.getColor(ctx, R.color.text_secondary);

        if (state == 2) {
            btnDone.setBackgroundResource(R.drawable.bg_pill_done_active);
            ivDone.setColorFilter(onFill);
            tvDone.setTextColor(onFill);

            btnMissed.setBackgroundResource(R.drawable.bg_pill_neutral);
            ivMissed.setColorFilter(neutralText);
            tvMissed.setTextColor(neutralText);
        } else if (state == 1) {
            btnDone.setBackgroundResource(R.drawable.bg_pill_neutral);
            ivDone.setColorFilter(neutralText);
            tvDone.setTextColor(neutralText);

            btnMissed.setBackgroundResource(R.drawable.bg_pill_missed_active);
            ivMissed.setColorFilter(onFill);
            tvMissed.setTextColor(onFill);
        } else {
            btnDone.setBackgroundResource(R.drawable.bg_pill_neutral);
            ivDone.setColorFilter(neutralText);
            tvDone.setTextColor(neutralText);

            btnMissed.setBackgroundResource(R.drawable.bg_pill_neutral);
            ivMissed.setColorFilter(neutralText);
            tvMissed.setTextColor(neutralText);
        }
    }
}
