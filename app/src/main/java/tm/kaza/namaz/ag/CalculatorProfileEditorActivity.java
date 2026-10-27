package tm.kaza.namaz.ag;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButtonToggleGroup;

import org.json.JSONObject;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Locale;

import tm.kaza.namaz.ag.data.DataStore;
import tm.kaza.namaz.ag.data.PrayerType;
import tm.kaza.namaz.ag.util.SystemBarsHelper;
import tm.kaza.namaz.ag.util.ThemeUtil;

/** Редактор сохранённых исходных данных автоматической оценки каза. */
public class CalculatorProfileEditorActivity extends AppCompatActivity {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private DataStore ds;
    private TextView birthDateValue, pubertyAgeValue, madhhabValue, regularStartValue;
    private MaterialButtonToggleGroup genderToggle;
    private LocalDate birthDate = LocalDate.now().minusYears(18);
    private LocalDate regularPrayerStart;
    private int pubertyAge = 12;
    private int gender;
    private int madhhab;
    private int periodDays = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtil.apply(this);
        super.onCreate(savedInstanceState);
        SystemBarsHelper.edgeToEdge(this);
        setContentView(R.layout.activity_calculator_profile_editor);
        ds = DataStore.get(this);
        loadSavedProfile();

        SystemBarsHelper.padTop(findViewById(R.id.calculatorEditToolbar));
        SystemBarsHelper.padBottom(findViewById(R.id.calculatorEditFooter));
        birthDateValue = findViewById(R.id.tvEditBirthDate);
        pubertyAgeValue = findViewById(R.id.tvEditPubertyAge);
        madhhabValue = findViewById(R.id.tvEditMadhhab);
        regularStartValue = findViewById(R.id.tvEditRegularStart);
        genderToggle = findViewById(R.id.editGenderToggle);

        findViewById(R.id.btnCalculatorEditBack).setOnClickListener(v -> finish());
        findViewById(R.id.rowEditBirthDate).setOnClickListener(v -> showDateWheel(false));
        findViewById(R.id.btnEditAgeMinus).setOnClickListener(v -> changePubertyAge(-1));
        findViewById(R.id.btnEditAgePlus).setOnClickListener(v -> changePubertyAge(1));
        findViewById(R.id.rowEditMadhhab).setOnClickListener(v -> showMadhhabOptions());
        findViewById(R.id.rowEditRegularStart).setOnClickListener(v -> showRegularStartOptions());
        findViewById(R.id.btnPreviewCalculatorEdit).setOnClickListener(v -> previewChanges());
        genderToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            gender = checkedId == R.id.btnEditGenderFemale ? 1 : 0;
        });

        renderValues();
    }

    private void loadSavedProfile() {
        try {
            JSONObject profile = new JSONObject(ds.getCalculatorProfile());
            String savedBirthDate = profile.optString("birthDate", "");
            if (!savedBirthDate.isEmpty()) birthDate = LocalDate.parse(savedBirthDate);
            pubertyAge = clamp(profile.optInt("pubertyAge", 12), 8, 18);
            gender = "female".equals(profile.optString("gender", "male")) ? 1 : 0;
            madhhab = clamp(profile.optInt("madhhab", 0), 0, 3);
            periodDays = clamp(profile.optInt("periodDaysPerMonth", 6), 0, 15);
            String regularStart = profile.optString("regularPrayerStart", "");
            if (!regularStart.isEmpty()) regularPrayerStart = LocalDate.parse(regularStart);
        } catch (Exception ignored) {
            birthDate = LocalDate.now().minusYears(18);
            pubertyAge = 12;
            gender = 0;
            madhhab = 0;
            periodDays = 6;
            regularPrayerStart = null;
        }
    }

    private void renderValues() {
        birthDateValue.setText(formatDate(birthDate));
        pubertyAgeValue.setText(getString(R.string.calculator_age_value, pubertyAge));
        madhhabValue.setText(madhhabName(madhhab));
        regularStartValue.setText(regularPrayerStart == null
                ? getString(R.string.calculator_regular_never) : formatDate(regularPrayerStart));
        genderToggle.check(gender == 1 ? R.id.btnEditGenderFemale : R.id.btnEditGenderMale);
    }

    private void changePubertyAge(int delta) {
        int updated = clamp(pubertyAge + delta, 8, 18);
        if (updated == pubertyAge) return;
        pubertyAge = updated;
        pubertyAgeValue.setText(getString(R.string.calculator_age_value, pubertyAge));
    }

    private void showMadhhabOptions() {
        int[] labels = {R.string.calculator_madhhab_hanafi, R.string.calculator_madhhab_shafii,
                R.string.calculator_madhhab_maliki, R.string.calculator_madhhab_hanbali};
        CharSequence[] names = new CharSequence[labels.length];
        for (int i = 0; i < labels.length; i++) names[i] = getString(labels[i]);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.calculator_edit_madhhab_label)
                .setSingleChoiceItems(names, madhhab, (dialog, which) -> {
                    madhhab = which;
                    madhhabValue.setText(madhhabName(madhhab));
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private String madhhabName(int value) {
        int[] labels = {R.string.calculator_madhhab_hanafi, R.string.calculator_madhhab_shafii,
                R.string.calculator_madhhab_maliki, R.string.calculator_madhhab_hanbali};
        return getString(labels[clamp(value, 0, labels.length - 1)]);
    }

    private void showRegularStartOptions() {
        String[] options = {getString(R.string.calculator_regular_never),
                getString(R.string.calculator_regular_choose_date)};
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.calculator_regular_options_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        regularPrayerStart = null;
                        renderValues();
                    } else {
                        showDateWheel(true);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /** Wheel-style day/month/year selector, matching the calculator's birth-date step. */
    private void showDateWheel(boolean forRegularStart) {
        LocalDate initial = forRegularStart
                ? (regularPrayerStart == null ? LocalDate.now() : regularPrayerStart) : birthDate;
        int latestYear = LocalDate.now().getYear();
        LinearLayout wheels = new LinearLayout(this);
        wheels.setOrientation(LinearLayout.HORIZONTAL);
        wheels.setGravity(Gravity.CENTER);
        wheels.setPadding(dp(4), dp(8), dp(4), dp(8));

        NumberPicker dayPicker = new NumberPicker(this);
        dayPicker.setMinValue(1);
        dayPicker.setMaxValue(31);
        dayPicker.setValue(initial.getDayOfMonth());
        dayPicker.setWrapSelectorWheel(false);
        dayPicker.setContentDescription(getString(R.string.calculator_birth_day));

        NumberPicker monthPicker = new NumberPicker(this);
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(getResources().getStringArray(R.array.calculator_months_wheel));
        monthPicker.setValue(initial.getMonthValue() - 1);
        monthPicker.setWrapSelectorWheel(true);
        monthPicker.setContentDescription(getString(R.string.calculator_birth_month));

        NumberPicker yearPicker = new NumberPicker(this);
        yearPicker.setMinValue(0);
        yearPicker.setMaxValue(latestYear - 1900);
        String[] years = new String[latestYear - 1899];
        for (int i = 0; i < years.length; i++) years[i] = String.valueOf(latestYear - i);
        yearPicker.setDisplayedValues(years);
        yearPicker.setValue(latestYear - initial.getYear());
        yearPicker.setWrapSelectorWheel(false);
        yearPicker.setContentDescription(getString(R.string.calculator_birth_year));

        monthPicker.setOnValueChangedListener((picker, oldValue, newValue) ->
                updateDayRange(dayPicker, monthPicker, yearPicker, latestYear));
        yearPicker.setOnValueChangedListener((picker, oldValue, newValue) ->
                updateDayRange(dayPicker, monthPicker, yearPicker, latestYear));
        LinearLayout.LayoutParams wheelParams = new LinearLayout.LayoutParams(0, dp(210), 1f);
        wheels.addView(dayPicker, wheelParams);
        wheels.addView(monthPicker, new LinearLayout.LayoutParams(0, dp(210), 1f));
        wheels.addView(yearPicker, new LinearLayout.LayoutParams(0, dp(210), 1.15f));
        updateDayRange(dayPicker, monthPicker, yearPicker, latestYear);

        new MaterialAlertDialogBuilder(this)
                .setTitle(forRegularStart ? R.string.calculator_regular_options_title
                        : R.string.calculator_birth_title)
                .setView(wheels)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.calculator_date_select, (dialog, which) -> {
                    LocalDate selected = LocalDate.of(latestYear - yearPicker.getValue(),
                            monthPicker.getValue() + 1, dayPicker.getValue());
                    if (selected.isAfter(LocalDate.now())) {
                        Toast.makeText(this, R.string.calculator_birth_future, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (forRegularStart) regularPrayerStart = selected;
                    else birthDate = selected;
                    renderValues();
                })
                .show();
    }

    private void updateDayRange(NumberPicker dayPicker, NumberPicker monthPicker,
                                NumberPicker yearPicker, int latestYear) {
        int year = latestYear - yearPicker.getValue();
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, monthPicker.getValue(), 1);
        int maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        int day = Math.min(dayPicker.getValue(), maxDay);
        dayPicker.setMaxValue(maxDay);
        dayPicker.setValue(day);
    }

    private void previewChanges() {
        LocalDate today = LocalDate.now();
        LocalDate obligationStart = birthDate.plusYears(pubertyAge);
        LocalDate prayerEnd = regularPrayerStart == null || regularPrayerStart.isAfter(today)
                ? today : regularPrayerStart;
        long prayerElapsed = Math.max(0, ChronoUnit.DAYS.between(obligationStart, prayerEnd));
        long prayerPeriodDays = gender == 1 ? Math.round(prayerElapsed * periodDays / 30.44d) : 0;
        int prayerDebt = clamp((int) Math.min(36500,
                Math.max(0, prayerElapsed - prayerPeriodDays)), 0, 36500);

        long totalElapsed = Math.max(0, ChronoUnit.DAYS.between(obligationStart, today));
        double obligationYears = totalElapsed / 365.2425d;
        int fastingDaysPerYear = Math.max(0, 30 - (gender == 1 ? periodDays : 0));
        int fastDebt = (int) Math.min(36500,
                Math.max(0, Math.round(obligationYears * fastingDaysPerYear)));

        String profile;
        try {
            profile = new JSONObject()
                    .put("birthDate", birthDate.toString())
                    .put("pubertyAge", pubertyAge)
                    .put("gender", gender == 1 ? "female" : "male")
                    .put("madhhab", madhhab)
                    .put("periodDaysPerMonth", periodDays)
                    .put("regularPrayerStart", regularPrayerStart == null ? "" : regularPrayerStart.toString())
                    .put("fastDebtDays", fastDebt)
                    .toString();
        } catch (Exception e) {
            Toast.makeText(this, R.string.calculator_error_date, Toast.LENGTH_SHORT).show();
            return;
        }

        final String newProfile = profile;
        String message = getString(R.string.calculator_edit_preview_message,
                formatNumber(prayerDebt), formatNumber(fastDebt));
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.calculator_edit_preview_title)
                .setMessage(message)
                .setNegativeButton(R.string.back, null)
                .setPositiveButton(R.string.calculator_edit_apply, (dialog, which) -> {
                    int[] counts = ds.getCounterCounts();
                    for (int i = 0; i < PrayerType.values().length; i++) counts[i] = prayerDebt;
                    counts[DataStore.COUNTER_COUNT - 1] = fastDebt;
                    ds.applyCounterCounts(counts);
                    ds.setCalculatorProfile(newProfile);
                    Toast.makeText(this, R.string.calculator_edit_saved, Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .show();
    }

    private String formatNumber(int value) {
        return String.format(Locale.US, "%,d", value).replace(',', '.');
    }

    private String formatDate(LocalDate date) {
        return DATE_FORMAT.format(date);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
