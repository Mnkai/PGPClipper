package moe.minori.pgpclipper;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import moe.minori.pgpclipper.activities.PGPClipperResultShowActivity;
import moe.minori.pgpclipper.util.PGPBlockDetector;

/**
 * Created by Minori on 2015-09-26.
 */
public class PGPClipperService extends Service {


    ClipboardManager clipboardManager;
    ClipboardManager.OnPrimaryClipChangedListener onPrimaryClipChangedListener;

    public static final String TRY_DECRYPT = "TRY_DECRYPT";
    public static final String DATA = "DATA";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        startClipboardMonitoring();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopClipboardMonitoring();


    }

    private void startClipboardMonitoring() {
        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        if (clipboardManager != null) {
            clipboardManager.addPrimaryClipChangedListener(onPrimaryClipChangedListener = new ClipboardManager.OnPrimaryClipChangedListener() {
                @Override
                public void onPrimaryClipChanged() {
                    Log.d("PGPClipperService", "Clipboard data change detected!");

                    // get current clipboard data to string
                    String currentData;

                    if (clipboardManager.hasPrimaryClip() && clipboardManager.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {

                        try {
                            currentData = clipboardManager.getPrimaryClip().getItemAt(0).getText().toString();
                        } catch (NullPointerException e) {
                            // should not happen since clipboard contains text, but returned null.
                            // best effort String parsing

                            try {
                                currentData = clipboardManager.getPrimaryClip().getItemAt(0).coerceToText(PGPClipperService.this).toString();

                            } catch (Exception e2) {
                                // best attempt failed... return this method

                                return;
                            }

                        }
                    } else {
                        return;
                    }

                    // tidy once
                    currentData = PGPBlockDetector.pgpInputTidy(currentData);

                    // check if this contains ASCII armored PGP data

                    if (PGPBlockDetector.isBlockPresent(currentData)) {
                        // notify user

                        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

                        Intent intentToLaunchWhenNotificationClicked = new Intent(getApplicationContext(), PGPClipperService.class);
                        intentToLaunchWhenNotificationClicked.putExtra(TRY_DECRYPT, true);
                        intentToLaunchWhenNotificationClicked.putExtra(DATA, currentData);

                        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, intentToLaunchWhenNotificationClicked, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_UPDATE_CURRENT);

                        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext())
                                .setSmallIcon(R.drawable.ic_noti)
                                .setTicker(getString(R.string.NotificationTickerText))
                                .setContentTitle(getString(R.string.NotificationTitleText))
                                .setContentText(getString(R.string.NotificationContentText))
                                .setDefaults(Notification.DEFAULT_LIGHTS)
                                .setContentIntent(pendingIntent)
                                .setPriority(NotificationCompat.PRIORITY_MAX)
                                .setWhen(System.currentTimeMillis())
                                .setAutoCancel(true);

                        notificationManager.notify(8591274, notificationBuilder.build());
                    }

                }
            });
        }

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d("PGPClipperService", "onStartCommand called!");

        if (intent != null) {
            if (intent.getBooleanExtra(TRY_DECRYPT, false)) {
                Log.d("PGPClipperService", "Trying Decryption/Verification");
                Intent launchActivity = new Intent(getApplicationContext(), PGPClipperResultShowActivity.class);
                launchActivity.putExtra(DATA, intent.getStringExtra(DATA));
                launchActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                startActivity(launchActivity);
            }
        }


        return START_STICKY;
    }

    private void stopClipboardMonitoring() {
        if (onPrimaryClipChangedListener != null) {
            clipboardManager.removePrimaryClipChangedListener(onPrimaryClipChangedListener);
        }
    }

}
