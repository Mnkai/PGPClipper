package moe.minori.pgpclipper.activities;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
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

    RelativeLayout parent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.nfcauthlayout);

        parent = (RelativeLayout) findViewById(R.id.parent);
        screen = (RelativeLayout) findViewById(R.id.setupScreen);

        layoutInflater(stage);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        editor = sharedPreferences.edit();

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (!nfcAdapter.isEnabled()) {
            Toast.makeText(this, "Enable NFC in settings", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
        }

        // just to be sure, initialize settings

        initSetting(editor);

        // make screen on always on this activity

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


    }

    public void onClick(View v) {
        if (v == findViewById(R.id.nextBtn)) {
            switch (stage) {
                case 1:
                case 2:
                case 3:
                case 4:
                case 6:
                    // go to next stage
                    goNextStage();
                    break;

                case 5:
                    // store password and proceed to next stage

                    EditText passwordInput = (EditText) findViewById(R.id.passwordInput);

                    password = passwordInput.getText().toString();

                    goNextStage();
                    break;
                case 8:
                    gotoStage(12);
                    break;
                case 11:
                    // return to stage 6
                    gotoStage(6);
                    break;

            }
        }

        if (v == findViewById(R.id.closeBtn)) {
            this.setResult(RESULT_CANCELED);
            finish();
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

        if (stage == 6) // where NFC is required
        {
            if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

                nfcTagUUID = tag.getId();

                // turn off reading mode
                //disableTagReading(nfcAdapter);
            }

            // check if tag uuid is usable

            if (nfcTagUUID.length == 0) {
                // exception - cannot use this tag.
                // goto stage 11

                gotoStage(11);
            } else {
                // tag has uuid. check if this tag dynamically generates uuid.

                goNextStage();
            }


        } else if (stage == 7) {
            // check if current uuid matches previous one.

            if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

                // turn off reading mode
                //disableTagReading(nfcAdapter);

                if (EncryptionUtils.byteArrayToHex(nfcTagUUID).equals(EncryptionUtils.byteArrayToHex(tag.getId()))) {
                    // OK, can use this tag. goto next stage.

                    goNextStage();
                } else {
                    // exception - cannot use this tag. This tag generates different uuid every time.
                    // goto stage 11

                    gotoStage(11);
                }
            }
        }
        else if ( stage == 8 )
        {
            gotoStage(12);
        }
    }

    private void layoutInflater(int stage) {
        switch (stage) {
            case 1:
                flushScreen(screen);

                screen = (RelativeLayout) getLayoutInflater().inflate(R.layout.nfcwizard01_start, parent, false);
                parent.addView(screen);
                break;
            case 2:
                flushScreen(screen);

                screen = (RelativeLayout) getLayoutInflater().inflate(R.layout.nfcwizard02_generate_salt, parent, false);
                parent.addView(screen);
                break;
            case 3:
                flushScreen(screen);

                screen = (RelativeLayout) getLayoutInflater().inflate(R.layout.nfcwizard03_generated_salt, parent, false);
                parent.addView(screen);
                break;
            case 4:
                flushScreen(screen);

                screen = (RelativeLayout) getLayoutInflater().inflate(R.layout.nfcwizard04_notify_password_usage, parent, false);
                parent.addView(screen);
                break;
            case 5:
                flushScreen(screen);

                screen = (RelativeLayout) getLayoutInflater().inflate(R.layout.nfcwizard05_get_password, parent, false);
                parent.addView(screen);
                break;
            case 6:
                flushScreen(screen);

                screen = (RelativeLayout) getLayoutInflater().inflate(R.layout.nfcwizard06_check_token, parent, false);
                parent.addView(screen);
                break;
            case 7:
                flushScreen(screen);

                screen = (RelativeLayout) getLayoutInflater().inflate(R.layout.nfcwizard07_check_token_again, parent, false);
                parent.addView(screen);
                break;
            case 8:
                flushScreen(screen);

                screen = (RelativeLayout) getLayoutInflater().inflate(R.layout.nfcwizard08_token_success, parent, false);
                parent.addView(screen);
                break;
            case 11:
                flushScreen(screen);

                screen = (RelativeLayout) getLayoutInflater().inflate(R.layout.nfcwizard11_token_fail, parent, false);
                parent.addView(screen);
                break;
            case 12:
                flushScreen(screen);

                screen = (RelativeLayout) getLayoutInflater().inflate(R.layout.nfcwizard12_nearly_done, parent, false);
                parent.addView(screen);
                break;
        }

    }

    private void flushScreen(RelativeLayout screen) {
        parent.removeView(screen);
    }

    public static void initSetting(SharedPreferences.Editor editor) {
        editor.remove("deviceSalt");
        editor.remove("encryptedKeyPass");

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
        final Button nextBtn = (Button) findViewById(R.id.nextBtn);

        this.stage = stage;
        layoutInflater(stage);

        if (stage == 2) {
            // disable next button
            nextBtn.setEnabled(false);

            // generate salt

            salt = PBKDF2Helper.makeSalt();

            // enable next button
            nextBtn.setEnabled(true);

            goNextStage();
        } else if (stage == 5) {
            nextBtn.setEnabled(false);

            final EditText passwordInput = (EditText) findViewById(R.id.passwordInput);
            EditText passwordReInput = (EditText) findViewById(R.id.passwordReInput);

            passwordReInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.toString().equals(passwordInput.getText().toString())) {
                        nextBtn.setEnabled(true);
                    } else {
                        nextBtn.setEnabled(false);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
        } else if (stage == 6) // where NFC is required
        {
            nextBtn.setEnabled(false);

            enableTagReading(nfcAdapter);
        } else if (stage == 8) {
            nextBtn.setEnabled(true);
        } else if (stage == 11) {
            nextBtn.setEnabled(true);
        } else if (stage == 12) {
            nextBtn.setEnabled(false);
            // do the work

            AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {

                @Override
                protected Boolean doInBackground(Void... params) {
                    return finalStage(nfcTagUUID, salt, password);
                }

                @Override
                protected void onPostExecute(Boolean aBoolean) {
                    if (aBoolean) {
                        setResult(RESULT_OK);
                        //Toast.makeText(NFCAuthenticationSetupActivity.this, "NFC auth enabled", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        setResult(RESULT_CANCELED);
                        //Toast.makeText(NFCAuthenticationSetupActivity.this, "NFC auth failed to enable", Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
            }.execute();

        }
    }

    private boolean finalStage(byte[] nfcTagUUID, byte[] salt, String toProtect) {
        try {
            // based on input, write to sharedPref

            editor.putString("deviceSalt", EncryptionUtils.byteArrayToHex(salt));

            String nfcTagUUIDHex = EncryptionUtils.byteArrayToHex(nfcTagUUID);

            byte[] aesPassword;

            aesPassword = PBKDF2Helper.createSaltedHash(nfcTagUUIDHex, salt);

            byte[] encryptedData;

            encryptedData = AESHelper.encrypt(EncryptionUtils.stringToByteArray(toProtect), aesPassword);

            editor.putString("encryptedKeyPass", EncryptionUtils.byteArrayToHex(encryptedData));

        } catch (NoSuchPaddingException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException | InvalidKeySpecException e) {
            return false;
        }

        editor.commit();

        return true;
    }
}
