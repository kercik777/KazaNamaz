package tm.kaza.namaz.ag;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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
    private static final int SCREEN_COUNTER = 2;
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

    /* Счётчик */
    private TextView tvPrayerRemaining, tvPrayerDoneTotal, tvPrayerPercent;
    private TextView tvFastRemaining, tvFastDoneTotal, tvFastPercent;
    private ProgressBar prayerProgress, fastProgress;
    private LinearLayout counterPrayerContainer;
    private LinearLayout counterOtherContainer;
    private View allDoneRow;
    private boolean debtJustSettled;

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

        /* Главный экран */
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

        /* Календарь */
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        tvMonthTitle = findViewById(R.id.tvMonthTitle);
        btnCalendarToday = findViewById(R.id.btnCalendarToday);
        tvMonthSummary = findViewById(R.id.tvMonthSummary);
        rvCalendar = findViewById(R.id.rvCalendar);

        /* Счётчик */
        tvPrayerRemaining = findViewById(R.id.tvPrayerRemaining);
        tvPrayerDoneTotal = findViewById(R.id.tvPrayerDoneTotal);
        tvPrayerPercent = findViewById(R.id.tvPrayerPercent);
        prayerProgress = findViewById(R.id.pbPrayerDebtProgress);
        tvFastRemaining = findViewById(R.id.tvFastRemaining);
        tvFastDoneTotal = findViewById(R.id.tvFastDoneTotal);
        tvFastPercent = findViewById(R.id.tvFastPercent);
        fastProgress = findViewById(R.id.pbFastDebtProgress);
        counterPrayerContainer = findViewById(R.id.counterPrayerContainer);
        counterOtherContainer = findViewById(R.id.counterOtherContainer);
        allDoneRow = findViewById(R.id.rowCounterAllDone);

        /* Настройки */
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
            } else if (id == R.id.nav_counter) {
                switchTo(SCREEN_COUNTER);
                renderCounter();
                showCounterHintIfNeeded();
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
        /* Главный экран — навигация по дням */
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

        /* Быстрые действия */
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

        /* Заметка */
        btnSaveNote.setOnClickListener(v -> {
            ds.setNote(DataStore.dateKey(selectedDate), etNote.getText().toString());
            haptic(v);
            Toast.makeText(this, R.string.note_saved, Toast.LENGTH_SHORT).show();
        });

        /* Календарь */
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

        /* Счётчик */
        findViewById(R.id.btnOpenCalculator).setOnClickListener(v ->
                startActivity(new Intent(this, CalculatorActivity.class)));
        findViewById(R.id.btnResetCounters).setOnClickListener(v -> confirmResetCounters());

        /* Тема */
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

        /* Настройки — редактирование расчёта */
        findViewById(R.id.rowEditCalculatorProfile).setOnClickListener(v ->
                startActivity(new Intent(this, CalculatorProfileEditorActivity.class)));

        /* Резервное копирование */
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
        int[] ids = {R.id.screen_main, R.id.screen_calendar, R.id.screen_counter, R.id.screen_settings};
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
        renderCounter();
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

        // Прогресс
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

        Calendar now = Calendar.getInstance();
        boolean isCurrentMonth = calYear == now.get(Calendar.YEAR)
                && calMonth == now.get(Calendar.MONTH);
        btnCalendarToday.setVisibility(isCurrentMonth ? View.GONE : View.VISIBLE);

        List<CalendarAdapter.Day> cells = new ArrayList<>(42);
        Calendar first = Calendar.getInstance();
        first.set(calYear, calMonth, 1);
        int offset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7;
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

    /* ==================== Счётчик ==================== */

    private void renderCounter() {
        counterPrayerContainer.removeAllViews();
        counterOtherContainer.removeAllViews();
        int[] counts = ds.getCounterCounts();
        int[] completed = ds.getCounterDoneCounts();
        int prayerRemaining = 0;
        int prayerCompleted = 0;
        for (int i = 0; i < PrayerType.values().length; i++) {
            prayerRemaining += counts[i];
            prayerCompleted += completed[i];
        }
        int fastRemaining = counts[DataStore.COUNTER_COUNT - 1];
        int fastCompleted = completed[DataStore.COUNTER_COUNT - 1];
        updateProgressCard(tvPrayerRemaining, tvPrayerDoneTotal, tvPrayerPercent,
                prayerProgress, prayerRemaining, prayerCompleted, 23);
        updateProgressCard(tvFastRemaining, tvFastDoneTotal, tvFastPercent,
                fastProgress, fastRemaining, fastCompleted, 23);
        boolean allSettled = prayerRemaining + fastRemaining == 0;
        boolean showSettled = allSettled && debtJustSettled;
        allDoneRow.setVisibility(showSettled ? View.VISIBLE : View.GONE);
        if (showSettled) {
            allDoneRow.setAlpha(0f);
            allDoneRow.animate().alpha(1f).setDuration(300).start();
        }
        if (!allSettled) debtJustSettled = false;
        int[] icons = {
                R.drawable.ic_sun, R.drawable.ic_sun, R.drawable.ic_sun,
                R.drawable.ic_sun, R.drawable.ic_moon, R.drawable.ic_moon,
                R.drawable.ic_sparkle
        };
        for (PrayerType prayer : PrayerType.values()) {
            addCounterRow(counterPrayerContainer, prayer.getName(this), prayer.getIndex(), icons[prayer.getIndex()], counts);
        }
        addCounterRow(counterOtherContainer, getString(R.string.prayer_fast), 6, icons[6], counts);
    }

    private void updateProgressCard(TextView remainingView, TextView doneTotalView,
                                    TextView percentView, ProgressBar progressBar,
                                    int remaining, int completed, int baseTextSize) {
        int total = remaining + completed;
        float percent = total == 0 ? 0f : completed * 100f / total;
        String remainingText = formatCounterNumber(remaining);
        remainingView.setText(remainingText);
        int textSize = Math.max(18, baseTextSize - Math.max(0, remainingText.length() - 5) * 2);
        remainingView.setTextSize(textSize);
        doneTotalView.setText(getString(R.string.counter_progress_done_total,
                formatCounterNumber(completed), formatCounterNumber(total)));
        progressBar.setMax(1000);
        progressBar.setProgress(Math.round(percent * 10f));
        String percentText = formatPercent(percent);
        if (!percentText.contentEquals(percentView.getText())) {
            percentView.setText(percentText);
            pulse(percentView);
        }
    }

    private void pulse(View view) {
        if (view == null) return;
        view.animate().cancel();
        view.setScaleX(1.25f);
        view.setScaleY(1.25f);
        view.animate().scaleX(1f).scaleY(1f).setDuration(300)
                .setInterpolator(new OvershootInterpolator(2.5f)).start();
    }

    private String formatPercent(float percent) {
        return String.format(java.util.Locale.forLanguageTag("ru-RU"), "%.1f%%", percent);
    }

    private String formatCounterNumber(int value) {
        return String.format(java.util.Locale.US, "%,d", value).replace(',', '.');
    }

    private void addCounterRow(LinearLayout parent, String name, int index, int iconRes, int[] counts) {
        View row = getLayoutInflater().inflate(R.layout.item_counter_prayer, parent, false);
        TextView label = row.findViewById(R.id.tvCounterPrayerName);
        TextView value = row.findViewById(R.id.tvCounterPrayerCount);
        ImageView icon = row.findViewById(R.id.ivCounterPrayer);
        TextView minus = row.findViewById(R.id.btnCounterMinus);
        TextView plus = row.findViewById(R.id.btnCounterPlus);
        label.setText(name);
        value.setText(String.valueOf(counts[index]));
        icon.setImageResource(iconRes);
        icon.setColorFilter(ContextCompat.getColor(this, R.color.primary_light));
        minus.setEnabled(counts[index] > 0);
        minus.setAlpha(counts[index] > 0 ? 1f : 0.45f);

        minus.setOnClickListener(v -> updateCounter(v, index, -1));
        plus.setOnClickListener(v -> updateCounter(v, index, 1));
        value.setOnClickListener(v -> editCounterValue(index));
        value.setContentDescription(getString(R.string.counter_edit_value, name));
        parent.addView(row);
    }

    private void updateCounter(View source, int index, int delta) {
        ds.adjustCounterCount(index, delta);
        if (delta < 0 && ds.totalCounterDebt() == 0) debtJustSettled = true;
        renderCounter();

        LinearLayout parent = index < PrayerType.values().length ? counterPrayerContainer : counterOtherContainer;
        int rowIndex = index < PrayerType.values().length ? index : 0;
        View row = parent.getChildAt(rowIndex);
        View changedButton = row.findViewById(delta < 0
                ? R.id.btnCounterMinus : R.id.btnCounterPlus);
        TextView changedValue = row.findViewById(R.id.tvCounterPrayerCount);
        animateCounterTap(changedButton);
        animateCounterValue(changedValue);
    }

    private void animateCounterTap(View view) {
        if (view == null) return;
        view.animate().cancel();
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.animate().scaleX(0.72f).scaleY(0.72f).setDuration(70)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> view.animate().scaleX(1f).scaleY(1f)
                        .setDuration(240)
                        .setInterpolator(new OvershootInterpolator(4f))
                        .start())
                .start();
    }

    private void animateCounterValue(TextView value) {
        if (value == null) return;
        value.animate().cancel();
        value.setScaleX(1.5f);
        value.setScaleY(1.5f);
        value.animate().scaleX(1f).scaleY(1f).setDuration(260)
                .setInterpolator(new OvershootInterpolator(3f)).start();

        int accent = ContextCompat.getColor(this, R.color.primary_light);
        int normal = ContextCompat.getColor(this, R.color.text_primary);
        ValueAnimator flash = ValueAnimator.ofObject(new ArgbEvaluator(), accent, normal);
        flash.setDuration(420);
        flash.addUpdateListener(animation ->
                value.setTextColor((int) animation.getAnimatedValue()));
        flash.start();
    }

    private void editCounterValue(int index) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        input.setSingleLine(true);
        input.setSelectAllOnFocus(true);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        input.setBackgroundResource(R.drawable.bg_input);
        input.setText(String.valueOf(ds.getCounterCounts()[index]));
        input.setSelection(input.length());

        String prayerName = counterName(index);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.counter_value_title, prayerName))
                .setView(input)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_yes, (d, which) -> {
                    String value = input.getText().toString().trim();
                    int count = value.isEmpty() ? 0 : Integer.parseInt(value);
                    ds.setCounterCount(index, count);
                    if (count == 0 && ds.totalCounterDebt() == 0) debtJustSettled = true;
                    renderCounter();
                }).show();
        input.requestFocus();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    private String counterName(int index) {
        return index < PrayerType.values().length
                ? PrayerType.values()[index].getName(this) : getString(R.string.prayer_fast);
    }

    private void showCounterHintIfNeeded() {
        if (!ds.isCounterHintShown()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.counter_hint_title)
                    .setMessage(R.string.counter_hint_message)
                    .setPositiveButton(R.string.counter_hint_got_it, (d, w) -> {
                        ds.setCounterHintShown(true);
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    private void confirmResetCounters() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.counter_reset_title)
                .setMessage(R.string.counter_reset_message)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.counter_reset, (dialog, which) -> {
                    ds.resetCounterCounts();
                    debtJustSettled = false;
                    renderCounter();
                    Toast.makeText(this, R.string.counter_reset_done, Toast.LENGTH_SHORT).show();
                }).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /* ==================== Настройки ==================== */

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
                .setMessage(getString(R.string.backup_import_confirm_msg, res.count))
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_import, (d, w) -> {
                    ds.restoreBackup(res.records, res.notes, res.counterCounts, res.counterDoneCounts,
                            res.themeMode, res.appMode, res.calculatorProfile,
                            res.settingsIncluded);
                    ThemeUtil.apply(this);
                    Toast.makeText(this, getString(R.string.import_done,
                            DateUtils.pluralRecords(this, res.count)),
                            Toast.LENGTH_LONG).show();
                    // Перезапустить Activity с главного экрана
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
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
