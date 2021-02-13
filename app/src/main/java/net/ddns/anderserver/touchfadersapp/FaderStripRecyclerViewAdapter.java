package net.ddns.anderserver.touchfadersapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class FaderStripRecyclerViewAdapter extends RecyclerView.Adapter<FaderStripRecyclerViewAdapter.ViewHolder> {

    private final int currentMix;
    private final ArrayList<Integer> faderLevels = new ArrayList<>();
    private final ArrayList<BoxedVertical> faders = new ArrayList<>();
    private final LayoutInflater inflator;
    private BoxedVertical.OnValuesChangeListener valuesChangeListener;

    int[] colourArray;
    int[] colourArrayLighter;

    public FaderStripRecyclerViewAdapter(Context context, int numChannels, int currentMix) {
        this.inflator = LayoutInflater.from(context);
        this.currentMix = currentMix;
        for (int channel = 0; channel < numChannels; channel++ ){
            faderLevels.add(823);
            faders.add(new BoxedVertical(context, null));
        }
        colourArray = context.getResources().getIntArray(R.array.mix_colours);
        colourArrayLighter = context.getResources().getIntArray(R.array.mix_colours_lighter);
    }

    @NonNull
    @Override
    public FaderStripRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflator.inflate(R.layout.recyclerview_fader_strip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (faders.get(position) != holder.fader) {
            faders.set(position, holder.fader);
        }
        int level = faderLevels.get(position);
        holder.fader.setValue(level);
        holder.fader.setGradientEnd(colourArray[currentMix - 1]);
        holder.fader.setGradientStart(colourArrayLighter[currentMix - 1]);
        String number = String.valueOf((position + 1));
        holder.channelNumber.setText(number);
    }

    @Override
    public int getItemCount() {
        //return faderLevels.size();
        return faders.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements BoxedVertical.OnValuesChangeListener {

        BoxedVertical fader;
        TextView channelNumber;

        ViewHolder (View itemView) {
            super(itemView);
            fader = itemView.findViewById(R.id.fader);
            fader.setOnBoxedPointsChangeListener(this);
            channelNumber = itemView.findViewById(R.id.channelNumber);
        }

        @Override
        public void onPointsChanged(BoxedVertical boxedPoints, int points) {
            faderLevels.set(faders.indexOf(boxedPoints), points);
            if (valuesChangeListener != null) valuesChangeListener.onPointsChanged(boxedPoints, points);
        }
    }

    int getFaderIndex (BoxedVertical fader) { return faders.indexOf(fader); }

    void setValuesChangeListener (BoxedVertical.OnValuesChangeListener listener) {
        valuesChangeListener = listener;
    }

    public interface FaderValueChangedListener {
        void onValueChanged(View view, int index);
    }
}
