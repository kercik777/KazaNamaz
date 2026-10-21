package tm.kaza.namaz.ag.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import tm.kaza.namaz.ag.R;

/**
 * Круглый индикатор прогресса с градиентной дугой (зелёный → золотой).
 * Полностью кастомный, без сторонних библиотек.
 */
public class ProgressRingView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rectF = new RectF();

    private int progress = 0;
    private int max = 1;
    private String centerText = "0/6";

    public ProgressRingView(Context context) {
        super(context);
        init();
    }

    public ProgressRingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    /** Установить прогресс: выполнено done из total. */
    public void setProgress(int done, int total) {
        this.progress = done;
        this.max = Math.max(total, 1);
        this.centerText = done + "/" + total;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float stroke = dp(8f);
        float r = Math.min(cx, cy) - stroke / 2f - dp(2f);
        rectF.set(cx - r, cy - r, cx + r, cy + r);

        trackPaint.setStrokeWidth(stroke);
        trackPaint.setColor(ContextCompat.getColor(getContext(), R.color.surface_alt));
        canvas.drawCircle(cx, cy, r, trackPaint);

        float sweep = 360f * progress / max;
        if (sweep > 0.5f) {
            int green = ContextCompat.getColor(getContext(), R.color.primary_light);
            int gold = ContextCompat.getColor(getContext(), R.color.accent_gold);
            SweepGradient shader = new SweepGradient(cx, cy, green, gold);
            Matrix matrix = new Matrix();
            matrix.setRotate(-90f, cx, cy);
            shader.setLocalMatrix(matrix);
            arcPaint.setShader(shader);
            arcPaint.setStrokeWidth(stroke);
            canvas.drawArc(rectF, -90f, sweep, false, arcPaint);
            arcPaint.setShader(null);
        }

        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        textPaint.setTextSize(dp(21f));
        canvas.drawText(centerText, cx, cy + dp(6f), textPaint);

        labelPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
        labelPaint.setTextSize(dp(10f));
        canvas.drawText(getContext().getString(R.string.ring_label), cx, cy + dp(22f), labelPaint);
    }
}
