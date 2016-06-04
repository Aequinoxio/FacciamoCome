package com.example.utente.facciamocome;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteConstraintException;
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
    public final static String settingsWidgetUpdateFilter= "com.example.utente.facciamocome.SettingsUpdateWidget";

    public final static int    alarmWidgetUpdateActionRequestCode=7901;
    public final static int    settingsWidgetUpdateActionRequestCode=7902;

    public final static int    notificationID = 8901;

    public final static int    SETTINGS_RESULTCODE=9876;

    public static final String TAG_PHRASE= "phrase";
    public static final String TAG_ID = "id";
    public static final String TAG_COUNTRY_ID = "country_id";
    public static final String TAG_COLOR= "color";

    public static DataAdapter mDbHelper=null;

    public final static int phraseFromWidget=0;  // Costanti per indicare da quale parte inserisco le frasi nel DB
    public final static int phraseFromApp=1;

    private static boolean notificationEnabled;
    private static boolean notificationSoundEnabled;
    private static boolean loadFromLocalDBEnabled;
    private static boolean showToastOnConnection;

    private static int     alarmRepeatSecs=10*60; // inizializzo a 10 minuti per sicurezza
    private final static int minAlarmRepeatSecs=10*60;

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
        return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
    }

    // Aggiorno il DB creandolo se necessario. Ne conservo una istanza
    public static DataAdapter getDatabaseInstance(Context context){
        if (mDbHelper==null) {
            mDbHelper = new DataAdapter(context);
            mDbHelper.createDatabase();
        }
        return mDbHelper;
    }

    /**
     * Aggiorno il DB con una nuova frase
     * @param context
     * @param phrase_ID
     * @param phrase
     * @param widgetOrApp   // 0 se devo aggiornare le frasi dell'app, 1 per il widget
     */
    public static void updateLocalDB(Context context, int phrase_ID, String phrase, int widgetOrApp){

        int historyLimit=context.getResources().getInteger(R.integer.sqlHistoryLimit);

        mDbHelper=getDatabaseInstance(context);

        mDbHelper.open();

        String sql = "INSERT INTO history (id, phrase, source_widget, created_at) VALUES("+ String.valueOf(phrase_ID) +", "+
                DatabaseUtils.sqlEscapeString(phrase)+", "+String.valueOf(widgetOrApp) +", datetime())";
        try {
            mDbHelper.setValues(sql);
        } catch (SQLiteConstraintException sqlCE){
            sqlCE.printStackTrace();
        }

        String widgetOrAppString = String.valueOf(widgetOrApp);
        // Conto se siamo già a "historyLimit" frasi memorizzate
        int numFrasi = Integer.valueOf(mDbHelper.getValues("SELECT count (*) FROM history where source_widget = "+widgetOrAppString,0).get(0));

        if (numFrasi>historyLimit) {
            sql = "delete from history where source_widget = "+ widgetOrAppString +" and autocounter not in (select autocounter from history where source_widget = "+widgetOrAppString+" order by created_at desc limit "+String.valueOf(historyLimit)+")";
            // Elimino le frasi in più
            mDbHelper.setValues(sql);
        }

        mDbHelper.close();
    }

    public static PhraseData getPhraseAndIdFromLocalDB(Context context){
        PhraseData p = new PhraseData();

        // Aggiorno il DB creandolo se necessario
        mDbHelper = ApplicationUtils.getDatabaseInstance(context);

        // Prendo la frase dal DB locale
        mDbHelper.open();

        Cursor cursor = mDbHelper.getCursor(context.getString(R.string.sqlSelectLocalRandomIdPhrase));
        p.phrase_ID= cursor.getInt(0);
        p.phrase =cursor.getString(1);

        mDbHelper.close();

        return (p);
    }


    public static void loadSharedPreferences(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        notificationEnabled = sharedPreferences.getBoolean(context.getString(R.string.settingsReceiveNotifications),true);
        notificationSoundEnabled = sharedPreferences.getBoolean(context.getString(R.string.settingsReceiveNotificationsSound),false);
        loadFromLocalDBEnabled = sharedPreferences.getBoolean(context.getString(R.string.settingsLoadFromLocalDB),true);
        showToastOnConnection = sharedPreferences.getBoolean(context.getString(R.string.settingsShowToast),true);

        // Per sicurezza imposto il minimo valore a minAlarmRepeatSecs
        alarmRepeatSecs = Math.max(minAlarmRepeatSecs,Integer.valueOf(
                sharedPreferences.getString(context.getString(R.string.settingsRefreshTime),context.getString(R.string.app_name))
        ));
    }

    public static boolean isNotificationEnabled() {
        return notificationEnabled;
    }

    public static boolean isNotificationSoundEnabled() {
        return notificationSoundEnabled;
    }

    public static boolean isLoadFromLocalDBEnabled() {
        return loadFromLocalDBEnabled;
    }

    public static boolean isShowToastOnConnection() {
        return showToastOnConnection;
    }

    public static int getAlarmRepeatSecs() {
        return alarmRepeatSecs;
    }
}
