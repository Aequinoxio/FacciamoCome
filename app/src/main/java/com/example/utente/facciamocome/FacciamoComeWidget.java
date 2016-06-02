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

/**
 * Implementation of App Widget functionality.
 */
public class FacciamoComeWidget extends AppWidgetProvider implements AsyncTaskCompleteListener<String> {

    private static DataAdapter mDbHelper = null;

    private static String phrase;
    private Context ctx;

    private Handler mHandler = new Handler();  // handler per attendere una frazione di secondo e mostrare il blink del widget e la progress bar

    private static boolean showProgressBar=false;
    private static Date nextUpdate = new Date();

    private Notification myNotication;

    // Chiave per passare la frase alla ShareActivity per la condivisione della frase
    //  public static final String ACTION_BUTTON1_CLICKED = "com.example.utente.FacciamoCome.BUTTON1_CLICKED";

    // Id unico per l'intent. In caso contrario non viene passata alcuna stringa ma viene riutilizzato uno degli intent già esistenti
    private static int shareActivityRequestCodeWidget =789;
    private static int shareActivityRequestCodeNotification=790;
    private static int broadCastRequestCode=791;

    public void onTaskComplete(String result){
        // Aggiorna la label del widget
        // Log.e("Widget",result);

        phrase=result;
       // Log.e(Thread.currentThread().getStackTrace()[2].getMethodName(),phrase);

        showProgressBar = false;
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

        // Creo un intent specifico per lanciare la ShareActivity (l'ho resa non visibile nel manifest) al clico sul pulsante nel widget
        Intent intentBtn = new Intent(context, ShareActivity.class);
        // intentBtn.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intentBtn.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intentBtn.putExtra(context.getString(R.string.IntentExtraPhrase),phrase); // Passo la frase alla nuova attività
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

        // Aggiorno il DB creandolo se necessario
        mDbHelper = ApplicationUtils.getDatabaseInstance(context);

        // Inizializzo la textarea della frase
        //phrase=ApplicationUtils.loadLatestPhrase(context, ApplicationUtils.SharedWidgetLatestPhraseKey);

       // Log.e(Thread.currentThread().getStackTrace()[2].getMethodName(),"");

        // Salvo il contesto per ogni evenienza
        ctx=context;

        // Check per il primo avvio: Se la frase è quella di default e sono connesso ad internet, avvio la richiesta al server
        if (/*phrase.equals(context.getString(R.string.app_name)) && */ ApplicationUtils.isInternetAvailable(context)){
            new GetAsyncServerResponse(context, this).execute();
        } else {
            // Prendo la frase dal DB locale
            mDbHelper.open();
            phrase=mDbHelper.getValues(context.getString(R.string.sqlSelectLocalRandomPhrase), 0).get(0);
            mDbHelper.close();
        }

        setAlarm(context);
      //  updateUI(context);
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled

        // Disabilito l'alarm

       // Log.e(Thread.currentThread().getStackTrace()[2].getMethodName(),"");

        disableAlarm(context);
        super.onDisabled(context);
    }

    @Override
    public void onReceive(Context context, Intent intent){
        super.onReceive(context, intent);

        String action = intent.getAction();
        // Per sicurezza. In questo modo evito un controllo sull'action nulla in seguito
        if (action==null) {
            action="";
        }

        // Log.e(Thread.currentThread().getStackTrace()[2].getMethodName(),action);
        // Log.e(Thread.currentThread().getStackTrace()[2].getMethodName(),"");

        if (ApplicationUtils.isInternetAvailable(context)) {
            // Se aggiorno il widget tramite azioni diverse dall'alarm, disabilito l'alarm (lo riabilito alla fine del metodo)
            // Se scatta l'alarm si disabilita da solo (non è repeated)
            if (!action.equals(ApplicationUtils.alarmWidgetUpdateFilter)){
                disableAlarm(context);
            }

            // Prelevo la prossima frase
            new GetAsyncServerResponse(context, this).execute();
            showProgressBar=true;

        } else {
            // Estraggo una frase casuale dal db locale
            // TODO: Punto per i settings
            if (mDbHelper==null){
                // DEBUG
                // Log.e (Thread.currentThread().getStackTrace()[2].getMethodName(),"mDbHelper NULL");
                //
                mDbHelper=ApplicationUtils.getDatabaseInstance(context);
            }

            // Prendo la frase dal db locale
            mDbHelper.open();
            phrase=mDbHelper.getValues(context.getString(R.string.sqlSelectLocalRandomPhrase),0).get(0);
            mDbHelper.close();

            // Se aggiorno il widget tramite azioni di UPDATE, disabilito l'alarm (lo riabilito alla fine del metodo)
            // Se scatta l'alarm si disabilita da solo (non è repeated)
            // Mostro in questo caso un Toast sulla connettività
            if (action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
                showToastMessage(context);
                disableAlarm(context);
            }
        }

        // Reiposto sempre l'allarme anche se non c'è connettività. Evito di registrare un broadcastReceiver
        // per monitorare lo stato della connettività. Leggi:
        // https://developer.android.com/training/monitoring-device-state/connectivity-monitoring.html
        setAlarm(context);

        // Mostro sempre la notifica con il precedente messaggio
        updateUI(context);
        showNotification(context);
    }


    /**
     * Gestisco con un handler il ritardo ne refresh del widget. Sembra che senza di questo il widget non venga aggiornato
     * durante il download della frase
     *
     * @param context
     */
    private void updateUI(Context context){
        // Ritardo il ringtone
        ctx=context;
        if (phrase==null){
            phrase=ApplicationUtils.loadLatestPhrase(context, ApplicationUtils.SharedWidgetLatestPhraseKey);
        } else {
            ApplicationUtils.saveLatestPhrase(context, ApplicationUtils.SharedWidgetLatestPhraseKey, phrase);
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

    private void showToastMessage(Context context){
        Toast toast = Toast.makeText(context, context.getString(R.string.noInternet),Toast.LENGTH_SHORT);
        TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
        if( v != null) v.setGravity(Gravity.CENTER);
        toast.show();
    }

    private void showNotification(Context context){
       // Log.e(Thread.currentThread().getStackTrace()[2].getMethodName(),phrase);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        /**************************/
        // Creo un intent specifico per lanciare la ShareActivity (l'ho resa non visibile nel manifest) al clico sul pulsante nel widget
        Intent intentBtn2 = new Intent(context, ShareActivity.class);
        intentBtn2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intentBtn2.putExtra(context.getString(R.string.IntentExtraPhrase),phrase); // Passo la frase alla nuova attività

        // Devo passare un altro codice altrimenti viene utilizzato il pending intent del bottone sul widget
        // STRANO, essendo impostato update_current dovrebbe sostituirlo...
        PendingIntent pendingIntentBtn2 = PendingIntent.getActivity(context, shareActivityRequestCodeNotification, intentBtn2, PendingIntent.FLAG_CANCEL_CURRENT);
        /**************************/

//        Log.e("showToastMessage", intentBtn2.getStringExtra("com.example.utente.facciamocome.Notifica"));

        // Intent per la notifica
        //API level 11
        Intent intent = new Intent(); //(context, MainActivity.class);
        //intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,ApplicationUtils.notificationID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(context);

        builder.setAutoCancel(true);
        builder.setTicker(phrase);
        builder.setContentTitle(context.getString(R.string.notification_Title));
        builder.setContentText(phrase);

        // Log.e(Thread.currentThread().getStackTrace()[2].getMethodName()+" - 2",phrase);

        builder.setSmallIcon(R.drawable.ic_account_balance_white_18dp);
        builder.setContentIntent(pendingIntent);
        builder.setOngoing(true);
        builder.setSubText(context.getString(R.string.notification_SubText));   //API level 16

        builder.addAction(R.drawable.ic_account_balance_black_18dp,"Condividi", pendingIntentBtn2);

        // TODO:punto per i settings
        // Decommenta se vuoi un suono nella notifica
        //builder.setSound(Uri.parse(RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION).getPath()));
        //builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
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

        // TODO: Punto per i settings
        nextUpdate.setTime(System.currentTimeMillis()+ 1000 * ApplicationUtils.alarmRepeatSecs);
        am.set(AlarmManager.RTC, nextUpdate.getTime(), pi);
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

