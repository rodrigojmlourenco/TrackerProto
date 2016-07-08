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

import android.provider.BaseColumns;

/**
 * Created by Rodrigo Lourenço on 06/07/2016.
 */
interface RouteLocationEntry extends BaseColumns, BaseTypes {
    String TABLE_NAME = "traces";

    String COLUMN_SESSION = "session";
    String COLUMN_LATITUDE = "latitude";
    String COLUMN_LONGITUDE = "longitude";
    String COLUMN_TIMESTAMP = "timestamp";

    String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    _ID + " " + IDENTIFIER_TYPE + SEPARATOR +
                    COLUMN_SESSION + INT_TYPE + SEPARATOR +
                    COLUMN_LATITUDE + DOUBLE_TYPE + SEPARATOR +
                    COLUMN_LONGITUDE + DOUBLE_TYPE + SEPARATOR +
                    COLUMN_TIMESTAMP + DATE_TYPE + SEPARATOR +
                    " FOREIGN KEY ( " + COLUMN_SESSION + " ) " +
                    " REFERENCES " + RouteSummaryEntry.TABLE_NAME + " ( " + RouteSummaryEntry._ID + " ) " +
                    " ON DELETE CASCADE)";

    String SQL_DELETE_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;
}
