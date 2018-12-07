package moe.minori.pgpclipper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by Minori on 2015-09-26.
 */
public class BootListener extends BroadcastReceiver {

    public final static String ANDROID_ON = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("PGPClipperBootListener", "Received Boot Completed broadcast");

        // BOOT_COMPLETED” start Service
        if (ANDROID_ON.equals(intent.getAction())) {

            // get preference
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

            boolean isEnabled = preferences.getBoolean("pgpClipperEnabledCheckbox", false);

            Intent serviceIntent = new Intent(context, PGPClipperService.class);

            //Service
            if ( isEnabled )
            {
                Log.d("PGPClipperBootListener", "Starting service since PGPClipper is enabled");
                context.startService(serviceIntent);
            }
            else
            {
                Log.d("PGPClipperBootListener", "Stopping service since PGPClipper is disabled");
                context.stopService(serviceIntent);
            }
        }
    }
}
