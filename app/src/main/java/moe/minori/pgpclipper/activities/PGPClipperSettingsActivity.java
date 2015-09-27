package moe.minori.pgpclipper.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import moe.minori.pgpclipper.PGPClipperService;
import moe.minori.pgpclipper.R;

public class PGPClipperSettingsActivity extends AppCompatActivity {

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingFragment()).commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    public static class SettingFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.pref_general);

            findPreference("enabledCheckBox").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {

                    if ((boolean) newValue)
                        // enabled
                        getActivity().startService(new Intent(getActivity(), PGPClipperService.class));
                    else
                        // disabled
                        getActivity().stopService(new Intent(getActivity(), PGPClipperService.class));
                    return true;
                }
            });

            findPreference("issueTrackerPreferenceItem").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    Intent webBrowserLaunchIntent = new Intent(Intent.ACTION_VIEW);
                    webBrowserLaunchIntent.setData(Uri.parse("https://github.com/Mnkai/PGPClipper"));
                    startActivity(webBrowserLaunchIntent);

                    return true;
                }
            });

            findPreference("licensePreferenceItem").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    Intent webBrowserLaunchIntent = new Intent(Intent.ACTION_VIEW);
                    webBrowserLaunchIntent.setData(Uri.parse("https://github.com/Mnkai/PGPClipper/blob/master/LICENSE"));
                    startActivity(webBrowserLaunchIntent);

                    return true;
                }
            });

            findPreference("thirdPartyLicensePreferenceItem").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    Intent webBrowserLaunchIntent = new Intent(Intent.ACTION_VIEW);
                    webBrowserLaunchIntent.setData(Uri.parse("https://github.com/Mnkai/PGPClipper/blob/master/LICENSE"));
                    startActivity(webBrowserLaunchIntent);

                    return true;
                }
            });

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            final ListPreference themePref = (ListPreference) findPreference("themeSelection");

            themePref.setEntryValues(R.array.themes_values);
            themePref.setEntries(R.array.themes);

            String currentVal = sharedPreferences.getString("themeSelection", "dark");
            switch (currentVal)
            {
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

                    switch ((String) newValue)
                    {
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


        }
    }

}
