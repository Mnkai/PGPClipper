package moe.minori.pgpclipper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Minori on 2015-09-26.
 */
public class BootListener extends BroadcastReceiver {

    public final static String ANDROID_ON = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        // BOOT_COMPLETED” start Service
        if (ANDROID_ON.equals(intent.getAction())) {

            // get preference
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

            boolean isEnabled = preferences.getBoolean("enabledCheckBox", false);

            Intent serviceIntent = new Intent(context, PGPClipperService.class);

            //Service
            if ( isEnabled )
            {

                context.startService(serviceIntent);
            }
            else
            {
                context.stopService(serviceIntent);
            }
        }
    }
}
