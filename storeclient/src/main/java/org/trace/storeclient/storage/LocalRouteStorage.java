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


import android.content.Context;

public class LocalRouteStorage {

    private RouteStorageDBHelper mDBHelper;

    private LocalRouteStorage(Context context){
        mDBHelper = new RouteStorageDBHelper(context, "LocalRouteStorage.db");
    }

    private static LocalRouteStorage LOCAL_ROUTE_STORAGE = null;
    public LocalRouteStorage getStorageInstance(Context context){
        synchronized (LocalRouteStorage.class){
            if(LOCAL_ROUTE_STORAGE == null)
                LOCAL_ROUTE_STORAGE = new LocalRouteStorage(context);
        }

        return LOCAL_ROUTE_STORAGE;
    }
}
