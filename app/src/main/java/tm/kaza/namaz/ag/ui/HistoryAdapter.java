package tm.kaza.namaz.ag.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import tm.kaza.namaz.ag.R;

/**
 * Адаптер списка «Последние пропуски» на главном экране.
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {

    public static class Item {
        public String key;
        public String title;
        public String subtitle;
        public int missed;
    }

    public interface OnItemClickListener {
        void onItemClick(Item item);
    }

    private final List<Item> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public HistoryAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Item> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        final Item item = items.get(position);
        h.title.setText(item.title);
        h.subtitle.setText(item.subtitle);
        h.count.setText(String.valueOf(item.missed));
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView subtitle;
        final TextView count;

        VH(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvHistoryTitle);
            subtitle = itemView.findViewById(R.id.tvHistorySub);
            count = itemView.findViewById(R.id.tvHistoryCount);
        }
    }
}
