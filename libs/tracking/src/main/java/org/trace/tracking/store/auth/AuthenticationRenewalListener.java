package org.trace.tracking.store.auth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import org.trace.tracking.TrackingConstants;
import org.trace.tracking.store.TRACEStore;
import org.trace.tracking.store.TraceAuthenticationManager;
import org.trace.tracking.tracker.storage.data.Track;

/**
 * Created by Rodrigo Lourenço on 30/03/2016.
 */
public class AuthenticationRenewalListener extends BroadcastReceiver {

    private TraceAuthenticationManager mAuthManager;

    public AuthenticationRenewalListener(TraceAuthenticationManager manager){
        mAuthManager = manager;
    }

    public static IntentFilter getAuthenticationRenewalFilter(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(TrackingConstants.store.TOKEN_EXPIRED_ACTION);
        return filter;
    }

    public static Intent getFailedToSubmitTrackIntent(Track track){
        return new Intent(TrackingConstants.store.TOKEN_EXPIRED_ACTION)
                .putExtra(TrackingConstants.store.FAILED_OPERATION_KEY, TRACEStore.Operations.submitTrack)
                .putExtra(TrackingConstants.store.FAILED_OPERATION_EXTRAS, track);
    }

    public static Intent getFailedToInitiateSessionIntent(){
        return new Intent(TrackingConstants.store.TOKEN_EXPIRED_ACTION)
                .putExtra(TrackingConstants.store.FAILED_OPERATION_KEY, TRACEStore.Operations.initiateSession);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        //TODO: handle os diferentes tipos de operações que podem falhar
    }
}
