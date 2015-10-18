package moe.minori.pgpclipper.activities;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import moe.minori.pgpclipper.R;
import moe.minori.pgpclipper.encryption.AESHelper;
import moe.minori.pgpclipper.encryption.PBKDF2Helper;
import moe.minori.pgpclipper.util.EncryptionUtils;

/**
 * Created by Minori on 2015-10-18.
 */
public class NFCAuthenticationSetupActivity extends Activity {

    byte[] salt;
    byte[] nfcTagUUID;
    String password;

    boolean pinUse;
    String PIN;

    RelativeLayout screen;

    int stage = 1;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    NfcAdapter nfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.nfcauthlayout);

        screen = (RelativeLayout) findViewById(R.id.setupScreen);

        layoutInflater(stage, screen);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        editor = sharedPreferences.edit();

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (!nfcAdapter.isEnabled()) {
            Toast.makeText(this, "Enable NFC in settings", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
        }

        // just to be sure, initialize settings

        initSetting(editor);


    }

    public void onClick(View v) {
        if (v == findViewById(R.id.nextBtn)) {
            switch (stage) {
                case 1:
                case 2:
                case 3:
                case 4:
                case 6:
                case 8:
                    // go to next stage
                    goNextStage();
                    break;

                case 5:
                    // store password and proceed to next stage

                    EditText passwordInput = (EditText) findViewById(R.id.passwordInput);

                    password = passwordInput.getText().toString();

                    goNextStage();
                    break;

                case 10:
                    // store PIN and proceed to next stage

                    EditText pinInput = (EditText) findViewById(R.id.pinInput);

                    PIN = pinInput.getText().toString();

                    goNextStage();
                    break;
                case 9:
                    // check PIN enabled, if enabled - proceed to stage 10 / if disabled - proceed to end
                    CheckBox pinEnableCheckbox = (CheckBox) findViewById(R.id.isPinEnabled);

                    if ( pinEnableCheckbox.isChecked() )
                    {
                        pinUse = true;

                        gotoStage(10);
                    }
                    else
                    {
                        pinUse = false;

                        gotoStage(12);
                    }
                    break;
                case 11:
                    // return to stage 6
                        gotoStage(6);
                    break;

            }
        }

        if (v == findViewById(R.id.closeBtn)) {
            finishActivity(-1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (stage == 6 || stage == 7) // where NFC is required
        {
            enableTagReading(nfcAdapter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        disableTagReading(nfcAdapter);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if ( stage == 6 ) // where NFC is required
        {
            if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

                nfcTagUUID = tag.getId();

                // turn off reading mode
                disableTagReading(nfcAdapter);
            }

            // check if tag uuid is usable

            if ( nfcTagUUID.length == 0 )
            {
                // exception - cannot use this tag.
                // goto stage 11

                gotoStage(11);
            }
            else
            {
                // tag has uuid. check if this tag dynamically generates uuid.

                goNextStage();
            }



        }
        else if ( stage == 7 )
        {
            // check if current uuid matches previous one.

            if ( intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

                // turn off reading mode
                disableTagReading(nfcAdapter);

                if ( nfcTagUUID == tag.getId() )
                {
                    // OK, can use this tag. goto next stage.

                    goNextStage();
                }
                else
                {
                    // exception - cannot use this tag. This tag generates different uuid every time.
                    // goto stage 11

                    gotoStage(11);
                }
            }
        }
    }

    private void layoutInflater(int stage, RelativeLayout screen) {
        switch (stage) {
            case 1:
                flushScreen(screen);

                View internal = getLayoutInflater().inflate(R.layout.nfcwizard01_start, null);
                screen.addView(internal);

                break;
            case 2:
                flushScreen(screen);

                internal = getLayoutInflater().inflate(R.layout.nfcwizard02_generate_salt, null);
                screen.addView(internal);
                break;
            case 3:
                flushScreen(screen);

                internal = getLayoutInflater().inflate(R.layout.nfcwizard03_generated_salt, null);
                screen.addView(internal);
                break;
            case 4:
                flushScreen(screen);

                internal = getLayoutInflater().inflate(R.layout.nfcwizard04_notify_password_usage, null);
                screen.addView(internal);
                break;
            case 5:
                flushScreen(screen);

                internal = getLayoutInflater().inflate(R.layout.nfcwizard05_get_password, null);
                screen.addView(internal);
                break;
            case 6:
                flushScreen(screen);

                internal = getLayoutInflater().inflate(R.layout.nfcwizard06_check_token, null);
                screen.addView(internal);
                break;
            case 7:
                flushScreen(screen);

                internal = getLayoutInflater().inflate(R.layout.nfcwizard07_check_token_again, null);
                screen.addView(internal);
                break;
            case 8:
                flushScreen(screen);

                internal = getLayoutInflater().inflate(R.layout.nfcwizard08_token_success, null);
                screen.addView(internal);
                break;
            case 9:
                flushScreen(screen);

                internal = getLayoutInflater().inflate(R.layout.nfcwizard09_ask_pin_validation, null);
                screen.addView(internal);
                break;
            case 10:
                flushScreen(screen);

                internal = getLayoutInflater().inflate(R.layout.nfcwizard10_get_pin, null);
                screen.addView(internal);
                break;
            case 11:
                flushScreen(screen);

                internal = getLayoutInflater().inflate(R.layout.nfcwizard11_token_fail, null);
                screen.addView(internal);
                break;
            case 12:
                flushScreen(screen);

                internal = getLayoutInflater().inflate(R.layout.nfcwizard12_nearly_done, null);
                screen.addView(internal);
                break;
        }

    }

    private void flushScreen(RelativeLayout screen) {
        screen.removeAllViews();
    }

    public static void initSetting(SharedPreferences.Editor editor) {
        editor.remove("deviceSalt");
        editor.remove("encryptedKeyPass");
        editor.remove("isPIN");

        editor.commit();
    }

    private void enableTagReading(NfcAdapter adapter) {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        adapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    private void disableTagReading(NfcAdapter adapter) {
        adapter.disableForegroundDispatch(this);
    }

    private void goNextStage() {
        gotoStage(stage + 1);
    }

    private void gotoStage(int stage) {
        this.stage = stage;
        layoutInflater(stage, screen);

        if (stage == 6 || stage == 7) // where NFC is required
        {
            enableTagReading(nfcAdapter);
        }

        if ( stage == 12 )
        {
            // do the work

            if ( finalStage(nfcTagUUID, salt, password, pinUse, PIN) )
            {
                finishActivity(0);
            }
            else
            {
                finishActivity(-1);
            }

        }
    }

    private boolean finalStage (byte[] nfcTagUUID, byte[] salt, String toProtect, boolean isPIN, String PIN)
    {
        try
        {
            // based on input, write to sharedPref

            editor.putString("deviceSalt", EncryptionUtils.byteArrayToHex(salt));
            editor.putBoolean("isPIN", isPIN);

            String nfcTagUUIDHex = EncryptionUtils.byteArrayToHex(nfcTagUUID);

            byte[] aesPassword;

            if ( isPIN )
            {
                aesPassword = PBKDF2Helper.createSaltedHash(nfcTagUUIDHex + PIN, salt);
            }
            else
            {
                aesPassword = PBKDF2Helper.createSaltedHash(nfcTagUUIDHex, salt);
            }

            byte[] encryptedData;

            encryptedData = AESHelper.encrypt(EncryptionUtils.stringToByteArray(toProtect), aesPassword);

            editor.putString("encryptedKeyPass", EncryptionUtils.byteArrayToHex(encryptedData));

        } catch (NoSuchPaddingException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException | InvalidKeySpecException e) {
            return false;
        }

        return true;
    }
}
