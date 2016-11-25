package com.example.utente.facciamocome.databaseLocale;

/**
 * Created by utente on 02/06/2016.
 */
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DataAdapter
{
    protected static final String TAG = "DataAdapter";

    private final Context mContext;
    private SQLiteDatabase mDb;
    private DatabaseHandler mDbHelper;

    public DataAdapter(Context context)
    {
        this.mContext = context;
        mDbHelper = new DatabaseHandler(mContext);
    }

    public DataAdapter createDatabase() throws SQLException
    {
        try
        {
            mDbHelper.createDataBase();
        }
        catch (IOException mIOException)
        {
            Log.e(TAG, mIOException.toString() + "  UnableToCreateDatabase");
            throw new Error("UnableToCreateDatabase");
        }
        return this;
    }

    public DataAdapter open() throws SQLException
    {
        try
        {
            mDbHelper.openDataBase();
            mDbHelper.close();
            mDb = mDbHelper.getReadableDatabase();
        }
        catch (SQLException mSQLException)
        {
            Log.e(TAG, "open >>"+ mSQLException.toString());
            throw mSQLException;
        }
        return this;
    }

    public void close()
    {
        mDbHelper.close();
    }

    public Cursor getCursor(String sql)
    {
        try
        {
            Cursor mCur = mDb.rawQuery(sql, null);
            if (mCur!=null)
            {
                //mCur.moveToNext();
                mCur.moveToFirst();
            }
            return mCur;
        }
        catch (SQLException mSQLException)
        {
            Log.e(TAG, "getTestData >>"+ mSQLException.toString());
            throw mSQLException;
        }
    }

    /**
     * Getting all labels
     * returns list of labels
     * */
    public List<String> getValues(String selectQuery, int index){
        List<String> labels = new ArrayList<String>();

        //SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = mDb.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                labels.add(cursor.getString(index));
            } while (cursor.moveToNext());
        }

        // returning lables
        return labels;
    }

    /**
     *
     */
    public void setValues(String updateQuery){
        //Cursor cursor = mDb.rawQuery(updateQuery, null);
        mDb.execSQL(updateQuery);
    }

}