package moe.minori.pgpclipper.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

import java.security.KeyStore;
import java.security.KeyStoreException;

import co.infinum.goldfinger.Goldfinger;
import moe.minori.pgpclipper.PGPClipperService;
import moe.minori.pgpclipper.R;
import moe.minori.pgpclipper.util.Constants;

public class PGPClipperSettingsActivity extends Activity {

    SettingFragment fragment;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, fragment = new SettingFragment()).commit();


        fragment.findPreference("enabledCheckBox").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                if ((boolean) newValue)
                    // enabled
                    startService(new Intent(PGPClipperSettingsActivity.this, PGPClipperService.class));
                else
                    // disabled
                    stopService(new Intent(PGPClipperSettingsActivity.this, PGPClipperService.class));
                return true;
            }
        });

        fragment.findPreference("issueTrackerPreferenceItem").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                Intent webBrowserLaunchIntent = new Intent(Intent.ACTION_VIEW);
                webBrowserLaunchIntent.setData(Uri.parse("https://github.com/Mnkai/PGPClipper"));
                startActivity(webBrowserLaunchIntent);

                return true;
            }
        });

        fragment.findPreference("licensePreferenceItem").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                Intent webBrowserLaunchIntent = new Intent(Intent.ACTION_VIEW);
                webBrowserLaunchIntent.setData(Uri.parse("https://github.com/Mnkai/PGPClipper/blob/master/LICENSE"));
                startActivity(webBrowserLaunchIntent);

                return true;
            }
        });

        fragment.findPreference("thirdPartyLicensePreferenceItem").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                Intent webBrowserLaunchIntent = new Intent(Intent.ACTION_VIEW);
                webBrowserLaunchIntent.setData(Uri.parse("https://github.com/Mnkai/PGPClipper/blob/master/LICENSE"));
                startActivity(webBrowserLaunchIntent);

                return true;
            }
        });

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        final ListPreference themePref = (ListPreference) fragment.findPreference("themeSelection");

        themePref.setEntryValues(R.array.themes_values);
        themePref.setEntries(R.array.themes);

        String currentVal = sharedPreferences.getString("themeSelection", "dark");
        switch (currentVal) {
            case "dark":
                themePref.setSummary(getResources().getString(R.string.darkText));
                break;
            case "light":
                themePref.setSummary(getResources().getString(R.string.lightText));
                break;
        }

        themePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                switch ((String) newValue) {
                    case "dark":
                        themePref.setSummary(getResources().getString(R.string.darkText));
                        break;
                    case "light":
                        themePref.setSummary(getResources().getString(R.string.lightText));
                        break;
                }

                return true;
            }
        });

        final CheckBoxPreference pgpClipperEnabledChekcbox = (CheckBoxPreference) fragment.findPreference("enabledCheckBox");

        fragment.findPreference("pgpServiceProviderApp").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String providerApp = (String) newValue;
                if (providerApp == null || "".equals(providerApp)) {
                    pgpClipperEnabledChekcbox.setEnabled(false);
                    pgpClipperEnabledChekcbox.setChecked(false);
                    stopService(new Intent(PGPClipperSettingsActivity.this, PGPClipperService.class));
                } else {
                    pgpClipperEnabledChekcbox.setEnabled(true);
                }
                return true;
            }
        });
        String providerApp = sharedPreferences.getString("pgpServiceProviderApp", null);
        if (providerApp == null || "".equals(providerApp)) {
            pgpClipperEnabledChekcbox.setEnabled(false);
            pgpClipperEnabledChekcbox.setChecked(false);
            stopService(new Intent(PGPClipperSettingsActivity.this, PGPClipperService.class));
        } else {
            if (pgpClipperEnabledChekcbox.isChecked()) {
                startService(new Intent(PGPClipperSettingsActivity.this, PGPClipperService.class));
            }
            pgpClipperEnabledChekcbox.setEnabled(true);
        }

        // for NFC authentication

        fragment.findPreference("enableNFCAuth").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!((boolean) newValue)) {
                    // delete current hash and encrypted data

                    NFCAuthenticationSetupActivity.initSetting(editor);
                } else {
                    // start NFCAuthSetupActivity
                    Intent intent = new Intent(PGPClipperSettingsActivity.this, NFCAuthenticationSetupActivity.class);

                    startActivityForResult(intent, Constants.NFC_SETUP_REQUEST_CODE);
                }
                return true;
            }
        });

        // For Fingerprint authentication

        final CheckBoxPreference fingerprintCheckboxPreference = (CheckBoxPreference) fragment.findPreference("enableFingerprintAuth");

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) // Fingerprint API not supported below M
            fingerprintCheckboxPreference.setEnabled(false);
        else {
            fragment.findPreference("enableFingerprintAuth").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

                @SuppressLint("NewApi")
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (!((boolean) newValue)) {
                        try {
                            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                            keyStore.deleteEntry(Constants.FINGERPRINT_KEYNAME);
                        } catch (KeyStoreException e) {
                            e.printStackTrace();

                            // I tried my best, key will be overwritten in next setup anyway...
                        }


                    } else {
                        Goldfinger goldfinger = new Goldfinger.Builder(PGPClipperSettingsActivity.this).build();

                        if (!goldfinger.hasFingerprintHardware() || !goldfinger.hasEnrolledFingerprint())
                            return false;

                        // Will be able to continue (hopefully), start fingerprint setup activity

                        Intent intent = new Intent(PGPClipperSettingsActivity.this, FingerprintSetupActivity.class);

                        startActivityForResult(intent, Constants.FINGERPRINT_SETUP_REQUEST_CODE);
                    }
                    return true;
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public static class SettingFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.pref_general);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("SettingsActivity", "onActivityResult called");

        if (requestCode == Constants.NFC_SETUP_REQUEST_CODE) // NFCAuthSetupResult
        {
            if (resultCode != RESULT_OK) // error or user canceled auth operation
            {
                CheckBoxPreference checkBoxPreference = (CheckBoxPreference) fragment.findPreference("enableNFCAuth");
                checkBoxPreference.setChecked(false); // Will execute cleanup automatically
            }
        }

        if (requestCode == Constants.FINGERPRINT_SETUP_REQUEST_CODE) // FingerprintSetupResult
        {
            if (resultCode != RESULT_OK) // error or user canceled auth operation
            {
                CheckBoxPreference checkBoxPreference = (CheckBoxPreference) fragment.findPreference("enableFingerprintAuth");
                checkBoxPreference.setChecked(false); // Will execute cleanup automatically
            }
        }
    }
}
