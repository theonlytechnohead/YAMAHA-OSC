package net.ddns.anderserver.yamahaosc;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.lukelorusso.verticalseekbar.VerticalSeekBar;

import java.io.Console;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {

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
				SendOSCFaderValue(integer);
				return null;
			}
		});
		//startActivity(new Intent(this, Mix1Activity.class));
	}

	public void SendOSCFaderValue(int faderValue) {
		Log.d("Fader", "1: " + faderValue);
	}

}