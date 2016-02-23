package org.trace.trackerproto.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Rodrigo Lourenço on 22/02/2016.
 */
public class TracksOpenHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Trace.db";

    private static final String TRACK_TABLE_NAME = "tracks";

    private static final String TRACK_TABLE_CREATE =
            "CREATE TABLE "+TRACK_TABLE_NAME+" ( "+
                    "Id UNSIGNED INT PRIMARY KEY AUTO_INCREMENT "+
                    "Lat DOUBLE NOT NULL, "+
                    "Lon DOUBLE NOT NULL, "+
                    "Ele DOUBLE NOT NULL, "+
                    "Speed DOUBLE NOT NULL, "+
                    "Accuracy DOUBLE NOT NULL, "+
                    "Activity TEXT, "+
                    "SessionId TEXT NOT NULL, "+
                    "Timestamp DATE NOT NULL);";

    private static final String TRACE_CLEAR_ENTRIES =
            "DROP TABLE IF EXISTS "+TRACK_TABLE_NAME;

    protected TracksOpenHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TRACK_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(TRACE_CLEAR_ENTRIES);
    }
}
