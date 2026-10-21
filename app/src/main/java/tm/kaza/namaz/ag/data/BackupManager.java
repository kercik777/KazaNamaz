package tm.kaza.namaz.ag.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Экспорт и импорт резервной копии в формате JSON.
 * Используется только встроенный org.json (часть Android SDK).
 */
public class BackupManager {

    public static final String APP_TAG = "KazaNamaz";
    public static final int FORMAT_VERSION = 1;

    /** Результат разбора файла резервной копии. */
    public static class ImportResult {
        public boolean valid;
        public int count;
        public String error;
        public final Map<String, int[]> records = new LinkedHashMap<>();
        public final Map<String, String> notes = new LinkedHashMap<>();
    }

    /** Собрать JSON-строку из всех данных. */
    public static String exportJson(DataStore ds) {
        try {
            JSONObject root = new JSONObject();
            root.put("app", APP_TAG);
            root.put("formatVersion", FORMAT_VERSION);
            root.put("exportedAt", new Date().toString());

            JSONArray arr = new JSONArray();
            for (Map.Entry<String, int[]> e : ds.allRecords().entrySet()) {
                JSONObject o = new JSONObject();
                o.put("date", e.getKey());
                StringBuilder sb = new StringBuilder();
                for (int v : e.getValue()) sb.append(v);
                o.put("flags", sb.toString());
                String note = ds.getNote(e.getKey());
                if (note != null && !note.isEmpty()) {
                    o.put("note", note);
                }
                arr.put(o);
            }
            root.put("records", arr);
            return root.toString(2);
        } catch (Exception e) {
            return null;
        }
    }

    /** Разобрать JSON-строку резервной копии. */
    public static ImportResult parseImport(String json) {
        ImportResult res = new ImportResult();
        try {
            JSONObject root = new JSONObject(json);
            String app = root.optString("app", "");
            if (!APP_TAG.equals(app)) {
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
                if (!note.isEmpty()) {
                    res.notes.put(date, note);
                }
            }
            res.count = res.records.size();
            res.valid = true;
        } catch (Exception e) {
            res.error = "parse_error";
        }
        return res;
    }
}
