package net.ddns.anderserver.touchfadersapp;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.os.Debug;
import android.util.Log;
import android.util.TypedValue;
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
    private final ArrayList<String> channelNames = new ArrayList<>();
    private final ArrayList<String> channelPatchIn = new ArrayList<>();
    private FaderValueChangedListener faderValueChangedListener;

    int[] colourArray;
    int[] colourArrayLighter;

    public FaderStripRecyclerViewAdapter(Context context, int numChannels, int currentMix) {
        this.context = context;
        this.currentMix = currentMix;
        TypedArray array = context.obtainStyledAttributes(R.style.Widget_Theme_TouchFaders_BoxedVerticalSeekBar, new int[]{R.attr.startValue});
        for (int channel = 0; channel < numChannels; channel++ ){
            faderLevels.add(array.getInt(0, 623));
            channelNames.add("CH " + (channel + 1));
            channelPatchIn.add(String.format("IN %02d", channel + 1));
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
        if (currentMix <= colourArray.length) {
            holder.fader.setGradientEnd(colourArray[currentMix - 1]);
        }
        if (currentMix <= colourArrayLighter.length) {
            holder.fader.setGradientStart(colourArrayLighter[currentMix - 1]);
        }
        String number = String.valueOf((position + 1));
        holder.channelNumber.setText(number);
        holder.channelPatch.setText(channelPatchIn.get(position));
        holder.channelName.setText(channelNames.get(position));

        // Set channel name sizing, per channel name length
        if (holder.channelName.getText().length() <= 3) {
            holder.channelName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        }
        if (holder.channelName.getText().length() == 4) {
            holder.channelName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        }
        if (holder.channelName.getText().length() == 5) {
            holder.channelName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        }
        if (holder.channelName.getText().length() == 6) {
            holder.channelName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
            if (position == faderLevels.size() - 1) {
                DisplayCutout cutout = getActivity(context).getWindow().getDecorView().getRootWindowInsets().getDisplayCutout();
                if (cutout != null) marginLayoutParams.rightMargin = cutout.getSafeInsetRight();
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
        TextView channelPatch;
        TextView channelName;

        FaderStripViewHolder(View itemView) {
            super(itemView);
            fader = itemView.findViewById(R.id.fader);
            fader.setOnBoxedPointsChangeListener((boxedPoints, points) -> {
                faderLevels.set(position, points);
                faderValueChangedListener.onValueChanged(boxedPoints.getRootView(), position, boxedPoints, points);
            });
            channelNumber = itemView.findViewById(R.id.channelNumber);
            channelPatch = itemView.findViewById(R.id.channelPatch);
            channelName = itemView.findViewById(R.id.channelName);
        }

        @Override
        public void onValueChanged(View view, int index, BoxedVertical boxedVertical, int points) {
            if (faderValueChangedListener != null) faderValueChangedListener.onValueChanged(view, index, boxedVertical, points);
        }
    }

    void setFaderLevel (int index, int level) {
        faderLevels.set(index, level);
    }

    void setChannelPatchIn (int index, String patchIn) {
        channelPatchIn.set(index, patchIn);
    }

    void setChannelName (int index, String name) {
        channelNames.set(index, name);
    }

    void setValuesChangeListener (FaderValueChangedListener listener) {
        faderValueChangedListener = listener;
    }

    public interface FaderValueChangedListener {
        void onValueChanged(View view, int index, BoxedVertical boxedVertical, int points);
    }
}
