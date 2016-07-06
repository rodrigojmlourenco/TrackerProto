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
import android.location.Location;
import android.util.Log;

import com.google.gson.JsonObject;

import org.trace.storeclient.data.Route;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class RemoteRouteStorage extends RouteStorage{

    private final String LOG_TAG = "RemoteRouteStorage";
    private RouteStorageDBHelper mDBHelper;

    private RemoteRouteStorage(Context context){
        mDBHelper = new RouteStorageDBHelper(context, "RemoteRouteStorage.db");
    }

    private static RemoteRouteStorage LOCAL_ROUTE_STORAGE = null;

    public static RemoteRouteStorage getLocalStorage(Context context){
        synchronized (RemoteRouteStorage.class){
            if(LOCAL_ROUTE_STORAGE == null){
                LOCAL_ROUTE_STORAGE = new RemoteRouteStorage(context);
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
}
