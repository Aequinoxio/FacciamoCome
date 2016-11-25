package com.example.utente.facciamocome;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.acra.ACRA;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by utente on 21/05/2016.
 */
/**
 * Async task class to get json by making HTTP call
 * */
public class GetAsyncServerResponse extends AsyncTask<Void, Void, Void> {

    private AsyncTaskCompleteListener<Integer, String> callback;

    Context context;
    private ProgressDialog pDialog;

    String id ;
    String color;
    String country_id ;
    String phrase ;

    private static String url ;
    // JSONArray jsonRisposta;
    // ArrayList<HashMap<String, String>> mappaRisposta;

    private String TAG_PHRASE= ApplicationUtils.TAG_PHRASE;
    private String TAG_ID = ApplicationUtils.TAG_ID;
    private String TAG_COUNTRY_ID = ApplicationUtils.TAG_COUNTRY_ID;
    private String TAG_COLOR= ApplicationUtils.TAG_COLOR;

    public GetAsyncServerResponse(Context context, AsyncTaskCompleteListener<Integer, String> cb) {
        this.context = context;
        this.callback = cb;

        // Piccolo trick, se il sito non capisce un parametro torna una frase a caso. Per cui se passo 0 con un asola url ho
        // entrambi i casi trattati (Tutte le nazioni o solo una nazione)
        //url = context.getString(R.string.serverURL);
        url = context.getString(R.string.serverURLCountry)+ApplicationUtils.getCountryTarget();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // Showing progress dialog
        if (context!=null) {
            pDialog = new ProgressDialog(context);
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(false);
//            pDialog.show();
        }
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        // Creating service handler class instance
        ServiceHandler sh = new ServiceHandler();

        // Making a request to url and getting response
        String jsonStr = sh.makeWebServiceCall(url, ServiceHandler.GETRequest);

        // Log.d("Response: ", "> " + jsonStr);

        if (jsonStr != null) {
            try {
                JSONObject jsonObj = new JSONObject(jsonStr);

                id = jsonObj.getString(TAG_ID);
                color = jsonObj.getString(TAG_COLOR);
                country_id = jsonObj.getString(TAG_COUNTRY_ID);
                phrase = jsonObj.getString(TAG_PHRASE);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            // Log.e("ServiceHandler", "Couldn't get any data from the url");
            Toast.makeText(context, context.getString(R.string.jsonStringError), Toast.LENGTH_SHORT).show();
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        // Dismiss the progress dialog
        if (context !=null) {
            if (pDialog.isShowing())
                pDialog.dismiss();
        }

        // DEBUG
        if (id==null||phrase==null) {
            ACRA.getErrorReporter().putCustomData("AsyncTask caller", callback.getClass().getSimpleName());
            ACRA.getErrorReporter().putCustomData("AsyncTask:json id", (id==null)?"NULL":id);
            ACRA.getErrorReporter().putCustomData("AsyncTask:json phrase", (phrase==null)?"NULL":phrase);

//            // Se il valore ritornato dal server è null allora ne imposto uno di default
//            // TODO: impostare una frase dal DB locale se previsto e informare con un toast l'utente dei problemi sul DB remoto
//            id=context.getString(R.string.settingsFirstPhraseID);
//            phrase=context.getString(R.string.settingsFirstPhrase);
//            color="BLACK";
//            country_id="0";

            callback.onTaskComplete(null, null);
        } else {
            callback.onTaskComplete(Integer.valueOf(id), phrase);
        }
    }
}
