package net.ddns.anderserver.touchfadersapp;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class MixSelectActivity extends AppCompatActivity implements MixSelectRecyclerViewAdapter.MixButtonClickListener {

    public static String EXTRA_MIX_INDEX = "EXTRA_MIX_INDEX";

    private String ipAddress;
    private byte receivePort;
    private byte sendPort;
    private byte numChannels;
    private byte numMixes;

    MixSelectRecyclerViewAdapter adapter;

    private boolean disconnect = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.mix_selection);

        ipAddress = getIntent().getStringExtra(StartupActivity.EXTRA_IP_ADDRESS);
        receivePort = getIntent().getByteExtra(StartupActivity.EXTRA_RECEIVE_PORT, (byte) 0x0);
        sendPort = getIntent().getByteExtra(StartupActivity.EXTRA_SEND_PORT, (byte) 0x0);
        numChannels = getIntent().getByteExtra(StartupActivity.EXTRA_NUM_CHANNELS, (byte) 0x40);
        numMixes = getIntent().getByteExtra(StartupActivity.EXTRA_NUM_MIXES, (byte) 0x6);

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

        disconnect = true;

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
        disconnect = false;
        int mix = index + 1;
        //Toast.makeText(this, "You clicked " + adapter.getItem(index) + " which is mix " + mix, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getBaseContext(), MainActivity.class);
        intent.putExtra(StartupActivity.EXTRA_IP_ADDRESS, ipAddress);
        intent.putExtra(StartupActivity.EXTRA_RECEIVE_PORT, receivePort);
        intent.putExtra(StartupActivity.EXTRA_SEND_PORT, sendPort);
        intent.putExtra(StartupActivity.EXTRA_NUM_CHANNELS, numChannels);
        intent.putExtra(MixSelectActivity.EXTRA_MIX_INDEX, index);
        startActivity(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (disconnect) Disconnect();
    }

    private void Disconnect() {
        AsyncTask.execute(() -> {
            byte[] data = Build.MODEL.getBytes();
            InetAddress address = null;
            try {
                address = InetAddress.getByName(ipAddress);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            DatagramPacket packet = new DatagramPacket(data, data.length, address, 8878);
            try {
                DatagramSocket socket = new DatagramSocket();
                socket.send(packet);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
