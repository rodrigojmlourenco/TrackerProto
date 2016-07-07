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
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.gson.JsonObject;

import org.trace.storeclient.cache.exceptions.RouteNotFoundException;
import org.trace.storeclient.data.Route;
import org.trace.storeclient.data.RouteSummary;
import org.trace.storeclient.data.RouteWaypoint;

import java.util.ArrayList;
import java.util.List;


public abstract class RouteStorage {

    private static final String LOG_TAG = "RouteStorage";

    protected SQLiteOpenHelper mDBHelper;

    public void dumpStorageLog(){

        SQLiteDatabase db = mDBHelper.getReadableDatabase();

        StringBuilder query = new StringBuilder();
        StringBuilder subQuery = new StringBuilder();
        StringBuilder countQuery = new StringBuilder();

        countQuery.append("SELECT "+RouteLocationEntry.COLUMN_SESSION+", COUNT(*) AS rPoints FROM "+RouteLocationEntry.TABLE_NAME);
        countQuery.append(" GROUP BY "+RouteLocationEntry.COLUMN_SESSION);

        subQuery.append("SELECT A.id as session, A.points, A.startedAt, B.rPoints ");
        subQuery.append("FROM "+RouteSummaryEntry.TABLE_NAME+" AS A JOIN ("+countQuery+") AS B ");
        subQuery.append("ON A."+RouteSummaryEntry.COLUMN_ID+"="+RouteLocationEntry.COLUMN_SESSION);

        query.append("SELECT M.session, M.startedAt, M.points, M.rPoints, S.isLocal, S.isComplete ");
        query.append("FROM ("+subQuery+") AS M JOIN "+RouteStateEntry.TABLE_NAME+" AS S ");
        query.append("ON M.session = S."+RouteStateEntry.COLUMN_ROUTE);

        Cursor c = db.rawQuery(query.toString(), null);

        Log.w(LOG_TAG, "[START] Dumping Route Storage information");
        while (c.moveToNext()){

            JsonObject dump = new JsonObject();
            dump.addProperty("session", c.getString(0));
            dump.addProperty("startedAt", c.getLong(1));
            dump.addProperty("points", c.getInt(2));
            dump.addProperty("rPoints", c.getInt(3));
            dump.addProperty("isLocal", c.getInt(4) > 0 );
            dump.addProperty("isComplete", c.getInt(5) > 0);

            Log.d(LOG_TAG, dump.toString());
        }
        Log.w(LOG_TAG, "[END] Information dumped");

    }

    public boolean storeCompleteRoute(Route route) {

        boolean success = false;
        String session = route.getSession();
        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        ContentValues summaryValues = new ContentValues();
        List<ContentValues> locationValues= new ArrayList<>();

        summaryValues.put(RouteSummaryEntry.COLUMN_ID, route.getSession());
        summaryValues.put(RouteSummaryEntry.COLUMN_STARTED_AT, route.getStartedAt());
        summaryValues.put(RouteSummaryEntry.COLUMN_ENDED_AT, route.getEndedAt());
        summaryValues.put(RouteSummaryEntry.COLUMN_DISTANCE, route.getElapsedDistance());
        summaryValues.put(RouteSummaryEntry.COLUMN_AVG_SPEED, route.getAvgSpeed());
        summaryValues.put(RouteSummaryEntry.COLUMN_TOP_SPEED, route.getTopSpeed());
        summaryValues.put(RouteSummaryEntry.COLUMN_POINTS, route.getPoints());
        summaryValues.put(RouteSummaryEntry.COLUMN_MODALITY, route.getModality());

        for(RouteWaypoint location : route.getTrace()){
            ContentValues aux = new ContentValues();
            aux.put(RouteLocationEntry.COLUMN_SESSION, session);
            aux.put(RouteLocationEntry.COLUMN_TIMESTAMP, location.getTimestamp());
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
            success = true;
        }catch (Exception e){
            e.printStackTrace();
            Log.e(LOG_TAG, e.getMessage());
        } finally {
            db.endTransaction();
            db.close();
        }

        return success;
    }

    public boolean storeRouteSummary(RouteSummary summary, boolean isLocal, boolean isComplete){
        boolean success = false;
        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        ContentValues summaryValues = new ContentValues();
        ContentValues stateValues   = new ContentValues();

        summaryValues.put(RouteSummaryEntry.COLUMN_ID, summary.getSession());
        summaryValues.put(RouteSummaryEntry.COLUMN_STARTED_AT, summary.getStartedAt());
        summaryValues.put(RouteSummaryEntry.COLUMN_ENDED_AT, summary.getEndedAt());
        summaryValues.put(RouteSummaryEntry.COLUMN_DISTANCE, summary.getElapsedDistance());
        summaryValues.put(RouteSummaryEntry.COLUMN_AVG_SPEED, summary.getAvgSpeed());
        summaryValues.put(RouteSummaryEntry.COLUMN_TOP_SPEED, summary.getTopSpeed());
        summaryValues.put(RouteSummaryEntry.COLUMN_POINTS, summary.getPoints());
        summaryValues.put(RouteSummaryEntry.COLUMN_MODALITY, summary.getModality());

        stateValues.put(RouteStateEntry.COLUMN_ROUTE, summary.getSession());
        stateValues.put(RouteStateEntry.COLUMN_IS_LOCAL, isLocal);
        stateValues.put(RouteStateEntry.COLUMN_IS_COMPLETE, isComplete);


        try {
            db.beginTransaction();
            db.insert(RouteSummaryEntry.TABLE_NAME, null, summaryValues);

            db.insertWithOnConflict(RouteStateEntry.TABLE_NAME, RouteStateEntry.COLUMN_ROUTE, stateValues, SQLiteDatabase.CONFLICT_REPLACE);

            db.setTransactionSuccessful();
            success = true;
        }catch (Exception e){
            e.printStackTrace();
            Log.e(LOG_TAG, e.getMessage());
        } finally {
            db.endTransaction();
            db.close();
        }

        return success;
    }

    public boolean updateRouteStateAndTrace(String session, boolean isLocal, boolean isComplete, List<RouteWaypoint> trace){

        boolean success = false;
        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        ContentValues stateValues = new ContentValues();
        List<ContentValues> locationValues= new ArrayList<>();

        stateValues.put(RouteStateEntry.COLUMN_IS_LOCAL, isLocal);
        stateValues.put(RouteStateEntry.COLUMN_IS_COMPLETE, isComplete);

        for(RouteWaypoint location : trace){
            ContentValues aux = new ContentValues();
            aux.put(RouteLocationEntry.COLUMN_SESSION, session);
            aux.put(RouteLocationEntry.COLUMN_TIMESTAMP, location.getTimestamp());
            aux.put(RouteLocationEntry.COLUMN_LATITUDE, location.getLatitude());
            aux.put(RouteLocationEntry.COLUMN_LONGITUDE, location.getLongitude());
            locationValues.add(aux);
        }

        try {
            db.beginTransaction();

            db.update(RouteStateEntry.TABLE_NAME, stateValues, RouteStateEntry.COLUMN_ROUTE + "=?", new String[]{session});

            for (ContentValues value : locationValues)
                db.insert(RouteLocationEntry.TABLE_NAME, null, value);

            db.setTransactionSuccessful();
            success = true;
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            db.endTransaction();
            db.close();
        }

        return success;
    }

    public boolean storeCompleteRoute(Route route, boolean isLocal, boolean isComplete) {

        boolean success = false;
        String session = route.getSession();
        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        ContentValues summaryValues = new ContentValues();
        ContentValues stateValues   = new ContentValues();
        List<ContentValues> locationValues= new ArrayList<>();

        summaryValues.put(RouteSummaryEntry.COLUMN_ID, route.getSession());
        summaryValues.put(RouteSummaryEntry.COLUMN_STARTED_AT, route.getStartedAt());
        summaryValues.put(RouteSummaryEntry.COLUMN_ENDED_AT, route.getEndedAt());
        summaryValues.put(RouteSummaryEntry.COLUMN_DISTANCE, route.getElapsedDistance());
        summaryValues.put(RouteSummaryEntry.COLUMN_AVG_SPEED, route.getAvgSpeed());
        summaryValues.put(RouteSummaryEntry.COLUMN_TOP_SPEED, route.getTopSpeed());
        summaryValues.put(RouteSummaryEntry.COLUMN_POINTS, route.getPoints());
        summaryValues.put(RouteSummaryEntry.COLUMN_MODALITY, route.getModality());

        stateValues.put(RouteStateEntry.COLUMN_ROUTE, route.getSession());
        stateValues.put(RouteStateEntry.COLUMN_IS_LOCAL, isLocal);
        stateValues.put(RouteStateEntry.COLUMN_IS_COMPLETE, isComplete);

        for(RouteWaypoint location : route.getTrace()){
            ContentValues aux = new ContentValues();
            aux.put(RouteLocationEntry.COLUMN_SESSION, session);
            aux.put(RouteLocationEntry.COLUMN_TIMESTAMP, location.getTimestamp());
            aux.put(RouteLocationEntry.COLUMN_LATITUDE, location.getLatitude());
            aux.put(RouteLocationEntry.COLUMN_LONGITUDE, location.getLongitude());
            locationValues.add(aux);
        }

        try {
            db.beginTransaction();
            db.insertOrThrow(RouteSummaryEntry.TABLE_NAME, null, summaryValues);

            db.insertWithOnConflict(RouteStateEntry.TABLE_NAME, RouteStateEntry.COLUMN_ROUTE, stateValues, SQLiteDatabase.CONFLICT_REPLACE);

            for (ContentValues value : locationValues) {
                db.insert(RouteLocationEntry.TABLE_NAME, null, value);
            }
            db.setTransactionSuccessful();
            success = true;
        }catch (Exception e){
            e.printStackTrace();
            Log.e(LOG_TAG, e.getMessage());
        } finally {
            db.endTransaction();
            db.close();
        }

        return success;
    }

    public boolean deleteRoute(String trackId) {

        boolean wasDeleted = false;
        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        String summarySelectionClause   = RouteSummaryEntry.COLUMN_ID + " = ?";
        String stateSelectionClause     = RouteStateEntry.COLUMN_ROUTE + " = ?";
        String waypointsSelectionClause = RouteLocationEntry.COLUMN_SESSION + " = ?";

        String[] selectionValues = new String[]{ trackId };

        try {
            db.beginTransaction();
            db.delete(RouteSummaryEntry.TABLE_NAME, summarySelectionClause, selectionValues);
            db.delete(RouteLocationEntry.TABLE_NAME, waypointsSelectionClause, selectionValues);
            db.delete(RouteStateEntry.TABLE_NAME, stateSelectionClause, selectionValues);
            db.setTransactionSuccessful();
            wasDeleted = true;
        }catch (Exception e){
            e.printStackTrace();
            Log.e(LOG_TAG, e.getMessage());
        }finally {
            db.endTransaction();
            db.close();
        }

        return wasDeleted;
    }

    public List<String> getLocalRoutesSessions() {

        List<String> sessions = new ArrayList<>();

        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        String selectionClause = RouteStateEntry.COLUMN_IS_LOCAL + " = 1";
        String[] columns = new String[]{ RouteStateEntry.COLUMN_ROUTE };

        Cursor c = db.query(RouteStateEntry.TABLE_NAME, columns, selectionClause, null, "", "", "");

        while (c.moveToNext()){
            sessions.add(c.getString(0));
        }

        return sessions;
    }

    public RouteSummary getRouteSummary(String session) throws RouteNotFoundException {

        RouteSummary summary = new RouteSummary();
        SQLiteDatabase db = mDBHelper.getReadableDatabase();

        String[] columns = new String[]{
            RouteSummaryEntry.COLUMN_ID,
            RouteSummaryEntry.COLUMN_STARTED_AT,
            RouteSummaryEntry.COLUMN_ENDED_AT,
            RouteSummaryEntry.COLUMN_DISTANCE,
            RouteSummaryEntry.COLUMN_POINTS,
            RouteSummaryEntry.COLUMN_MODALITY,
            RouteSummaryEntry.COLUMN_AVG_SPEED,
            RouteSummaryEntry.COLUMN_TOP_SPEED
        };
        String selectionClause = RouteSummaryEntry.COLUMN_ID + " = ?";
        String[] selectionArgs = new String[] { session };

        Cursor cursor = db.query(RouteSummaryEntry.TABLE_NAME, columns, selectionClause, selectionArgs, "", "", "");

        if(!cursor.moveToNext())
            throw new RouteNotFoundException(session);

        summary.setSession(cursor.getString(0));
        summary.setStartedAt(cursor.getLong(1));
        summary.setEndedAt(cursor.getLong(2));
        summary.setElapsedDistance(cursor.getDouble(3));
        summary.setPoints(cursor.getInt(4));
        summary.setModality(cursor.getInt(5));
        summary.setAvgSpeed(cursor.getFloat(6));
        summary.setTopSpeed(cursor.getFloat(7));

        db.close();

        return summary;
    }

    public List<RouteWaypoint> getRouteTrace(String session) throws RouteNotFoundException{
        List<RouteWaypoint> trace = new ArrayList<>();

        SQLiteDatabase db = mDBHelper.getReadableDatabase();

        String[] columns = new String[]{
            RouteLocationEntry.COLUMN_TIMESTAMP,
            RouteLocationEntry.COLUMN_LATITUDE,
            RouteLocationEntry.COLUMN_LONGITUDE
        };
        String selectionClause = RouteLocationEntry.COLUMN_SESSION + " = ?";
        String[] selectionArgs = new String[] { session };

        Cursor cursor = db.query(RouteLocationEntry.TABLE_NAME, columns, selectionClause, selectionArgs, "", "", "");

        if(cursor.getCount() <= 0)
            throw new RouteNotFoundException(session);

        while (cursor.moveToNext()){
            RouteWaypoint waypoint = new RouteWaypoint();

            waypoint.setSession(session);
            waypoint.setTimestamp(cursor.getLong(0));
            waypoint.setLatitude(cursor.getDouble(1));
            waypoint.setLongitude(cursor.getDouble(2));
            waypoint.setAttributes("");

            trace.add(waypoint);
        }

        db.close();

        return trace;
    }

    public boolean isLocalRoute(String session){

        boolean isLocal;
        SQLiteDatabase db = mDBHelper.getReadableDatabase();

        String[] columns = new String[] { RouteStateEntry.COLUMN_IS_LOCAL };
        String selectionClause = RouteStateEntry.COLUMN_ROUTE + " = ? ";
        String[] selectionArgs = new String[] { session };

        Cursor cursor = db.query(RouteStateEntry.TABLE_NAME, columns, selectionClause, selectionArgs, "", "" ,"");
        isLocal = cursor.getInt(0) > 0;
        db.close();

        return isLocal;
    }

    public boolean isCompleteRoute(String session){

        boolean isComplete;
        SQLiteDatabase db = mDBHelper.getReadableDatabase();

        String[] columns = new String[] { RouteStateEntry.COLUMN_IS_COMPLETE };
        String selectionClause = RouteStateEntry.COLUMN_ROUTE + " = ? ";
        String[] selectionArgs = new String[] { session };

        Cursor cursor = db.query(RouteStateEntry.TABLE_NAME, columns, selectionClause, selectionArgs, "", "" ,"");
        isComplete = cursor.getInt(0) > 0;
        db.close();

        return isComplete;
    }

    public boolean hasPendingRoutes(){
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        long locals = DatabaseUtils.queryNumEntries(db, RouteStateEntry.TABLE_NAME, RouteStateEntry.COLUMN_IS_LOCAL+"=1");
        return locals > 0;
    }


    public List<RouteSummary> getAllRoutes(){

        List<RouteSummary> summaries = new ArrayList<>();

        SQLiteDatabase db = mDBHelper.getReadableDatabase();

        String[] columns = new String[]{
                RouteSummaryEntry.COLUMN_ID,
                RouteSummaryEntry.COLUMN_STARTED_AT,
                RouteSummaryEntry.COLUMN_ENDED_AT,
                RouteSummaryEntry.COLUMN_DISTANCE,
                RouteSummaryEntry.COLUMN_POINTS,
                RouteSummaryEntry.COLUMN_MODALITY,
                RouteSummaryEntry.COLUMN_AVG_SPEED,
                RouteSummaryEntry.COLUMN_TOP_SPEED
        };



        Cursor cursor = db.query(RouteSummaryEntry.TABLE_NAME, columns, "", null, "", "", "");

        while (cursor.moveToNext()) {
            RouteSummary summary = new RouteSummary();

            summary.setSession(cursor.getString(0));
            summary.setStartedAt(cursor.getLong(1));
            summary.setEndedAt(cursor.getLong(2));
            summary.setElapsedDistance(cursor.getDouble(3));
            summary.setPoints(cursor.getInt(4));
            summary.setModality(cursor.getInt(5));
            summary.setAvgSpeed(cursor.getFloat(6));
            summary.setTopSpeed(cursor.getFloat(7));

            summaries.add(summary);
        }

        db.close();
        return summaries;
    }
}

