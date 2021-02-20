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
import androidx.recyclerview.widget.RecyclerView;

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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends AppCompatActivity {

	SharedPreferences sharedPreferences;

	OSCPortOut oscPortOut;
	OSCPortIn oscPortIn;
	ArrayList<BoxedVertical> faders = new ArrayList<>();
	RecyclerView recyclerView;
	FaderStripRecyclerViewAdapter adapter;
	BoxedVertical mixMeter;

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
					if (0 <= faderIndex && faderIndex < adapter.getItemCount()) {
						adapter.setFaderLevel(faderIndex, (int) message.getArguments().get(0));
						Handler handler = new Handler(getMainLooper());
						handler.post(() -> adapter.notifyDataSetChanged());
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

		mixMeter = findViewById(R.id.mixMeter);

		new Thread(new ClientListen()).start();
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
		adapter = new FaderStripRecyclerViewAdapter(this, numChannels, currentMix);
		adapter.setValuesChangeListener((view, index, boxedVertical, points) -> SendOSCFaderValue(index + 1, points));
		recyclerView = findViewById(R.id.faderRecyclerView);
		recyclerView.setAdapter(adapter);

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
			frameLayout.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
				DisplayCutout cutout = getWindow().getDecorView().getRootWindowInsets().getDisplayCutout();
				//RecyclerView faderLayoutView = findViewById(R.id.faderRecyclerView);
				//RecyclerView faderLayoutView = recyclerView;
				BoxedVertical meter = findViewById(R.id.mixMeter);
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

	public static String getBroadcast() {
		String found_bcast_address = null;
		System.setProperty("java.net.preferIPv4Stack", "true");
		try {
			Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces();
			while (niEnum.hasMoreElements()) {
				NetworkInterface ni = niEnum.nextElement();
				if (!ni.isLoopback()) {
					for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
						found_bcast_address = interfaceAddress.getBroadcast().toString();
						found_bcast_address = found_bcast_address.substring(1);
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}

		return found_bcast_address;
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

	public class ClientListen implements Runnable {
		@Override
		public void run() {
			Handler handler = new Handler(Looper.getMainLooper());
			DatagramSocket socket = null;
			boolean run = true;
			while (run) {
				try {
					byte[] recvBuf = new byte[16];
					if (socket == null) {
						socket = new DatagramSocket(8874);
						socket.setBroadcast(true);
					}
					if (socket.isClosed()) {
						socket = new DatagramSocket(8874);
						socket.setBroadcast(true);
					}
					DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
					socket.receive(packet);
					//String senderIP = packet.getAddress().getHostAddress();

					byte[] meteringData = packet.getData();
					byte meter = meteringData[currentMix - 1];
					handler.post(() -> mixMeter.setValue(meter));
				} catch (IOException e) {
					Log.e("UDP client has IOException", "error: ", e);
					run = false;
				}
			}
		}
	}

}