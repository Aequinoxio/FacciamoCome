package com.example.utente.facciamocome;

import android.app.Application;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

/**
 * Implementation of App Widget functionality.
 */
public class FacciamoComeWidget extends AppWidgetProvider implements AsyncTaskCompleteListener<String> {

    private static String phrase;
    private Context ctx;

    private static boolean showProgressBar=false;

    // Chiave per passare la frase alla ShareActivity per la condivisione della frase
    //  public static final String ACTION_BUTTON1_CLICKED = "com.example.utente.FacciamoCome.BUTTON1_CLICKED";

    // Id unico per l'intent. In caso contrario non viene passata alcuna stringa ma viene riutilizzato uno degli intent già esistenti
    private static int shareActivityRequestCode=789;
    private static int broadCastRequestCode=790;

    public void onTaskComplete(String result){
        // Aggiorna la label del widget
        // Log.e("Widget",result);

        phrase=result;

        showProgressBar=false;

        updateUI(ctx);
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        CharSequence widgetText = phrase;
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.facciamo_come_widget);
        views.setTextViewText(R.id.txtPhraseWidget, widgetText);

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
        PendingIntent pendingIntentBtn = PendingIntent.getActivity(context, shareActivityRequestCode, intentBtn, PendingIntent.FLAG_UPDATE_CURRENT);
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

        ctx=context;

        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        // Log.e("Widget_enabled","enabled");

        // Inizializzo la textarea della frase
        phrase=ApplicationUtils.loadLatestPhrase(context, ApplicationUtils.SharedWidgetLatestPhraseKey);

        // Salvo il contesto per ogni evenienza
        ctx=context;

        // Check per il prio avvio: Se la frase è quella di default e sono connesso ad internet, avvio la richiesta al server
        if (phrase.equals(context.getString(R.string.app_name)) && ApplicationUtils.isInternetAvailable(context)){
            new GetAsyncServerResponse(context, this).execute();
        }


//        if (ApplicationUtils.isInternetAvailable(context)) {
//            new GetAsyncServerResponse(context, this).execute();
//        } else {
//            // Decommenta se vuoi vedere il toast anche quando metti il widget sulla schermata
//            // showToastMessage(context);
//        }
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        super.onReceive(context, intent);


        // Log.e("Widget_onReceive","onReceive");
        // Prelevo la prossima frase
        if (ApplicationUtils.isInternetAvailable(context)) {
            new GetAsyncServerResponse(context, this).execute();
            showProgressBar=true;
        } else {
            if (intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
                showToastMessage(context);
            }
        }

        updateUI(context);

    }

    /**
     * Aggiorna l'UI. Sono istruzioni presenti nel metodo onReceive ma estratte per essere riutilizzate altrove
     * @param context
     */
    private void updateUI(Context context){
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.facciamo_come_widget);

        if (phrase==null){
            phrase=ApplicationUtils.loadLatestPhrase(context, ApplicationUtils.SharedWidgetLatestPhraseKey);
        } else {
            ApplicationUtils.saveLatestPhrase(context, ApplicationUtils.SharedWidgetLatestPhraseKey, phrase);
        }

        // find your TextView here by id here and update it.
     //   views.setTextViewText(R.id.txtPhraseWidget, phrase);
        //views.setProgressBar(R.id.progressBar,10,10,progressBarShowd);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisAppWidget = new ComponentName(context.getPackageName(), FacciamoComeWidget.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

        onUpdate(context, appWidgetManager, appWidgetIds);
    }

    private void showToastMessage(Context context){
        Toast.makeText(context, context.getString(R.string.noInternet),Toast.LENGTH_SHORT).show();
    }
}

