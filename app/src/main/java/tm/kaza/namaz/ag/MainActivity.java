package tm.kaza.namaz.ag;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import tm.kaza.namaz.ag.data.BackupManager;
import tm.kaza.namaz.ag.data.DataStore;
import tm.kaza.namaz.ag.data.PrayerType;
import tm.kaza.namaz.ag.ui.CalendarAdapter;
import tm.kaza.namaz.ag.ui.DayDetailSheet;
import tm.kaza.namaz.ag.ui.HistoryAdapter;
import tm.kaza.namaz.ag.ui.PrayerRowFactory;
import tm.kaza.namaz.ag.ui.ProgressRingView;
import tm.kaza.namaz.ag.util.DateUtils;
import tm.kaza.namaz.ag.util.SystemBarsHelper;
import tm.kaza.namaz.ag.util.ThemeUtil;

public class MainActivity extends AppCompatActivity {

    private static final int SCREEN_MAIN = 0;
    private static final int SCREEN_CALENDAR = 1;
    private static final int SCREEN_STATS = 2;
    private static final int SCREEN_SETTINGS = 3;

    private DataStore ds;

    /* Главный экран */
    private ImageButton btnPrevDay, btnNextDay;
    private TextView tvDateTitle;
    private TextView btnToday;
    private ProgressRingView ringView;
    private TextView tvProgressTitle, tvProgressSub;
    private ProgressBar pbDay;
    private TextView btnAllDone, btnAllMissed, btnClearDay;
    private LinearLayout prayerContainer;
    private EditText etNote;
    private TextView btnSaveNote;
    private TextView tvHistoryEmpty;
    private RecyclerView rvHistory;
    private HistoryAdapter historyAdapter;

    /* Календарь */
    private ImageButton btnPrevMonth, btnNextMonth;
    private TextView tvMonthTitle, btnCalendarToday, tvMonthSummary;
    private RecyclerView rvCalendar;
    private CalendarAdapter calendarAdapter;

    /* Статистика */
    private TextView tvTotalMissed, tvTotalDone, tvDaysTracked, tvBestStreak, tvCurrentStreak;
    private LinearLayout statsPrayerContainer, statsMonthContainer;

    /* Настройки */
    private MaterialButtonToggleGroup themeToggle;
    private TextView tvVersion;

    /* Прочее */
    private TextView tvHeaderDebt;
    private BottomNavigationView bottomNav;

    private final Calendar selectedDate = Calendar.getInstance();
    private Calendar calSelected = null;
    private int calYear, calMonth;

    private ActivityResultLauncher<String> exportLauncher;
    private ActivityResultLauncher<String[]> importLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtil.apply(this);
        super.onCreate(savedInstanceState);
        SystemBarsHelper.edgeToEdge(this);
        setContentView(R.layout.activity_main);

        ds = DataStore.get(this);
        bindViews();
        SystemBarsHelper.padTop(findViewById(R.id.headerContent));
        setupLaunchers();
        setupNavigation();
        setupListeners();

        Calendar today = Calendar.getInstance();
        calYear = today.get(Calendar.YEAR);
        calMonth = today.get(Calendar.MONTH);

        bottomNav.setSelectedItemId(R.id.nav_main);
        refreshAll();
    }

    /* ==================== Инициализация ==================== */

    private void bindViews() {
        tvHeaderDebt = findViewById(R.id.tvHeaderDebt);
        bottomNav = findViewById(R.id.bottomNav);

        btnPrevDay = findViewById(R.id.btnPrevDay);
        btnNextDay = findViewById(R.id.btnNextDay);
        tvDateTitle = findViewById(R.id.tvDateTitle);
        btnToday = findViewById(R.id.btnToday);
        ringView = findViewById(R.id.ringView);
        tvProgressTitle = findViewById(R.id.tvProgressTitle);
        tvProgressSub = findViewById(R.id.tvProgressSub);
        pbDay = findViewById(R.id.pbDay);
        btnAllDone = findViewById(R.id.btnAllDone);
        btnAllMissed = findViewById(R.id.btnAllMissed);
        btnClearDay = findViewById(R.id.btnClearDay);
        prayerContainer = findViewById(R.id.prayerContainer);
        etNote = findViewById(R.id.etNote);
        btnSaveNote = findViewById(R.id.btnSaveNote);
        tvHistoryEmpty = findViewById(R.id.tvHistoryEmpty);
        rvHistory = findViewById(R.id.rvHistory);

        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        tvMonthTitle = findViewById(R.id.tvMonthTitle);
        btnCalendarToday = findViewById(R.id.btnCalendarToday);
        tvMonthSummary = findViewById(R.id.tvMonthSummary);
        rvCalendar = findViewById(R.id.rvCalendar);

        tvTotalMissed = findViewById(R.id.tvTotalMissed);
        tvTotalDone = findViewById(R.id.tvTotalDone);
        tvDaysTracked = findViewById(R.id.tvDaysTracked);
        tvBestStreak = findViewById(R.id.tvBestStreak);
        tvCurrentStreak = findViewById(R.id.tvCurrentStreak);
        statsPrayerContainer = findViewById(R.id.statsPrayerContainer);
        statsMonthContainer = findViewById(R.id.statsMonthContainer);

        themeToggle = findViewById(R.id.themeToggle);
        tvVersion = findViewById(R.id.tvVersion);
    }

    private void setupLaunchers() {
        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/json"),
                uri -> {
                    if (uri != null) writeExport(uri);
                });

        importLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) handleImportUri(uri);
                });
    }

    private void setupNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_main) {
                switchTo(SCREEN_MAIN);
            } else if (id == R.id.nav_calendar) {
                switchTo(SCREEN_CALENDAR);
                renderCalendar();
            } else if (id == R.id.nav_stats) {
                switchTo(SCREEN_STATS);
                renderStats();
            } else if (id == R.id.nav_settings) {
                switchTo(SCREEN_SETTINGS);
                renderSettings();
            }
            return true;
        });

        historyAdapter = new HistoryAdapter(item ->
                DayDetailSheet.show(this, ds, DateUtils.parseKey(item.key), this::refreshAll));
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(historyAdapter);
        rvHistory.setNestedScrollingEnabled(false);

        calendarAdapter = new CalendarAdapter(date -> {
            calSelected = (Calendar) date.clone();
            renderCalendar();
            DayDetailSheet.show(this, ds, date, this::refreshAll);
        });
        rvCalendar.setLayoutManager(new GridLayoutManager(this, 7));
        rvCalendar.setAdapter(calendarAdapter);
        rvCalendar.setNestedScrollingEnabled(false);
        rvCalendar.setItemViewCacheSize(42);
    }

    private void setupListeners() {
        btnPrevDay.setOnClickListener(v -> {
            selectedDate.add(Calendar.DAY_OF_MONTH, -1);
            haptic(v);
            renderMain();
        });
        btnNextDay.setOnClickListener(v -> {
            selectedDate.add(Calendar.DAY_OF_MONTH, 1);
            haptic(v);
            renderMain();
        });
        btnToday.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            selectedDate.setTimeInMillis(now.getTimeInMillis());
            haptic(v);
            renderMain();
        });

        btnAllDone.setOnClickListener(v -> {
            ds.markAllDone(DataStore.dateKey(selectedDate));
            haptic(v);
            refreshAll();
        });
        btnAllMissed.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.all_missed_confirm_title)
                .setMessage(R.string.all_missed_confirm_msg)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_yes, (d, w) -> {
                    ds.markAllMissed(DataStore.dateKey(selectedDate));
                    refreshAll();
                })
                .show());
        btnClearDay.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.clear_day_confirm_title)
                .setMessage(R.string.clear_day_confirm_msg)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_yes, (d, w) -> {
                    ds.clearDay(DataStore.dateKey(selectedDate));
                    etNote.setText("");
                    refreshAll();
                })
                .show());

        btnSaveNote.setOnClickListener(v -> {
            ds.setNote(DataStore.dateKey(selectedDate), etNote.getText().toString());
            haptic(v);
            Toast.makeText(this, R.string.note_saved, Toast.LENGTH_SHORT).show();
        });

        btnPrevMonth.setOnClickListener(v -> {
            moveMonth(-1);
            haptic(v);
        });
        btnNextMonth.setOnClickListener(v -> {
            moveMonth(1);
            haptic(v);
        });
        btnCalendarToday.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            calYear = now.get(Calendar.YEAR);
            calMonth = now.get(Calendar.MONTH);
            haptic(v);
            renderCalendar();
        });

        themeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            String mode;
            if (checkedId == R.id.btnThemeLight) mode = DataStore.THEME_LIGHT;
            else if (checkedId == R.id.btnThemeDark) mode = DataStore.THEME_DARK;
            else mode = DataStore.THEME_AUTO;
            if (!mode.equals(ds.getThemeMode())) {
                ds.setThemeMode(mode);
                ThemeUtil.apply(this);
                recreate();
            }
        });

        findViewById(R.id.rowExport).setOnClickListener(v -> {
            haptic(v);
            exportLauncher.launch(DateUtils.backupFileName());
        });
        findViewById(R.id.rowImport).setOnClickListener(v -> {
            haptic(v);
            importLauncher.launch(new String[]{"application/json", "application/octet-stream", "*/*"});
        });
        findViewById(R.id.rowShare).setOnClickListener(v -> {
            haptic(v);
            shareBackup();
        });
        findViewById(R.id.rowReset).setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.reset_confirm_title)
                .setMessage(R.string.reset_confirm_msg)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_reset, (d, w) -> {
                    ds.clearAll();
                    etNote.setText("");
                    refreshAll();
                    Toast.makeText(this, R.string.reset_done, Toast.LENGTH_SHORT).show();
                })
                .show());
    }

    /* ==================== Навигация ==================== */

    private void switchTo(int which) {
        int[] ids = {R.id.screen_main, R.id.screen_calendar, R.id.screen_stats, R.id.screen_settings};
        for (int i = 0; i < ids.length; i++) {
            View v = findViewById(ids[i]);
            if (v == null) continue;
            if (i == which) {
                v.setVisibility(View.VISIBLE);
                v.setAlpha(0f);
                v.animate().alpha(1f).setDuration(180).start();
            } else {
                v.setVisibility(View.GONE);
            }
        }
    }

    /* ==================== Отрисовка ==================== */

    private void refreshAll() {
        refreshHeader();
        renderMain();
        renderCalendar();
        renderStats();
    }

    private void refreshHeader() {
        tvHeaderDebt.setText(String.valueOf(ds.totalMissed()));
    }

    private void renderMain() {
        String key = DataStore.dateKey(selectedDate);
        Calendar today = Calendar.getInstance();
        boolean isToday = DataStore.dateKey(today).equals(key);

        String label = DateUtils.relativeLabel(this, selectedDate);
        if (label != null) {
            tvDateTitle.setText(label + " · " + DateUtils.formatFull(this, selectedDate));
        } else {
            tvDateTitle.setText(DateUtils.formatFull(this, selectedDate));
        }
        // Кнопка «Сегодня» показывается только когда выбран не сегодняшний день
        btnToday.setVisibility(isToday ? View.GONE : View.VISIBLE);

        // Строки намазов
        prayerContainer.removeAllViews();
        int[] flags = ds.getFlags(key);
        for (PrayerType p : PrayerType.values()) {
            prayerContainer.addView(PrayerRowFactory.build(this, prayerContainer, p, flags,
                    (prayerIndex, newState) -> {
                        ds.setStatus(key, prayerIndex, newState);
                        prayerContainer.post(this::refreshAll);
                    }));
        }

        // Прогресс: считаются только явные отметки пользователя
        int missed = ds.countMissed(key);
        int done = ds.countDone(key);
        int unmarked = DataStore.PRAYER_COUNT - done - missed;
        ringView.setProgress(done, DataStore.PRAYER_COUNT);
        pbDay.setMax(DataStore.PRAYER_COUNT);
        pbDay.setProgress(done);
        tvProgressTitle.setText(R.string.progress_title);
        tvProgressSub.setText(getString(R.string.progress_sub, missed, unmarked));

        // Заметка
        if (!etNote.hasFocus()) {
            etNote.setText(ds.getNote(key));
        }

        // История
        List<HistoryAdapter.Item> items = new ArrayList<>();
        Calendar c = Calendar.getInstance();
        for (int i = 0; i < 60; i++) {
            String k = DataStore.dateKey(c);
            int m = ds.countMissed(k);
            if (m > 0) {
                HistoryAdapter.Item it = new HistoryAdapter.Item();
                it.key = k;
                it.title = DateUtils.relativeDay(this, k);
                it.missed = m;
                it.subtitle = missedPrayerNames(k);
                items.add(it);
            }
            c.add(Calendar.DAY_OF_MONTH, -1);
        }
        historyAdapter.setItems(items);
        tvHistoryEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void renderCalendar() {
        tvMonthTitle.setText(DateUtils.formatMonthYear(this, calYear, calMonth));

        // Скрываем кнопку «Сегодня» если уже на текущем месяце
        Calendar now = Calendar.getInstance();
        boolean isCurrentMonth = calYear == now.get(Calendar.YEAR)
                && calMonth == now.get(Calendar.MONTH);
        btnCalendarToday.setVisibility(isCurrentMonth ? View.GONE : View.VISIBLE);

        List<CalendarAdapter.Day> cells = new ArrayList<>(42);
        Calendar first = Calendar.getInstance();
        first.set(calYear, calMonth, 1);
        int offset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7; // понедельник — первый
        Calendar cursor = (Calendar) first.clone();
        cursor.add(Calendar.DAY_OF_MONTH, -offset);
        Calendar today = Calendar.getInstance();

        for (int i = 0; i < 42; i++) {
            CalendarAdapter.Day d = new CalendarAdapter.Day();
            d.date = (Calendar) cursor.clone();
            d.inMonth = d.date.get(Calendar.MONTH) == calMonth
                    && d.date.get(Calendar.YEAR) == calYear;
            String k = DataStore.dateKey(d.date);
            d.missed = ds.countMissed(k);
            d.done = ds.countDone(k);
            d.isToday = d.date.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                    && d.date.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
            cells.add(d);
            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }
        calendarAdapter.setData(cells, calSelected);

        int missedM = ds.totalMissedInMonth(calYear, calMonth);
        int doneM = ds.totalDoneInMonth(calYear, calMonth);
        tvMonthSummary.setText(getString(R.string.calendar_month_summary, missedM, doneM));
    }

    private void renderStats() {
        int missed = ds.totalMissed();
        int done = ds.totalDone();
        int days = ds.daysWithRecords();

        tvTotalMissed.setText(String.valueOf(missed));
        tvTotalDone.setText(String.valueOf(done));
        tvDaysTracked.setText(String.valueOf(days));
        tvBestStreak.setText(String.valueOf(ds.bestStreak()));
        int streak = ds.currentStreak();
        tvCurrentStreak.setText(getString(R.string.stat_current_streak,
                DateUtils.pluralDays(this, streak)));

        // По намазам
        statsPrayerContainer.removeAllViews();
        for (PrayerType p : PrayerType.values()) {
            View row = getLayoutInflater().inflate(R.layout.item_stat_prayer, statsPrayerContainer, false);
            TextView name = row.findViewById(R.id.tvStatName);
            TextView counts = row.findViewById(R.id.tvStatCounts);
            ProgressBar bar = row.findViewById(R.id.pbStat);

            int pm = ds.missedForPrayer(p.getIndex());
            int pd = ds.doneForPrayer(p.getIndex());
            int total = pm + pd;

            name.setText(p.getName(this));
            if (total == 0) {
                counts.setText(R.string.stat_no_data);
                bar.setProgress(0);
            } else {
                counts.setText(getString(R.string.stat_missed_done, pm, pd));
                bar.setMax(100);
                bar.setProgress((int) Math.round(100f * pm / Math.max(total, 1)));
            }
            statsPrayerContainer.addView(row);
        }

        // По месяцам (последние 6)
        statsMonthContainer.removeAllViews();
        Calendar c = Calendar.getInstance();
        for (int i = 5; i >= 0; i--) {
            Calendar m = (Calendar) c.clone();
            m.add(Calendar.MONTH, -i);
            int mm = ds.totalMissedInMonth(m.get(Calendar.YEAR), m.get(Calendar.MONTH));
            int md = ds.totalDoneInMonth(m.get(Calendar.YEAR), m.get(Calendar.MONTH));
            int total = mm + md;

            View row = getLayoutInflater().inflate(R.layout.item_stat_prayer, statsMonthContainer, false);
            TextView name = row.findViewById(R.id.tvStatName);
            TextView counts = row.findViewById(R.id.tvStatCounts);
            ProgressBar bar = row.findViewById(R.id.pbStat);

            String label = DateUtils.monthShort(this, m.get(Calendar.MONTH));
            if (m.get(Calendar.YEAR) != c.get(Calendar.YEAR)) {
                label += " " + (m.get(Calendar.YEAR) % 100);
            }
            name.setText(label);
            if (total == 0) {
                counts.setText(R.string.stat_no_data);
                bar.setProgress(0);
            } else {
                counts.setText(getString(R.string.stat_missed_done, mm, md));
                bar.setMax(100);
                bar.setProgress((int) Math.round(100f * mm / Math.max(total, 1)));
            }
            statsMonthContainer.addView(row);
        }
    }

    private void renderSettings() {
        String mode = ds.getThemeMode();
        int checked;
        if (DataStore.THEME_LIGHT.equals(mode)) checked = R.id.btnThemeLight;
        else if (DataStore.THEME_DARK.equals(mode)) checked = R.id.btnThemeDark;
        else checked = R.id.btnThemeAuto;
        themeToggle.check(checked);

        try {
            tvVersion.setText(getString(R.string.about_version,
                    getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
        } catch (Exception e) {
            tvVersion.setText(getString(R.string.about_version, "1.0.0"));
        }
    }

    /* ==================== Вспомогательное ==================== */

    private void moveMonth(int delta) {
        calMonth += delta;
        if (calMonth < 0) {
            calMonth = 11;
            calYear--;
        } else if (calMonth > 11) {
            calMonth = 0;
            calYear++;
        }
        renderCalendar();
    }

    private String missedPrayerNames(String key) {
        int[] flags = ds.getFlags(key);
        StringBuilder sb = new StringBuilder();
        for (PrayerType p : PrayerType.values()) {
            if (flags[p.getIndex()] == 1) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(p.getName(this));
            }
        }
        return sb.toString();
    }

    private void haptic(View v) {
        v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
    }

    /* ==================== Резервное копирование ==================== */

    private void writeExport(Uri uri) {
        String json = BackupManager.exportJson(ds);
        if (json == null) {
            Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
            if (os == null) throw new IOException("null stream");
            os.write(json.getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this, R.string.export_saved, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void handleImportUri(Uri uri) {
        String text = readText(uri);
        if (text == null || text.trim().isEmpty()) {
            showError(R.string.import_failed_read);
            return;
        }
        BackupManager.ImportResult res = BackupManager.parseImport(text);
        if (!res.valid) {
            showError(R.string.import_invalid);
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.import_confirm_title)
                .setMessage(getString(R.string.import_confirm_msg,
                        DateUtils.pluralRecords(this, res.count)))
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_import, (d, w) -> {
                    ds.replaceAll(res.records, res.notes);
                    refreshAll();
                    Toast.makeText(this, getString(R.string.import_done,
                            DateUtils.pluralRecords(this, res.count)),
                            Toast.LENGTH_LONG).show();
                })
                .show();
    }

    private String readText(Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            return bos.toString("UTF-8");
        } catch (IOException e) {
            return null;
        }
    }

    private void shareBackup() {
        String json = BackupManager.exportJson(ds);
        if (json == null) {
            Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, json);
        startActivity(Intent.createChooser(send, getString(R.string.share_chooser)));
    }

    private void showError(int msgRes) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_error)
                .setMessage(msgRes)
                .setPositiveButton(R.string.ok, null)
                .show();
    }
}
