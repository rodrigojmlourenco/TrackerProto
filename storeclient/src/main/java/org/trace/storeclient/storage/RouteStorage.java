/*
 * Copyright (c) 2016 Rodrigo Lourenço, Miguel Costa, Paulo Ferreira, João Barreto @  INESC-ID.
 *
 * This file is part of TRACE.
 *
 * TRACE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TRACE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TRACE.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.trace.storeclient.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.provider.BaseColumns;
import android.util.Log;

import com.google.gson.JsonObject;

import org.trace.storeclient.data.Route;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class RouteStorage {

    private final String LOG_TAG = "RouteStorage";
    private RouteStorageDBHelper mDBHelper;

    private RouteStorage(Context context){
        mDBHelper = new RouteStorageDBHelper(context);
    }

    private static RouteStorage LOCAL_ROUTE_STORAGE = null;

    public static RouteStorage getLocalStorage(Context context){
        synchronized (RouteStorage.class){
            if(LOCAL_ROUTE_STORAGE == null){
                LOCAL_ROUTE_STORAGE = new RouteStorage(context);
            }
        }

        return LOCAL_ROUTE_STORAGE;
    }

    /* Logging
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    private void dumpRouteSummaryTable(){

        SQLiteDatabase db = mDBHelper.getReadableDatabase();

        String[] columns = new String[]{
                RouteSummaryEntry._ID,
                RouteSummaryEntry.COLUMN_ID,
                RouteSummaryEntry.COLUMN_STARTED_AT,
                RouteSummaryEntry.COLUMN_ENDED_AT,
                RouteSummaryEntry.COLUMN_DISTANCE,
                RouteSummaryEntry.COLUMN_MODALITY,
                RouteSummaryEntry.COLUMN_SENSING_TYPE,
                RouteSummaryEntry.COLUMN_POINTS,
                RouteSummaryEntry.COLUMN_AVG_SPEED,
                RouteSummaryEntry.COLUMN_TOP_SPEED
        };

        Cursor cursor = db.query(RouteSummaryEntry.TABLE_NAME, columns, "", new String[]{}, "", "" ,"");

        Log.d(LOG_TAG, "");
        Log.d(LOG_TAG, "");
        Log.d(LOG_TAG, "Dumping track summaries");
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd/MM");

        while(cursor.moveToNext()){

            String _id = cursor.getString(0);
            String _start   = cursor.getString(2);
            String _stopped = cursor.getString(3);
            String _length  = cursor.getString(4);


            JsonObject track = new JsonObject();
            track.addProperty("id", _id);
            track.addProperty("startedAt", sdf.format(new Date(Long.valueOf(_start))));
            track.addProperty("endedAt", sdf.format(new Date(Long.valueOf(_stopped))));
            track.addProperty("length", _length);

            Log.i("TrackSummary", track.toString());
        }

        db.close();

    }

    public void storeCompleteRoute(Route route) {

        String session = route.getSession();
        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        ContentValues summaryValues = new ContentValues();
        List<ContentValues> locationValues= new ArrayList<>();

        summaryValues.put(RouteSummaryEntry._ID, route.getSession());
        summaryValues.put(RouteSummaryEntry.COLUMN_STARTED_AT, route.getStartedAt());
        summaryValues.put(RouteSummaryEntry.COLUMN_ENDED_AT, route.getEndedAt());
        summaryValues.put(RouteSummaryEntry.COLUMN_DISTANCE, route.getElapsedDistance());
        summaryValues.put(RouteSummaryEntry.COLUMN_AVG_SPEED, route.getAvgSpeed());
        summaryValues.put(RouteSummaryEntry.COLUMN_TOP_SPEED, route.getTopSpeed());
        summaryValues.put(RouteSummaryEntry.COLUMN_POINTS, route.getPoints());
        summaryValues.put(RouteSummaryEntry.COLUMN_MODALITY, route.getModality());

        for(Location location : route.getTrace()){
            ContentValues aux = new ContentValues();
            aux.put(RouteLocationEntry.COLUMN_SESSION, session);
            aux.put(RouteLocationEntry.COLUMN_TIMESTAMP, location.getTime());
            aux.put(RouteLocationEntry.COLUMN_LATITUDE, location.getLatitude());
            aux.put(RouteLocationEntry.COLUMN_LONGITUDE, location.getLongitude());
            locationValues.add(aux);
        }

        try {
            db.beginTransaction();
            db.insert(RouteSummaryEntry.TABLE_NAME, null, summaryValues);

            for (ContentValues value : locationValues) {
                db.insert(RouteLocationEntry.TABLE_NAME, null, value);
            }
            db.setTransactionSuccessful();
        }catch (Exception e){
            e.printStackTrace();
            Log.e(LOG_TAG, e.getMessage());
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    /*
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    private class RouteStorageDBHelper extends SQLiteOpenHelper {

        public static final int DATABASE_VERSION = 1;
        public static final String DATABASE_NAME = "RouteStorage.db";

        public RouteStorageDBHelper(Context context){
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(RouteSummaryEntry.SQL_CREATE_TABLE);
            db.execSQL(RouteLocationEntry.SQL_CREATE_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(LOG_TAG, "Upgrading db to version "+newVersion);
            db.execSQL(RouteLocationEntry.SQL_DELETE_TABLE);
            db.execSQL(RouteSummaryEntry.SQL_DELETE_TABLE);
            onCreate(db);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }

    private interface BaseTypes {
        String TEXT_TYPE        = " TEXT";
        String IDENTIFIER_TYPE  = " INTEGER PRIMARY KEY AUTOINCREMENT";
        String DOUBLE_TYPE      = " DOUBLE DEFAULT 0";
        String DATE_TYPE        = " LONG DEFAULT 0";
        String TIMESTAMP_TYPE   = " LONG NOT NULL DEFAULT 0";
        String BOOLEAN_TYPE     = " INTEGER DEFAULT 0";
        String STRING_TYPE      = " VARCHAR(255)";
        String POINT_TYPE       = " VARCHAR(128)";
        String INT_TYPE         = " INTEGER";
        String ADDR_TYPE        = " TEXT DEFAULT ''";

        String SEPARATOR = ", ";
    }

    private interface RouteSummaryEntry extends BaseColumns, BaseTypes {
        String TABLE_NAME = "RouteSummaries";

        String COLUMN_ID            = " id ";

        String COLUMN_STARTED_AT    = " startedAt ";
        String COLUMN_ENDED_AT = " finishedAt ";
        String COLUMN_DISTANCE      = " distance ";
        String COLUMN_SENSING_TYPE  = " sensingType ";
        String COLUMN_MODALITY      = " modality ";
        String COLUMN_POINTS        = " points ";
        String COLUMN_AVG_SPEED     = " avgSpeed";
        String COLUMN_TOP_SPEED     = " topSpeed";



        String SQL_CREATE_TABLE =
                "CREATE TABLE "+ TABLE_NAME +" ( " +
                        _ID                 + IDENTIFIER_TYPE   + SEPARATOR +
                        COLUMN_ID           + TEXT_TYPE         + SEPARATOR +
                        COLUMN_STARTED_AT   + TIMESTAMP_TYPE    + SEPARATOR +
                        COLUMN_ENDED_AT + TIMESTAMP_TYPE    + SEPARATOR +
                        COLUMN_DISTANCE     + DOUBLE_TYPE       + SEPARATOR +
                        COLUMN_SENSING_TYPE + INT_TYPE          + SEPARATOR +
                        COLUMN_MODALITY     + INT_TYPE          + SEPARATOR +
                        COLUMN_POINTS       + INT_TYPE          + SEPARATOR +
                        COLUMN_AVG_SPEED    + DOUBLE_TYPE       + SEPARATOR +
                        COLUMN_TOP_SPEED    + DOUBLE_TYPE
                        + ")" ;

        String SQL_DELETE_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    private interface RouteLocationEntry extends BaseColumns, BaseTypes {
        String TABLE_NAME = "traces";

        String COLUMN_SESSION = "session";
        String COLUMN_LATITUDE = "latitude";
        String COLUMN_LONGITUDE = "longitude";
        String COLUMN_TIMESTAMP = "timestamp";

        String SQL_CREATE_TABLE =
                "CREATE TABLE "+ TABLE_NAME +" ("+
                        _ID + " "                + IDENTIFIER_TYPE   + SEPARATOR +
                        COLUMN_SESSION + INT_TYPE          + SEPARATOR +
                        COLUMN_LATITUDE + DOUBLE_TYPE       + SEPARATOR +
                        COLUMN_LONGITUDE + DOUBLE_TYPE       + SEPARATOR +
                        COLUMN_TIMESTAMP + DATE_TYPE         + SEPARATOR +
                        " FOREIGN KEY ( "+ COLUMN_SESSION +" ) " +
                        " REFERENCES "+ RouteSummaryEntry.TABLE_NAME + " ( "+ RouteSummaryEntry._ID+" ) " +
                        " ON DELETE CASCADE)";

        String SQL_DELETE_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }
}
