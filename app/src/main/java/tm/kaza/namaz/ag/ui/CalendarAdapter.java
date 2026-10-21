package tm.kaza.namaz.ag.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import tm.kaza.namaz.ag.R;
import tm.kaza.namaz.ag.util.DateUtils;

/**
 * Адаптер сетки календаря (7 колонок, 42 ячейки).
 * Дни с пропущенными намазами помечаются красным кружком с числом,
 * полностью выполненные дни — зелёной точкой, сегодня — золотой рамкой.
 */
public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.VH> {

    public static class Day {
        public Calendar date;
        public int missed;
        public int done;
        public boolean inMonth;
        public boolean isToday;
    }

    public interface OnDayClickListener {
        void onDayClick(Calendar date);
    }

    private final List<Day> days = new ArrayList<>();
    private final OnDayClickListener listener;
    private Calendar selected;

    public CalendarAdapter(OnDayClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<Day> data, Calendar selectedDate) {
        days.clear();
        days.addAll(data);
        this.selected = selectedDate;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Day d = days.get(position);
        if (d == null || d.date == null) {
            h.number.setText("");
            h.badge.setVisibility(View.INVISIBLE);
            h.dot.setVisibility(View.INVISIBLE);
            h.itemView.setBackgroundResource(R.drawable.bg_day_cell);
            h.itemView.setClickable(false);
            return;
        }

        h.number.setText(String.valueOf(d.date.get(Calendar.DAY_OF_MONTH)));

        boolean isSelected = selected != null
                && d.date.get(Calendar.YEAR) == selected.get(Calendar.YEAR)
                && d.date.get(Calendar.DAY_OF_YEAR) == selected.get(Calendar.DAY_OF_YEAR);

        if (isSelected) {
            h.itemView.setBackgroundResource(R.drawable.bg_day_selected);
            h.number.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.text_on_primary));
        } else if (d.isToday) {
            h.itemView.setBackgroundResource(R.drawable.bg_day_today);
            h.number.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.accent_gold));
        } else {
            h.itemView.setBackgroundResource(R.drawable.bg_day_cell);
            h.number.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.text_primary));
        }

        if (!d.inMonth) {
            h.number.setAlpha(0.35f);
        } else {
            h.number.setAlpha(1f);
        }

        // Красный кружок с количеством пропущенных
        if (d.inMonth && d.missed > 0) {
            h.badge.setVisibility(View.VISIBLE);
            h.badge.setText(String.valueOf(d.missed));
            h.dot.setVisibility(View.INVISIBLE);
        } else if (d.inMonth && d.done >= 6 && d.missed == 0) {
            // День выполнен полностью
            h.badge.setVisibility(View.INVISIBLE);
            h.dot.setVisibility(View.VISIBLE);
        } else {
            h.badge.setVisibility(View.INVISIBLE);
            h.dot.setVisibility(View.INVISIBLE);
        }

        h.itemView.setClickable(d.inMonth);
        h.itemView.setOnClickListener(v -> {
            if (listener != null && d.inMonth) {
                listener.onDayClick(d.date);
            }
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView number;
        final TextView badge;
        final View dot;

        VH(View itemView) {
            super(itemView);
            number = itemView.findViewById(R.id.tvDayNumber);
            badge = itemView.findViewById(R.id.tvBadge);
            dot = itemView.findViewById(R.id.dotGreen);
        }
    }
}
