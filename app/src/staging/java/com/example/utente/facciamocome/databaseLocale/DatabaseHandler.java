package com.example.utente.facciamocome.databaseLocale;

/**
 * Created by utente on 02/06/2016.
 */

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DatabaseHandler extends SQLiteOpenHelper {
    // Database Version
    private static final int DATABASE_VERSION = 3;

//    // Labels table name
//    private static final String TABLE_LIVELLI = "livelli";

    private static String TAG = "DataBaseHandler"; // Tag just for the LogCat window

    //destination path (location) of our database on device
    private static String DB_PATH = "";
    private static String DB_NAME ="facciamocome.db";// Database name
    private SQLiteDatabase mDataBase;
    private final Context mContext;

    public DatabaseHandler(Context context) {
        super(context, DB_NAME, null, DATABASE_VERSION);
        if(android.os.Build.VERSION.SDK_INT >= 17){
            DB_PATH = context.getApplicationInfo().dataDir + "/databases/";
        }
        else
        {
            DB_PATH = "/data/data/" + context.getPackageName() + "/databases/";
        }
        this.mContext = context;
    }

    public void createDataBase() throws IOException
    {
        //If the database does not exist, copy it from the assets.

        boolean mDataBaseExist = checkDataBase();
        if(!mDataBaseExist)
        {
            this.getReadableDatabase();
            this.close();
            try
            {
                //Copy the database from assests
                copyDataBase();
                Log.e(TAG, "createDatabase database created");
            }
            catch (IOException mIOException)
            {
                throw new Error("ErrorCopyingDataBase");
            }
        }
    }

    //Check that the database exists here: /data/data/your package/databases/Da Name
    private boolean checkDataBase()
    {
        File dbFile = new File(DB_PATH + DB_NAME);
        //Log.v("dbFile", dbFile + "   "+ dbFile.exists());
        return dbFile.exists();
    }

    //Copy the database from assets
    private void copyDataBase() throws IOException
    {
        InputStream mInput = mContext.getAssets().open(DB_NAME);
        String outFileName = DB_PATH + DB_NAME;
        OutputStream mOutput = new FileOutputStream(outFileName);
        byte[] mBuffer = new byte[1024];
        int mLength;
        while ((mLength = mInput.read(mBuffer))>0)
        {
            mOutput.write(mBuffer, 0, mLength);
        }
        mOutput.flush();
        mOutput.close();
        mInput.close();
    }
    //Open the database, so we can query it
    public boolean openDataBase() throws SQLException
    {
        String mPath = DB_PATH + DB_NAME;
        //Log.v("mPath", mPath);
        mDataBase = SQLiteDatabase.openDatabase(mPath, null, SQLiteDatabase.CREATE_IF_NECESSARY);
        //mDataBase = SQLiteDatabase.openDatabase(mPath, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        return mDataBase != null;
    }

    @Override
    public synchronized void close()
    {
        if(mDataBase != null)
            mDataBase.close();
        super.close();
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
//    // TODO: inserire copia DB da assets a /data/data/com.example.utente.wordbrainhelper/databases
//        try {
//            createDataBase();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        // Per ora esco
//        return ;
//        /* per ora commento
//        // Category table create query
//        String CREATE_CATEGORIES_TABLE = "CREATE TABLE " + TABLE_LABELS + "(" + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT)";
//        db.execSQL(CREATE_CATEGORIES_TABLE);
//
//        */
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//
//        // TODO: Vedere cosa fa se un db già esiste. per ora esco
//        return;
///* per ora lo commento
//        // Drop older table if existed
//        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LABELS);
//
//        // Create tables again
//        onCreate(db);
//        */
    }

//    /**
//     * Inserting new lable into lables table
//     * */
///* Per ora non mi serve
//    public void insertLabel(String label){
//        SQLiteDatabase db = this.getWritableDatabase();
//
//        ContentValues values = new ContentValues();
//        values.put(KEY_NAME, label);
//
//        // Inserting Row
//        db.insert(TABLE_LABELS, null, values);
//        db.close(); // Closing database connection
//    }
//*/

////    /**
////     * Getting all labels
////     * returns list of labels
////     * */
//    public List<String> getValues(String selectQuery){
//        List<String> labels = new ArrayList<String>();
//
//        // Select All Query
//        //String selectQuery = "SELECT  * FROM " + TABLE_LABELS;
//
//        SQLiteDatabase db = this.getReadableDatabase();
//        Cursor cursor = db.rawQuery(selectQuery, null);
//
//        // looping through all rows and adding to list
//        if (cursor.moveToFirst()) {
//            do {
//                labels.add(cursor.getString(1));
//            } while (cursor.moveToNext());
//        }
//
//        // closing connection
//        cursor.close();
//        db.close();
//
//        // returning lables
//        return labels;
//    }
}
