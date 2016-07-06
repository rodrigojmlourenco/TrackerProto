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

package org.trace.storeclient;

import android.content.Context;

import org.trace.storeclient.data.Route;
import org.trace.storeclient.data.RouteSummary;
import org.trace.storeclient.remote.BaseHttpClient;
import org.trace.storeclient.storage.RouteStorage;

import java.util.List;


public class RoutesCache {

    private BaseHttpClient mHttpClient;
    private RouteStorage mLocalStorage;

    private RoutesCache(Context context){
        mHttpClient = new BaseHttpClient();
        mLocalStorage = RouteStorage.getLocalStorage(context);
    }


    private static RoutesCache CACHE = null;

    public static RoutesCache getCacheInstance(Context context){

        synchronized (RoutesCache.class){
            if(CACHE == null)
                CACHE = new RoutesCache(context);
        }

        return CACHE;
    }


    public List<RouteSummary> getAllRouteSummaries(){
        return null;
    }

    public RouteSummary getRemoteRouteSummary(String routeSession){
        return null;
    }

    public void storeRoute(Route route){
        mLocalStorage.storeCompleteRoute(route);
    }

}
