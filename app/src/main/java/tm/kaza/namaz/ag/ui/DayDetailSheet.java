package tm.kaza.namaz.ag.ui;

import android.content.Context;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.Calendar;

import tm.kaza.namaz.ag.R;
import tm.kaza.namaz.ag.data.DataStore;
import tm.kaza.namaz.ag.data.PrayerType;
import tm.kaza.namaz.ag.util.DateUtils;

/**
 * Нижний лист с деталями конкретного дня: все намазы, заметка и быстрые действия.
 */
public class DayDetailSheet {

    public interface Listener {
        void onDayChanged();
    }

    public static void show(final Context context, final DataStore ds,
                            final Calendar date, final Listener listener) {
        final String key = DataStore.dateKey(date);
        final BottomSheetDialog dialog = new BottomSheetDialog(
                context, R.style.ThemeOverlay_KazaNamaz_BottomSheet);

        final View root = LayoutInflater.from(context)
                .inflate(R.layout.bottom_sheet_day, null, false);

        TextView title = root.findViewById(R.id.tvSheetTitle);
        final TextView sub = root.findViewById(R.id.tvSheetSub);
        final LinearLayout container = root.findViewById(R.id.sheetPrayerContainer);
        final EditText note = root.findViewById(R.id.etSheetNote);

        title.setText(DateUtils.formatFull(context, date));
        note.setText(ds.getNote(key));

        final Runnable[] renderRows = new Runnable[1];
        renderRows[0] = new Runnable() {
            @Override
            public void run() {
                container.removeAllViews();
                int[] flags = ds.getFlags(key);
                for (PrayerType p : PrayerType.values()) {
                    View row = PrayerRowFactory.build(context, container, p, flags,
                            (prayerIndex, newState) -> {
                                ds.setStatus(key, prayerIndex, newState);
                                rowHaptic(container);
                                if (listener != null) listener.onDayChanged();
                                container.post(renderRows[0]);
                            });
                    container.addView(row);
                }
                sub.setText(context.getString(R.string.sheet_sub,
                        ds.countMissed(key), ds.countDone(key)));
            }
        };
        renderRows[0].run();

        TextView allDone = root.findViewById(R.id.btnSheetAllDone);
        allDone.setOnClickListener(v -> {
            ds.markAllDone(key);
            rowHaptic(allDone);
            if (listener != null) listener.onDayChanged();
            container.post(renderRows[0]);
        });

        TextView reset = root.findViewById(R.id.btnSheetReset);
        reset.setOnClickListener(v -> {
            ds.clearDay(key);
            note.setText("");
            rowHaptic(reset);
            if (listener != null) listener.onDayChanged();
            container.post(renderRows[0]);
        });

        TextView saveNote = root.findViewById(R.id.btnSheetSaveNote);
        saveNote.setOnClickListener(v -> {
            ds.setNote(key, note.getText().toString());
            rowHaptic(saveNote);
            Toast.makeText(context, R.string.note_saved, Toast.LENGTH_SHORT).show();
        });

        dialog.setContentView(root);
        dialog.setOnDismissListener(d -> {
            if (listener != null) listener.onDayChanged();
        });
        dialog.show();
    }

    private static void rowHaptic(View v) {
        v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
    }
}
