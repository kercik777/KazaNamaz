package tm.kaza.namaz.ag;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.view.WindowManager;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import tm.kaza.namaz.ag.data.DataStore;
import tm.kaza.namaz.ag.data.PrayerType;
import tm.kaza.namaz.ag.util.SystemBarsHelper;
import tm.kaza.namaz.ag.util.ThemeUtil;

/** Отдельный, независимый режим простого счётчика каза-намазов. */
public class CounterActivity extends AppCompatActivity {

    private DataStore ds;
    private TextView tvPrayerRemaining, tvPrayerDoneTotal, tvPrayerPercent;
    private TextView tvFastRemaining, tvFastDoneTotal, tvFastPercent;
    private ProgressBar prayerProgress, fastProgress;
    private LinearLayout prayerContainer;
    private LinearLayout otherContainer;
    private View allDoneRow;
    private boolean debtJustSettled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtil.apply(this);
        super.onCreate(savedInstanceState);
        SystemBarsHelper.edgeToEdge(this);
        setContentView(R.layout.activity_counter);
        ds = DataStore.get(this);

        tvPrayerRemaining = findViewById(R.id.tvPrayerRemaining);
        tvPrayerDoneTotal = findViewById(R.id.tvPrayerDoneTotal);
        tvPrayerPercent = findViewById(R.id.tvPrayerPercent);
        prayerProgress = findViewById(R.id.pbPrayerDebtProgress);
        tvFastRemaining = findViewById(R.id.tvFastRemaining);
        tvFastDoneTotal = findViewById(R.id.tvFastDoneTotal);
        tvFastPercent = findViewById(R.id.tvFastPercent);
        fastProgress = findViewById(R.id.pbFastDebtProgress);
        prayerContainer = findViewById(R.id.counterPrayerContainer);
        otherContainer = findViewById(R.id.counterOtherContainer);
        allDoneRow = findViewById(R.id.rowCounterAllDone);
        SystemBarsHelper.padTop(findViewById(R.id.counterHeaderContent));
        SystemBarsHelper.padBottom(findViewById(R.id.counterScroll));
        findViewById(R.id.btnCounterSettings).setOnClickListener(v -> openSettings());
        findViewById(R.id.btnResetCounters).setOnClickListener(v -> confirmReset());
        findViewById(R.id.btnOpenCalculator).setOnClickListener(v ->
                startActivity(new Intent(this, CalculatorActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ds != null) renderCounters();
    }

    private void renderCounters() {
        prayerContainer.removeAllViews();
        otherContainer.removeAllViews();
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
            addCounterRow(prayerContainer, prayer.getName(this), prayer.getIndex(), icons[prayer.getIndex()], counts);
        }
        addCounterRow(otherContainer, getString(R.string.prayer_fast), 6, icons[6], counts);
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

    /** Короткий акцент на изменённом проценте, чтобы обновление было заметно. */
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
        renderCounters();

        LinearLayout parent = index < PrayerType.values().length ? prayerContainer : otherContainer;
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
                    renderCounters();
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void confirmReset() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.counter_reset_title)
                .setMessage(R.string.counter_reset_message)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.counter_reset, (dialog, which) -> {
                    ds.resetCounterCounts();
                    debtJustSettled = false;
                    renderCounters();
                    Toast.makeText(this, R.string.counter_reset_done, Toast.LENGTH_SHORT).show();
                }).show();
    }

    /** Открывает общую панель настроек и резервного копирования. */
    private void openSettings() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_COUNTER_SETTINGS_ONLY, true);
        startActivity(intent);
    }

}
