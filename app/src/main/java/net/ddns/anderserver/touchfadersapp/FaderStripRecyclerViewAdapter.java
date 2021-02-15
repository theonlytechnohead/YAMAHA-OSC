package net.ddns.anderserver.touchfadersapp;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.os.Debug;
import android.util.Log;
import android.view.DisplayCutout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class FaderStripRecyclerViewAdapter extends RecyclerView.Adapter<FaderStripRecyclerViewAdapter.FaderStripViewHolder> {

    private Context context;
    public static Activity getActivity(Context context) {
        if (context == null) return null;
        if (context instanceof Activity) return (Activity) context;
        if (context instanceof ContextWrapper) return getActivity(((ContextWrapper)context).getBaseContext());
        return null;
    }

    private final int currentMix;
    private final ArrayList<Integer> faderLevels = new ArrayList<>();
    //private BoxedVertical.OnValuesChangeListener valuesChangeListener;
    private FaderValueChangedListener faderValueChangedListener;

    int[] colourArray;
    int[] colourArrayLighter;

    public FaderStripRecyclerViewAdapter(Context context, int numChannels, int currentMix) {
        this.context = context;
        this.currentMix = currentMix;
        TypedArray array = context.obtainStyledAttributes(R.style.Widget_Theme_TouchFaders_BoxedVerticalSeekBar, new int[]{R.attr.startValue});
        for (int channel = 0; channel < numChannels; channel++ ){
            faderLevels.add(array.getInt(0, 623));
        }
        array.recycle();
        colourArray = context.getResources().getIntArray(R.array.mix_colours);
        colourArrayLighter = context.getResources().getIntArray(R.array.mix_colours_lighter);
    }

    @NonNull
    @Override
    public FaderStripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_fader_strip, parent, false);
        return new FaderStripViewHolder(view);
    }

    // Gets called every time a ViewHolder is reused (with a new position)
    @Override
    public void onBindViewHolder(@NonNull FaderStripViewHolder holder, int position) {
        holder.position = position;
        holder.fader.setValue(faderLevels.get(position));
        holder.fader.setGradientEnd(colourArray[currentMix - 1]);
        holder.fader.setGradientStart(colourArrayLighter[currentMix - 1]);
        String number = String.valueOf((position + 1));
        holder.channelNumber.setText(number);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
            if (position == faderLevels.size() - 1) {
                DisplayCutout cutout = getActivity(context).getWindow().getDecorView().getRootWindowInsets().getDisplayCutout();
                marginLayoutParams.rightMargin = cutout.getSafeInsetRight();
            } else {
                marginLayoutParams.rightMargin = 0;
            }
            holder.itemView.setLayoutParams(marginLayoutParams);
        }
    }

    @Override
    public int getItemCount() {
        return faderLevels.size();
    }

    public class FaderStripViewHolder extends RecyclerView.ViewHolder implements FaderStripRecyclerViewAdapter.FaderValueChangedListener {

        int position;
        BoxedVertical fader;
        TextView channelNumber;

        FaderStripViewHolder(View itemView) {
            super(itemView);
            fader = itemView.findViewById(R.id.fader);
            fader.setOnBoxedPointsChangeListener((boxedPoints, points) -> faderValueChangedListener.onValueChanged(boxedPoints.getRootView(), position, boxedPoints, points));
            channelNumber = itemView.findViewById(R.id.channelNumber);
        }

        @Override
        public void onValueChanged(View view, int index, BoxedVertical boxedVertical, int points) {
            faderLevels.set(position, points);
        }
    }

    void setValuesChangeListener (FaderValueChangedListener listener) {
        faderValueChangedListener = listener;
    }

    public interface FaderValueChangedListener {
        void onValueChanged(View view, int index, BoxedVertical boxedVertical, int points);
    }
}
