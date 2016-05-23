package com.example.utente.facciamocome;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by utente on 21/05/2016.
 */
/**
 * Async task class to get json by making HTTP call
 * */
public class GetAsyncServerResponse extends AsyncTask<Void, Void, Void> {

    private AsyncTaskCompleteListener<String> callback;

    Context context;
    private ProgressDialog pDialog;

    String id ;
    String color;
    String country_id ;
    String phrase ;

    private static String url ;
    // JSONArray jsonRisposta;
    // ArrayList<HashMap<String, String>> mappaRisposta;

    private static final String TAG_PHRASE= "phrase";
    private static final String TAG_ID = "id";
    private static final String TAG_COUNTRY_ID = "country_id";
    private static final String TAG_COLOR= "color";

    public GetAsyncServerResponse(Context context, AsyncTaskCompleteListener<String> cb) {
        this.context = context;
        this.callback = cb;
        url = context.getString(R.string.serverURL);
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
            Log.e("ServiceHandler", "Couldn't get any data from the url");
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
        callback.onTaskComplete(phrase);
    }
}
