package tm.kaza.namaz.ag;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import tm.kaza.namaz.ag.data.DataStore;
import tm.kaza.namaz.ag.data.PrayerType;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import tm.kaza.namaz.ag.util.SystemBarsHelper;
import tm.kaza.namaz.ag.util.ThemeUtil;

/** Отдельный, независимый режим простого счётчика каза-намазов. */
public class CounterActivity extends AppCompatActivity {

    private DataStore ds;
    private TextView tvTotal;
    private LinearLayout prayerContainer;
    private LinearLayout otherContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtil.apply(this);
        super.onCreate(savedInstanceState);
        SystemBarsHelper.edgeToEdge(this);
        setContentView(R.layout.activity_counter);
        ds = DataStore.get(this);

        tvTotal = findViewById(R.id.tvCounterTotal);
        prayerContainer = findViewById(R.id.counterPrayerContainer);
        otherContainer = findViewById(R.id.counterOtherContainer);
        SystemBarsHelper.padTop(findViewById(R.id.counterHeaderContent));
        SystemBarsHelper.padBottom(findViewById(R.id.counterScroll));
        findViewById(R.id.btnCounterSettings).setOnClickListener(v -> openSettings());
        findViewById(R.id.btnResetCounters).setOnClickListener(v -> confirmReset());
        findViewById(R.id.btnOpenCalculator).setOnClickListener(v -> showCalculatorOptions());
        renderCounters();
    }

    private void renderCounters() {
        prayerContainer.removeAllViews();
        otherContainer.removeAllViews();
        int[] counts = ds.getCounterCounts();
        int[] icons = {
                R.drawable.ic_sun, R.drawable.ic_sun, R.drawable.ic_sun,
                R.drawable.ic_sun, R.drawable.ic_moon, R.drawable.ic_moon,
                R.drawable.ic_sparkle
        };
        for (PrayerType prayer : PrayerType.values()) {
            addCounterRow(prayerContainer, prayer.getName(this), prayer.getIndex(), icons[prayer.getIndex()], counts);
        }
        addCounterRow(otherContainer, getString(R.string.prayer_imsak), 6, icons[6], counts);
        tvTotal.setText(String.valueOf(ds.totalCounterDebt()));
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
        parent.addView(row);
    }

    private void updateCounter(View source, int index, int delta) {
        int[] counts = ds.getCounterCounts();
        int updated = Math.max(0, Math.min(DataStore.MAX_COUNTER_VALUE, counts[index] + delta));
        ds.setCounterCount(index, updated);
        ds.setCalculatorProfile("");
        source.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
        renderCounters();
    }

    private void confirmReset() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.counter_reset_title)
                .setMessage(R.string.counter_reset_message)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.counter_reset, (dialog, which) -> {
                    ds.setCounterCounts(new int[DataStore.COUNTER_COUNT]);
                    ds.setCalculatorProfile("");
                    renderCounters();
                    Toast.makeText(this, R.string.counter_reset_done, Toast.LENGTH_SHORT).show();
                }).show();
    }

    private void showCalculatorOptions() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.calculator_title)
                .setMessage(R.string.calculator_options)
                .setItems(new CharSequence[]{getString(R.string.calculator_auto),
                        getString(R.string.calculator_known)}, (dialog, which) -> {
                    if (which == 0) startAutomaticCalculation();
                    else chooseKnownDebtMode();
                }).show();
    }

    private void chooseKnownDebtMode() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.calculator_known_title)
                .setItems(new CharSequence[]{getString(R.string.calculator_same),
                        getString(R.string.calculator_individual)}, (dialog, which) -> {
                    if (which == 0) {
                        showNumberPicker(getString(R.string.calculator_days_title), 0, 36500, 0,
                                this::confirmApply);
                    } else {
                        askIndividualDebt(0, ds.getCounterCounts());
                    }
                }).show();
    }

    private interface NumberResult { void onResult(int value); }

    private void showNumberPicker(String title, int min, int max, int initial,
                                  NumberResult callback) {
        NumberPicker picker = new NumberPicker(this);
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setValue(Math.max(min, Math.min(max, initial)));
        picker.setWrapSelectorWheel(false);
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(picker)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_yes, (dialog, which) -> callback.onResult(picker.getValue()))
                .show();
    }

    private void askIndividualDebt(int prayerIndex, int[] counts) {
        if (prayerIndex >= PrayerType.values().length) {
            applyCalculatedCounts(counts, "");
            return;
        }
        PrayerType prayer = PrayerType.values()[prayerIndex];
        showNumberPicker(getString(R.string.calculator_prayer_days, prayer.getName(this)),
                0, 36500, counts[prayerIndex], value -> {
            counts[prayerIndex] = value;
            askIndividualDebt(prayerIndex + 1, counts);
        });
    }

    private void startAutomaticCalculation() {
        java.util.Calendar today = java.util.Calendar.getInstance();
        java.util.Calendar initial = (java.util.Calendar) today.clone();
        initial.add(java.util.Calendar.YEAR, -18);
        DatePickerDialog picker = new DatePickerDialog(this, (DatePicker view, int year,
                int month, int dayOfMonth) -> choosePubertyAge(year, month + 1, dayOfMonth),
                initial.get(java.util.Calendar.YEAR), initial.get(java.util.Calendar.MONTH),
                initial.get(java.util.Calendar.DAY_OF_MONTH));
        picker.setTitle(R.string.calculator_birth_title);
        picker.show();
    }

    private void choosePubertyAge(int year, int month, int day) {
        showNumberPicker(getString(R.string.calculator_puberty_title), 8, 18, 12,
                age -> chooseGender(year, month, day, age));
    }

    private void chooseGender(int year, int month, int day, int pubertyAge) {
        String[] options = {getString(R.string.calculator_gender_male),
                getString(R.string.calculator_gender_female)};
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.calculator_gender_title)
                .setMessage(R.string.calculator_gender_help)
                .setItems(options, (dialog, which) -> chooseMadhhab(year, month, day,
                        pubertyAge, which == 1))
                .show();
    }

    private void chooseMadhhab(int year, int month, int day, int pubertyAge, boolean female) {
        String[] madhhabs = {
                getString(R.string.calculator_madhhab_hanafi),
                getString(R.string.calculator_madhhab_shafii),
                getString(R.string.calculator_madhhab_maliki),
                getString(R.string.calculator_madhhab_hanbali)
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.calculator_madhhab_title)
                .setMessage(R.string.calculator_madhhab_help)
                .setItems(madhhabs, (dialog, which) -> {
                    if (female) {
                        showNumberPicker(getString(R.string.calculator_period_title), 0, 15, 6,
                                periodDays -> calculateDebt(year, month, day, pubertyAge,
                                        female, which, periodDays));
                    } else {
                        calculateDebt(year, month, day, pubertyAge, false, which, 0);
                    }
                }).show();
    }

    private void calculateDebt(int year, int month, int day, int pubertyAge,
                               boolean female, int madhhab, int periodDays) {
        try {
            LocalDate today = LocalDate.now();
            LocalDate birth = LocalDate.of(year, month, day);
            LocalDate obligationStart = birth.plusYears(pubertyAge);
            long elapsedDays = Math.max(0, ChronoUnit.DAYS.between(obligationStart, today));
            long estimatedPeriodDays = Math.round(elapsedDays * (periodDays / 30.44d));
            int debtDays = (int) Math.min(36500, Math.max(0, elapsedDays - estimatedPeriodDays));
            String profile = new JSONObject()
                    .put("birthDate", birth.toString())
                    .put("pubertyAge", pubertyAge)
                    .put("gender", female ? "female" : "male")
                    .put("madhhab", madhhab)
                    .put("periodDaysPerMonth", periodDays)
                    .toString();
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.calculator_result_title)
                    .setMessage(getString(R.string.calculator_result_message, debtDays))
                    .setNegativeButton(R.string.btn_cancel, null)
                    .setPositiveButton(R.string.calculator_apply,
                            (dialog, which) -> confirmApply(debtDays, profile))
                    .show();
        } catch (Exception ignored) {
            Toast.makeText(this, R.string.calculator_error_date, Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmApply(int days) {
        int[] counts = ds.getCounterCounts();
        for (int i = 0; i < PrayerType.values().length; i++) counts[i] = days;
        applyCalculatedCounts(counts, "");
    }

    private void confirmApply(int days, String profile) {
        int[] counts = ds.getCounterCounts();
        for (int i = 0; i < PrayerType.values().length; i++) counts[i] = days;
        applyCalculatedCounts(counts, profile);
    }

    private void applyCalculatedCounts(int[] counts, String profile) {
        ds.setCounterCounts(counts);
        ds.setCalculatorProfile(profile);
        renderCounters();
        Toast.makeText(this, R.string.calculator_applied, Toast.LENGTH_LONG).show();
    }

    /** Открывает общую панель настроек и резервного копирования. */
    private void openSettings() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_COUNTER_SETTINGS_ONLY, true);
        startActivity(intent);
        finish();
    }

}
