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
interface RouteSummaryEntry extends BaseColumns, BaseTypes {
    String TABLE_NAME = "RouteSummaries";

    String COLUMN_ID = " id ";

    String COLUMN_STARTED_AT = " startedAt ";
    String COLUMN_ENDED_AT = " finishedAt ";
    String COLUMN_DISTANCE = " distance ";
    String COLUMN_SENSING_TYPE = " sensingType ";
    String COLUMN_MODALITY = " modality ";
    String COLUMN_POINTS = " points ";
    String COLUMN_AVG_SPEED = " avgSpeed";
    String COLUMN_TOP_SPEED = " topSpeed";


    String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " ( " +
                    _ID + IDENTIFIER_TYPE + SEPARATOR +
                    COLUMN_ID + TEXT_TYPE + " UNIQUE " +SEPARATOR +
                    COLUMN_STARTED_AT + TIMESTAMP_TYPE + SEPARATOR +
                    COLUMN_ENDED_AT + TIMESTAMP_TYPE + SEPARATOR +
                    COLUMN_DISTANCE + DOUBLE_TYPE + SEPARATOR +
                    COLUMN_SENSING_TYPE + INT_TYPE + SEPARATOR +
                    COLUMN_MODALITY + INT_TYPE + SEPARATOR +
                    COLUMN_POINTS + INT_TYPE + SEPARATOR +
                    COLUMN_AVG_SPEED + DOUBLE_TYPE + SEPARATOR +
                    COLUMN_TOP_SPEED + DOUBLE_TYPE
                    + ")";

    String SQL_DELETE_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;
}
