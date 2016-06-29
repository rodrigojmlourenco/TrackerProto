package org.trace.tracker.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.trace.tracker.storage.data.TraceLocation;
import org.trace.tracker.storage.data.Track;
import org.trace.tracker.storage.data.TrackSummary;

import java.util.ArrayList;
import java.util.List;

//TODO: integrar elapsedTime e elapsedDistance nas queries

/**
 * The PersistentTrackStorage manages the stored tracks. The storage is performed using the device's
 * native SQLite support.
 */
public class PersistentTrackStorage {

    private TrackStorageDBHelper mDBHelper;

    public PersistentTrackStorage(Context context){
        mDBHelper = new TrackStorageDBHelper(context);
    }

    /* Version 2.0
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    public TrackSummary createNewTrackSummary(long startTime, int modality, int sensingType){

        String trackID = this.getNextAvailableId();
        this.createTrack(trackID, false); //TODO: remove this once this is arranged for

        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(TrackSummaryEntry._ID, trackID);
        values.put(TrackSummaryEntry.COLUMN_ID, trackID);
        values.put(TrackSummaryEntry.COLUMN_STARTED_AT, startTime);
        values.put(TrackSummaryEntry.COLUMN_FINISHED_AT, startTime);
        values.put(TrackSummaryEntry.COLUMN_DISTANCE, 0);
        values.put(TrackSummaryEntry.COLUMN_SENSING_TYPE, sensingType);
        values.put(TrackSummaryEntry.COLUMN_MODALITY, modality);

        long success = db.insert(TrackSummaryEntry.TABLE_NAME, null, values);
        db.close();

        if(success == -1)
            //TODO: deal with unsuccess better somehow...
            throw new RuntimeException("Unable to create a new track summary");
        else {
            TrackSummary summary = new TrackSummary();
            summary.setSession(trackID);
            summary.setStartTimestamp(startTime);
            summary.setStoppedTimestamp(startTime);
            summary.setElapsedDistance(0);
            summary.setModality(modality);
            summary.setSensingType(sensingType);

            return summary;
        }
    }

    public void updateTrackSummaryDistanceAndTime(TrackSummary summary){

        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        String selectionClause = TrackSummaryEntry.COLUMN_ID + " = ?";
        String[] selectionArgs = new String[1];

        ContentValues values = new ContentValues();
        values.put(TrackSummaryEntry.COLUMN_FINISHED_AT, summary.getStop());
        values.put(TrackSummaryEntry.COLUMN_DISTANCE, summary.getElapsedDistance());
        selectionArgs[0] = summary.getSession();


        int affected = db.update(TrackSummaryEntry.TABLE_NAME, values,selectionClause, selectionArgs);

        db.close();

        if(affected < 0)
            throw new RuntimeException("Did not update any row!!! updateTrackSummaryDistanceAndTime@PersistentTrackStorage");

    }

    public Track getCompleteTrack(TrackSummary summary){
        return  this.getTrack(summary.getSession());
    }

    public boolean removeTrackSummaryAndTrace(TrackSummary summary){
        return this.deleteTrackById(summary.getSession());
    }


    /* Constructors
    /* Constructors
    /* Constructors
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    /**
     * Create a new track entry.
     * @param session The track's session identifier.
     * @param isValid If the session was provided by the TraceStore server.
     *
     * @return The track's identifier.
     */
    public long createTrack(String session, boolean isValid){

        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(TraceEntry.COLUMN_NAME_SESSION, session);
        values.put(TraceEntry.COLUMN_NAME_IS_CLOSED, 0);
        values.put(TraceEntry.COLUMN_NAME_IS_VALID, (isValid ? 1 : 0));
        values.put(TraceEntry.COLUMN_NAME_ELAPSED_TIME, 0);
        values.put(TraceEntry.COLUMN_NAME_ELAPSED_DISTANCE, 0);

        return db.insert(TraceEntry.TABLE_NAME_TRACKS,null,values);
    }

    // TODO: sempre que uma nova localização é adicionada é actualizado o elapsed time e distance.
    /**
     * Stores a new location, which is associated with a track.
     *
     * @param location The new location.
     * @param session The session identifier that identifies the track.
     * @param isRemote True if the session identifier is valid, false otherwise. I.e, if the session is not local.
     */
    public void storeLocation(TraceLocation location, String session, boolean isRemote){

        long trackId;

        if((trackId = getTrackId(session)) == -1)
            trackId = createTrack(session, isRemote);

        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(TraceEntry.COLUMN_NAME_LATITUDE, location.getLatitude());
        values.put(TraceEntry.COLUMN_NAME_LONGITUDE, location.getLongitude());
        values.put(TraceEntry.COLUMN_NAME_ATTRIBUTES, location.getSecondaryAttributesAsJson().toString());
        values.put(TraceEntry.COLUMN_NAME_TIMESTAMP, location.getTime());
        values.put(TraceEntry.COLUMN_NAME_TRACK_ID, trackId);

        db.insert(TraceEntry.TABLE_NAME_TRACES, null, values);
    }

    /* Getters
    /* Getters
    /* Getters
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    /**
     * Returns the track identifier, given the provided session identifier.
     * @param session The track's identifier, or -1 if the track was not found.
     * @return The track's sqlite identifier.
     */
    public int getTrackId(String session){
        int trackId;
        SQLiteDatabase db = mDBHelper.getReadableDatabase();

        String[] projection = { TraceEntry._ID };
        String selection = TraceEntry.COLUMN_NAME_SESSION+" = ?";
        String[] selectionArgs = { session };

        Cursor c = db.query(true, TraceEntry.TABLE_NAME_TRACKS, projection, selection, selectionArgs, "", "", "", "");

        if(!c.moveToFirst()){
            db.close();
            return -1;
        }

        trackId = c.getInt(c.getColumnIndex(TraceEntry._ID));

        db.close();

        return trackId;
    }

    /**
     * Fetches a track as a Track object, given the provided session identifier.
     * @param session The session identifier
     * @return The Track
     * @see Track
     */
    public Track getTrack(String session){

        SQLiteDatabase db = mDBHelper.getReadableDatabase();

        String[] selectionArgs = { session };

        Cursor c = db.rawQuery(ContractHelper.SQL_RAW_QUERY_COMPLETE_TRACKS, selectionArgs);

        if(!c.moveToFirst()) return null;

        boolean isClosed, isValid;
        String storedSession;

        storedSession  = c.getString(c.getColumnIndex(TraceEntry.COLUMN_NAME_SESSION));

        isClosed= c.getInt(c.getColumnIndex(TraceEntry.COLUMN_NAME_IS_CLOSED)) != 0;
        isValid = c.getInt(c.getColumnIndex(TraceEntry.COLUMN_NAME_IS_VALID)) != 0;

        Track track = new Track();
        track.setSessionId(storedSession);
        if(isClosed) track.upload();
        track.setIsValid(isValid);
        track.setTravelledDistance(c.getDouble(c.getColumnIndex(TraceEntry.COLUMN_NAME_ELAPSED_DISTANCE)));

        JsonParser parser = new JsonParser();
        TraceLocation location;
        do {
            location = new TraceLocation();

            location.setLatitude(c.getDouble(c.getColumnIndex(TraceEntry.COLUMN_NAME_LATITUDE)));
            location.setLongitude(c.getDouble(c.getColumnIndex(TraceEntry.COLUMN_NAME_LONGITUDE)));
            location.setTime(c.getLong(c.getColumnIndex(TraceEntry.COLUMN_NAME_TIMESTAMP)));

            String attributes = c.getString(c.getColumnIndex(TraceEntry.COLUMN_NAME_ATTRIBUTES));
            location.setSecondaryAttributes((JsonObject) parser.parse(attributes));

            track.addTracedLocation(location);

        }while (c.moveToNext());

        db.close();

        return track;
    }

    /**
     * Fetches a list of all stored tracks. These are provided as simplified Tracks that contain only
     * top level information.
     * @return List of simplified tracks
     * @see TrackSummary
     */
    public List<TrackSummary> getTracksSessions(){

        List<TrackSummary> simplifiedTracks = new ArrayList<>();
        SQLiteDatabase db = mDBHelper.getReadableDatabase();

        String[] projection = {
                TraceEntry.COLUMN_NAME_SESSION,
                TraceEntry.COLUMN_NAME_IS_CLOSED,
                TraceEntry.COLUMN_NAME_IS_VALID
        };


        Cursor c = db.query(true, TraceEntry.TABLE_NAME_TRACKS, projection, "", null, "", "", "", "");



        if(c.moveToFirst()) {

            boolean isClosed, isValid;
            String session;
            do {
                session = c.getString(c.getColumnIndex(TraceEntry.COLUMN_NAME_SESSION));

                isClosed= c.getInt(c.getColumnIndex(TraceEntry.COLUMN_NAME_IS_CLOSED)) == 1;
                isValid = c.getInt(c.getColumnIndex(TraceEntry.COLUMN_NAME_IS_VALID)) == 1;

                simplifiedTracks.add(new TrackSummary(session, isClosed, isValid));

            } while (c.moveToNext());
        }

        db.close();

        return simplifiedTracks;
    }

    /**
     * Returns the number of tracks currently stored in the database.
     * @return The tracks count.
     */
    public int getTracksCount(){

        int count;
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        count = (int) DatabaseUtils.queryNumEntries(db, TraceEntry.TABLE_NAME_TRACKS,"", null);

        return count;
    }

    public int getTracksCount(boolean isClosed){

        int count;
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        count = (int) DatabaseUtils.queryNumEntries(
                db,
                TraceEntry.TABLE_NAME_TRACKS,
                TraceEntry.COLUMN_NAME_IS_CLOSED+"=?",
                new String[]{String.valueOf((isClosed ? 1 : 0))});

        return count;
    }

    public String getNextAvailableId(){
        int nextId;

        SQLiteDatabase db = mDBHelper.getReadableDatabase();

        //Cursor c = db.rawQuery("SELECT MAX("+TraceEntry._ID+") FROM "+TraceEntry.TABLE_NAME_TRACKS, null);
        Cursor c = db.query(TraceEntry.TABLE_NAME_TRACKS, new String[]{"MAX("+ TraceEntry._ID+")"}, null, null, null, null, null);

        if(c.moveToFirst()){
            nextId = c.getInt(0)+1;
        }else{
            nextId = -1;
        }

        return String.valueOf(nextId);
    }


    /* Updaters
    /* Updaters
    /* Updaters
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    public boolean updateTrackSession(String oldSession, String newSession){

        int trackId = getTrackId(oldSession);

        if(trackId == -1) return false;

        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(TraceEntry.COLUMN_NAME_SESSION, newSession);
        values.put(TraceEntry.COLUMN_NAME_IS_VALID, 1);

        String selection = TraceEntry._ID + "= ?";
        String[] selectionArgs = {String.valueOf(trackId)};

        int count = db.update(TraceEntry.TABLE_NAME_TRACKS, values, selection, selectionArgs);

        db.close();

        return count > 0;
    }

    public boolean uploadTrack(String session){

        int trackId = getTrackId(session);

        if(trackId == -1) return false;

        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(TraceEntry.COLUMN_NAME_IS_CLOSED, 1);

        String selection = TraceEntry._ID + "= ?";
        String[] selectionArgs = {String.valueOf(trackId)};

        int count = db.update(TraceEntry.TABLE_NAME_TRACKS, values, selection, selectionArgs);

        db.close();

        return count > 0;
    }

    public boolean updateTravelledDistanceAndTime(String session, double distance, double time){
        int trackId = getTrackId(session);

        if(trackId == -1) return false;

        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(TraceEntry.COLUMN_NAME_ELAPSED_DISTANCE, distance);
        values.put(TraceEntry.COLUMN_NAME_ELAPSED_TIME, time);

        String selection = TraceEntry._ID + "= ?";
        String[] selectionArgs = {String.valueOf(trackId)};

        int count = db.update(TraceEntry.TABLE_NAME_TRACKS, values, selection, selectionArgs);

        db.close();

        return count > 0;
    }


    /* Delete
    /* Delete
    /* Delete
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    public boolean deleteTrackById(String session){
        int trackId = getTrackId(session);

        if(trackId == -1) return false;

        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        String selection = TraceEntry._ID + " = ?";
        String[] selectionArgs = {String.valueOf(trackId)};

        int affected = db.delete(TraceEntry.TABLE_NAME_TRACKS, selection, selectionArgs);
        Log.d("DELETED", "Rows deleted with session "+ session+" : "+String.valueOf(affected));

        db.close();

        return affected > 0;
    }

    /* Checks
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    public boolean trackExists(String session){
        int trackId = getTrackId(session);
        return trackId != -1;
    }


    /* DB Helpers
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    private class TrackStorageDBHelper extends SQLiteOpenHelper {

        public static final int DATABASE_VERSION = 5;
        public static final String DATABASE_NAME = "TraceTracker.db";

        public TrackStorageDBHelper(Context context){
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(ContractHelper.SQL_CREATE_TRACKS);
            db.execSQL(ContractHelper.SQL_CREATE_TRACES);
            db.execSQL(TrackSummaryEntry.SQL_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(ContractHelper.SQL_DELETE_TRACES_TABLE);
            db.execSQL(ContractHelper.SQL_DELETE_TRACKS_TABLE);
            db.execSQL(TrackSummaryEntry.SQL_DELETE);
            onCreate(db);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }

    public static abstract class TraceEntry implements BaseColumns {
        public static final String TABLE_NAME_TRACKS = "tracks";
        public static final String TABLE_NAME_TRACES = "traces";

        public static final String COLUMN_NAME_SESSION = "localSession";
        public static final String COLUMN_NAME_IS_VALID = "isValid";
        public static final String COLUMN_NAME_IS_CLOSED = "isClosed";
        public static final String COLUMN_NAME_ELAPSED_TIME = "elapsedTime";
        public static final String COLUMN_NAME_ELAPSED_DISTANCE = "elapsedDistance";

        public static final String COLUMN_NAME_LATITUDE = "latitude";
        public static final String COLUMN_NAME_LONGITUDE = "longitude";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_ATTRIBUTES = "attributes";
        public static final String COLUMN_NAME_TRACK_ID = "trackId";
    }


    private interface BaseTypes {
        String TEXT_TYPE        = " TEXT";
        String IDENTIFIER_TYPE  = " INTEGER PRIMARY KEY AUTOINCREMENT";
        String DOUBLE_TYPE      = " DOUBLE";
        String DATE_TYPE        = " LONG";
        String TIMESTAMP_TYPE   = " LONG NOT NULL";
        String BOOLEAN_TYPE     = " INTEGER DEFAULT 0";
        String STRING_TYPE      = " VARCHAR(255)";
        String POINT_TYPE       = " VARCHAR(128)";
        String INT_TYPE         = " INTEGER";

        String SEPARATOR = ", ";
    }

    private interface TrackSummaryEntry extends BaseColumns, BaseTypes {
        String TABLE_NAME = "TrackSummaries";

        String COLUMN_ID            = " id ";
        String COLUMN_STARTED_AT    = " startedAt ";
        String COLUMN_FINISHED_AT   = " finishedAt ";
        String COLUMN_FROM_LAT      = " fromLat ";
        String COLUMN_FROM_LON      = " fromLon ";
        String COLUMN_TO_LAT        = " toLat ";
        String COLUMN_TO_LON        = " toLon ";
        String COLUMN_DISTANCE      = " distance ";
        String COLUMN_SENSING_TYPE  = " sensingType ";
        String COLUMN_MODALITY      = " modality ";
        String COLUMNS_FROM         = " fromLocation ";
        String COLUMNS_TO           = " toLocation ";

        String SQL_CREATE =
                "CREATE TABLE "+ TABLE_NAME +" ( " +
                        _ID                 + IDENTIFIER_TYPE   + SEPARATOR +
                        COLUMN_ID           + INT_TYPE          + SEPARATOR +
                        COLUMN_FROM_LAT     + DOUBLE_TYPE       + SEPARATOR +
                        COLUMN_FROM_LON     + DOUBLE_TYPE       + SEPARATOR +
                        COLUMN_TO_LAT       + POINT_TYPE        + SEPARATOR +
                        COLUMN_TO_LON       + POINT_TYPE        + SEPARATOR +
                        COLUMN_STARTED_AT   + TIMESTAMP_TYPE    + SEPARATOR +
                        COLUMN_FINISHED_AT  + TIMESTAMP_TYPE    + SEPARATOR +
                        COLUMN_DISTANCE     + DOUBLE_TYPE       + SEPARATOR +
                        COLUMN_SENSING_TYPE + INT_TYPE          + SEPARATOR +
                        COLUMN_MODALITY     + INT_TYPE          + SEPARATOR +
                        COLUMNS_FROM        + TEXT_TYPE         + SEPARATOR +
                        COLUMNS_TO          + TEXT_TYPE         + SEPARATOR +
                        /*TODO: this bellow should eventually be deprecated*/
                        " FOREIGN KEY ( "+ COLUMN_ID +" ) " +
                        " REFERENCES "+ TraceEntry.TABLE_NAME_TRACKS+ " ( "+ TraceEntry._ID+" ) " +
                        " ON DELETE CASCADE)";

        String SQL_DELETE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    private interface ContractHelper {
        String TEXT_TYPE        = " TEXT";
        String IDENTIFIER_TYPE  = " INTEGER PRIMARY KEY AUTOINCREMENT";
        String DOUBLE_TYPE      = " DOUBLE";
        String DATE_TYPE        = " LONG";
        String BOOLEAN_TYPE     = " INTEGER DEFAULT 0";
        String INT_TYPE         = " INTEGER";

        String SEPARATOR = ", ";

        String SQL_CREATE_TRACKS =
                "CREATE TABLE "+ TraceEntry.TABLE_NAME_TRACKS +" ( " +
                        TraceEntry._ID + " "                + IDENTIFIER_TYPE   + SEPARATOR +
                        TraceEntry.COLUMN_NAME_SESSION      + TEXT_TYPE         + SEPARATOR +
                        TraceEntry.COLUMN_NAME_IS_VALID     + BOOLEAN_TYPE      + SEPARATOR +
                        TraceEntry.COLUMN_NAME_IS_CLOSED    + BOOLEAN_TYPE      + SEPARATOR +
                        TraceEntry.COLUMN_NAME_ELAPSED_TIME + DOUBLE_TYPE       + SEPARATOR +
                        TraceEntry.COLUMN_NAME_ELAPSED_DISTANCE + DOUBLE_TYPE + ")";

        String SQL_CREATE_TRACES =
                "CREATE TABLE "+ TraceEntry.TABLE_NAME_TRACES +" ("+
                        TraceEntry._ID + " "                + IDENTIFIER_TYPE   + SEPARATOR +
                        TraceEntry.COLUMN_NAME_LATITUDE     + DOUBLE_TYPE       + SEPARATOR +
                        TraceEntry.COLUMN_NAME_LONGITUDE    + DOUBLE_TYPE       + SEPARATOR +
                        TraceEntry.COLUMN_NAME_ATTRIBUTES   + TEXT_TYPE         + SEPARATOR +
                        TraceEntry.COLUMN_NAME_TIMESTAMP    + DATE_TYPE         + SEPARATOR +
                        TraceEntry.COLUMN_NAME_TRACK_ID     + INT_TYPE          + SEPARATOR +
                        " FOREIGN KEY ( "+ TraceEntry.COLUMN_NAME_TRACK_ID+" ) REFERENCES "+ TraceEntry.TABLE_NAME_TRACKS+ " ( "+ TraceEntry._ID+" ) ON DELETE CASCADE)";

        String SQL_DELETE_TRACKS_TABLE =
                "DROP TABLE IF EXISTS " + TraceEntry.TABLE_NAME_TRACKS;

        String SQL_DELETE_TRACES_TABLE =
                "DROP TABLE IF EXISTS " + TraceEntry.TABLE_NAME_TRACES;


        String SQL_RAW_QUERY_COMPLETE_TRACKS =
                "SELECT "+
                        TraceEntry.COLUMN_NAME_LATITUDE         + SEPARATOR +
                        TraceEntry.COLUMN_NAME_LONGITUDE        + SEPARATOR +
                        TraceEntry.COLUMN_NAME_TIMESTAMP        + SEPARATOR +
                        TraceEntry.COLUMN_NAME_ATTRIBUTES       + SEPARATOR +
                        TraceEntry.COLUMN_NAME_SESSION          + SEPARATOR +
                        TraceEntry.COLUMN_NAME_IS_CLOSED        + SEPARATOR +
                        TraceEntry.COLUMN_NAME_IS_VALID         + SEPARATOR +
                        TraceEntry.COLUMN_NAME_ELAPSED_DISTANCE + SEPARATOR +
                        TraceEntry.COLUMN_NAME_ELAPSED_TIME     +
                        " FROM "+ TraceEntry.TABLE_NAME_TRACKS+ " INNER JOIN "+ TraceEntry.TABLE_NAME_TRACES +
                        " ON "+ TraceEntry.TABLE_NAME_TRACKS+"."+ TraceEntry._ID+"="+ TraceEntry.TABLE_NAME_TRACES+"."+ TraceEntry.COLUMN_NAME_TRACK_ID+
                        " WHERE "+ TraceEntry.COLUMN_NAME_SESSION + " = ?";
    }
}
