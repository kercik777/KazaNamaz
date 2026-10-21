package tm.kaza.namaz.ag.data;

import android.content.Context;

import tm.kaza.namaz.ag.R;

/**
 * Шесть намазов, которые отслеживает приложение.
 * Названия берутся из строковых ресурсов для поддержки локализации.
 */
public enum PrayerType {
    FAJR(R.string.prayer_fajr, 0),
    ZUHR(R.string.prayer_zuhr, 1),
    ASR(R.string.prayer_asr, 2),
    MAGHRIB(R.string.prayer_maghrib, 3),
    ISHA(R.string.prayer_isha, 4),
    WITR(R.string.prayer_witr, 5);

    private final int nameResId;
    private final int index;

    PrayerType(int nameResId, int index) {
        this.nameResId = nameResId;
        this.index = index;
    }

    /** Получить локализованное название намаза. */
    public String getName(Context context) {
        return context.getString(nameResId);
    }

    /** Идентификатор строкового ресурса названия. */
    public int getNameResId() {
        return nameResId;
    }

    public int getIndex() {
        return index;
    }
}
