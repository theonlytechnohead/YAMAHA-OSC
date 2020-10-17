package net.ddns.anderserver.yamahaosc;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPacket;
import com.illposed.osc.OSCSerializeException;
import com.illposed.osc.transport.udp.OSCPortOut;
import com.lukelorusso.verticalseekbar.VerticalSeekBar;

import java.io.Console;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static java.lang.Thread.sleep;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {

	private String myIP = "192.168.1.50";
	private int myPort = 12345;
	OSCPortOut oscPortOut;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_fullscreen);

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

		Button mix1_button = findViewById(R.id.mix1_button);
		mix1_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//FullscreenActivity.this.startActivity(new Intent(FullscreenActivity.this, Mix1Activity.class));
			}
		});

		VerticalSeekBar fader1 = findViewById(R.id.fader1);
		fader1.setClickToSetProgress(true);
		fader1.setOnProgressChangeListener(new Function1<Integer, Unit>() {
			@Override
			public Unit invoke(Integer integer) {
				try {
					SendOSCFaderValue(integer);
				} catch (IOException | OSCSerializeException e) {
					e.printStackTrace();
				}
				return null;
			}
		});
		//startActivity(new Intent(this, Mix1Activity.class));



		oscThread.start();
		//Toast.makeText(getApplicationContext(), "Starting...", Toast.LENGTH_SHORT).show();
	}

	public void SendOSCFaderValue(int faderValue) throws IOException, OSCSerializeException {
		//android.os.Debug.waitForDebugger();
		//Log.d("Fader", "1: " + faderValue);
		ArrayList<Object> moreThingsToSend = new ArrayList<Object>();
		moreThingsToSend.add("Hello World2");
		moreThingsToSend.add(123456);
		moreThingsToSend.add(12.345);

		OSCMessage message2 = new OSCMessage("/mix1/fader1", moreThingsToSend);
		if (oscPortOut != null) {
			try {
				oscPortOut.send(message2);
				Log.d("OSC", "Sent message!");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}



	private Thread oscThread = new Thread() {
		@Override
		public void run() {
			/* The first part of the run() method initializes the OSCPortOut for sending messages.
			 *
			 * For more advanced apps, where you want to change the address during runtime, you will want
			 * to have this section in a different thread, but since we won't be changing addresses here,
			 * we only have to initialize the address once.
			 */
			Handler handler = new Handler(Looper.getMainLooper());

			try {
				// Connect to some IP address and port
				oscPortOut = new OSCPortOut(InetAddress.getByName(myIP), myPort);
			} catch(UnknownHostException e) {
				// Error handling when your IP isn't found
				handler.post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getApplicationContext(), "UnknownHostException", Toast.LENGTH_SHORT).show();
					}
				});
				return;
			} catch(Exception e) {
				// Error handling for any other errors
				handler.post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
					}
				});
				return;
			}


			/* The second part of the run() method loops infinitely and sends messages every 500
			 * milliseconds.
			 */
			while (true) {
				handler.post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getApplicationContext(), "Looped", Toast.LENGTH_SHORT).show();
					}
				});
				if (oscPortOut != null) {
					// Creating the message
					Object[] thingsToSend = new Object[3];
					thingsToSend[0] = "Hello World";
					thingsToSend[1] = 12345;
					thingsToSend[2] = 1.2345;

					/* The version of JavaOSC from the Maven Repository is slightly different from the one
					 * from the download link on the main website at the time of writing this tutorial.
					 *
					 * The Maven Repository version (used here), takes a Collection, which is why we need
					 * Arrays.asList(thingsToSend).
					 *
					 * If you're using the downloadable version for some reason, you should switch the
					 * commented and uncommented lines for message below
					 */
					OSCMessage message = new OSCMessage("/mix1/fader1", Arrays.asList(thingsToSend));
					// OSCMessage message = new OSCMessage(myIP, thingsToSend);


					/* NOTE: Since this version of JavaOSC uses Collections, we can actually use ArrayLists,
					 * or any other class that implements the Collection interface. The following code is
					 * valid for this version.
					 *
					 * The benefit of using an ArrayList is that you don't have to know how much information
					 * you are sending ahead of time. You can add things to the end of an ArrayList, but not
					 * to an Array.
					 *
					 * If you want to use this code with the downloadable version, you should switch the
					 * commented and uncommented lines for message2
					 */
					ArrayList<Object> moreThingsToSend = new ArrayList<Object>();
					moreThingsToSend.add("Hello World2");
					moreThingsToSend.add(123456);
					moreThingsToSend.add(12.345);

					OSCMessage message2 = new OSCMessage("/mix1/fader1", moreThingsToSend);
					//OSCMessage message2 = new OSCMessage(myIP, moreThingsToSend.toArray());

					try {
						// Send the messages
						oscPortOut.send(message);
						oscPortOut.send(message2);
						handler.post(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(getApplicationContext(), "OSC!?", Toast.LENGTH_SHORT).show();
							}
						});
						// Pause for half a second
						sleep(500);
					} catch (Exception e) {
						// Error handling for some error
					}
				}
			}
		}
	};

}