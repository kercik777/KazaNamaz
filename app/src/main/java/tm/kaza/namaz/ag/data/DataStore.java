package tm.kaza.namaz.ag.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Центральное хранилище данных на основе SharedPreferences.
 * Для каждого дня хранится строка из 6 символов, по одному на намаз:
 *   '0' — не отмечено (нейтральное состояние),
 *   '1' — пропущен (каза),
 *   '2' — выполнен (подтверждён явно).
 * Заметки хранятся отдельно.
 */
public class DataStore {

    public static final int PRAYER_COUNT = 6;

    private static final String PREFS = "kaza_namaz_data";
    private static final String KEY_DAY_PREFIX = "day_";
    private static final String KEY_NOTE_PREFIX = "note_";
    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_APP_MODE = "app_mode";
    private static final String KEY_CALCULATOR_PROFILE = "calculator_profile";
    private static final String KEY_COUNTER_PREFIX = "counter_";
    private static final String KEY_COUNTER_DONE_PREFIX = "counter_done_";
    private static final String KEY_COUNTER_INITIAL_PRAYER = "counter_initial_prayer";
    private static final String KEY_COUNTER_INITIAL_FAST = "counter_initial_fast";
    private static final String KEY_COUNTER_HINT_SHOWN = "counter_hint_shown";
    private static final String KEY_LAST_SCREEN = "last_screen";
    public static final int COUNTER_COUNT = 7; // шесть намазов и счётчик поста
    public static final int MAX_COUNTER_VALUE = 999999;

    public static final String MODE_EXTENDED = "extended";
    public static final String MODE_COUNTER = "counter";

    public static final String THEME_AUTO = "auto";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    private static DataStore instance;
    private final SharedPreferences prefs;

    private DataStore(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized DataStore get(Context context) {
        if (instance == null) {
            instance = new DataStore(context);
        }
        return instance;
    }

    /* ==================== Работа с датами ==================== */

    public static String dateKey(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date);
    }

    public static String dateKey(Calendar calendar) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.getTime());
    }

    public static String dateKey(int year, int month, int day) {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
    }

    /* ==================== Тема оформления ==================== */

    public String getThemeMode() {
        return prefs.getString(KEY_THEME, THEME_AUTO);
    }

    public void setThemeMode(String mode) {
        prefs.edit().putString(KEY_THEME, mode).apply();
    }

    public String getAppMode() {
        return MODE_COUNTER.equals(prefs.getString(KEY_APP_MODE, MODE_EXTENDED))
                ? MODE_COUNTER : MODE_EXTENDED;
    }

    public void setAppMode(String mode) {
        prefs.edit().putString(KEY_APP_MODE,
                MODE_COUNTER.equals(mode) ? MODE_COUNTER : MODE_EXTENDED).apply();
    }

    public String getCalculatorProfile() {
        return prefs.getString(KEY_CALCULATOR_PROFILE, "");
    }

    public void setCalculatorProfile(String profile) {
        SharedPreferences.Editor editor = prefs.edit();
        if (profile == null || profile.isEmpty()) editor.remove(KEY_CALCULATOR_PROFILE);
        else editor.putString(KEY_CALCULATOR_PROFILE, profile);
        editor.apply();
    }

    /* ==================== Подсказка счётчика ==================== */

    public boolean isCounterHintShown() {
        return prefs.getBoolean(KEY_COUNTER_HINT_SHOWN, false);
    }

    public void setCounterHintShown(boolean shown) {
        prefs.edit().putBoolean(KEY_COUNTER_HINT_SHOWN, shown).apply();
    }

    /* ==================== Начальный долг ==================== */

    public int getInitialPrayerTotal() {
        return prefs.getInt(KEY_COUNTER_INITIAL_PRAYER, 0);
    }

    public int getInitialFastTotal() {
        return prefs.getInt(KEY_COUNTER_INITIAL_FAST, 0);
    }

    public void setInitialTotals(int prayerTotal, int fastTotal) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_COUNTER_INITIAL_PRAYER, prayerTotal);
        editor.putInt(KEY_COUNTER_INITIAL_FAST, fastTotal);
        editor.apply();
    }

    /* ==================== Последний экран ==================== */

    public int getLastScreen() {
        return prefs.getInt(KEY_LAST_SCREEN, 0);
    }

    public void setLastScreen(int screen) {
        prefs.edit().putInt(KEY_LAST_SCREEN, screen).apply();
    }

    /* ==================== Счётчики режима каза ==================== */

    public int[] getCounterCounts() {
        int[] counts = new int[COUNTER_COUNT];
        for (int i = 0; i < COUNTER_COUNT; i++) {
            counts[i] = Math.max(0, Math.min(MAX_COUNTER_VALUE,
                    prefs.getInt(KEY_COUNTER_PREFIX + i, 0)));
        }
        return counts;
    }

    public int[] getCounterDoneCounts() {
        int[] counts = new int[COUNTER_COUNT];
        for (int i = 0; i < COUNTER_COUNT; i++) {
            counts[i] = Math.max(0, Math.min(MAX_COUNTER_VALUE,
                    prefs.getInt(KEY_COUNTER_DONE_PREFIX + i, 0)));
        }
        return counts;
    }

    public void setCounterCount(int index, int count) {
        if (index < 0 || index >= COUNTER_COUNT) return;
        prefs.edit().putInt(KEY_COUNTER_PREFIX + index,
                Math.max(0, Math.min(MAX_COUNTER_VALUE, count))).apply();
        settleCounterProgressIfDone();
    }

    /** Меняет остаток; уменьшение кнопкой «−» отдельно фиксирует восполненное число. */
    public int adjustCounterCount(int index, int delta) {
        if (index < 0 || index >= COUNTER_COUNT) return 0;
        int oldCount = Math.max(0, Math.min(MAX_COUNTER_VALUE,
                prefs.getInt(KEY_COUNTER_PREFIX + index, 0)));
        long candidate = (long) oldCount + delta;
        int newCount = (int) Math.max(0, Math.min(MAX_COUNTER_VALUE, candidate));
        int actualDelta = newCount - oldCount;
        if (actualDelta == 0) return oldCount;

        SharedPreferences.Editor editor = prefs.edit()
                .putInt(KEY_COUNTER_PREFIX + index, newCount);
        if (actualDelta < 0) {
            int done = Math.max(0, Math.min(MAX_COUNTER_VALUE,
                    prefs.getInt(KEY_COUNTER_DONE_PREFIX + index, 0)));
            editor.putInt(KEY_COUNTER_DONE_PREFIX + index,
                    Math.min(MAX_COUNTER_VALUE, done - actualDelta));
        }
        editor.apply();
        settleCounterProgressIfDone();
        return newCount;
    }

    public void setCounterCounts(int[] counts) {
        SharedPreferences.Editor editor = prefs.edit();
        int prayerTotal = 0;
        for (int i = 0; i < COUNTER_COUNT; i++) {
            editor.putInt(KEY_COUNTER_PREFIX + i,
                    counts != null && i < counts.length
                            ? Math.max(0, Math.min(MAX_COUNTER_VALUE, counts[i])) : 0);
            if (i < 6) { // Первые 6 намазов
                prayerTotal += counts != null && i < counts.length ? counts[i] : 0;
            }
        }
        // Сохраняем начальный долг для расчёта процентов
        int fastTotal = counts != null && counts.length > 6 ? counts[6] : 0;
        editor.putInt(KEY_COUNTER_INITIAL_PRAYER, prayerTotal);
        editor.putInt(KEY_COUNTER_INITIAL_FAST, fastTotal);
        editor.apply();
        settleCounterProgressIfDone();
    }

    /**
     * Применить новый расчёт: остаток задаётся заново, а прогресс «выполнено»
     * обнуляется, чтобы карточка на главном экране начинала счёт с чистого листа.
     */
    public void applyCounterCounts(int[] counts) {
        setCounterCounts(counts);
        clearCounterProgress();
    }

    /** Обнулить только показатели выполненного, не трогая оставшийся долг. */
    public void clearCounterProgress() {
        SharedPreferences.Editor editor = prefs.edit();
        for (int i = 0; i < COUNTER_COUNT; i++) editor.putInt(KEY_COUNTER_DONE_PREFIX + i, 0);
        editor.apply();
    }

    /** Когда весь долг закрыт, прогресс «выполнено» начинается заново. */
    private void settleCounterProgressIfDone() {
        for (int count : getCounterCounts()) {
            if (count > 0) return;
        }
        clearCounterProgress();
    }

    public void resetCounterCounts() {
        SharedPreferences.Editor editor = prefs.edit();
        for (int i = 0; i < COUNTER_COUNT; i++) {
            editor.putInt(KEY_COUNTER_PREFIX + i, 0);
            editor.putInt(KEY_COUNTER_DONE_PREFIX + i, 0);
        }
        editor.apply();
    }

    public int totalCounterDebt() {
        int total = 0;
        for (int count : getCounterCounts()) total += count;
        return total;
    }

    /* ==================== Флаги дня ==================== */

    /** Возвращает массив из 6 состояний для дня. Если записи нет — все нули. */
    public int[] getFlags(String key) {
        int[] flags = new int[PRAYER_COUNT];
        String s = prefs.getString(KEY_DAY_PREFIX + key, null);
        if (s != null) {
            for (int i = 0; i < PRAYER_COUNT && i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '1') flags[i] = 1;
                else if (c == '2') flags[i] = 2;
            }
        }
        return flags;
    }

    private void saveFlags(String key, int[] flags) {
        StringBuilder sb = new StringBuilder(PRAYER_COUNT);
        for (int f : flags) sb.append(f);
        prefs.edit().putString(KEY_DAY_PREFIX + key, sb.toString()).apply();
    }

    /** Установить статус одного намаза: пропущен или выполнен. */
    public void setMissed(String key, int prayerIndex, boolean missed) {
        int[] flags = getFlags(key);
        flags[prayerIndex] = missed ? 1 : 2;
        saveFlags(key, flags);
    }

    /**
     * Установить произвольный статус намаза:
     * 0 — не отмечено, 1 — пропущен (каза), 2 — выполнен.
     */
    public void setStatus(String key, int prayerIndex, int status) {
        int[] flags = getFlags(key);
        flags[prayerIndex] = status;
        saveFlags(key, flags);
    }

    /** Отметить все намазы дня выполненными. */
    public void markAllDone(String key) {
        saveFlags(key, new int[]{2, 2, 2, 2, 2, 2});
    }

    /** Отметить все намазы дня пропущенными. */
    public void markAllMissed(String key) {
        saveFlags(key, new int[]{1, 1, 1, 1, 1, 1});
    }

    /** Очистить данные за день (снять все отметки и заметку). */
    public void clearDay(String key) {
        prefs.edit().remove(KEY_DAY_PREFIX + key).remove(KEY_NOTE_PREFIX + key).apply();
    }

    /* ==================== Подсчёты ==================== */

    public int countMissed(String key) {
        int c = 0;
        for (int v : getFlags(key)) if (v == 1) c++;
        return c;
    }

    public int countDone(String key) {
        int c = 0;
        for (int v : getFlags(key)) if (v == 2) c++;
        return c;
    }

    /** День считается «чистым» (без пропусков), если нет ни одного '1'. */
    public boolean isCleanDay(String key) {
        return countMissed(key) == 0;
    }

    /* ==================== Заметки ==================== */

    public String getNote(String key) {
        return prefs.getString(KEY_NOTE_PREFIX + key, "");
    }

    public void setNote(String key, String note) {
        prefs.edit().putString(KEY_NOTE_PREFIX + key, note).apply();
    }

    /* ==================== Все записи ==================== */

    /** Возвращает карту всех непустых заметок: «дата -> текст». */
    public Map<String, String> allNotes() {
        Map<String, String> map = new LinkedHashMap<>();
        for (Map.Entry<String, ?> e : prefs.getAll().entrySet()) {
            String k = e.getKey();
            if (k.startsWith(KEY_NOTE_PREFIX)) {
                String note = prefs.getString(k, "");
                if (!note.isEmpty()) map.put(k.substring(KEY_NOTE_PREFIX.length()), note);
            }
        }
        return map;
    }

    /** Возвращает карту «дата -> флаги» для всех дней, где есть отметки. */
    public Map<String, int[]> allRecords() {
        Map<String, int[]> map = new LinkedHashMap<>();
        for (Map.Entry<String, ?> e : prefs.getAll().entrySet()) {
            String k = e.getKey();
            if (k.startsWith(KEY_DAY_PREFIX)) {
                String date = k.substring(KEY_DAY_PREFIX.length());
                map.put(date, getFlags(date));
            }
        }
        return map;
    }

    public int totalMissed() {
        int t = 0;
        for (int[] f : allRecords().values()) for (int v : f) if (v == 1) t++;
        return t;
    }

    public int totalDone() {
        int t = 0;
        for (int[] f : allRecords().values()) for (int v : f) if (v == 2) t++;
        return t;
    }

    public int missedForPrayer(int prayerIndex) {
        int t = 0;
        for (int[] f : allRecords().values()) if (f[prayerIndex] == 1) t++;
        return t;
    }

    public int doneForPrayer(int prayerIndex) {
        int t = 0;
        for (int[] f : allRecords().values()) if (f[prayerIndex] == 2) t++;
        return t;
    }

    public int totalMissedInMonth(int year, int month) {
        int t = 0;
        for (Map.Entry<String, int[]> e : allRecords().entrySet()) {
            String[] p = e.getKey().split("-");
            if (p.length == 3
                    && Integer.parseInt(p[0]) == year
                    && Integer.parseInt(p[1]) == month + 1) {
                for (int v : e.getValue()) if (v == 1) t++;
            }
        }
        return t;
    }

    public int totalDoneInMonth(int year, int month) {
        int t = 0;
        for (Map.Entry<String, int[]> e : allRecords().entrySet()) {
            String[] p = e.getKey().split("-");
            if (p.length == 3
                    && Integer.parseInt(p[0]) == year
                    && Integer.parseInt(p[1]) == month + 1) {
                for (int v : e.getValue()) if (v == 2) t++;
            }
        }
        return t;
    }

    /** Самая ранняя дата, для которой есть отметки, либо null. */
    private String earliestRecordDate() {
        String first = null;
        for (String date : allRecords().keySet()) {
            if (first == null || date.compareTo(first) < 0) first = date;
        }
        return first;
    }

    /** Текущая серия дней подряд без пропущенных намазов. */
    public int currentStreak() {
        String first = earliestRecordDate();
        if (first == null) return 0;
        Calendar c = Calendar.getInstance();
        if (countMissed(dateKey(c)) > 0) c.add(Calendar.DAY_OF_MONTH, -1);
        int streak = 0;
        while (true) {
            String k = dateKey(c);
            if (k.compareTo(first) < 0) break;
            if (countMissed(k) > 0) break;
            streak++;
            c.add(Calendar.DAY_OF_MONTH, -1);
        }
        return streak;
    }

    /** Рекордная серия дней подряд без пропусков. */
    public int bestStreak() {
        String first = earliestRecordDate();
        if (first == null) return 0;
        List<String> dates = new ArrayList<>(allRecords().keySet());
        Calendar start = Calendar.getInstance();
        try {
            start.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(first));
        } catch (Exception ignored) {
            return 0;
        }
        Calendar end = Calendar.getInstance();
        int best = 0, cur = 0;
        Calendar c = (Calendar) start.clone();
        while (!c.after(end)) {
            String k = dateKey(c);
            if (countMissed(k) > 0) {
                cur = 0;
            } else {
                cur++;
                if (cur > best) best = cur;
            }
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return best;
    }

    /** Количество дней, по которым есть хотя бы одна отметка. */
    public int daysWithRecords() {
        return allRecords().size();
    }

    /* ==================== Импорт / сброс ==================== */

    /** Восстановить полную резервную копию обоих режимов и настроек. */
    public void restoreBackup(Map<String, int[]> records, Map<String, String> notes,
                              int[] counterCounts, String themeMode, String appMode,
                              boolean settingsIncluded) {
        restoreBackup(records, notes, counterCounts, null, 0, 0, themeMode, appMode, "", settingsIncluded);
    }

    public void restoreBackup(Map<String, int[]> records, Map<String, String> notes,
                              int[] counterCounts, String themeMode, String appMode,
                              String calculatorProfile, boolean settingsIncluded) {
        restoreBackup(records, notes, counterCounts, null, 0, 0, themeMode, appMode,
                calculatorProfile, settingsIncluded);
    }

    public void restoreBackup(Map<String, int[]> records, Map<String, String> notes,
                              int[] counterCounts, int[] counterDoneCounts,
                              String themeMode, String appMode, String calculatorProfile,
                              boolean settingsIncluded) {
        restoreBackup(records, notes, counterCounts, counterDoneCounts, 0, 0, themeMode, appMode,
                calculatorProfile, settingsIncluded);
    }

    public void restoreBackup(Map<String, int[]> records, Map<String, String> notes,
                              int[] counterCounts, int[] counterDoneCounts,
                              int counterInitialPrayer, int counterInitialFast,
                              String themeMode, String appMode, String calculatorProfile,
                              boolean settingsIncluded) {
        SharedPreferences.Editor editor = prefs.edit();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(KEY_DAY_PREFIX) || key.startsWith(KEY_NOTE_PREFIX)
                    || (settingsIncluded && key.startsWith(KEY_COUNTER_PREFIX))) {
                editor.remove(key);
            }
        }
        for (Map.Entry<String, int[]> entry : records.entrySet()) {
            StringBuilder flags = new StringBuilder(PRAYER_COUNT);
            int[] values = entry.getValue();
            for (int i = 0; i < PRAYER_COUNT; i++) {
                int value = values != null && i < values.length ? values[i] : 0;
                flags.append(value == 1 || value == 2 ? value : 0);
            }
            editor.putString(KEY_DAY_PREFIX + entry.getKey(), flags.toString());
        }
        for (Map.Entry<String, String> entry : notes.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                editor.putString(KEY_NOTE_PREFIX + entry.getKey(), entry.getValue());
            }
        }
        if (settingsIncluded) {
            for (int i = 0; i < COUNTER_COUNT; i++) {
                int value = counterCounts != null && i < counterCounts.length ? counterCounts[i] : 0;
                editor.putInt(KEY_COUNTER_PREFIX + i,
                        Math.max(0, Math.min(MAX_COUNTER_VALUE, value)));
                int done = counterDoneCounts != null && i < counterDoneCounts.length
                        ? counterDoneCounts[i] : 0;
                editor.putInt(KEY_COUNTER_DONE_PREFIX + i,
                        Math.max(0, Math.min(MAX_COUNTER_VALUE, done)));
            }
            editor.putInt(KEY_COUNTER_INITIAL_PRAYER, counterInitialPrayer);
            editor.putInt(KEY_COUNTER_INITIAL_FAST, counterInitialFast);
            editor.putString(KEY_THEME, THEME_LIGHT.equals(themeMode) || THEME_DARK.equals(themeMode)
                    ? themeMode : THEME_AUTO);
            editor.putString(KEY_APP_MODE, MODE_COUNTER.equals(appMode) ? MODE_COUNTER : MODE_EXTENDED);
            if (calculatorProfile == null || calculatorProfile.isEmpty()) {
                editor.remove(KEY_CALCULATOR_PROFILE);
            } else {
                editor.putString(KEY_CALCULATOR_PROFILE, calculatorProfile);
            }
        }
        editor.apply();
    }

    /** Полная замена данных расширенного режима (обратная совместимость API). */
    public void replaceAll(Map<String, int[]> records, Map<String, String> notes) {
        SharedPreferences.Editor e = prefs.edit();
        // Удаляем все старые ключи дней и заметок
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            String k = entry.getKey();
            if (k.startsWith(KEY_DAY_PREFIX) || k.startsWith(KEY_NOTE_PREFIX)) {
                e.remove(k);
            }
        }
        for (Map.Entry<String, int[]> entry : records.entrySet()) {
            StringBuilder sb = new StringBuilder(PRAYER_COUNT);
            for (int v : entry.getValue()) sb.append(v);
            e.putString(KEY_DAY_PREFIX + entry.getKey(), sb.toString());
        }
        for (Map.Entry<String, String> entry : notes.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                e.putString(KEY_NOTE_PREFIX + entry.getKey(), entry.getValue());
            }
        }
        e.apply();
    }

    /** Полный сброс всех данных приложения. */
    public void clearAll() {
        SharedPreferences.Editor e = prefs.edit();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            String k = entry.getKey();
            if (k.startsWith(KEY_DAY_PREFIX) || k.startsWith(KEY_NOTE_PREFIX)
                    || k.startsWith(KEY_COUNTER_PREFIX) || KEY_CALCULATOR_PROFILE.equals(k)) {
                e.remove(k);
            }
        }
        e.apply();
    }
}
