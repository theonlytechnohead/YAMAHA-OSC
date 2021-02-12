package net.ddns.anderserver.touchfadersapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MixSelectRecyclerViewAdapter extends RecyclerView.Adapter<MixSelectRecyclerViewAdapter.ViewHolder> {

    private List<String> mixData;
    private LayoutInflater inflator;
    private MixButtonClickListener clickListener;

    MixSelectRecyclerViewAdapter(Context context, List<String> data) {
        this.inflator = LayoutInflater.from(context);
        this.mixData = data;
    }

    @NonNull
    @Override
    public MixSelectRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflator.inflate(R.layout.recyclerview_mix_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MixSelectRecyclerViewAdapter.ViewHolder holder, int position) {
        String mix = mixData.get(position);
        //holder.mixTextView.setText(mix);
        holder.mixSelectButton.setText(mix);
    }

    @Override
    public int getItemCount() {
        return mixData.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        //TextView mixTextView;
        Button mixSelectButton;

        ViewHolder(View itemView) {
            super(itemView);
            //mixTextView = itemView.findViewById(R.id.mix_select_text);
            //mixTextView.setOnClickListener(this);
            mixSelectButton = itemView.findViewById(R.id.mix_select_button);
            mixSelectButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (clickListener != null) clickListener.onItemClick(v, getAdapterPosition());
        }
    }

    // convenience method for getting data at click position
    String getItem(int id) {
        return mixData.get(id);
    }

    // allows clicks events to be caught
    void setClickListener(MixButtonClickListener onClickListener) {
        this.clickListener = onClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface MixButtonClickListener {
        void onItemClick(View view, int index);
    }
}
