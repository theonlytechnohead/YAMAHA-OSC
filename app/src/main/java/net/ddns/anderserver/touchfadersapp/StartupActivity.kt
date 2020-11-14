package net.ddns.anderserver.touchfadersapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class StartupActivity : AppCompatActivity() {

    var sharedPreferences: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.startup)

        // Making it fullscreen...
        val startupLayout = findViewById<View>(R.id.startupLayout)
        val actionBar = supportActionBar
        actionBar?.hide()
        startupLayout.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        // Fullscreen done!
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        checkNetwork()
        val ipEditText = findViewById<EditText>(R.id.ipEditText)
        /*
        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end,
                                       android.text.Spanned dest, int dstart, int dend) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart)
                            + source.subSequence(start, end)
                            + destTxt.substring(dend);
                    if (!resultingTxt
                            .matches("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    } else {
                        String[] splits = resultingTxt.split("\\.");
                        for (int i = 0; i < splits.length; i++) {
                            if (Integer.valueOf(splits[i]) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            }

        };
         */
        ipEditText.setText(sharedPreferences?.getString("ipAddress", "192.168.1.2"))
        val startButton = findViewById<Button>(R.id.startButton)
        ipEditText.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                val view = this.currentFocus
                if (view != null) {
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                }
                val handler = Handler(Looper.getMainLooper())
                handler.post { startButton.performClick() }
                return@setOnEditorActionListener true
            }
            false
        }
        ipEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                sharedPreferences?.edit()?.putString("ipAddress", s.toString())?.apply()
            }
        })
    }

    private fun checkNetwork() {
        val startButton = findViewById<Button>(R.id.startButton)
        if (isConnected(applicationContext)) {
            startButton.setOnClickListener { startActivity(Intent(this, FullscreenActivity::class.java)) }
        } else {
            startButton.setOnClickListener { checkNetwork() }
            Toast.makeText(this, "You're not connected to a network!", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun isConnected(context: Context): Boolean {
            val cm = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = cm.activeNetworkInfo
            return networkInfo != null && networkInfo.isAvailable && networkInfo.isConnected
        }
    }
}