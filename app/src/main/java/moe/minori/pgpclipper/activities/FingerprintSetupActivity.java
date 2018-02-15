package moe.minori.pgpclipper.activities;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import co.infinum.goldfinger.Error;
import co.infinum.goldfinger.Goldfinger;
import co.infinum.goldfinger.Warning;
import moe.minori.pgpclipper.R;
import moe.minori.pgpclipper.util.Constants;

/**
 * Created by Minori on 2018-02-15.
 */
public class FingerprintSetupActivity extends Activity {

    String password;

    RelativeLayout screen;

    int stage = 1;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    Goldfinger goldfinger;
    RelativeLayout parent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.wizardlayout);

        parent = findViewById(R.id.parent);
        screen = findViewById(R.id.setupScreen);

        layoutInflater(stage);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        editor = sharedPreferences.edit();

        goldfinger = new Goldfinger.Builder(this).build();

        // make screen on always on this activity
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void onClick(View v) {
        if (v == findViewById(R.id.nextBtn)) {
            switch (stage) {
                case 1:
                case 2:
                    // go to next stage
                    goNextStage();
                    break;
                case 3:
                    // store password and proceed to next stage

                    EditText passwordInput = findViewById(R.id.passwordInput);

                    password = passwordInput.getText().toString();

                    goNextStage();
                    break;
            }
        }

        if (v == findViewById(R.id.closeBtn)) {
            this.setResult(RESULT_CANCELED);
            finish();
        }
    }

    private void layoutInflater(int stage) {
        switch (stage) {
            case 1:
                flushScreen(screen);

                screen = (RelativeLayout) getLayoutInflater().inflate(R.layout.fingerprintwizard01_start, parent, false);
                parent.addView(screen);
                break;
            case 2:
                flushScreen(screen);

                screen = (RelativeLayout) getLayoutInflater().inflate(R.layout.fingerprintwizard02_notify_password_usage, parent, false);
                parent.addView(screen);
                break;
            case 3:
                flushScreen(screen);

                screen = (RelativeLayout) getLayoutInflater().inflate(R.layout.fingerprintwizard03_get_password, parent, false);
                parent.addView(screen);
                break;
            case 4:
                flushScreen(screen);

                screen = (RelativeLayout) getLayoutInflater().inflate(R.layout.fingerprintwizard04_fingerprint_check, parent, false);
                parent.addView(screen);
                break;
        }

    }

    private void flushScreen(RelativeLayout screen) {
        parent.removeView(screen);
    }

    private void goNextStage() {
        gotoStage(stage + 1);
    }

    private void gotoStage(int stage) {
        final Button nextBtn = (Button) findViewById(R.id.nextBtn);

        this.stage = stage;
        layoutInflater(stage);

        if (stage == 3) {
            nextBtn.setEnabled(false);

            final EditText passwordInput = findViewById(R.id.passwordInput);
            EditText passwordReInput = findViewById(R.id.passwordReInput);

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
        } else if (stage == 4) // where fingerprint reader is required
        {
            nextBtn.setEnabled(false);

            goldfinger.encrypt(Constants.FINGERPRINT_KEYNAME, password, new Goldfinger.Callback() {
                @Override
                public void onSuccess(String value) {
                    setResult(RESULT_OK);
                    finish();
                }

                @Override
                public void onWarning(Warning warning) {
                    Toast.makeText(FingerprintSetupActivity.this,
                            "Could not read fingerprint from sensor, try again.",
                            Toast.LENGTH_LONG)
                            .show();
                }

                @Override
                public void onError(Error error) {
                    Toast.makeText(FingerprintSetupActivity.this,
                            "Fatal error! Try again later.",
                            Toast.LENGTH_LONG)
                            .show();

                    setResult(RESULT_CANCELED);
                    finish();

                }
            });
        }
    }

}
