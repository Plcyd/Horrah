package com.placyd.horrah;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class HyacinthLauncherActivity extends Activity {

    private static final String TAG = "HyacinthLauncher";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This activity is the bridge. It executes the explicit launch and then immediately finishes.

        Log.d(TAG, "HyacinthLauncherActivity started. Attempting final explicit redirect to Project Hyacinth.");

        // 1. Define the explicit ComponentName for the *actual* target activity in the external app.
        // NOTE: This component name MUST be correct for the launch to succeed.
        ComponentName targetComponent = new ComponentName(
                "com.placyd.projecthyacinth",
                "com.placyd.projecthyacinth.CountdownActivity"
        );

        // 2. Create the explicit Intent
        Intent explicitIntent = new Intent(Intent.ACTION_MAIN);
        explicitIntent.setComponent(targetComponent);

        // Add flags to ensure the launched app starts correctly
        explicitIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // 3. Start the activity with robust error handling.
        try {
            startActivity(explicitIntent);
            Log.d(TAG, "Successfully redirected to Project Hyacinth.");
        } catch (ActivityNotFoundException e) {
            // The explicit component could not be found (Project Hyacinth is not installed or the component name is wrong).
            Log.e(TAG, "Failed to find Project Hyacinth target component: " + e.getMessage());
            Toast.makeText(getApplicationContext(), "Project Hyacinth app not found. Please install it.", Toast.LENGTH_LONG).show();
        } catch (SecurityException e) {
            // Catch potential permission issues when launching an external app
            Log.e(TAG, "Security exception when launching Project Hyacinth: " + e.getMessage());
            Toast.makeText(getApplicationContext(), "Cannot launch Hyacinth due to system restrictions.", Toast.LENGTH_LONG).show();
        }

        // 4. Crucially, finish this bridge activity immediately so it doesn't stay on the back stack.
        finish();
    }
}
