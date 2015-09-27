package moe.minori.pgpclipper;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import org.openintents.openpgp.OpenPgpDecryptionResult;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpKeyPreference;
import org.openintents.openpgp.util.OpenPgpServiceConnection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * Created by Minori on 2015-09-26.
 */
public class PGPClipperQuickReplyActivity extends Activity {

    OpenPgpServiceConnection serviceConnection;

    public static final int REQUEST_CODE_ENCRYPT = 9911;
    public static final int REQUEST_CODE_SIGN_AND_ENCRYPT = 9912;

    public static final String DATA = "DATA";

    private Intent intent;

    CheckBox sigCheckBox;
    EditText replyTextField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String currentTheme = sharedPreferences.getString("themeSelection", "dark");

        switch (currentTheme)
        {
            case "dark":
                setTheme(R.style.PseudoDialogDarkTheme);
                break;
            case "light":
                setTheme(R.style.PseudoDialogLightTheme);
                break;
        }
        super.onCreate(savedInstanceState);

        intent = getIntent();

        setContentView(R.layout.quickreplyactivitylayout);

        sigCheckBox = (CheckBox) findViewById(R.id.signatureCheck);
        replyTextField = (EditText) findViewById(R.id.replyText);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        String currentPgpProvider = preferences.getString("pgpServiceProviderApp", null);

        if ( currentPgpProvider == null )
        {
            // Default security provider is not set
            Log.e("PGPClipperService", "Security provider is not set!");
        }
        else
        {
            Log.d("PGPClipperService", "Current security provider: " + currentPgpProvider );

            serviceConnection = new OpenPgpServiceConnection(this, currentPgpProvider);
            serviceConnection.bindToService();

        }
    }

    private void tryEncryption ()
    {
        if ( serviceConnection.isBound() )
        {
            if ( intent != null )
            {
                attemptPgpApiAccess(replyTextField.getText().toString());
            }
        }
        else
        {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    tryEncryption();
                }
            }, 500);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        overridePendingTransition(0, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if ( serviceConnection != null )
            serviceConnection.unbindFromService();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ( resultCode == RESULT_OK )
        {
            switch (requestCode)
            {
                case 5298:
                {
                    data.putExtra(OpenPgpApi.EXTRA_USER_IDS, intent.getStringArrayExtra("KEY_ID"));
                    data.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);
                    if ( sigCheckBox.isChecked() )
                    {
                        // signature + encryption

                        data.setAction(OpenPgpApi.ACTION_SIGN_AND_ENCRYPT);
                    }
                    else
                    {
                        // only encryption

                        data.setAction(OpenPgpApi.ACTION_ENCRYPT);

                    }

                    InputStream is;
                    ByteArrayOutputStream os = new ByteArrayOutputStream();

                    try
                    {
                        is = new ByteArrayInputStream(replyTextField.getText().toString().getBytes("UTF-8"));
                    }
                    catch (UnsupportedEncodingException e)
                    {
                        e.printStackTrace();
                        return;
                    }

                    OpenPgpApi api = new OpenPgpApi(this, serviceConnection.getService());

                    if ( sigCheckBox.isChecked() )
                    {
                        api.executeApiAsync(data, is, os, new CallBack(os, REQUEST_CODE_SIGN_AND_ENCRYPT));
                    }
                    else
                    {
                        api.executeApiAsync(data, is, os, new CallBack(os, REQUEST_CODE_ENCRYPT));
                    }
                }
            }
        }
    }


    private void attemptPgpApiAccess (String input)
    {
        // try encryption (optionally signature) and copy data into clipboard

        Intent data = new Intent();
        data.putExtra(OpenPgpApi.EXTRA_USER_IDS, intent.getStringArrayExtra("KEY_ID"));
        data.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);
        if ( sigCheckBox.isChecked() )
        {
            // signature + encryption

            data.setAction(OpenPgpApi.ACTION_SIGN_AND_ENCRYPT);
        }
        else
        {
            // only encryption

            data.setAction(OpenPgpApi.ACTION_ENCRYPT);

        }

        InputStream is;
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        try
        {
            is = new ByteArrayInputStream(input.getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
            return;
        }

        OpenPgpApi api = new OpenPgpApi(this, serviceConnection.getService());

        if ( sigCheckBox.isChecked() )
        {
            api.executeApiAsync(data, is, os, new CallBack(os, REQUEST_CODE_SIGN_AND_ENCRYPT));
        }
        else
        {
            api.executeApiAsync(data, is, os, new CallBack(os, REQUEST_CODE_ENCRYPT));
        }

    }

    public void onClick (View v)
    {
        tryEncryption();
    }

    private class CallBack implements OpenPgpApi.IOpenPgpCallback
    {

        ByteArrayOutputStream os;
        int requestCode;

        private CallBack(ByteArrayOutputStream os, int requestCode)
        {
            this.os = os;
            this.requestCode = requestCode;
        }

        @Override
        public void onReturn(Intent result) {
            switch (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR) )
            {
                case OpenPgpApi.RESULT_CODE_SUCCESS:
                {
                    try
                    {
                        //TODO: Use this somewhere!
                        String finalResult = os.toString("UTF-8");

                        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("label", finalResult);
                        clipboardManager.setPrimaryClip(clip);

                        finish();

                    }
                    catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    break;
                }
                case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED:
                {
                    PendingIntent pi = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);

                    try
                    {
                        PGPClipperQuickReplyActivity.this.startIntentSenderFromChild(PGPClipperQuickReplyActivity.this, pi.getIntentSender(), 5298, null, 0, 0, 0);
                    }
                    catch (IntentSender.SendIntentException e)
                    {
                        e.printStackTrace();
                    }
                    break;
                }
                case OpenPgpApi.RESULT_CODE_ERROR:
                {
                    //TODO: Show user error dialog

                    replyTextField.setText("Error, cannot continue. Send bug report to author.");

                    break;
                }
            }
        }
    }
}
