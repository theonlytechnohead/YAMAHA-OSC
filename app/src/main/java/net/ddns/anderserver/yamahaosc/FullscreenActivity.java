package net.ddns.anderserver.yamahaosc;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCSerializeException;
import com.illposed.osc.transport.udp.OSCPortIn;
import com.illposed.osc.transport.udp.OSCPortOut;
import com.lukelorusso.verticalseekbar.VerticalSeekBar;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static java.lang.Thread.sleep;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {

	private String oscIP = "192.168.1.50";
	private int oscSendPort = 8000;
	OSCPortOut oscPortOut;
	OSCPortIn oscPortIn;

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
				// idk implement later
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
	}

	public void SendOSCFaderValue(int faderValue) throws IOException, OSCSerializeException {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				Handler handler = new Handler(Looper.getMainLooper());

				try {
					oscPortOut = new OSCPortOut(InetAddress.getByName(oscIP), oscSendPort);
				} catch(Exception e) {
					handler.post(() -> Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show());
					return;
				}

				if (oscPortOut != null) {
					ArrayList<Object> arguments = new ArrayList<>();
					arguments.add(faderValue);
					handler.post(() -> {
						TextView textView = findViewById(R.id.db_text);
						textView.setText(Integer.toString(faderValue));
					});
					OSCMessage message = new OSCMessage("/mix1/fader1", arguments);

					try {
						oscPortOut.send(message);
					} catch (Exception e) {
						// Error handling for some error
					}
				}
			}
		});
		t.start();
	}

}