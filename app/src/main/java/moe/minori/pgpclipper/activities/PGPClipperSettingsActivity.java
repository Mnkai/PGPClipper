package moe.minori.pgpclipper.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Objects;

import androidx.annotation.Nullable;
import co.infinum.goldfinger.Goldfinger;
import moe.minori.pgpclipper.PGPClipperService;
import moe.minori.pgpclipper.R;
import moe.minori.pgpclipper.util.Constants;

public class PGPClipperSettingsActivity extends Activity {

    SettingFragment fragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if ( Objects.requireNonNull(sharedPreferences.getString("themeSelection", "dark")).equals("dark") )
        {
            setTheme(R.style.AppThemeDark);
        }
        else
        {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content, fragment = new SettingFragment()).commit();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        Preference.OnPreferenceChangeListener pgpClipperEnabledCheckboxListener = new Preference.OnPreferenceChangeListener() {
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
        };

        Preference.OnPreferenceClickListener issueTrackerPreferenceItemListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                Intent webBrowserLaunchIntent = new Intent(Intent.ACTION_VIEW);
                webBrowserLaunchIntent.setData(Uri.parse("https://github.com/Mnkai/PGPClipper"));
                startActivity(webBrowserLaunchIntent);

                return true;
            }
        };

        Preference.OnPreferenceClickListener licensePreferenceItemListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                Intent webBrowserLaunchIntent = new Intent(Intent.ACTION_VIEW);
                webBrowserLaunchIntent.setData(Uri.parse("https://github.com/Mnkai/PGPClipper/blob/master/LICENSE"));
                startActivity(webBrowserLaunchIntent);

                return true;
            }
        };

        Preference.OnPreferenceClickListener thirdPartyLicensePreferenceItemListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                Intent webBrowserLaunchIntent = new Intent(Intent.ACTION_VIEW);
                webBrowserLaunchIntent.setData(Uri.parse("https://github.com/Mnkai/PGPClipper/blob/master/LICENSE"));
                startActivity(webBrowserLaunchIntent);

                return true;
            }
        };

        Preference.OnPreferenceChangeListener pgpServiceProviderAppListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String providerApp = (String) newValue;
                final CheckBoxPreference pgpClipperEnabledCheckbox = (CheckBoxPreference) fragment.findPreference("pgpClipperEnabledCheckbox");

                if (providerApp == null || "".equals(providerApp)) {
                    pgpClipperEnabledCheckbox.setEnabled(false);
                    pgpClipperEnabledCheckbox.setChecked(false);
                    stopService(new Intent(PGPClipperSettingsActivity.this, PGPClipperService.class));
                } else {
                    pgpClipperEnabledCheckbox.setEnabled(true);
                }
                return true;
            }
        };

        Preference.OnPreferenceChangeListener themePrefListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                final ListPreference themePref = (ListPreference) fragment.findPreference("themeSelection");

                switch ((String) newValue) {
                    case "dark":
                        themePref.setSummary(getResources().getString(R.string.darkText));
                        break;
                    case "light":
                        themePref.setSummary(getResources().getString(R.string.lightText));
                        break;
                }

                PGPClipperSettingsActivity.this.recreate();
                return true;
            }
        };

        Preference.OnPreferenceChangeListener NFCAuthPrefListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!((boolean) newValue)) {
                    // delete current hash and encrypted data
                    final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    final SharedPreferences.Editor editor = sharedPreferences.edit();

                    NFCAuthenticationSetupActivity.initSetting(editor);
                } else {
                    // start NFCAuthSetupActivity
                    Intent intent = new Intent(PGPClipperSettingsActivity.this, NFCAuthenticationSetupActivity.class);

                    startActivityForResult(intent, Constants.NFC_SETUP_REQUEST_CODE);
                }
                return true;
            }
        };

        Preference.OnPreferenceChangeListener fingerprintAuthPrefListener = new Preference.OnPreferenceChangeListener() {
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
        };

        fragment.findPreference("pgpClipperEnabledCheckbox").setOnPreferenceChangeListener(pgpClipperEnabledCheckboxListener);
        fragment.findPreference("issueTrackerPreferenceItem").setOnPreferenceClickListener(issueTrackerPreferenceItemListener);
        fragment.findPreference("licensePreferenceItem").setOnPreferenceClickListener(licensePreferenceItemListener);
        fragment.findPreference("thirdPartyLicensePreferenceItem").setOnPreferenceClickListener(thirdPartyLicensePreferenceItemListener);
        fragment.findPreference("themeSelection").setOnPreferenceChangeListener(themePrefListener);
        fragment.findPreference("pgpServiceProviderApp").setOnPreferenceChangeListener(pgpServiceProviderAppListener);
        fragment.findPreference("enableNFCAuth").setOnPreferenceChangeListener(NFCAuthPrefListener);
        fragment.findPreference("enableFingerprintAuth").setOnPreferenceChangeListener(fingerprintAuthPrefListener);

        // From M, PGPClipper requires opting out from battery optimisation for background clipboard monitoring
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            String PACKAGE_NAME = getApplicationContext().getPackageName();

            if (!pm.isIgnoringBatteryOptimizations(PACKAGE_NAME))
            {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(intent);

                PGPClipperSettingsActivity.this.startActivity(intent);
                Toast.makeText(getApplicationContext(), getString(R.string.battery_optimazation_information_toast), Toast.LENGTH_LONG).show();
            }
        }

        // Check AES-ECB vulnerability mitigation
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        if (!sharedPreferences.getBoolean("AESECBVulnMitigated", false))
        {
            Log.d("PGPClipSettingsActivity", "First run since AES algorithm change - flushing key database");

            // Since implementation before 0.22 was not suitable (AES-ECB), remove NFC key encryption data and start from scratch
            CheckBoxPreference nfcPref = (CheckBoxPreference) fragment.findPreference("enableNFCAuth");

            if (nfcPref.isChecked())
            {
                Toast.makeText(getApplicationContext(), getString(R.string.aes_key_init_notice_toast), Toast.LENGTH_LONG).show();
            }

            nfcPref.setChecked(false);

            // Listener should fire to remove data, but just to make sure...
            NFCAuthenticationSetupActivity.initSetting(editor);

            editor.putBoolean("AESECBVulnMitigated", true);

            editor.apply();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        final ListPreference themePref = (ListPreference) fragment.findPreference("themeSelection");

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final CheckBoxPreference fingerprintCheckboxPreference = (CheckBoxPreference) fragment.findPreference("enableFingerprintAuth");
        final CheckBoxPreference pgpClipperEnabledCheckbox = (CheckBoxPreference) fragment.findPreference("pgpClipperEnabledCheckbox");

        themePref.setEntryValues(R.array.themes_values);
        themePref.setEntries(R.array.themes);

        String currentVal = sharedPreferences.getString("themeSelection", "dark");
        if (currentVal != null) {
            switch (currentVal) {
                case "dark":
                    themePref.setSummary(getResources().getString(R.string.darkText));
                    break;
                case "light":
                    themePref.setSummary(getResources().getString(R.string.lightText));
                    break;
            }
        }

        String providerApp = sharedPreferences.getString("pgpServiceProviderApp", null);
        if (providerApp == null || "".equals(providerApp)) {
            pgpClipperEnabledCheckbox.setEnabled(false);
            pgpClipperEnabledCheckbox.setChecked(false);
            stopService(new Intent(PGPClipperSettingsActivity.this, PGPClipperService.class));
        } else {
            if (pgpClipperEnabledCheckbox.isChecked()) {
                startService(new Intent(PGPClipperSettingsActivity.this, PGPClipperService.class));
            }
            pgpClipperEnabledCheckbox.setEnabled(true);
        }

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) // Fingerprint API not supported below M
            fingerprintCheckboxPreference.setEnabled(false);
        else {
            fingerprintCheckboxPreference.setEnabled(true);
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
