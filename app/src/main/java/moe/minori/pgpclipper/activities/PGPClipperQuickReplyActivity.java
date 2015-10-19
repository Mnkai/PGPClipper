package moe.minori.pgpclipper.activities;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpServiceConnection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
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

    TextView nfcSignatureNotice;

    NfcAdapter adapter;
    SharedPreferences preferences;

    String pgpKeyPassword = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String currentTheme = sharedPreferences.getString("themeSelection", "dark");

        switch (currentTheme) {
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
        nfcSignatureNotice = (TextView) findViewById(R.id.nfcNotificationText);

        // get nfc adapter

        adapter = NfcAdapter.getDefaultAdapter(this);

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (preferences.getBoolean("enableNFCAuth", false) && adapter.isEnabled()) {
            nfcSignatureNotice.setVisibility(View.VISIBLE);

        } else {
            nfcSignatureNotice.setVisibility(View.GONE);
        }

        //setting hint if sender's signature key found
        String[] keyIDs = intent.getStringArrayExtra("KEY_ID");
        if (keyIDs != null) {
            String strConcatId = "";
            for (String id : keyIDs) {
                strConcatId += id;
                strConcatId += ", ";
            }
            if (strConcatId.endsWith(", ")) {
                strConcatId = strConcatId.substring(0, strConcatId.lastIndexOf(", "));
            }
            replyTextField.setHint(getString(R.string.hintRecipient) + strConcatId);
        }

        String currentPgpProvider = preferences.getString("pgpServiceProviderApp", null);

        if (currentPgpProvider == null || "".equals(currentPgpProvider)) {
            // Default security provider is not set
            Log.e("PGPClipperService", "Security provider is not set!");
        } else {
            Log.d("PGPClipperService", "Current security provider: " + currentPgpProvider);

            serviceConnection = new OpenPgpServiceConnection(this, currentPgpProvider);
            serviceConnection.bindToService();

        }

    }

    private void tryEncryption() {
        if (serviceConnection.isBound()) {
            if (intent != null) {
                attemptPgpApiAccess(replyTextField.getText().toString());
            }
        } else {
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

        if (nfcSignatureNotice.getVisibility() == View.VISIBLE) {
            enableTagReading(adapter);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (serviceConnection != null)
            serviceConnection.unbindFromService();

    }

    private void enableTagReading(NfcAdapter adapter) {
        try {
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

            adapter.enableForegroundDispatch(this, pendingIntent, null, null);
        } catch (Exception e) {
            // ignore
        }
    }

    private void disableTagReading(NfcAdapter adapter) {
        try {
            adapter.disableForegroundDispatch(this);
        } catch (Exception e) {
            // ignore
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 5298: {
                    String[] keyIDs = intent.getStringArrayExtra("KEY_ID");
                    if (keyIDs != null) {
                        data.putExtra(OpenPgpApi.EXTRA_USER_IDS, keyIDs);
                    }

                    data.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);

                    if (sigCheckBox.isChecked()) {
                        // signature + encryption

                        data.setAction(OpenPgpApi.ACTION_SIGN_AND_ENCRYPT);
                    } else {
                        // only encryption

                        data.setAction(OpenPgpApi.ACTION_ENCRYPT);

                    }

                    InputStream is;
                    ByteArrayOutputStream os = new ByteArrayOutputStream();

                    try {
                        is = new ByteArrayInputStream(replyTextField.getText().toString().getBytes("UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                        return;
                    }

                    OpenPgpApi api = new OpenPgpApi(this, serviceConnection.getService());

                    if (sigCheckBox.isChecked()) {
                        api.executeApiAsync(data, is, os, new CallBack(os, REQUEST_CODE_SIGN_AND_ENCRYPT));
                    } else {
                        api.executeApiAsync(data, is, os, new CallBack(os, REQUEST_CODE_ENCRYPT));
                    }
                }
            }
        }
    }


    private void attemptPgpApiAccess(String input) {
        // try encryption (optionally signature) and copy data into clipboard

        Intent data = new Intent();
        String[] keyIDs = intent.getStringArrayExtra("KEY_ID");
        if (keyIDs != null) {
            Log.d("QuickReplyActivity", "keyID found, embedding");
            data.putExtra(OpenPgpApi.EXTRA_USER_IDS, intent.getStringArrayExtra("KEY_ID"));
        }

        data.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);

        if (pgpKeyPassword != null) { // pgpKeyPassword provided - NFC token
            // always sign
            data.setAction(OpenPgpApi.ACTION_SIGN_AND_ENCRYPT);
            data.putExtra(OpenPgpApi.EXTRA_PASSPHRASE, pgpKeyPassword.toCharArray());
        } else {
            if (sigCheckBox.isChecked()) {
                // signature + encryption

                data.setAction(OpenPgpApi.ACTION_SIGN_AND_ENCRYPT);
            } else {
                // only encryption

                data.setAction(OpenPgpApi.ACTION_ENCRYPT);

            }

        }

        InputStream is;
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        try {
            is = new ByteArrayInputStream(input.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }

        OpenPgpApi api = new OpenPgpApi(this, serviceConnection.getService());

        if (sigCheckBox.isChecked() || pgpKeyPassword != null) {
            api.executeApiAsync(data, is, os, new CallBack(os, REQUEST_CODE_SIGN_AND_ENCRYPT));
        } else {
            api.executeApiAsync(data, is, os, new CallBack(os, REQUEST_CODE_ENCRYPT));
        }

    }

    public void onClick(View v) {
        tryEncryption();
        disableTagReading(adapter);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
            final Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);


            tryNfcSignEncryption(EncryptionUtils.byteArrayToHex(tag.getId()));
            disableTagReading(adapter);

        }
    }

    private void tryNfcSignEncryption(String nfcUUID) {
        try {
            // get device salt
            byte[] salt = EncryptionUtils.hexToByteArray(preferences.getString("deviceSalt", null));

            if (salt == null) {
                throw new Exception("System does not have salt value, but wizard has somehow finished");
            }

            // first generate aes password.
            byte[] aesPassword;

            aesPassword = PBKDF2Helper.createSaltedHash(nfcUUID, salt);

            // aes password generated, try decryption
            byte[] encryptedData = EncryptionUtils.hexToByteArray(preferences.getString("encryptedKeyPass", null));
            if (encryptedData == null)
                throw new Exception("System does not have encrypted password, but wizard has somehow finished");

            byte[] decryptedData;

            decryptedData = AESHelper.decrypt(encryptedData, aesPassword);

            pgpKeyPassword = EncryptionUtils.byteArrayToString(decryptedData);

            tryEncryption();

        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalBlockSizeException | NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException | BadPaddingException e2) {
            // NFC token or PIN was wrong.
            nfcSignatureNotice.setText(R.string.credentialWrongText);
            pgpKeyPassword = null;
            enableTagReading(adapter);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private class CallBack implements OpenPgpApi.IOpenPgpCallback {

        ByteArrayOutputStream os;
        int requestCode;

        private CallBack(ByteArrayOutputStream os, int requestCode) {
            this.os = os;
            this.requestCode = requestCode;
        }

        @Override
        public void onReturn(Intent result) {
            switch (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
                case OpenPgpApi.RESULT_CODE_SUCCESS: {
                    try {
                        String finalResult = os.toString("UTF-8");

                        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("label", finalResult);
                        clipboardManager.setPrimaryClip(clip);

                        finish();

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    break;
                }
                case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED: {

                    Log.d("QuickReplyActivity", "Interaction required");

                    PendingIntent pi = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);

                    try {
                        PGPClipperQuickReplyActivity.this.startIntentSenderFromChild(PGPClipperQuickReplyActivity.this, pi.getIntentSender(), 5298, null, 0, 0, 0);
                    } catch (IntentSender.SendIntentException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                case OpenPgpApi.RESULT_CODE_ERROR: {
                    replyTextField.setText(R.string.errorCannotContinue);

                    break;
                }
            }
        }
    }
}
