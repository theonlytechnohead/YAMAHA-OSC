package net.ddns.anderserver.touchfadersapp;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.DisplayCutout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MixSelectActivity extends AppCompatActivity implements MixSelectRecyclerViewAdapter.MixButtonClickListener {

    public static String EXTRA_MIX_INDEX = "EXTRA_MIX_INDEX";

    private int numChannels;
    private int numMixes;

    MixSelectRecyclerViewAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.mix_selection);

        numChannels = getIntent().getIntExtra(StartupActivity.EXTRA_NUM_CHANNELS, 64);
        numMixes = getIntent().getIntExtra(StartupActivity.EXTRA_NUM_MIXES, 6);

    }

    @Override
    protected void onResume () {
        super.onResume();
        // Making it fullscreen...
        View mixLayout = findViewById(R.id.mix_select_layout);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mixLayout.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        // Fullscreen done!

        // data to populate the RecyclerView with
        ArrayList<String> mixNames = new ArrayList<>();
        for (int i = 1; i <= numMixes; i ++) mixNames.add("Mix " + i);

        // set up the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.mix_select_recyclerview);
        adapter = new MixSelectRecyclerViewAdapter(this, mixNames);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(View view, int index) {
        int mix = index + 1;
        //Toast.makeText(this, "You clicked " + adapter.getItem(index) + " which is mix " + mix, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getBaseContext(), MainActivity.class);
        intent.putExtra(StartupActivity.EXTRA_NUM_CHANNELS, numChannels);
        intent.putExtra(MixSelectActivity.EXTRA_MIX_INDEX, index);
        startActivity(intent);
    }
}
