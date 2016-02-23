package org.trace.trackerproto.tracking;

import android.content.Context;
import android.location.Location;

import org.trace.trackerproto.store.TRACEStoreApiClient;
import org.trace.trackerproto.tracking.data.Track;
import org.trace.trackerproto.tracking.modules.location.FusedLocationModule;
import org.trace.trackerproto.tracking.storage.TrackInternalStorage;
import org.trace.trackerproto.tracking.storage.exceptions.UnableToStoreTrackException;

/**
 * Created by Rodrigo Lourenço on 23/02/2016.
 */
public class TRACETracker implements CollectorManager{

    private static TRACETracker TRACKER = null;

    private Context mContext;
    private GoogleClientManager mGoogleMan;

    private Track track = null;

    //Location Modules
    private FusedLocationModule fusedLocationModule = null;

    private TRACETracker(Context context){
        mContext = context;

        mGoogleMan = new GoogleClientManager(mContext);
        mGoogleMan.connect();
    }

    protected static TRACETracker getTracker(Context ctx){
        if(TRACKER == null)
            TRACKER = new TRACETracker(ctx);

        return TRACKER;
    }


    private void init(){

        fusedLocationModule = new FusedLocationModule(
                mContext,
                mGoogleMan.getApiClient());
    }


    @Override
    public void storeLocation(Location location) {

        if(track == null){
            track = new Track(TRACEStoreApiClient.getSessionId(), location);
        }else
            track.addTracedLocation(location);
    }


    public void startLocationUpdates(){
        if(fusedLocationModule==null) init();

        track = null;
        fusedLocationModule.startLocationUpdates();

    }

    public void stopLocationUpdates(){
        fusedLocationModule.stopTracking();

        if(track == null) return;

        track.setSessionId(TRACEStoreApiClient.getSessionId());

        try {
            TrackInternalStorage.storeTracedTrack(mContext, TRACEStoreApiClient.getSessionId(), track);
        } catch (UnableToStoreTrackException e) {
            e.printStackTrace();
        }
    }
}
