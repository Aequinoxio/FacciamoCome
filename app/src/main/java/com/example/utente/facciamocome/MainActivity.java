package com.example.utente.facciamocome;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.utente.facciamocome.databaseLocale.DataAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements AsyncTaskCompleteListener<String>{
    private static String url ;
    // private boolean showProgressBar=false;

    // DB locale
    private static DataAdapter mDbHelper = null;

    // ID per cancellare il file immagine dopo che l'ho condiviso
    static final int SHARE_PICKER=777;

    private String phrase;

    // private ProgressDialog pDialog;

    public void onTaskComplete(String result){
        // Aggiorna la label del widget
        phrase=result;
        TextView textView = (TextView) findViewById(R.id.txtPhrase);
        textView.setText(result);
        ApplicationUtils.saveLatestPhrase(this.getApplicationContext(),ApplicationUtils.SharedActivityLatestPhraseKey, phrase);

        //showProgressBar=false;
        showProgressBar(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        url = getString(R.string.serverURL);

        // Carico l'ultima frase mostrata
        phrase=ApplicationUtils.loadLatestPhrase(this.getApplicationContext(),ApplicationUtils.SharedActivityLatestPhraseKey);
        TextView textView = (TextView) findViewById(R.id.txtPhrase);
        textView.setText(phrase);

        // Aggiorno il DB creandolo se necessario
        mDbHelper = ApplicationUtils.getDatabaseInstance(this.getApplicationContext());

        // Check per il primo avvio: Se la frase è quella di default e sono connesso ad internet, avvio la richiesta al server
        if (/*phrase.equals(context.getString(R.string.app_name)) && */ ApplicationUtils.isInternetAvailable(MainActivity.this)){
            startServerRequest();
        } else {
            // Prendo la frase dal DB locale
            mDbHelper.open();
            phrase=mDbHelper.getValues(getString(R.string.sqlSelectLocalRandomPhrase), 0).get(0);
            mDbHelper.close();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void getPhrase(View v){
        startServerRequest();
    }

    private void startServerRequest(){
        // Occorre passare il contesto della main activity e non dell'applicatione (getApplicationContext)
        Context context = MainActivity.this;

        //showProgressBar=true;
        showProgressBar(true);

        if (ApplicationUtils.isInternetAvailable(context)) {
            new GetAsyncServerResponse(context, this).execute();
        } else {
            Toast toast = Toast.makeText(context, context.getString(R.string.noInternet),Toast.LENGTH_SHORT);
            TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
            if( v != null) v.setGravity(Gravity.CENTER);
            toast.show();

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

            // Chiamo esplicitamente onTackComplete per aggiornare l'Activity
            onTaskComplete(phrase);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_about) {
            String s = getString(R.string.app_name) +" - Ver. " + BuildConfig.VERSION_NAME ;
            s+="\nby "+ getString(R.string.Autore);
            s+="\n\n"+getString(R.string.descrizione);
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.action_about)
                    .setMessage(s)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    }).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void scegliShareMethod(View view){
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);

        sharingIntent.setType(getString(R.string.ShareMimeType));
        sharingIntent.putExtra(Intent.EXTRA_TEXT, phrase);

        startActivityForResult(Intent.createChooser(sharingIntent, getString(R.string.CondividiCon)), SHARE_PICKER);
    }

    private void showProgressBar(boolean showProgressBar){
        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar2);
        progressBar.setVisibility((showProgressBar && ApplicationUtils.isInternetAvailable(MainActivity.this)) ?
                View.VISIBLE:View.INVISIBLE);
    }
}
