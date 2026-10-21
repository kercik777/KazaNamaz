package tm.kaza.namaz.ag;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowManager;
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
        updateProgressCard(tvPrayerRemaining, tvPrayerDoneTotal, tvPrayerPercent,
                prayerProgress, prayerRemaining, prayerCompleted, 36);
        updateProgressCard(tvFastRemaining, tvFastDoneTotal, tvFastPercent,
                fastProgress, counts[DataStore.COUNTER_COUNT - 1],
                completed[DataStore.COUNTER_COUNT - 1], 32);
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
        int textSize = Math.max(28, baseTextSize - Math.max(0, remainingText.length() - 5) * 3);
        remainingView.setTextSize(textSize);
        doneTotalView.setText(getString(R.string.counter_progress_done_total,
                formatCounterNumber(completed), formatCounterNumber(total)));
        percentView.setText(String.format(java.util.Locale.forLanguageTag("ru-RU"), "%.1f%%", percent));
        progressBar.setMax(1000);
        progressBar.setProgress(Math.round(percent * 10f));
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
        ds.setCalculatorProfile("");
        source.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
        renderCounters();
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
                    ds.setCalculatorProfile("");
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
                    ds.setCalculatorProfile("");
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
