package tm.kaza.namaz.ag;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import java.text.DateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Locale;

import tm.kaza.namaz.ag.data.DataStore;
import tm.kaza.namaz.ag.data.PrayerType;
import tm.kaza.namaz.ag.util.SystemBarsHelper;
import tm.kaza.namaz.ag.util.ThemeUtil;

/** Пошаговый калькулятор каза с автоматическим и ручным сценариями. */
public class CalculatorActivity extends AppCompatActivity {

    private static final int METHOD = 0;
    private static final int AUTO_BIRTH = 1;
    private static final int AUTO_AGE = 2;
    private static final int AUTO_GENDER = 3;
    private static final int AUTO_MADHHAB = 4;
    private static final int AUTO_PERIOD = 5;
    private static final int AUTO_RESULT = 6;
    private static final int KNOWN_TYPE = 10;
    private static final int KNOWN_DAYS = 11;
    private static final int KNOWN_INDIVIDUAL = 12;
    private static final int KNOWN_REVIEW = 13;

    private static final int FLOW_AUTO = 1;
    private static final int FLOW_KNOWN = 2;

    private DataStore ds;
    private LinearLayout content;
    private TextView breadcrumb, stepLabel, title, subtitle, footerHint;
    private ProgressBar progress;
    private MaterialButton nextButton;
    private int screen = METHOD;
    private int flow;
    private int selectedMethod = -1;
    private int knownType = -1;
    private int gender = -1;
    private int madhhab = -1;
    private int pubertyAge = 12;
    private int periodDays = 6;
    private int knownDays;
    private int calculatedDays;
    private int reviewFrom = KNOWN_DAYS;
    private Calendar birthDate = Calendar.getInstance();
    private NumberPicker activePicker;
    private int[] draftCounts;
    private TextView draftTotal;
    private int draftTotalForSix;
    private String calculatorProfile = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtil.apply(this);
        super.onCreate(savedInstanceState);
        SystemBarsHelper.edgeToEdge(this);
        setContentView(R.layout.activity_calculator);
        ds = DataStore.get(this);

        breadcrumb = findViewById(R.id.tvCalcBreadcrumb);
        stepLabel = findViewById(R.id.tvCalcStep);
        title = findViewById(R.id.tvCalcTitle);
        subtitle = findViewById(R.id.tvCalcSubtitle);
        footerHint = findViewById(R.id.tvCalcFooterHint);
        progress = findViewById(R.id.pbCalcProgress);
        content = findViewById(R.id.calculatorStepContent);
        nextButton = findViewById(R.id.btnCalcNext);

        SystemBarsHelper.padTop(findViewById(R.id.calculatorToolbar));
        SystemBarsHelper.padBottom(findViewById(R.id.calculatorFooter));
        findViewById(R.id.btnCalcBack).setOnClickListener(v -> goBack());
        findViewById(R.id.btnCalcCancel).setOnClickListener(v -> finish());
        nextButton.setOnClickListener(v -> goNext());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goBack();
            }
        });

        birthDate.add(Calendar.YEAR, -18);
        draftCounts = ds.getCounterCounts();
        renderScreen();
    }

    private void renderScreen() {
        content.removeAllViews();
        activePicker = null;
        footerHint.setText("");
        footerHint.setVisibility(View.GONE);
        nextButton.setEnabled(true);
        nextButton.setText(R.string.next);

        if (screen == METHOD) {
            setCopy(R.string.calculator_method_title, R.string.calculator_method_subtitle);
            addOptionCard(R.string.calculator_auto, R.string.calculator_method_auto_subtitle,
                    selectedMethod == 0, () -> { selectedMethod = 0; renderScreen(); });
            addOptionCard(R.string.calculator_known, R.string.calculator_method_known_subtitle,
                    selectedMethod == 1, () -> { selectedMethod = 1; renderScreen(); });
            if (selectedMethod < 0) nextButton.setEnabled(false);
        } else if (screen == AUTO_BIRTH) {
            setCopy(R.string.calculator_birth_title, R.string.calculator_birth_subtitle);
            MaterialButton dateButton = new MaterialButton(this);
            dateButton.setText(DateFormat.getDateInstance(DateFormat.LONG, new Locale("ru"))
                    .format(birthDate.getTime()));
            dateButton.setTextAllCaps(false);
            dateButton.setIconResource(R.drawable.ic_calendar);
            dateButton.setIconTint(ColorStateList.valueOf(color(R.color.primary)));
            dateButton.setCornerRadius(dp(16));
            dateButton.setOnClickListener(v -> showBirthDatePicker());
            content.addView(dateButton, matchWrap());
        } else if (screen == AUTO_AGE) {
            setCopy(R.string.calculator_puberty_title, R.string.calculator_age_subtitle);
            activePicker = addPicker(8, 18, pubertyAge);
            addCenteredUnit(getString(R.string.calculator_years_unit));
        } else if (screen == AUTO_GENDER) {
            setCopy(R.string.calculator_gender_title, R.string.calculator_gender_subtitle);
            addOptionCard(R.string.calculator_gender_male, R.string.calculator_gender_male_subtitle,
                    gender == 0, () -> { gender = 0; renderScreen(); });
            addOptionCard(R.string.calculator_gender_female, R.string.calculator_gender_female_subtitle,
                    gender == 1, () -> { gender = 1; renderScreen(); });
            if (gender < 0) nextButton.setEnabled(false);
        } else if (screen == AUTO_MADHHAB) {
            setCopy(R.string.calculator_madhhab_title, R.string.calculator_madhhab_subtitle);
            int[] labels = {R.string.calculator_madhhab_hanafi, R.string.calculator_madhhab_shafii,
                    R.string.calculator_madhhab_maliki, R.string.calculator_madhhab_hanbali};
            for (int i = 0; i < labels.length; i++) {
                final int value = i;
                addOptionCard(labels[i], R.string.calculator_madhhab_card_subtitle,
                        madhhab == i, () -> { madhhab = value; renderScreen(); });
            }
            if (madhhab < 0) nextButton.setEnabled(false);
        } else if (screen == AUTO_PERIOD) {
            setCopy(R.string.calculator_period_title, R.string.calculator_period_subtitle);
            activePicker = addPicker(0, 15, periodDays);
            addCenteredUnit(getString(R.string.calculator_days_per_month_unit));
        } else if (screen == AUTO_RESULT) {
            setCopy(R.string.calculator_result_title, R.string.calculator_result_subtitle);
            addCompletionMark();
            addSummaryCard(draftCounts);
            addInfoCard(R.string.calculator_result_info);
            nextButton.setText(R.string.calculator_start_tracking);
        } else if (screen == KNOWN_TYPE) {
            setCopy(R.string.calculator_known_title, R.string.calculator_known_type_subtitle);
            addOptionCard(R.string.calculator_same, R.string.calculator_same_subtitle,
                    knownType == 0, () -> { knownType = 0; renderScreen(); });
            addOptionCard(R.string.calculator_individual, R.string.calculator_individual_subtitle,
                    knownType == 1, () -> { knownType = 1; renderScreen(); });
            if (knownType < 0) nextButton.setEnabled(false);
        } else if (screen == KNOWN_DAYS) {
            setCopy(R.string.calculator_days_title, R.string.calculator_days_subtitle);
            activePicker = addPicker(0, 36500, knownDays);
            addCenteredUnit(getString(R.string.calculator_days_unit));
        } else if (screen == KNOWN_INDIVIDUAL) {
            setCopy(R.string.calculator_individual, R.string.calculator_individual_subtitle_short);
            addIndividualPrayerRows();
            nextButton.setText(R.string.next);
        } else if (screen == KNOWN_REVIEW) {
            setCopy(R.string.calculator_review_title, R.string.calculator_review_subtitle);
            addSummaryCard(draftCounts);
            addInfoCard(R.string.calculator_result_info);
            nextButton.setText(R.string.calculator_save_counts);
        }

        updateProgressAndToolbar();
        content.setAlpha(0f);
        content.animate().alpha(1f).setDuration(150).start();
    }

    private void setCopy(int titleRes, int subtitleRes) {
        title.setText(titleRes);
        subtitle.setText(subtitleRes);
    }

    private void updateProgressAndToolbar() {
        int current;
        int total;
        boolean autoProgress = flow == FLOW_AUTO || (screen == METHOD && selectedMethod != 1);
        if (autoProgress) {
            total = 7;
            current = screen == METHOD ? 1 : screen + 1;
            if (screen == AUTO_RESULT) current = 7;
        } else {
            total = 4;
            if (screen == METHOD) current = 1;
            else if (screen == KNOWN_TYPE) current = 2;
            else if (screen == KNOWN_DAYS || screen == KNOWN_INDIVIDUAL) current = 3;
            else current = 4;
        }
        stepLabel.setText(getString(R.string.calculator_step, current, total));
        progress.setProgress(Math.round(100f * current / total));
        if (screen == METHOD) breadcrumb.setText(R.string.calculator_open);
        else breadcrumb.setText(flow == FLOW_AUTO ? R.string.calculator_auto : R.string.calculator_known);
    }

    private void goNext() {
        if (screen == METHOD) {
            if (selectedMethod < 0) return;
            if (selectedMethod == 0) {
                flow = FLOW_AUTO;
                screen = AUTO_BIRTH;
            } else {
                flow = FLOW_KNOWN;
                screen = KNOWN_TYPE;
            }
        } else if (screen == AUTO_BIRTH) {
            screen = AUTO_AGE;
        } else if (screen == AUTO_AGE) {
            pubertyAge = activePicker.getValue();
            screen = AUTO_GENDER;
        } else if (screen == AUTO_GENDER) {
            if (gender < 0) return;
            screen = AUTO_MADHHAB;
        } else if (screen == AUTO_MADHHAB) {
            if (madhhab < 0) return;
            if (gender == 1) screen = AUTO_PERIOD;
            else calculateAutomaticDebt(0);
        } else if (screen == AUTO_PERIOD) {
            periodDays = activePicker.getValue();
            calculateAutomaticDebt(periodDays);
        } else if (screen == AUTO_RESULT) {
            saveAndFinish(calculatorProfile);
            return;
        } else if (screen == KNOWN_TYPE) {
            if (knownType < 0) return;
            screen = knownType == 0 ? KNOWN_DAYS : KNOWN_INDIVIDUAL;
        } else if (screen == KNOWN_DAYS) {
            knownDays = activePicker.getValue();
            draftCounts = ds.getCounterCounts();
            for (int i = 0; i < PrayerType.values().length; i++) draftCounts[i] = knownDays;
            reviewFrom = KNOWN_DAYS;
            screen = KNOWN_REVIEW;
        } else if (screen == KNOWN_INDIVIDUAL) {
            reviewFrom = KNOWN_INDIVIDUAL;
            screen = KNOWN_REVIEW;
        } else if (screen == KNOWN_REVIEW) {
            saveAndFinish("");
            return;
        }
        renderScreen();
    }

    private void goBack() {
        if (screen == METHOD) {
            finish();
            return;
        }
        if (screen == AUTO_BIRTH || screen == KNOWN_TYPE) {
            screen = METHOD;
        } else if (screen == AUTO_AGE) {
            screen = AUTO_BIRTH;
        } else if (screen == AUTO_GENDER) {
            screen = AUTO_AGE;
        } else if (screen == AUTO_MADHHAB) {
            screen = AUTO_GENDER;
        } else if (screen == AUTO_PERIOD) {
            screen = AUTO_MADHHAB;
        } else if (screen == AUTO_RESULT) {
            screen = gender == 1 ? AUTO_PERIOD : AUTO_MADHHAB;
        } else if (screen == KNOWN_DAYS || screen == KNOWN_INDIVIDUAL) {
            screen = KNOWN_TYPE;
        } else if (screen == KNOWN_REVIEW) {
            screen = reviewFrom;
        }
        renderScreen();
    }

    private void showBirthDatePicker() {
        DatePickerDialog picker = new DatePickerDialog(this,
                (DatePicker view, int year, int month, int dayOfMonth) -> {
                    birthDate.set(year, month, dayOfMonth, 12, 0, 0);
                    birthDate.set(Calendar.MILLISECOND, 0);
                    renderScreen();
                }, birthDate.get(Calendar.YEAR), birthDate.get(Calendar.MONTH),
                birthDate.get(Calendar.DAY_OF_MONTH));
        picker.setTitle(R.string.calculator_birth_title);
        picker.getDatePicker().setMaxDate(System.currentTimeMillis());
        picker.show();
    }

    private void calculateAutomaticDebt(int averagePeriodDays) {
        try {
            LocalDate today = LocalDate.now();
            LocalDate birth = LocalDate.of(birthDate.get(Calendar.YEAR),
                    birthDate.get(Calendar.MONTH) + 1, birthDate.get(Calendar.DAY_OF_MONTH));
            LocalDate start = birth.plusYears(pubertyAge);
            long elapsed = Math.max(0, ChronoUnit.DAYS.between(start, today));
            long excluded = gender == 1 ? Math.round(elapsed * (averagePeriodDays / 30.44d)) : 0;
            calculatedDays = (int) Math.min(36500, Math.max(0, elapsed - excluded));
            draftCounts = ds.getCounterCounts();
            for (int i = 0; i < PrayerType.values().length; i++) draftCounts[i] = calculatedDays;
            calculatorProfile = new JSONObject()
                    .put("birthDate", birth.toString())
                    .put("pubertyAge", pubertyAge)
                    .put("gender", gender == 1 ? "female" : "male")
                    .put("madhhab", madhhab)
                    .put("periodDaysPerMonth", averagePeriodDays)
                    .toString();
            screen = AUTO_RESULT;
            renderScreen();
        } catch (Exception e) {
            Toast.makeText(this, R.string.calculator_error_date, Toast.LENGTH_SHORT).show();
        }
    }

    private NumberPicker addPicker(int min, int max, int value) {
        NumberPicker picker = new NumberPicker(this);
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setValue(Math.max(min, Math.min(max, value)));
        picker.setWrapSelectorWheel(false);
        LinearLayout.LayoutParams lp = matchWrap();
        lp.height = dp(180);
        content.addView(picker, lp);
        return picker;
    }

    private void addCenteredUnit(String text) {
        TextView unit = new TextView(this);
        unit.setText(text);
        unit.setGravity(Gravity.CENTER);
        unit.setTextColor(color(R.color.text_secondary));
        unit.setTextSize(15);
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(-18);
        lp.bottomMargin = dp(12);
        content.addView(unit, lp);
    }

    private void addOptionCard(int titleRes, int subtitleRes, boolean selected, Runnable action) {
        addOptionCard(getString(titleRes), getString(subtitleRes), selected, action);
    }

    private void addOptionCard(String cardTitle, String cardSubtitle, boolean selected, Runnable action) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(20));
        card.setCardElevation(0);
        card.setCardBackgroundColor(color(R.color.surface));
        card.setStrokeWidth(dp(selected ? 2 : 1));
        card.setStrokeColor(color(selected ? R.color.primary_light : R.color.divider));
        card.setClickable(true);
        card.setFocusable(true);
        LinearLayout.LayoutParams cardLp = matchWrap();
        cardLp.bottomMargin = dp(12);
        card.setLayoutParams(cardLp);

        LinearLayout body = new LinearLayout(this);
        body.setGravity(Gravity.CENTER_VERTICAL);
        body.setOrientation(LinearLayout.HORIZONTAL);
        body.setPadding(dp(18), dp(16), dp(16), dp(16));
        LinearLayout textBox = new LinearLayout(this);
        textBox.setOrientation(LinearLayout.VERTICAL);
        TextView heading = new TextView(this);
        heading.setText(cardTitle);
        heading.setTextColor(color(R.color.text_primary));
        heading.setTextSize(16);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        TextView detail = new TextView(this);
        detail.setText(cardSubtitle);
        detail.setTextColor(color(R.color.text_secondary));
        detail.setTextSize(13);
        LinearLayout.LayoutParams detailLp = matchWrap();
        detailLp.topMargin = dp(4);
        textBox.addView(heading);
        textBox.addView(detail, detailLp);
        body.addView(textBox, new LinearLayout.LayoutParams(0, -2, 1));

        TextView check = new TextView(this);
        check.setText(selected ? "✓" : "○");
        check.setTextColor(color(selected ? R.color.primary_light : R.color.text_tertiary));
        check.setTextSize(selected ? 24 : 22);
        check.setGravity(Gravity.CENTER);
        body.addView(check, new LinearLayout.LayoutParams(dp(34), dp(40)));
        card.addView(body);
        card.setOnClickListener(v -> action.run());
        content.addView(card);
    }

    private void addIndividualPrayerRows() {
        int[] icons = {R.drawable.ic_sun, R.drawable.ic_sun, R.drawable.ic_sun,
                R.drawable.ic_sun, R.drawable.ic_moon, R.drawable.ic_moon};
        for (PrayerType prayer : PrayerType.values()) {
            int index = prayer.getIndex();
            View row = getLayoutInflater().inflate(R.layout.item_counter_prayer, content, false);
            TextView label = row.findViewById(R.id.tvCounterPrayerName);
            TextView count = row.findViewById(R.id.tvCounterPrayerCount);
            TextView minus = row.findViewById(R.id.btnCounterMinus);
            TextView plus = row.findViewById(R.id.btnCounterPlus);
            ImageView icon = row.findViewById(R.id.ivCounterPrayer);
            label.setText(prayer.getName(this));
            icon.setImageResource(icons[index]);
            icon.setColorFilter(color(R.color.primary_light));
            updateIndividualRow(count, minus, index);
            minus.setOnClickListener(v -> changeDraftCount(index, -1, count, minus));
            plus.setOnClickListener(v -> changeDraftCount(index, 1, count, minus));
            count.setOnClickListener(v -> editDraftCount(index, count, minus));
            content.addView(row);
        }
        draftTotal = new TextView(this);
        draftTotal.setTextColor(color(R.color.primary_light));
        draftTotal.setTextSize(16);
        draftTotal.setTypeface(null, android.graphics.Typeface.BOLD);
        draftTotal.setGravity(Gravity.END);
        LinearLayout.LayoutParams totalLp = matchWrap();
        totalLp.topMargin = dp(8);
        content.addView(draftTotal, totalLp);
        updateDraftTotal();
    }

    private void changeDraftCount(int index, int delta, TextView count, TextView minus) {
        draftCounts[index] = Math.max(0, Math.min(DataStore.MAX_COUNTER_VALUE,
                draftCounts[index] + delta));
        updateIndividualRow(count, minus, index);
        updateDraftTotal();
    }

    private void editDraftCount(int index, TextView count, TextView minus) {
        NumberPicker picker = new NumberPicker(this);
        picker.setMinValue(0);
        picker.setMaxValue(36500);
        picker.setValue(Math.min(36500, draftCounts[index]));
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.calculator_value_title,
                        PrayerType.values()[index].getName(this)))
                .setView(picker)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_yes, (dialog, which) -> {
                    draftCounts[index] = picker.getValue();
                    updateIndividualRow(count, minus, index);
                    updateDraftTotal();
                }).show();
    }

    private void updateIndividualRow(TextView count, TextView minus, int index) {
        count.setText(String.valueOf(draftCounts[index]));
        minus.setEnabled(draftCounts[index] > 0);
        minus.setAlpha(draftCounts[index] > 0 ? 1f : 0.45f);
    }

    private void updateDraftTotal() {
        draftTotalForSix = 0;
        for (int i = 0; i < PrayerType.values().length; i++) draftTotalForSix += draftCounts[i];
        if (draftTotal != null) {
            draftTotal.setText(getString(R.string.calculator_total_summary,
                    formatNumber(draftTotalForSix)));
        }
    }

    private void addSummaryCard(int[] counts) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(20));
        card.setCardElevation(0);
        card.setCardBackgroundColor(color(R.color.surface));
        card.setStrokeColor(color(R.color.divider));
        card.setStrokeWidth(dp(1));
        LinearLayout.LayoutParams cardLp = matchWrap();
        cardLp.bottomMargin = dp(14);
        card.setLayoutParams(cardLp);

        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setPadding(dp(18), dp(18), dp(18), dp(18));
        wrapper.setOrientation(LinearLayout.VERTICAL);
        TextView cardTitle = new TextView(this);
        cardTitle.setText(R.string.calculator_summary_title);
        cardTitle.setTextColor(color(R.color.primary_light));
        cardTitle.setTextSize(16);
        cardTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        wrapper.addView(cardTitle);

        for (int row = 0; row < 2; row++) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setPadding(0, dp(14), 0, 0);
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                LinearLayout cell = new LinearLayout(this);
                cell.setOrientation(LinearLayout.VERTICAL);
                TextView prayerName = new TextView(this);
                prayerName.setText(PrayerType.values()[index].getName(this));
                prayerName.setTextColor(color(R.color.text_secondary));
                prayerName.setTextSize(12);
                TextView prayerCount = new TextView(this);
                prayerCount.setText(formatNumber(counts[index]));
                prayerCount.setTextColor(color(R.color.text_primary));
                prayerCount.setTextSize(20);
                prayerCount.setTypeface(null, android.graphics.Typeface.BOLD);
                LinearLayout.LayoutParams countLp = matchWrap();
                countLp.topMargin = dp(3);
                cell.addView(prayerName);
                cell.addView(prayerCount, countLp);
                line.addView(cell, new LinearLayout.LayoutParams(0, -2, 1));
            }
            wrapper.addView(line);
        }
        TextView total = new TextView(this);
        int sum = 0;
        for (int i = 0; i < PrayerType.values().length; i++) sum += counts[i];
        total.setText(getString(R.string.calculator_total_summary, formatNumber(sum)));
        total.setTextColor(color(R.color.primary_light));
        total.setTextSize(16);
        total.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams totalLp = matchWrap();
        totalLp.topMargin = dp(18);
        wrapper.addView(total, totalLp);
        card.addView(wrapper);
        content.addView(card);
    }

    private void addCompletionMark() {
        ImageView check = new ImageView(this);
        check.setImageResource(R.drawable.ic_check);
        check.setColorFilter(color(R.color.success));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(64), dp(64));
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.bottomMargin = dp(10);
        content.addView(check, lp);
    }

    private void addInfoCard(int textRes) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(16));
        card.setCardElevation(0);
        card.setCardBackgroundColor(color(R.color.surface_alt));
        card.setStrokeWidth(0);
        TextView text = new TextView(this);
        text.setText(textRes);
        text.setTextColor(color(R.color.text_secondary));
        text.setTextSize(13);
        text.setLineSpacing(dp(3), 1f);
        text.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.addView(text);
        content.addView(card, matchWrap());
    }

    private void saveAndFinish(String profile) {
        ds.setCounterCounts(draftCounts);
        ds.setCalculatorProfile(profile);
        Toast.makeText(this, R.string.calculator_applied_screen, Toast.LENGTH_LONG).show();
        finish();
    }

    private String formatNumber(int value) {
        return java.text.NumberFormat.getIntegerInstance(new Locale("ru")).format(value);
    }

    private void addCenteredUnit(String text) {
        TextView unit = new TextView(this);
        unit.setText(text);
        unit.setGravity(Gravity.CENTER);
        unit.setTextColor(color(R.color.text_secondary));
        unit.setTextSize(15);
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(-18);
        lp.bottomMargin = dp(12);
        content.addView(unit, lp);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private int color(int res) {
        return ContextCompat.getColor(this, res);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
