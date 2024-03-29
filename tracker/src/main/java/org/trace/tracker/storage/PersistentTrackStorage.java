package org.trace.tracker.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.provider.BaseColumns;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.trace.tracker.storage.data.TraceLocation;
import org.trace.tracker.storage.data.Track;
import org.trace.tracker.storage.data.TrackSummary;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The PersistentTrackStorage manages the stored tracks. The storage is performed using the device's
 * native SQLite support.
 */
public class PersistentTrackStorage {

    private static final String LOG_TAG = "TrackStorage";
    private TrackStorageDBHelper mDBHelper;

    public PersistentTrackStorage(Context context){
        mDBHelper = new TrackStorageDBHelper(context);
    }

    /* Constructors && Destructors
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    public TrackSummary createNewTrackSummary(long startTime, int modality, int sensingType){

        String trackID = this.getNextAvailableId();

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
            summary.setTrackId(trackID);
            summary.setStartTimestamp(startTime);
            summary.setStoppedTimestamp(startTime);
            summary.setElapsedDistance(0);
            summary.setModality(modality);
            summary.setSensingType(sensingType);

            return summary;
        }
    }


    /**
     * Stores a new location, which is associated with a track.
     *
     * @param location The new location.
     * @param trackId The session identifier that identifies the track.
     */
    public void storeLocation(TraceLocation location, String trackId){

        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(TrackLocationEntry.COLUMN_LATITUDE, location.getLatitude());
        values.put(TrackLocationEntry.COLUMN_LONGITUDE, location.getLongitude());
        values.put(TrackLocationEntry.COLUMN_ATTRIBUTES, location.getSecondaryAttributesAsJson().toString());
        values.put(TrackLocationEntry.COLUMN_TIMESTAMP, location.getTime());
        values.put(TrackLocationEntry.COLUMN_TRACK_ID, trackId);

        db.insert(TrackLocationEntry.TABLE_NAME, null, values);

        db.close();
    }

    public boolean deleteTrack(String trackId){

        deleteTrackTraces(trackId);
        deleteTrackSummary(trackId);

        return true;
    }

    private void deleteTrackTraces(String trackId){
        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        String selectionClause = TrackLocationEntry.COLUMN_TRACK_ID + "=?";
        String[] selectionArgs = new String[] { trackId };

        db.delete(TrackLocationEntry.TABLE_NAME, selectionClause, selectionArgs);
        db.close();

    }

    private void deleteTrackSummary(String trackId){
        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        String selectionClause = TrackSummaryEntry.COLUMN_ID + "=?";
        String[] selectionArgs = new String[] { trackId };

        int affected = db.delete(TrackSummaryEntry.TABLE_NAME, selectionClause, selectionArgs);
        db.close();

        if(affected < 0)
            throw new RuntimeException("Did not remove any row!!! deleteTrackSummary@PersistentTrackStorage");
    }

    /* Updaters
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    public void updateTrackSummaryDistanceAndTime(TrackSummary summary){

        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        String selectionClause = TrackSummaryEntry.COLUMN_ID + " = ?";
        String[] selectionArgs = new String[1];

        ContentValues values = new ContentValues();
        values.put(TrackSummaryEntry.COLUMN_FINISHED_AT, summary.getStop());
        values.put(TrackSummaryEntry.COLUMN_DISTANCE, summary.getElapsedDistance());
        selectionArgs[0] = summary.getTrackId();


        int affected = db.update(TrackSummaryEntry.TABLE_NAME, values,selectionClause, selectionArgs);
        db.close();

        if(affected < 0)
            throw new RuntimeException("Did not update any row!!! updateTrackSummaryDistanceAndTime@PersistentTrackStorage");

    }

    public void updateTrackSummary(TrackSummary summary){

        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        String selectionClause = TrackSummaryEntry.COLUMN_ID + "=?";
        String[] selectionArgs = new String[1];

        ContentValues values = new ContentValues();
        values.put(TrackSummaryEntry.COLUMN_STARTED_AT,  summary.getStart());
        values.put(TrackSummaryEntry.COLUMN_FINISHED_AT, summary.getStop());
        values.put(TrackSummaryEntry.COLUMN_MODALITY, summary.getModality());
        values.put(TrackSummaryEntry.COLUMN_DISTANCE, summary.getElapsedDistance());

        if(summary.getFromLocation() != null){
            values.put(TrackSummaryEntry.COLUMN_FROM_LAT, summary.getFromLocation().getLatitude());
            values.put(TrackSummaryEntry.COLUMN_FROM_LON, summary.getFromLocation().getLongitude());
        }

        if(summary.getToLocation() != null){
            values.put(TrackSummaryEntry.COLUMN_TO_LAT, summary.getToLocation().getLatitude());
            values.put(TrackSummaryEntry.COLUMN_TO_LON, summary.getToLocation().getLongitude());
        }

        if(summary.getSemanticFromLocation() != null && !summary.getSemanticFromLocation().isEmpty())
            values.put(TrackSummaryEntry.COLUMN_FROM, summary.getSemanticFromLocation());

        if(summary.getSemanticToLocation() != null && !summary.getSemanticToLocation().isEmpty())
            values.put(TrackSummaryEntry.COLUMN_TO, summary.getSemanticToLocation());

        selectionArgs[0] = summary.getTrackId();

        int affected = db.update(TrackSummaryEntry.TABLE_NAME, values, selectionClause, selectionArgs);
        db.close();

        if(affected <= 0) {
            dumpTrackSummaryTable();
            throw new RuntimeException("Did not update any row for track "+summary.getTrackId()+"!!! updateTrackSummary@PersistentTrackStorage");
        }

    }

    /* Getters
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    public Track getCompleteTrack(TrackSummary summary){
        return  this.getTrack(summary.getTrackId());
    }

    public TrackSummary getTrackSummary(String trackId){
        TrackSummary summary = new TrackSummary();

        SQLiteDatabase db = mDBHelper.getReadableDatabase();

        String[] columns = new String[]{
                TrackSummaryEntry.COLUMN_ID,
                TrackSummaryEntry.COLUMN_STARTED_AT,
                TrackSummaryEntry.COLUMN_FINISHED_AT,
                TrackSummaryEntry.COLUMN_DISTANCE,
                TrackSummaryEntry.COLUMN_MODALITY,
                TrackSummaryEntry.COLUMN_SENSING_TYPE,
                TrackSummaryEntry.COLUMN_FROM,
                TrackSummaryEntry.COLUMN_TO,
                TrackSummaryEntry.COLUMN_FROM_LAT,
                TrackSummaryEntry.COLUMN_FROM_LON,
                TrackSummaryEntry.COLUMN_TO_LAT,
                TrackSummaryEntry.COLUMN_TO_LON
        };

        String selectionClause = TrackSummaryEntry.COLUMN_ID + "=?";
        String[] selectionArgs = new String[]{ trackId };

        Cursor cursor = db.query(TrackSummaryEntry.TABLE_NAME, columns, selectionClause, selectionArgs, "", "" ,"");

        if (cursor.moveToNext()){
            String _session = cursor.getString(0);
            long _start     = cursor.getLong(1);
            long _stopped   = cursor.getLong(2);
            double _length  = cursor.getDouble(3);
            int _mod        = cursor.getInt(4);
            int _sens       = cursor.getInt(5);
            String _from    = cursor.getString(6);
            String _to      = cursor.getString(7);
            double _fromLat = cursor.getDouble(8);
            double _fromLon = cursor.getDouble(9);
            double _toLat   = cursor.getDouble(10);
            double _toLon   = cursor.getDouble(11);

            summary.setTrackId(_session);
            summary.setStartTimestamp(_start);
            summary.setStoppedTimestamp(_stopped);
            summary.setElapsedDistance(_length);
            summary.setModality(_mod);
            summary.setSensingType(_sens);
            summary.setSemanticFromLocation(_from);
            summary.setSemanticToLocation(_to);

            Location from = new Location(""), to = new Location("");
            from.setLatitude(_fromLat);
            from.setLongitude(_fromLon);
            to.setLatitude(_toLat);
            to.setLongitude(_toLon);

            summary.setFromLocation(from);
            summary.setToLocation(to);
        }else{
            db.close();
            dumpTrackSummaryTable();
            throw new RuntimeException("No track with id = {"+trackId+"} getTrackSummary@PersistentTrackStorage");
        }

        db.close();

        return summary;

    }

    public List<TrackSummary> getAllTrackSummaries(){

        List<TrackSummary> summaries = new ArrayList<>();

        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        String[] columns = new String[]{
                TrackSummaryEntry.COLUMN_ID,
                TrackSummaryEntry.COLUMN_STARTED_AT,
                TrackSummaryEntry.COLUMN_FINISHED_AT,
                TrackSummaryEntry.COLUMN_DISTANCE,
                TrackSummaryEntry.COLUMN_MODALITY,
                TrackSummaryEntry.COLUMN_SENSING_TYPE,
                TrackSummaryEntry.COLUMN_FROM,
                TrackSummaryEntry.COLUMN_TO,
                TrackSummaryEntry.COLUMN_FROM_LAT,
                TrackSummaryEntry.COLUMN_FROM_LON,
                TrackSummaryEntry.COLUMN_TO_LAT,
                TrackSummaryEntry.COLUMN_TO_LON
        };

        Cursor cursor = db.query(TrackSummaryEntry.TABLE_NAME, columns, "", new String[]{}, "", "" ,"");

        while (cursor.moveToNext()){
            String _session = cursor.getString(0);
            long _start     = cursor.getLong(1);
            long _stopped   = cursor.getLong(2);
            double _length  = cursor.getDouble(3);
            int _mod        = cursor.getInt(4);
            int _sens       = cursor.getInt(5);
            String _from    = cursor.getString(6);
            String _to      = cursor.getString(7);
            double _fromLat = cursor.getDouble(8);
            double _fromLon = cursor.getDouble(9);
            double _toLat   = cursor.getDouble(10);
            double _toLon   = cursor.getDouble(11);

            TrackSummary summary = new TrackSummary();
            summary.setTrackId(_session);
            summary.setStartTimestamp(_start);
            summary.setStoppedTimestamp(_stopped);
            summary.setElapsedDistance(_length);
            summary.setModality(_mod);
            summary.setSensingType(_sens);
            summary.setSemanticFromLocation(_from);
            summary.setSemanticToLocation(_to);

            Location from = new Location(""), to = new Location("");
            from.setLatitude(_fromLat);
            from.setLongitude(_fromLon);
            to.setLatitude(_toLat);
            to.setLongitude(_toLon);

            summary.setFromLocation(from);
            summary.setToLocation(to);


            summaries.add(summary);
        }

        db.close();

        return summaries;
    }

    public Track getTrack(String trackId){

        Track track = new Track(getTrackSummary(trackId));
        track.setTracedTrack(getTrackTraces(trackId));

        return track;
    }

    private List<TraceLocation> getTrackTraces(String trackId){

        List<TraceLocation> trace = new ArrayList<>();

        SQLiteDatabase db = mDBHelper.getReadableDatabase();

        String[] columns = new String[]{
                TrackLocationEntry.COLUMN_TRACK_ID,
                TrackLocationEntry.COLUMN_LATITUDE,
                TrackLocationEntry.COLUMN_LONGITUDE,
                TrackLocationEntry.COLUMN_TIMESTAMP,
                TrackLocationEntry.COLUMN_ATTRIBUTES
        };

        String selectionClause = TrackLocationEntry.COLUMN_TRACK_ID + "=?";
        String[] selectionArgs = new String[]{ trackId };

        Cursor c = db.query(TrackLocationEntry.TABLE_NAME, columns, selectionClause, selectionArgs, "", "", "");

        JsonParser parser = new JsonParser();
        TraceLocation location;
        while(c.moveToNext()) {
            location = new TraceLocation();

            location.setLatitude(c.getDouble(c.getColumnIndex(TrackLocationEntry.COLUMN_LATITUDE)));
            location.setLongitude(c.getDouble(c.getColumnIndex(TrackLocationEntry.COLUMN_LONGITUDE)));
            location.setTime(c.getLong(c.getColumnIndex(TrackLocationEntry.COLUMN_TIMESTAMP)));

            String attributes = c.getString(c.getColumnIndex(TrackLocationEntry.COLUMN_ATTRIBUTES));
            location.setSecondaryAttributes((JsonObject) parser.parse(attributes));

            trace.add(location);
        }

        db.close();

        return trace;
    }

    /**
     * Returns the number of tracks currently stored in the database.
     * @return The tracks count.
     */
    public int getTracksCount(){

        int count;
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        count = (int) DatabaseUtils.queryNumEntries(db, TrackSummaryEntry.TABLE_NAME,"", null);
        db.close();

        return count;
    }


    public String getNextAvailableId(){
        int nextId;

        SQLiteDatabase db = mDBHelper.getReadableDatabase();

        Cursor c = db.query(
                TrackSummaryEntry.TABLE_NAME,
                new String[]{"MAX("+ TrackSummaryEntry._ID+")"},
                null, null, null, null, null);

        if(c.moveToFirst()){
            nextId = c.getInt(0)+1;
        }else{
            nextId = -1;
        }

        db.close();

        return String.valueOf(nextId);
    }


    /* Logging
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    public void dumpTrackSummaryTable(){

        SQLiteDatabase db = mDBHelper.getReadableDatabase();

        String[] columns = new String[]{
                TrackSummaryEntry._ID,
                TrackSummaryEntry.COLUMN_ID,
                TrackSummaryEntry.COLUMN_STARTED_AT,
                TrackSummaryEntry.COLUMN_FINISHED_AT,
                TrackSummaryEntry.COLUMN_DISTANCE,
                TrackSummaryEntry.COLUMN_MODALITY,
                TrackSummaryEntry.COLUMN_SENSING_TYPE,
                TrackSummaryEntry.COLUMN_FROM,
                TrackSummaryEntry.COLUMN_TO,
                TrackSummaryEntry.COLUMN_FROM_LAT,
                TrackSummaryEntry.COLUMN_FROM_LON,
                TrackSummaryEntry.COLUMN_TO_LAT,
                TrackSummaryEntry.COLUMN_TO_LON,
        };

        Cursor cursor = db.query(TrackSummaryEntry.TABLE_NAME, columns, "", new String[]{}, "", "" ,"");

        Log.d("TrackSummary", "");
        Log.d("TrackSummary", "");
        Log.d("TrackSummary", "Dumping track summaries");
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd/MM");

        while(cursor.moveToNext()){


            String _id = cursor.getString(0);
            String _start   = cursor.getString(2);
            String _stopped = cursor.getString(3);
            String _length  = cursor.getString(4);
            String _from    = cursor.getString(7);
            String _to    = cursor.getString(8);


            JsonObject track = new JsonObject();
            track.addProperty("id", _id);
            track.addProperty("startedAt", sdf.format(new Date(Long.valueOf(_start))));
            track.addProperty("endedAt", sdf.format(new Date(Long.valueOf(_stopped))));
            track.addProperty("length", _length);
            track.addProperty("from", _from);
            track.addProperty("to", _to);

            Log.i("TrackSummary", track.toString());
        }

        db.close();
    }

    public void dumpTraceSummarized(){
        SQLiteDatabase db = mDBHelper.getReadableDatabase();

        Cursor c = db.query(TrackLocationEntry.TABLE_NAME, null, "", null, "", "", "");
        Log.i("TrackSummary", "There are currently "+c.getCount()+" stored locations");

        db.close();

    }

    public void dumpTraces(){
        SQLiteDatabase db = mDBHelper.getReadableDatabase();

        Cursor c = db.query(TrackLocationEntry.TABLE_NAME, new String[]{"*"}, "", null, "", "", "");

        while(c.moveToNext()){
            /*
            String COLUMN_TRACK_ID = "trackId";
            String COLUMN_LATITUDE = "latitude";
            String COLUMN_LONGITUDE = "longitude";
            String COLUMN_TIMESTAMP = "timestamp";
            String COLUMN_ATTRIBUTES = "Attributes";
            */

            JsonObject jtrace = new JsonObject();
            jtrace.addProperty("_id", c.getString(0));
            jtrace.addProperty("trackId", c.getString(1));
            jtrace.addProperty("timestamp", c.getString(4));


            Log.i("TrackSummary", jtrace.toString());
        }

        db.close();

    }

    /* Deprecated - Soon to be removed
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
    @Deprecated
    public long createTrack(String session, boolean isValid){

        /*
        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(TraceEntry.COLUMN_NAME_SESSION, session);
        values.put(TraceEntry.COLUMN_NAME_IS_CLOSED, 0);
        values.put(TraceEntry.COLUMN_NAME_IS_VALID, (isValid ? 1 : 0));
        values.put(TraceEntry.COLUMN_NAME_ELAPSED_TIME, 0);
        values.put(TraceEntry.COLUMN_NAME_ELAPSED_DISTANCE, 0);

        return db.insert(TraceEntry.TABLE_NAME_TRACKS,null,values);
        */
        throw new RuntimeException("Deprecated : createTrack@PersistentTrackStorage");
    }

    @Deprecated
    public void storeLocation(TraceLocation location, String trackId, boolean isRemote){

        /*
        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(TrackLocationEntry.COLUMN_LATITUDE, location.getLatitude());
        values.put(TrackLocationEntry.COLUMN_LONGITUDE, location.getLongitude());
        values.put(TrackLocationEntry.COLUMN_ATTRIBUTES, location.getSecondaryAttributesAsJson().toString());
        values.put(TrackLocationEntry.COLUMN_TIMESTAMP, location.getTime());
        values.put(TrackLocationEntry.COLUMN_TRACK_ID, trackId);

        db.insert(TrackLocationEntry.TABLE_NAME, null, values);
        */
        throw new RuntimeException("Deprecated : storeLocation(TraceLocation, String, bool)@PersistentTrackStorage");

    }

    /**
     * Fetches a track as a Track object, given the provided session identifier.
     * @param session The session identifier
     * @return The Track
     * @see Track
     */
    @Deprecated
    public Track getTrack_DEPRECATED(String session){

        /*
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
        track.setTrackId(storedSession);
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

            String Attributes = c.getString(c.getColumnIndex(TraceEntry.COLUMN_NAME_ATTRIBUTES));
            location.setSecondaryAttributes((JsonObject) parser.parse(Attributes));

            track.addTracedLocation(location);

        }while (c.moveToNext());

        db.close();

        return track;
        */

        throw new RuntimeException("Deprecated : getTrack_DEPRECATED@PersistentTrackStorage");
    }

    /**
     * Fetches a list of all stored tracks. These are provided as simplified Tracks that contain only
     * top level information.
     * @return List of simplified tracks
     * @see TrackSummary
     */
    @Deprecated
    public List<TrackSummary> getTracksSessions(){

        /*
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

                //simplifiedTracks.add(new TrackSummary(session, isClosed, isValid));
                simplifiedTracks.add(new TrackSummary(session));

            } while (c.moveToNext());
        }

        db.close();

        return simplifiedTracks;
        */

        throw new RuntimeException("Deprecated : getTrackSessions@PersistentTrackStorage");
    }

    @Deprecated
    public boolean updateTravelledDistanceAndTime(String session, double distance, double time){

        /*
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
        */

        throw new RuntimeException("Deprecated : updateTravelledDistanceAndTime@PersistentTrackStorage");
    }

    @Deprecated
    public boolean deleteTrackById(String session){

        /*
        int trackId = getTrackId(session);

        if(trackId == -1) return false;

        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        String selection = TraceEntry._ID + " = ?";
        String[] selectionArgs = {String.valueOf(trackId)};

        int affected = db.delete(TraceEntry.TABLE_NAME_TRACKS, selection, selectionArgs);
        Log.d("DELETED", "Rows deleted with session "+ session+" : "+String.valueOf(affected));

        db.close();

        return affected > 0;
        */
        throw new RuntimeException("Deprecated : deleteTrackById@PersistentTrackStorage");
    }



    /* DB Helpers
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    private class TrackStorageDBHelper extends SQLiteOpenHelper {

        public static final int DATABASE_VERSION = 2;
        public static final String DATABASE_NAME = "TraceTracker.db";

        public TrackStorageDBHelper(Context context){
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(TrackSummaryEntry.SQL_CREATE_TABLE);
            db.execSQL(TrackLocationEntry.SQL_CREATE_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(LOG_TAG, "Upgrading db to version "+newVersion);
            db.execSQL(TrackLocationEntry.SQL_DELETE_TABLE);
            db.execSQL(TrackSummaryEntry.SQL_DELETE_TABLE);
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
        String COLUMN_FROM          = " fromLocation ";
        String COLUMN_TO            = " toLocation ";

        String SQL_CREATE_TABLE =
                "CREATE TABLE "+ TABLE_NAME +" ( " +
                        _ID                 + IDENTIFIER_TYPE   + SEPARATOR +
                        COLUMN_ID           + TEXT_TYPE         + SEPARATOR +
                        COLUMN_FROM_LAT     + DOUBLE_TYPE       + SEPARATOR +
                        COLUMN_FROM_LON     + DOUBLE_TYPE       + SEPARATOR +
                        COLUMN_TO_LAT       + POINT_TYPE        + SEPARATOR +
                        COLUMN_TO_LON       + POINT_TYPE        + SEPARATOR +
                        COLUMN_STARTED_AT   + TIMESTAMP_TYPE    + SEPARATOR +
                        COLUMN_FINISHED_AT  + TIMESTAMP_TYPE    + SEPARATOR +
                        COLUMN_DISTANCE     + DOUBLE_TYPE       + SEPARATOR +
                        COLUMN_SENSING_TYPE + INT_TYPE          + SEPARATOR +
                        COLUMN_MODALITY     + INT_TYPE          + SEPARATOR +
                        COLUMN_FROM + ADDR_TYPE         + SEPARATOR +
                        COLUMN_TO + ADDR_TYPE         + ")" ;

        String SQL_DELETE_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    private interface TrackLocationEntry extends BaseColumns, BaseTypes {
        String TABLE_NAME = "traces";

        String COLUMN_TRACK_ID = "trackId";
        String COLUMN_LATITUDE = "latitude";
        String COLUMN_LONGITUDE = "longitude";
        String COLUMN_TIMESTAMP = "timestamp";
        String COLUMN_ATTRIBUTES = "attributes";

        String SQL_CREATE_TABLE =
                "CREATE TABLE "+ TABLE_NAME +" ("+
                        _ID + " "                + IDENTIFIER_TYPE   + SEPARATOR +
                        COLUMN_TRACK_ID + INT_TYPE          + SEPARATOR +
                        COLUMN_LATITUDE + DOUBLE_TYPE       + SEPARATOR +
                        COLUMN_LONGITUDE + DOUBLE_TYPE       + SEPARATOR +
                        COLUMN_TIMESTAMP + DATE_TYPE         + SEPARATOR +
                        COLUMN_ATTRIBUTES + TEXT_TYPE         + SEPARATOR +
                " FOREIGN KEY ( "+ COLUMN_TRACK_ID +" ) " +
                " REFERENCES "+ TrackSummaryEntry.TABLE_NAME + " ( "+ TrackSummaryEntry._ID+" ) " +
                " ON DELETE CASCADE)";

        String SQL_DELETE_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }
}
