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

package org.trace.storeclient.cache.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class RouteStorageDBHelper extends SQLiteOpenHelper{

    private static final String LOG_TAG = "RouteStorageDBHelper";

    public static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "Routes.db";

    public RouteStorageDBHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(RouteSummaryEntry.SQL_CREATE_TABLE);
        db.execSQL(RouteLocationEntry.SQL_CREATE_TABLE);
        db.execSQL(RouteStateEntry.SQL_CREATE_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(LOG_TAG, "Upgrading db to version "+newVersion);
        db.execSQL(RouteLocationEntry.SQL_DELETE_TABLE);
        db.execSQL(RouteSummaryEntry.SQL_DELETE_TABLE);
        db.execSQL(RouteStateEntry.SQL_DELETE_TABLE);
        onCreate(db);
    }
}
