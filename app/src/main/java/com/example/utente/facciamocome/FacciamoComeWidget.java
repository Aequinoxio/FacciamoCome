package com.example.utente.facciamocome;

import android.app.Application;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
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

    public void onTaskComplete(String result){
        // Aggiorna la label del widget
        // Log.e("Widget",result);

        phrase=result;
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
        PendingIntent pendingSync = PendingIntent.getBroadcast(context,0, intentSync, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.txtPhraseWidget,pendingSync);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        // Log.e("Widget_update","update");
        //RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.facciamo_come_widget);

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
        phrase=context.getString(R.string.app_name);

        // Salvo il contesto per ogni evenienza
        ctx=context;
        new GetAsyncServerResponse(context,this).execute();
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        super.onReceive(context, intent);

        updateUI(context);

        // Log.e("Widget_onReceive","onReceive");
        new GetAsyncServerResponse(context,this).execute();
    }

    /**
     * Aggiorna l'UI. Sono istruzioni presenti nel metodo onReceive ma estratte per essere riutilizzate altrove
     * @param context
     */
    private void updateUI(Context context){
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.facciamo_come_widget);

        // find your TextView here by id here and update it.
        views.setTextViewText(R.id.txtPhraseWidget, phrase);
        //views.setProgressBar(R.id.progressBar,10,10,progressBarShowd);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisAppWidget = new ComponentName(context.getPackageName(), FacciamoComeWidget.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

        onUpdate(context, appWidgetManager, appWidgetIds);
    }
}

