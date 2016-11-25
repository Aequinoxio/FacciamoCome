package com.example.utente.facciamocome;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.example.utente.facciamocome.databaseLocale.DataAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Implementation of App Widget functionality.
 */

public class FacciamoComeWidget extends AppWidgetProvider implements AsyncTaskCompleteListener<Integer, String> {

    ApplicationUtils applicationUtils = ApplicationUtils.getInstance();
   // private static DataAdapter mDbHelper = null;

    private static String phrase;
    private static int    phrase_id;

    private Context ctx;

    private Handler mHandler = new Handler();  // handler per attendere una frazione di secondo e mostrare il blink del widget e la progress bar

    private static boolean showProgressBar=false;
    private static Date nextUpdate = new Date();

    private Notification myNotication;

    // Id unico per l'intent. In caso contrario non viene passata alcuna stringa ma viene riutilizzato uno degli intent già esistenti
    private static int shareActivityRequestCodeWidget =789;
    private static int shareActivityRequestCodeNotification=790;
    private static int broadCastRequestCode=791;

    public void onTaskComplete(Integer id, String result){
        // Aggiorna la label del widget
        // Log.e("Widget",result);
        if (id==null||result==null){
            // TODO: Frase di default in caso il server non risponda. Se previsto dai settings mostrare una frase dal DB locale
            // ed eventualmente cambiare il colore del widget o dell'activity. Per ora inserisco un default
            id= Integer.valueOf (ctx.getString(R.string.settingsFirstPhraseID));
            phrase=ctx.getString(R.string.settingsFirstPhrase);
        } else {
            phrase=result;
            phrase_id=id;
        }

       // Log.e(Thread.currentThread().getStackTrace()[2].getMethodName(),phrase);

        showProgressBar = false;
        ApplicationUtils.updateLocalDB(ctx,phrase_id,phrase,ApplicationUtils.phraseFromWidget);
        updateUI(ctx);
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // Log.e(Thread.currentThread().getStackTrace()[2].getMethodName(),"");
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.facciamo_come_widget);

        CharSequence widgetText = phrase;
        views.setTextViewText(R.id.txtPhraseWidget, widgetText);

        // Aggiorno l'indicatore della prossima frase
        SimpleDateFormat ft = new SimpleDateFormat ("HH:mm");
        views.setTextViewText(R.id.txtTime, "Next\n"+ft.format(nextUpdate));

        // Click sulla label per il refresh
        Intent intentSync = new Intent(context, FacciamoComeWidget.class);
        intentSync.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE); //You need to specify the action for the intent. Right now that intent is doing nothing for there is no action to be broadcasted.
        //You need to specify a proper flag for the intent. Or else the intent will become deleted.
        PendingIntent pendingSync = PendingIntent.getBroadcast(context,broadCastRequestCode, intentSync, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.txtPhraseWidget,pendingSync);
        views.setOnClickPendingIntent(R.id.relativeLayout,pendingSync);

        // Creo un intent specifico per lanciare la ShareActivity (l'ho resa non visibile nel manifest) al click sul pulsante nel widget
        Intent intentBtn = new Intent(context, ShareActivity.class);
        // intentBtn.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intentBtn.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);

        intentBtn.putExtra(context.getString(R.string.IntentExtraPhrase),phrase); // Passo la frase alla nuova attività
        //intentBtn.putExtra(context.getString(R.string.IntentExtraLink),context.getString(R.string.serverURL)); // Passo la frase alla nuova attività

        PendingIntent pendingIntentBtn = PendingIntent.getActivity(context, shareActivityRequestCodeWidget, intentBtn, PendingIntent.FLAG_UPDATE_CURRENT);
        // Get the layout for the App Widget and attach an on-click listener to the button
        views.setOnClickPendingIntent(R.id.buttonWdgShare, pendingIntentBtn);

        // Mostro o nascondo la progressbar
        if (showProgressBar){
            views.setViewVisibility(R.id.progressBar, View.VISIBLE);
            views.setInt(R.id.relativeLayout, "setBackgroundResource", R.drawable.roundedrect_solid);
        } else {
            // Impostandolo in INVISIBLE per motivi ignoti in Lollipop Samsung A5 resta a volte visibile anche se ferma
            views.setViewVisibility(R.id.progressBar, View.GONE);
            views.setInt(R.id.relativeLayout, "setBackgroundResource", R.drawable.roundedrect);
        }

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them

        // Log.e(Thread.currentThread().getStackTrace()[2].getMethodName(),"");

        ctx=context;
        for (int appWidgetId : appWidgetIds) {

            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        // Log.e("Widget_enabled","enabled");
        // Salvo il contesto per ogni evenienza
        ctx=context;

        ApplicationUtils.loadSharedPreferences(context);
        // Inizializzo con la frase delel preferences
        PhraseData p = ApplicationUtils.loadLatestPhrase(context, ApplicationUtils.phraseFromWidget);
        phrase=p.phrase;
        phrase_id=p.phrase_ID;

        //Log.e(Thread.currentThread().getStackTrace()[2].getMethodName(),"-----------------");

        // Non faccio nulla al primo avvio. Viene comunque invocato il metodo con l'azione Update.
        // L'aggiornamento lo faccio li
//        // Check per il primo avvio: Se la frase è quella di default e sono connesso ad internet, avvio la richiesta al server
//        if (ApplicationUtils.isInternetAvailable(context)){
//            new GetAsyncServerResponse(context, this).execute();
//        } else {
//            if (ApplicationUtils.isLoadFromLocalDBEnabled()) {
//                p = ApplicationUtils.getPhraseAndIdFromLocalDB(context);
//                phrase = p.phrase;
//                phrase_id = p.phrase_ID;
//            }
//        }

        setAlarm(context);
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        // Log.e(Thread.currentThread().getStackTrace()[2].getMethodName(),"");

        // Disabilito l'alarm
        disableAlarm(context);
        super.onDisabled(context);
    }

    @Override
    public void onReceive(Context context, Intent intent){
        super.onReceive(context, intent);
        // Log.e(Thread.currentThread().getStackTrace()[2].getMethodName(),"in on receive");
        ctx=context;

        ApplicationUtils applicationUtils = ApplicationUtils.getInstance();

        String action = intent.getAction();
        // Per sicurezza. In questo modo evito un controllo sull'action nulla in seguito
        if (action==null) {
            action="null";
        }

        //Log.e(Thread.currentThread().getStackTrace()[2].getMethodName(),action);

        // Esco subito se il widget è stato caricato o se è disabilitato o rimosso
        // Negli altri casi lo aggiorno
        if (action.equals(AppWidgetManager.ACTION_APPWIDGET_ENABLED) ||
                action.equals(AppWidgetManager.ACTION_APPWIDGET_DISABLED) ||
                action.equals(AppWidgetManager.ACTION_APPWIDGET_DELETED)){
            return;
        }

        // Se sono chiamato dalla main activity a seguito di un cambio di preferenze, reimposto solo l'allarme
        if (action.equals(ApplicationUtils.settingsWidgetUpdateFilter)){
            disableAlarm(context);
            setAlarm(context);
            updateUI(context);
            //Log.e(Thread.currentThread().getStackTrace()[2].getMethodName(),"Return");

            return;
        }

        // Log.e(Thread.currentThread().getStackTrace()[2].getMethodName(),action);
        // Log.e(Thread.currentThread().getStackTrace()[2].getMethodName(),"");

        if (ApplicationUtils.isInternetAvailable(context)) {
            // Prelevo la prossima frase
            new GetAsyncServerResponse(context, this).execute();
            showProgressBar=true;
            //Log.e(Thread.currentThread().getStackTrace()[2].getMethodName(),"IsInternetAvailable true");

        } else {
            // Estraggo una frase casuale dal db locale
            String toastMsg;
            //Log.e(Thread.currentThread().getStackTrace()[2].getMethodName(),"IsInternetAvailable false");

            if (ApplicationUtils.isLoadFromLocalDBEnabled()) {
                //Log.e(Thread.currentThread().getStackTrace()[2].getMethodName(),"isLoadFromLocalDBEnabled true");

                PhraseData p = ApplicationUtils.getPhraseAndIdFromLocalDB(context);
                phrase=p.phrase;
                phrase_id=p.phrase_ID;

                toastMsg=context.getString(R.string.noInternetWithLocalDB);

                // Aggiorno solo se ho una frase in locale
                ApplicationUtils.updateLocalDB(context,phrase_id,phrase,ApplicationUtils.phraseFromWidget);

            } else {
                //Log.e(Thread.currentThread().getStackTrace()[2].getMethodName(),"isLoadFromLocalDBEnabled false");

                toastMsg=context.getString(R.string.noInternet);
            }
            // Se aggiorno il widget tramite azioni di UPDATE mostro in questo caso un Toast sulla connettività
            if (action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
                showToastMessage(context, toastMsg);
            } else{
                //Log.e(Thread.currentThread().getStackTrace()[2].getMethodName(),"Not ACTION_APPWIDGET_UPDATE: "+action);
            }
        }

        // Se aggiorno il widget tramite azioni diverse dall'alarm, disabilito l'alarm (lo riabilito alla fine del metodo)
        // Se scatta l'alarm si disabilita da solo (non è repeated)
        if (!action.equals(ApplicationUtils.alarmWidgetUpdateFilter)){
            disableAlarm(context);
        } else{
            //Log.e(Thread.currentThread().getStackTrace()[2].getMethodName(),"Not alarmWidgetUpdateFilter: "+action);
        }

        // Reiposto sempre l'allarme anche se non c'è connettività. Evito di registrare un broadcastReceiver
        // per monitorare lo stato della connettività. Leggi:
        // https://developer.android.com/training/monitoring-device-state/connectivity-monitoring.html
        setAlarm(context);

        // Mostro sempre la notifica con il precedente messaggio
        updateUI(context);

        if (ApplicationUtils.isNotificationEnabled()) {
            showNotification(context);
        } else {
            //Log.e(Thread.currentThread().getStackTrace()[2].getMethodName(),"Not isNotificationEnabled: false"+action);

        }
    }


    /**
     * Gestisco con un handler il ritardo ne refresh del widget. Sembra che senza di questo il widget non venga aggiornato
     * durante il download della frase
     *
     * @param context
     */
    private void updateUI(Context context){
        // Ritardo l'aggiornamendo dell'UI
        ctx=context;
        if (phrase==null){
            PhraseData p = ApplicationUtils.loadLatestPhrase(context, ApplicationUtils.phraseFromWidget);
            phrase=p.phrase;
            phrase_id=p.phrase_ID;
        } else {
            ApplicationUtils.saveLatestPhrase(context, ApplicationUtils.phraseFromWidget, phrase_id, phrase);
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                updateUINow(ctx);
            }
        };

        // Aspetto un millisecondo
        mHandler.postDelayed(runnable, 1);
    }

    /**
     * Aggiorna l'UI. Sono istruzioni presenti nel metodo onReceive ma estratte per essere riutilizzate altrove
     * @param context
     */
    private void updateUINow(Context context){
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.facciamo_come_widget);
        // Log.e(Thread.currentThread().getStackTrace()[2].getMethodName(),phrase);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisAppWidget = new ComponentName(context.getPackageName(), FacciamoComeWidget.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

        onUpdate(context, appWidgetManager, appWidgetIds);
    }

    private void showToastMessage(Context context, String msg){
        if (ApplicationUtils.isShowToastOnConnection()) {
            Toast toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
            TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
            if (v != null) v.setGravity(Gravity.CENTER);
            toast.show();
        }
    }

    private void showNotification(Context context){
       // Log.e(Thread.currentThread().getStackTrace()[2].getMethodName(),phrase);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        /**************************/
        // Share
        /**************************/
        // Creo un intent specifico per lanciare la ShareActivity (l'ho resa non visibile nel manifest) al clico sul pulsante nel widget
        Intent intentBtn2 = new Intent(context, ShareActivity.class);
        intentBtn2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intentBtn2.putExtra(context.getString(R.string.IntentExtraPhrase),phrase); // Passo la frase alla nuova attività

        // Devo passare un altro codice altrimenti viene utilizzato il pending intent del bottone sul widget
        // STRANO, essendo impostato update_current dovrebbe sostituirlo...
        PendingIntent pendingIntentBtn2 = PendingIntent.getActivity(context, shareActivityRequestCodeNotification, intentBtn2, PendingIntent.FLAG_CANCEL_CURRENT);

//        Log.e("showToastMessage", intentBtn2.getStringExtra("com.example.utente.facciamocome.Notifica"));

        /**************************/
        // Notifica
        /**************************/
        //API level 11
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,ApplicationUtils.notificationID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(context);

        builder.setAutoCancel(false);
        builder.setTicker(phrase);
        builder.setContentTitle(context.getString(R.string.notification_Title));
        builder.setContentText(phrase);

        // Log.e(Thread.currentThread().getStackTrace()[2].getMethodName()+" - 2",phrase);

        builder.setSmallIcon(R.drawable.ic_account_balance_white_18dp);
        builder.setContentIntent(pendingIntent);
        builder.setOngoing(false);
        builder.setSubText(context.getString(R.string.notification_SubText));   //API level 16

        builder.addAction(R.drawable.ic_account_balance_black_18dp,"Condividi", pendingIntentBtn2);

        if (ApplicationUtils.isNotificationSoundEnabled()) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        }
        builder.setOnlyAlertOnce(false);

        builder.setStyle(new Notification.BigTextStyle(builder).bigText(phrase));

        myNotication= builder.build();

        // myNotication.flags |= Notification.FLAG_INSISTENT;
        myNotication.flags |= Notification.VISIBILITY_PUBLIC;

        manager.notify(ApplicationUtils.notificationID, myNotication);
    }

    private void setAlarm(Context context){

        // Log.e(Thread.currentThread().getStackTrace()[2].getMethodName(),"----");

        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, FacciamoComeWidget.class);
        intent.setAction(ApplicationUtils.alarmWidgetUpdateFilter);
        PendingIntent pi = PendingIntent.getBroadcast(context,
                ApplicationUtils.alarmWidgetUpdateActionRequestCode ,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Ripeto l'alarm -- Mi sembra che rimanga settato anche quando lo disabilito. Faccio in un altro modo... Reimposto l'alarm quando mi serve
//        am.setRepeating(AlarmManager.RTC, System.currentTimeMillis()+ 1000 * ApplicationUtils.alarmStartSecs,
//                1000 * ApplicationUtils.alarmRepeatSecs, pi);


        // Calcolo l'inizio del minuto
        long ora = ((System.currentTimeMillis()/60000)*60000);
        nextUpdate.setTime( ora + 1000 * ApplicationUtils.getAlarmRepeatSecs());
        am.set(AlarmManager.RTC_WAKEUP, nextUpdate.getTime(), pi);
    }

    private void disableAlarm(Context context){
        // Disabilito l'alarm
        // Log.e(Thread.currentThread().getStackTrace()[2].getMethodName(),"----");

        Intent intent = new Intent(context, FacciamoComeWidget.class);
        intent.setAction(ApplicationUtils.alarmWidgetUpdateFilter);
        PendingIntent sender = PendingIntent.getBroadcast(context,
                ApplicationUtils.alarmWidgetUpdateActionRequestCode ,
                intent,
                PendingIntent.FLAG_NO_CREATE);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
}

