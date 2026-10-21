package tm.kaza.namaz.ag.util;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import tm.kaza.namaz.ag.R;

/**
 * Вспомогательные методы работы с датами и форматированием.
 * Все пользовательские строки берутся из ресурсов для поддержки локализации.
 */
public class DateUtils {

    /** «Пятница, 22 августа» — с большой буквы. */
    public static String formatFull(Context ctx, Calendar c) {
        String[] wd = ctx.getResources().getStringArray(R.array.weekdays_capitalized);
        String[] mg = ctx.getResources().getStringArray(R.array.months_genitive);
        return wd[c.get(Calendar.DAY_OF_WEEK) - 1]
                + ", " + c.get(Calendar.DAY_OF_MONTH) + " "
                + mg[c.get(Calendar.MONTH)];
    }

    /** «22 августа 2026» */
    public static String formatDayMonthYear(Context ctx, Calendar c) {
        String[] mg = ctx.getResources().getStringArray(R.array.months_genitive);
        return c.get(Calendar.DAY_OF_MONTH) + " "
                + mg[c.get(Calendar.MONTH)] + " "
                + c.get(Calendar.YEAR);
    }

    /** «Август 2026» */
    public static String formatMonthYear(Context ctx, int year, int month) {
        String[] mn = ctx.getResources().getStringArray(R.array.months_nominative);
        return mn[month] + " " + year;
    }

    /** Короткое имя месяца: «авг» */
    public static String monthShort(Context ctx, int month) {
        String[] ms = ctx.getResources().getStringArray(R.array.months_short);
        return ms[month];
    }

    /** Склонение: «5 намазов», «1 намаз», «2 намаза». */
    public static String pluralNamaz(Context ctx, int n) {
        return ctx.getResources().getQuantityString(R.plurals.plural_namaz, n, n);
    }

    /** Склонение: «5 дней», «1 день», «2 дня». */
    public static String pluralDays(Context ctx, int n) {
        return ctx.getResources().getQuantityString(R.plurals.plural_day, n, n);
    }

    /** Склонение: «5 записей», «1 запись», «2 записи». */
    public static String pluralRecords(Context ctx, int n) {
        return ctx.getResources().getQuantityString(R.plurals.plural_record, n, n);
    }

    /** Имя файла резервной копии: kazanamaz_backup_2026-08-22.json */
    public static String backupFileName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return "kazanamaz_backup_" + sdf.format(new Date()) + ".json";
    }

    /** «Сегодня» / «Вчера» / дата — для списка истории. */
    public static String relativeDay(Context ctx, String dateKey) {
        Calendar today = Calendar.getInstance();
        Calendar target = parseKey(dateKey);
        if (target == null) return dateKey;
        Calendar yesterday = (Calendar) today.clone();
        yesterday.add(Calendar.DAY_OF_MONTH, -1);
        if (keyOf(today).equals(dateKey))
            return ctx.getString(R.string.relative_today);
        if (keyOf(yesterday).equals(dateKey))
            return ctx.getString(R.string.relative_yesterday);
        String[] mg = ctx.getResources().getStringArray(R.array.months_genitive);
        return target.get(Calendar.DAY_OF_MONTH) + " "
                + mg[target.get(Calendar.MONTH)]
                + (target.get(Calendar.YEAR) != today.get(Calendar.YEAR)
                ? " " + target.get(Calendar.YEAR) : "");
    }

    /** «Сегодня» / «Вчера» / «Завтра» или null для прочих дат. */
    public static String relativeLabel(Context ctx, Calendar c) {
        Calendar today = Calendar.getInstance();
        if (sameDay(c, today)) return ctx.getString(R.string.relative_today);
        Calendar y = (Calendar) today.clone();
        y.add(Calendar.DAY_OF_MONTH, -1);
        if (sameDay(c, y)) return ctx.getString(R.string.relative_yesterday);
        Calendar t = (Calendar) today.clone();
        t.add(Calendar.DAY_OF_MONTH, 1);
        if (sameDay(c, t)) return ctx.getString(R.string.relative_tomorrow);
        return null;
    }

    private static boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    /** «yyyy-MM-dd» для Calendar. */
    public static String keyOf(Calendar c) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
    }

    /** Разобрать «yyyy-MM-dd» в Calendar. */
    public static Calendar parseKey(String key) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date d = sdf.parse(key);
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            return c;
        } catch (Exception e) {
            return null;
        }
    }
}
