package com.placyd.horrah;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Date;

public class FullscreenActivity extends AppCompatActivity {

    private static final String TAG = "FullscreenActivity";
    // Define the custom action used by the HyacinthLauncherActivity bridge
    private static final String HYACINTH_ACTION = "com.placyd.horrah.ACTION_SHOW_HYACINTH_COUNTDOWN";

    private TextView clockText;
    private Handler clockHandler = new Handler(Looper.getMainLooper());
    private Runnable clockRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);

        // Hide system bars for a true fullscreen experience
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN      // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);


        clockText = findViewById(R.id.clock_text);
        Button hyacinthButton = findViewById(R.id.hyacinth_button);

        // Clock update every second
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                @SuppressLint("SimpleDateFormat")
                String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                clockText.setText(currentTime);
                clockHandler.postDelayed(this, 1000);
            }
        };
        clockHandler.post(clockRunnable);

        // RE-ADDED: Hover warning toast (Uses MotionEvent for broader compatibility)
        hyacinthButton.setOnHoverListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
                // Ensure Toast runs on the main thread (though generally not strictly needed for hover listeners)
                runOnUiThread(() -> Toast.makeText(this, "Don't say I didn't warn you", Toast.LENGTH_SHORT).show());
            }
            return false;
        });


        // Click to launch ProjectHyacinth countdown via the HyacinthLauncherActivity bridge
        hyacinthButton.setOnClickListener(v -> {
            Log.d(TAG, "Hyacinth button clicked. Launching bridge activity via implicit intent.");

            // FIX: Use the implicit intent targeting the local HyacinthLauncherActivity.
            // This is the safest, most foolproof method, as the local HyacinthLauncherActivity
            // is guaranteed to exist and handle the risky explicit launch.
            Intent hyacinthIntent = new Intent(HYACINTH_ACTION);
            hyacinthIntent.addCategory(Intent.CATEGORY_DEFAULT);

            try {
                // Attempt to launch the implicit component (HyacinthLauncherActivity)
                startActivity(hyacinthIntent);
                Log.d(TAG, "Successfully launched Hyacinth bridge.");
            } catch (ActivityNotFoundException e) {
                // This catch is mostly for safety, as the local HyacinthLauncherActivity should always be found.
                Log.e(TAG, "FATAL ERROR: HyacinthLauncherActivity (bridge) not found: " + e.getMessage());
                Toast.makeText(this, "Internal error launching Hyacinth bridge.", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop the clock handler to prevent memory leaks
        clockHandler.removeCallbacks(clockRunnable);
        Log.d(TAG, "Activity destroyed, clock handler stopped.");
    }
}
