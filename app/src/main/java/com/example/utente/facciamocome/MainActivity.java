package com.example.utente.facciamocome;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.IntegerRes;
import android.support.annotation.StringDef;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.utente.facciamocome.databaseLocale.DataAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// TODO: vedere perchè il widget ha mostrato molte frasi di seguito e poi si è fermato ma sono state memorizzate nello storico solo "Facciamo come..."

public class MainActivity extends AppCompatActivity implements AsyncTaskCompleteListener<Integer, String>,
        AdapterView.OnItemSelectedListener, AdapterView.OnItemLongClickListener {

    private static String url ;

    // DB locale
    private static DataAdapter mDbHelper = null;
    private static boolean historicalDataFromWidget=false ;  // false o true a seconda se mostro i dati del db storico dell'app o del widget
    // ID per cancellare il file immagine dopo che l'ho condiviso
    static final int SHARE_PICKER=777;

    private String phrase;
    private int phrase_ID;

    // private ProgressDialog pDialog;

    public void onTaskComplete(Integer id, String result){
        // Aggiorna la label del widget
        phrase=result;
        phrase_ID=id;
        TextView textView = (TextView) findViewById(R.id.txtPhrase);
        textView.setText(result);
        ApplicationUtils.saveLatestPhrase(this.getApplicationContext(),ApplicationUtils.SharedActivityLatestPhraseKey, phrase);

        // Non aggiorno il db qui per non caricare subito la frase appena scaricata nell'history
        // Lo faccio al click sul bottone di refresh o della textview con la frase
        // Aggiornamento: Devo farlo qui e non nel medoto del pulsante perchè se l'attività viene fatta ripartire perdo la frase
        ApplicationUtils.updateLocalDB(this, phrase_ID, phrase,0);

        //showProgressBar=false;
        showProgressBar(false);
        loadListView();
    }

    //
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Se torno dalla setting activity allora aggiorno il widget
        if (requestCode==ApplicationUtils.SETTINGS_RESULTCODE){
            Intent intent = new Intent(this, FacciamoComeWidget.class);
            intent.setAction(ApplicationUtils.settingsWidgetUpdateFilter);
            int ids[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), FacciamoComeWidget.class));
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,ids);
            sendBroadcast(intent);
        }

    }//onActivityResult

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Imposto le preferenze al default
        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);

        url = getString(R.string.serverURL);

        // Carico l'ultima frase mostrata
        phrase=ApplicationUtils.loadLatestPhrase(this.getApplicationContext(),ApplicationUtils.SharedActivityLatestPhraseKey);

        // Aggiorno il DB creandolo se necessario
        mDbHelper = ApplicationUtils.getDatabaseInstance(this.getApplicationContext());

        // Check per il primo avvio: Se la frase è quella di default e sono connesso ad internet, avvio la richiesta al server
        if (ApplicationUtils.isInternetAvailable(MainActivity.this)){
            startServerRequest();
        } else {
            // Prendo la frase dal DB locale
            PhraseData p = ApplicationUtils.getPhraseAndIdFromLocalDB(this);
            phrase=p.phrase;
            phrase_ID=p.phrase_ID;
        }

        // Aggiorno la frase mostrata
        TextView textView = (TextView) findViewById(R.id.txtPhrase);
        textView.setText(phrase);

        // Aggiorno lo storico
        loadListView();
        ListView listView = (ListView)findViewById(R.id.listView);
        listView.setOnItemLongClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ApplicationUtils.loadSharedPreferences(this);
        loadListView();

        // Imposto la visualizzazione dello switch in modo corretto
        Switch aSwitch = (Switch)findViewById(R.id.switch1);
        aSwitch.setChecked(historicalDataFromWidget);
    }

    public void getPhrase(View v){
        // Devo aggiornare il DB in onTask altrimenti perdo la frase se l'applicazione viene fatta ripartire
        //ApplicationUtils.updateLocalDB(this, phrase_ID, phrase,0);
        startServerRequest();
    }

    public void switchWidgetApp(View v){
        Switch aSwitch= (Switch)findViewById(R.id.switch1);
        if (aSwitch.isChecked()){
            historicalDataFromWidget=true;
        } else {
            historicalDataFromWidget=false;
        }

//        // Imposto le label - Per ora è commentato in quanto sono troppo lunghe
//         // Lasciato per il futuro
//        TextView textView = (TextView)findViewById(R.id.textView4);
//        textView.setText( historicalDataFromWidget?getString(R.string.txtStoricoWidget):
//                getString(R.string.txtStoricoApp)
//        );

        loadListView();
    }

    private void startServerRequest(){
        // Occorre passare il contesto della main activity e non dell'applicatione (getApplicationContext)
        Context context = MainActivity.this;

        //showProgressBar=true;
        showProgressBar(true);

        if (ApplicationUtils.isInternetAvailable(context)) {
            new GetAsyncServerResponse(context, this).execute();
        } else {
            if (ApplicationUtils.isLoadFromLocalDBEnabled()) {
                if (ApplicationUtils.isShowToastOnConnection()) {
                    Toast toast = Toast.makeText(context, context.getString(R.string.noInternetWithLocalDB), Toast.LENGTH_SHORT);
                    TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
                    if (v != null) v.setGravity(Gravity.CENTER);
                    toast.show();
                }
                // Reinizializzo il DB se l'Activity è stata rimossa dalla memoria
                if (mDbHelper == null) {
                    mDbHelper = ApplicationUtils.getDatabaseInstance(context);
                }

                // Prendo la frase dal db locale
                PhraseData p = ApplicationUtils.getPhraseAndIdFromLocalDB(context);
                phrase=p.phrase;
                phrase_ID=p.phrase_ID;

                // Chiamo esplicitamente onTackComplete per aggiornare l'Activity
                onTaskComplete(phrase_ID,phrase);
            } else {
                if (ApplicationUtils.isShowToastOnConnection()) {
                    Toast toast = Toast.makeText(context, context.getString(R.string.noInternet), Toast.LENGTH_SHORT);
                    TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
                    if (v != null) v.setGravity(Gravity.CENTER);
                    toast.show();
                }
            }
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

        if (id == R.id.action_settings){
            Intent intentSettings= new Intent(getApplicationContext(),SettingsActivity.class);
            startActivityForResult(intentSettings, ApplicationUtils.SETTINGS_RESULTCODE);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void scegliShareMethod (View view, String s){
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);

        sharingIntent.setType(getString(R.string.ShareMimeType));
        sharingIntent.putExtra(Intent.EXTRA_TEXT, s);

        startActivityForResult(Intent.createChooser(sharingIntent, getString(R.string.CondividiCon)), SHARE_PICKER);
    }

    public void scegliShareMethod(View view){
        scegliShareMethod(view, phrase);
    }

    private void showProgressBar(boolean showProgressBar){
        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar2);
        progressBar.setVisibility((showProgressBar && ApplicationUtils.isInternetAvailable(MainActivity.this)) ?
                View.VISIBLE:View.INVISIBLE);
    }

    /**
     * Function to load the spinner data from SQLite database
     * */
    private void loadListView() {

        mDbHelper.open();

        String sql = (historicalDataFromWidget)?getApplicationContext().getString(R.string.sqlSelectHistoryPhrasesWidget):
                getApplicationContext().getString(R.string. sqlSelectHistoryPhrasesApp);

        List<String> frasi=mDbHelper.getValues(sql,0);

        ListView listView = (ListView) findViewById(R.id.listView);
        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, frasi);

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
        listView.setAdapter(dataAdapter);

        mDbHelper.close();
    }

    // Lasciato nel caso servisse in futuro. Per ora non faccio nulla
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // On selecting a spinner item
        String label = parent.getItemAtPosition(position).toString();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        //Log.v("long clicked", "pos: " + position + parent.getItemAtPosition(position).toString());

        String s = parent.getItemAtPosition(position).toString();
        scegliShareMethod(view,s);

        return false;
    }


    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub

    }
}
