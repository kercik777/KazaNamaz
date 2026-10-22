package tm.kaza.namaz.ag.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;

/**
 * JSON-резервная копия всего приложения. Формат v4 хранит данные обоих
 * режимов, прогресс счётчиков и настройки; старые форматы также принимаются.
 */
public class BackupManager {

    public static final String APP_TAG = "KazaNamaz";
    public static final int FORMAT_VERSION = 4;

    public static class ImportResult {
        public boolean valid;
        public boolean settingsIncluded;
        public int count;
        public String error;
        public String themeMode = DataStore.THEME_AUTO;
        public String appMode = DataStore.MODE_EXTENDED;
        public String calculatorProfile = "";
        public boolean counterVibrationEnabled = true;
        public int[] counterCounts = new int[DataStore.COUNTER_COUNT];
        public int[] counterDoneCounts = new int[DataStore.COUNTER_COUNT];
        public final Map<String, int[]> records = new LinkedHashMap<>();
        public final Map<String, String> notes = new LinkedHashMap<>();
    }

    /** Собрать JSON из расширенных записей, счётчиков и настроек приложения. */
    public static String exportJson(DataStore ds) {
        try {
            JSONObject root = new JSONObject();
            root.put("app", APP_TAG);
            root.put("formatVersion", FORMAT_VERSION);
            root.put("exportedAt", new Date().toString());

            Map<String, int[]> records = ds.allRecords();
            Map<String, String> notes = ds.allNotes();
            Set<String> dates = new LinkedHashSet<>();
            dates.addAll(records.keySet());
            dates.addAll(notes.keySet());
            JSONArray arr = new JSONArray();
            for (String date : dates) {
                JSONObject o = new JSONObject();
                o.put("date", date);
                int[] flags = records.get(date);
                StringBuilder sb = new StringBuilder(DataStore.PRAYER_COUNT);
                for (int i = 0; i < DataStore.PRAYER_COUNT; i++) {
                    sb.append(flags != null && i < flags.length ? flags[i] : 0);
                }
                o.put("flags", sb.toString());
                String note = notes.get(date);
                if (note != null && !note.isEmpty()) o.put("note", note);
                arr.put(o);
            }
            root.put("records", arr);

            JSONArray counters = new JSONArray();
            for (int count : ds.getCounterCounts()) counters.put(count);
            root.put("counterCounts", counters);
            JSONArray completedCounters = new JSONArray();
            for (int count : ds.getCounterDoneCounts()) completedCounters.put(count);
            root.put("counterDoneCounts", completedCounters);

            JSONObject settings = new JSONObject();
            settings.put("themeMode", ds.getThemeMode());
            settings.put("appMode", ds.getAppMode());
            settings.put("calculatorProfile", ds.getCalculatorProfile());
            settings.put("counterVibrationEnabled", ds.isCounterVibrationEnabled());
            root.put("settings", settings);
            return root.toString(2);
        } catch (Exception e) {
            return null;
        }
    }

    /** Разобрать v1 или v2 резервную копию. */
    public static ImportResult parseImport(String json) {
        ImportResult res = new ImportResult();
        try {
            JSONObject root = new JSONObject(json);
            if (!APP_TAG.equals(root.optString("app", ""))) {
                res.error = "file_not_backup";
                return res;
            }
            JSONArray arr = root.optJSONArray("records");
            if (arr == null) {
                res.error = "no_records";
                return res;
            }
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String date = o.getString("date");
                String flags = o.optString("flags", "");
                int[] f = new int[DataStore.PRAYER_COUNT];
                for (int j = 0; j < DataStore.PRAYER_COUNT && j < flags.length(); j++) {
                    char c = flags.charAt(j);
                    if (c == '1') f[j] = 1;
                    else if (c == '2') f[j] = 2;
                }
                res.records.put(date, f);
                String note = o.optString("note", "");
                if (!note.isEmpty()) res.notes.put(date, note);
            }

            int version = root.optInt("formatVersion", 1);
            if (version >= 2) {
                JSONArray counters = root.optJSONArray("counterCounts");
                if (counters != null) {
                    for (int i = 0; i < Math.min(counters.length(), DataStore.COUNTER_COUNT); i++) {
                        res.counterCounts[i] = Math.max(0, Math.min(DataStore.MAX_COUNTER_VALUE,
                                counters.optInt(i, 0)));
                    }
                }
                JSONArray completedCounters = root.optJSONArray("counterDoneCounts");
                if (completedCounters != null) {
                    for (int i = 0; i < Math.min(completedCounters.length(), DataStore.COUNTER_COUNT); i++) {
                        res.counterDoneCounts[i] = Math.max(0, Math.min(DataStore.MAX_COUNTER_VALUE,
                                completedCounters.optInt(i, 0)));
                    }
                }
                JSONObject settings = root.optJSONObject("settings");
                if (settings != null) {
                    res.themeMode = settings.optString("themeMode", DataStore.THEME_AUTO);
                    res.appMode = settings.optString("appMode", DataStore.MODE_EXTENDED);
                    res.calculatorProfile = settings.optString("calculatorProfile", "");
                    res.counterVibrationEnabled = settings.optBoolean("counterVibrationEnabled", true);
                }
                res.settingsIncluded = true;
            }
            res.count = res.records.size();
            res.valid = true;
        } catch (Exception e) {
            res.error = "parse_error";
        }
        return res;
    }
}
