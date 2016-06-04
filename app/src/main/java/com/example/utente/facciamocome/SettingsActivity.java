package com.example.utente.facciamocome;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar2);
        setSupportActionBar(toolbar);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        try {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }catch (NullPointerException npe){
            npe.printStackTrace();
        }
        // Display the fragment as the main content
        getFragmentManager().beginTransaction()
                .replace(R.id.content_layout, new SettingsFragment())
                .commit();
    }

    @Override
    public void onPause(){
        super.onPause();
        setResult(ApplicationUtils.SETTINGS_RESULTCODE, new Intent(this, MainActivity.class));
    }


    public static class SettingsFragment extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.user_settings);

            setListSummary("refreshTime");
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen()
                    .getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen()
                    .getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("refreshTime")) {
                setListSummary(key);
            }
        }

        private void setListSummary(String key){
            Preference pref = findPreference(key);
            ListPreference mylistpreference= (ListPreference) getPreferenceScreen().findPreference(key);
            pref.setSummary(mylistpreference.getEntry());
        }
    }

}
