package com.example.utente.facciamocome;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

import com.example.utente.facciamocome.databaseLocale.DataAdapter;

/**
 * Created by utente on 30/05/2016.
 */
public class ApplicationUtils {
    public final static String SharedWidgetLatestPhraseKey="widgetLatestPhrase";
    public final static String SharedActivityLatestPhraseKey="activityLatestPhrase";

    public final static String alarmWidgetUpdateFilter= "com.example.utente.facciamocome.AlarmUpdateWidget";
    public final static int    alarmWidgetUpdateActionRequestCode=7901;

    public final static int    notificationID = 8901;

    public final static int    alarmStartSecs = 1;
    public final static int    alarmRepeatSecs = 10*60; // 10 minuti

    public static final String TAG_PHRASE= "phrase";
    public static final String TAG_ID = "id";
    public static final String TAG_COUNTRY_ID = "country_id";
    public static final String TAG_COLOR= "color";

    public static DataAdapter mDbHelper=null;

    private static ApplicationUtils ourInstance = new ApplicationUtils();

    public static ApplicationUtils getInstance() {
        return ourInstance;
    }

    private ApplicationUtils() {
    }

    // Carico l'ultima
    //frase mostrata dalle shared preferences e se non la trovo o se è vuota uso l'appname
    public static String loadLatestPhrase(Context context, String sharedKey){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        String s=sharedPreferences.getString(sharedKey,context.getString(R.string.app_name));

        if (s.trim().equals("")) s=context.getString(R.string.app_name);

        return (s);
    }

    // Salvo nelle shared prefs l'ultima frase mostrata
    public static void saveLatestPhrase(Context context, String sharedKey, String sharedValue){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(sharedKey, sharedValue);
        editor.apply();
    }

    public static boolean isInternetAvailable(Context context){
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }

    // Aggiorno il DB creandolo se necessario. Ne conservo una istanza
    public static DataAdapter getDatabaseInstance(Context context){
        if (mDbHelper==null) {
            mDbHelper = new DataAdapter(context);
            mDbHelper.createDatabase();
        }
        return mDbHelper;
    }
}
