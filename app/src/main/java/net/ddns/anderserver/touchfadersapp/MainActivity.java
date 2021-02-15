package net.ddns.anderserver.touchfadersapp;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.DisplayCutout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.illposed.osc.OSCBadDataEvent;
import com.illposed.osc.OSCBundle;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPacket;
import com.illposed.osc.OSCPacketEvent;
import com.illposed.osc.OSCPacketListener;
import com.illposed.osc.transport.udp.OSCPortIn;
import com.illposed.osc.transport.udp.OSCPortOut;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends AppCompatActivity {

	SharedPreferences sharedPreferences;

	OSCPortOut oscPortOut;
	OSCPortIn oscPortIn;
	ArrayList<BoxedVertical> faders = new ArrayList<>();
	FaderStripRecyclerViewAdapter adapter;

	private int numChannels;
	private int currentMix;

	OSCPacketListener packetListener = new OSCPacketListener() {
		@Override
		public void handlePacket (OSCPacketEvent event) {
			OSCPacket packet = event.getPacket();
			if (packet instanceof OSCMessage) {
				OSCMessage message = (OSCMessage) packet;
				if (message.getAddress().contains("/mix" + currentMix + "/fader")) {
					String[] segments = message.getAddress().split("/");
					int faderIndex = Integer.parseInt(segments[2].replaceAll("\\D+", "")) - 1; // extract only digits via RegEx
					if (0 <= faderIndex && faderIndex < 16) {
						BoxedVertical fader = faders.get(faderIndex);
						fader.setValue((int) message.getArguments().get(0));
					}
				}
			}
			if (packet instanceof OSCBundle) {
				OSCBundle bundle = (OSCBundle) packet;
				List<OSCPacket> packetList = bundle.getPackets();
				for (OSCPacket pack : packetList) {
					OSCMessage message = (OSCMessage) pack;
					Log.i("OSC message", message.getAddress() + " : " + message.getArguments().get(0).toString());
				}
			}
		}

		@Override
		public void handleBadData (OSCBadDataEvent event) {
			Log.d("OSC message", "Bad packet received");
		}
	};

	@SuppressLint("StaticFieldLeak")
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//BasicConfigurator.configure();

		numChannels = getIntent().getIntExtra(StartupActivity.EXTRA_NUM_CHANNELS, 64);
		currentMix = getIntent().getIntExtra(MixSelectActivity.EXTRA_MIX_INDEX, 0) + 1;

		setContentView(R.layout.main);

		AsyncTask.execute(this::OpenOSCPortIn);
		AsyncTask.execute(this::OpenOSCPortOut);

		RecyclerView recyclerView = findViewById(R.id.faderRecyclerView);
		adapter = new FaderStripRecyclerViewAdapter(this, numChannels, currentMix);
		adapter.setValuesChangeListener((boxedPoints, points) -> SendOSCFaderValue(adapter.getFaderIndex(boxedPoints) + 1, points));
		recyclerView.setAdapter(adapter);

	}

	@Override
	protected void onResume () {
		super.onResume();
		// Making it fullscreen...
		View frameLayout = findViewById(R.id.fullscreen_frame);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.hide();
		}
		frameLayout.setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LOW_PROFILE
						| View.SYSTEM_UI_FLAG_FULLSCREEN
						| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
						| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
						| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
		// Fullscreen done!

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
			frameLayout.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
				DisplayCutout cutout = getWindow().getDecorView().getRootWindowInsets().getDisplayCutout();
				//LinearLayout faderLayout = findViewById(R.id.faderLayout);
				//RecyclerView faderLayoutView = findViewById(R.id.faderRecyclerView);
				BoxedVertical meter = findViewById(R.id.mixMeter);
				//ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) faderLayoutView.getLayoutParams();
				ViewGroup.LayoutParams meterParams = meter.getLayoutParams();
				assert cutout != null;
				final float scale = getApplicationContext().getResources().getDisplayMetrics().density;
				int pixels = (int) (35 * scale + 0.5f);
				if (cutout.getSafeInsetLeft() == meterParams.width) return;
				meterParams.width = cutout.getSafeInsetLeft();
				if (meterParams.width == 0) {
					meterParams.width = pixels;
				}
				Handler handler = new Handler(Looper.getMainLooper());
				handler.post(() -> meter.setLayoutParams(meterParams));
				//Log.i("CUTOUT", "safeLeft: " + cutout.getSafeInsetLeft() + "  safeRight: " + cutout.getSafeInsetRight());
			});
		}
	}

	private String GetLocalIP () {
		try {
			String localAddress = "";
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
				NetworkInterface networkInterface = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = networkInterface.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLinkLocalAddress()) {
						localAddress = inetAddress.getHostAddress();
					}
				}
			}
			return localAddress;
		} catch (SocketException e) {
			e.printStackTrace();
			return null;
		}
	}

	private void OpenOSCPortIn () {
		Handler handler = new Handler(Looper.getMainLooper());
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		String localAddress = GetLocalIP();
		int oscReceivePort = sharedPreferences.getInt("receivePort", 9001);
		SocketAddress receiveSocket = new InetSocketAddress(localAddress, oscReceivePort);

		try {
			oscPortIn = new OSCPortIn(receiveSocket);
		} catch (BindException e) {
			handler.post(() -> Toast.makeText(getApplicationContext(), "Failed to bind IP to OSC!", Toast.LENGTH_SHORT).show());
		} catch (IOException e) {
			e.printStackTrace();
		}

		while (true) {
			if (oscPortIn != null) {
				oscPortIn.getDispatcher().setAlwaysDispatchingImmediately(true);
				oscPortIn.addPacketListener(packetListener);
				break;
			} else {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				}
			}
		}
		oscPortIn.startListening();
	}

	private void OpenOSCPortOut () {
		Handler handler = new Handler(Looper.getMainLooper());
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		String oscIP = sharedPreferences.getString("ipAddress", "192.168.1.2");
		int oscSendPort = sharedPreferences.getInt("sendPort", 8001);
		SocketAddress sendSocket = new InetSocketAddress(oscIP, oscSendPort);

		try {
			oscPortOut = new OSCPortOut(sendSocket);
		} catch (Exception e) {
			handler.post(() -> Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show());
		}
	}

	public void setupButtons () {
		findViewById(R.id.mix1_button).setOnClickListener(v -> selectMix(1));
		findViewById(R.id.mix2_button).setOnClickListener(v -> selectMix(2));
		findViewById(R.id.mix3_button).setOnClickListener(v -> selectMix(3));
		findViewById(R.id.mix4_button).setOnClickListener(v -> selectMix(4));
		findViewById(R.id.mix5_button).setOnClickListener(v -> selectMix(5));
		findViewById(R.id.mix6_button).setOnClickListener(v -> selectMix(6));
	}

	public void setupFaders () {
		faders.add(findViewById(R.id.fader1));
		faders.add(findViewById(R.id.fader2));
		faders.add(findViewById(R.id.fader3));
		faders.add(findViewById(R.id.fader4));
		faders.add(findViewById(R.id.fader5));
		faders.add(findViewById(R.id.fader6));
		faders.add(findViewById(R.id.fader7));
		faders.add(findViewById(R.id.fader8));
		faders.add(findViewById(R.id.fader9));
		faders.add(findViewById(R.id.fader10));
		faders.add(findViewById(R.id.fader11));
		faders.add(findViewById(R.id.fader12));
		faders.add(findViewById(R.id.fader13));
		faders.add(findViewById(R.id.fader14));
		faders.add(findViewById(R.id.fader15));
		faders.add(findViewById(R.id.fader16));

		/*
		faders.get(0).setGradientEnd(getApplicationContext().getColor(R.color.mix1));
		faders.get(0).setGradientStart(getApplicationContext().getColor(R.color.mix1_lighter));

		faders.get(1).setGradientEnd(getApplicationContext().getColor(R.color.mix2));
		faders.get(1).setGradientStart(getApplicationContext().getColor(R.color.mix2_lighter));

		faders.get(2).setGradientEnd(getApplicationContext().getColor(R.color.mix3));
		faders.get(2).setGradientStart(getApplicationContext().getColor(R.color.mix3_lighter));

		faders.get(3).setGradientEnd(getApplicationContext().getColor(R.color.mix4));
		faders.get(3).setGradientStart(getApplicationContext().getColor(R.color.mix4_lighter));

		faders.get(4).setGradientEnd(getApplicationContext().getColor(R.color.mix5));
		faders.get(4).setGradientStart(getApplicationContext().getColor(R.color.mix5_lighter));

		faders.get(5).setGradientEnd(getApplicationContext().getColor(R.color.mix6));
		faders.get(5).setGradientStart(getApplicationContext().getColor(R.color.mix6_lighter));
		 */

		for (BoxedVertical fader: faders) {
			fader.setOnBoxedPointsChangeListener((boxedPoints, points) -> SendOSCFaderValue(faders.indexOf(fader) + 1, points));
		}
	}

	public void selectMix (int mix) {
		currentMix = mix;
		SendOSCGetMix(mix);
		int[] colourArray = getApplicationContext().getResources().getIntArray(R.array.mix_colours);
		int[] colourArrayLighter = getApplicationContext().getResources().getIntArray(R.array.mix_colours_lighter);

		for (BoxedVertical fader : faders) {
			fader.setGradientEnd(colourArray[mix - 1]);
			fader.setGradientStart(colourArrayLighter[mix - 1]);
		}
	}

	public void SendOSCFaderValue (int fader, int faderValue) {
		AsyncTask.execute(() -> {
			Handler handler = new Handler(Looper.getMainLooper());

			if (oscPortOut != null) {
				ArrayList<Object> arguments = new ArrayList<>();
				arguments.add(faderValue);
				OSCMessage message = new OSCMessage("/mix" + currentMix + "/fader" + fader, arguments);

				try {
					oscPortOut.send(message);
					Thread.sleep(5);
					//Log.i("OSC message", message.getAddress() + " : " + message.getArguments().get(0).toString());
				} catch (Exception e) {
					handler.post(() -> Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show());
				}
			}
		});
	}

	public void SendOSCGetMix (int mix) {
		AsyncTask.execute(() -> {
			Handler handler = new Handler(Looper.getMainLooper());

			if (oscPortOut != null) {
				ArrayList<Object> arguments = new ArrayList<>();
				arguments.add(1);
				OSCMessage message = new OSCMessage("/mix" + mix, arguments);

				try {
					oscPortOut.send(message);
					//Log.i("OSC message", message.getAddress() + " : " + message.getArguments().get(0).toString());
				} catch (Exception e) {
					handler.post(() -> Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show());
				}
			}
		});
	}

}