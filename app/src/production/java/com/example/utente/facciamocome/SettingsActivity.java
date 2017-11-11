package com.example.utente.facciamocome;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

public class SettingsActivity extends AppCompatActivity {

    static int oldSettingsTimeSecs=0;

    @Override
    public void onBackPressed() {
        setSecondsChangedResult();
        super.onBackPressed();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // salvo il vecchio valore
        oldSettingsTimeSecs=ApplicationUtils.getAlarmRepeatSecs();

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
    }


    public static class SettingsFragment extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.user_settings);

            // TODO: Non mi sembra un metodo buono, trovarne un altro migliore per impostare il summary corretto nelle liste
            setListSummary("refreshTime");
            setListSummary("countryTarget");
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
            // TODO: Non mi sembra un metodo buono, trovarne un altro migliore per impostare il summary corretto nelle liste
            if (key.equals("refreshTime")|| key.equals("countryTarget")) {
                setListSummary(key);
            }
        }

        private void setListSummary(String key){
            Preference pref = findPreference(key);
            ListPreference mylistpreference= (ListPreference) getPreferenceScreen().findPreference(key);
            pref.setSummary(mylistpreference.getEntry());

//            if (oldSettingsTimeSecs==Integer.valueOf(mylistpreference.getValue().toString())){
//                ApplicationUtils.setSecsPreferencesChanged(false);
//            }else {
//                ApplicationUtils.setSecsPreferencesChanged(true);
//            }
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Up Action
        if (item.getItemId()==android.R.id.home) {
            setSecondsChangedResult();
        }
        return super.onOptionsItemSelected(item);
    }

    private void setSecondsChangedResult(){
        Intent intent = new Intent(this, MainActivity.class);

        ApplicationUtils.loadSharedPreferences(this);
        Boolean secsChanged;
        secsChanged = oldSettingsTimeSecs != ApplicationUtils.getAlarmRepeatSecs();
        // Ritorno il valore precedente
        intent.putExtra(ApplicationUtils.oldSettingsTimeSecsKey, secsChanged);

        setResult(ApplicationUtils.SETTINGS_RESULTCODE, intent);
    }
}
