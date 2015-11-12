package moe.minori.pgpclipper.activities;

import android.app.Activity;
import android.app.PendingIntent;
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
import android.widget.EditText;
import android.widget.TextView;

import org.openintents.openpgp.OpenPgpDecryptionResult;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpServiceConnection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

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
public class PGPClipperResultShowActivity extends Activity {

    OpenPgpServiceConnection serviceConnection;

    public static final int REQUEST_CODE_DECRYPT_AND_VERIFY = 9913;

    public static final String DATA = "DATA";
    public ArrayList<String> KEY_ID = null;

    private Intent intent;

    TextView sigStatus;
    TextView decStatus;
    EditText decResult;
    TextView fastReplyIndc;

    NfcAdapter adapter;
    SharedPreferences preferences;

    String pgpKeyPassword = null;

    boolean isReplyable = false;

    boolean waitingNFC = false;

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

        setContentView(R.layout.resultactivitylayout);

        sigStatus = (TextView) findViewById(R.id.sigStatusTitle);
        decStatus = (TextView) findViewById(R.id.decryptionStatusTitle);
        decResult = (EditText) findViewById(R.id.decryptionResultText);
        fastReplyIndc = (TextView) findViewById(R.id.fastReplyIndicator);

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        adapter = NfcAdapter.getDefaultAdapter(this);

        String currentPgpProvider = preferences.getString("pgpServiceProviderApp", null);

        if (currentPgpProvider == null || "".equals(currentPgpProvider)) {
            // Default security provider is not set
            Log.e("PGPClipperService", "Security provider is not set!");
        } else {
            Log.d("PGPClipperService", "Current security provider: " + currentPgpProvider);

            serviceConnection = new OpenPgpServiceConnection(this, currentPgpProvider);
            serviceConnection.bindToService();

            if (preferences.getBoolean("enableNFCAuth", false) && adapter.isEnabled()) {
                waitingNFC = true;
                fastReplyIndc.setText(R.string.nfcReadyResultShowText);

            } else {
                tryDecryption();
            }

        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
            final Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            tryNfcDecryption(EncryptionUtils.byteArrayToHex(tag.getId()));
            waitingNFC = false;
            disableTagReading(adapter);
        }
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
    private void tryNfcDecryption(String nfcUUID) {

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

            //attemptPseudoEncryptionApiAccess();

            tryDecryption();

        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalBlockSizeException | NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException | BadPaddingException e2) {
            // NFC token or PIN was wrong.
            fastReplyIndc.setText(R.string.credentialWrongText);
            pgpKeyPassword = null;
            //enableTagReading(adapter);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void tryDecryption() {
        Log.d("ResultShowActivity", "tryDecryption called");

        if (serviceConnection != null) {
            if (serviceConnection.isBound()) {
                if (intent != null) {
                    attemptPgpApiAccess(intent.getStringExtra(DATA));
                }
            } else {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        tryDecryption();
                    }
                }, 500);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        disableTagReading(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        overridePendingTransition(0, 0);

        if (waitingNFC) {
            // start adapter listening

            enableTagReading(adapter);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (serviceConnection != null)
            serviceConnection.unbindFromService();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 5298: {
                    tryDecryption();
                }
            }
        }
    }


    private void attemptPgpApiAccess(String input) {
        // PGP data (possibly) detected, try using OpenPGP API

        Log.d("ResultShowActivity", "Attempting decryption");

        Intent data = new Intent();
        data.setAction(OpenPgpApi.ACTION_DECRYPT_VERIFY);
        if (pgpKeyPassword != null) {
            Log.d("ResultShowActivity", "Key applied - embedding key");
            data.putExtra(OpenPgpApi.EXTRA_PASSPHRASE, pgpKeyPassword.toCharArray());
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
        api.executeApiAsync(data, is, os, new CallBack(os, REQUEST_CODE_DECRYPT_AND_VERIFY));
    }

    public void onClick(View v) {
        if (waitingNFC) {
            disableTagReading(adapter);
            waitingNFC = false;
            tryDecryption();
        } else if (isReplyable) {
            // start quick reply activity
            Intent intent = new Intent(this, PGPClipperQuickReplyActivity.class);
            if (KEY_ID != null) {

                // KEY_ID may not have e-mail address to associate with pgp key - thus causing parsing error - Thanks ibanferreira!
                // issue number #8: https://github.com/Mnkai/PGPClipper/issues/8
                try
                {
                    intent.putExtra("KEY_ID", convertToStringArray(KEY_ID));
                }
                catch (StringIndexOutOfBoundsException e)
                {
                    // ignore, treat as null KEY_ID variable
                }

            }

            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        } else {
            //try again
            tryDecryption();
        }
    }


    private String[] convertToStringArray(ArrayList<String> input) throws StringIndexOutOfBoundsException {

        try
        {
            String[] toReturn = new String[input.size()];

            for (int i = 0; i < input.size(); i++) {
                int startIdx = input.get(i).lastIndexOf("<");
                int endIdx = input.get(i).lastIndexOf(">");

                toReturn[i] = input.get(i).substring(startIdx + 1, endIdx);
            }

            return toReturn;
        }
        catch (StringIndexOutOfBoundsException e)
        {
            throw e;
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
                        //TODO: Use this somewhere!
                        String finalResult = os.toString("UTF-8");
                        OpenPgpSignatureResult signatureResult = result.getParcelableExtra(OpenPgpApi.RESULT_SIGNATURE);
                        OpenPgpDecryptionResult decryptionResult = result.getParcelableExtra(OpenPgpApi.RESULT_DECRYPTION);

                        if (signatureResult.getResult() == 1) {
                            sigStatus.setText(getString(R.string.signatureStatusText) + "O \n(" + signatureResult.getPrimaryUserId() + ")");
                            KEY_ID = signatureResult.getUserIds();
                        } else {
                            sigStatus.setText(getString(R.string.signatureStatusText) + "X");
                        }


                        if (decryptionResult.getResult() == 1) {
                            decStatus.setText(getString(R.string.decryptionStatusText) + "O");
                        } else {
                            decStatus.setText(getString(R.string.decryptionStatusText) + "X");
                        }

                        if (finalResult != null) {
                            decResult.setText(finalResult);
                            fastReplyIndc.setText(R.string.fastReplyText);
                            isReplyable = true;
                        } else
                            decResult.setText(R.string.errorCannotProcess);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    break;
                }
                case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED: {
                    PendingIntent pi = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);

                    Log.d("ResultShowActivity", "Interaction required, trying again");
                    try {
                        PGPClipperResultShowActivity.this.startIntentSenderFromChild(PGPClipperResultShowActivity.this, pi.getIntentSender(), 5298, null, 0, 0, 0);
                    } catch (IntentSender.SendIntentException e) {
                        e.printStackTrace();
                    }


                    break;
                }
                case OpenPgpApi.RESULT_CODE_ERROR: {
                    //TODO: Show user error dialog

                    sigStatus.setText(getString(R.string.signatureStatusText) + "X");
                    decStatus.setText(getString(R.string.decryptionStatusText) + "X");
                    decResult.setText(R.string.errorCannotProcess);

                    break;
                }
            }
        }
    }
}
