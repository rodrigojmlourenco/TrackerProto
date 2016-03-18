package org.trace.tracking;

import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationServices;

/**
 * Created by Rodrigo Lourenço on 17/02/2016.
 */
public class GoogleClientManager
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private final GoogleApiClient mGoogleApiClient;

    public GoogleClientManager(Context ctx){

        mGoogleApiClient = new GoogleApiClient.Builder(ctx)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(ActivityRecognition.API)
                .build();
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }


    public boolean isConnected(){
        return mGoogleApiClient.isConnected();
    }

    public void connect(){
        mGoogleApiClient.connect();
    }

    public void disconnect(){
        mGoogleApiClient.disconnect();
    }

    public GoogleApiClient getApiClient(){
        return mGoogleApiClient;
    }
}
