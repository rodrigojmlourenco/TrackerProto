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
import org.trace.storeclient.remote.RouteHttpClient;
import org.trace.storeclient.storage.LocalRouteStorage;
import org.trace.storeclient.storage.RemoteRouteStorage;
import org.trace.storeclient.utils.ConnectivityUtils;


public class RouteCache {

    private Context mContext;
    private RouteHttpClient mHttpClient;
    private RemoteRouteStorage mRemoteStorage;
    private LocalRouteStorage mLocalStorage;
    private TraceAuthenticationManager mAuthManager;

    private RouteCache(Context context){
        mContext = context;
        mHttpClient = new RouteHttpClient();
        mRemoteStorage = RemoteRouteStorage.getLocalStorage(context);
    }


    private static RouteCache CACHE = null;

    public static RouteCache getCacheInstance(Context context){

        synchronized (RouteCache.class){
            if(CACHE == null)
                CACHE = new RouteCache(context);
        }

        return CACHE;
    }


    public void storeRoute(Route route){

        if(!ConnectivityUtils.isConnected(mContext))
            //Store the route locally until it is capable of uploading it
            mLocalStorage.storeCompleteRoute(route);
        else{
            //TODO: how to acquire the authToken???
        }
    }
}
