package net.ddns.anderserver.touchfadersapp;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class Mix1Activity extends AppCompatActivity {
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mix1);
		// Making it fullscreen...
		View frameLayout = findViewById(R.id.mix1_layout);
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
	}
}
