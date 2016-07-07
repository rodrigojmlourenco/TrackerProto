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

package org.trace.storeclient.cache;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.GoogleApiClient;

import org.trace.storeclient.TraceAuthenticationManager;
import org.trace.storeclient.exceptions.UserIsNotLoggedException;
import org.trace.storeclient.utils.ConnectivityUtils;

import java.util.Set;

/**
 * Created by Rodrigo Lourenço on 07/07/2016.
 */
public class CacheConnectivityMonitor extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        Set<String> keys = extras.keySet();

        RouteCache cache = RouteCache.getCacheInstance(context);

        if(ConnectivityUtils.isConnected(context)){

            GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(Auth.GOOGLE_SIGN_IN_API)
                    .addApi(Auth.CREDENTIALS_API)
                    .build();

            TraceAuthenticationManager mAuthManager
                    = TraceAuthenticationManager.getAuthenticationManager(context, mGoogleApiClient);

            try {
                String authToken = mAuthManager.getAuthenticationToken();
                cache.postPendingRoutes(authToken);
            } catch (UserIsNotLoggedException e) {
                e.printStackTrace();
            }



        }else{
            Log.w("t", "is no longer connected");
        }
    }
}
