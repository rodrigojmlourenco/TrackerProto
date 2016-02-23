package org.trace.trackerproto.tracking;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import org.trace.trackerproto.store.TRACEStore;
import org.trace.trackerproto.store.TRACEStoreApiClient;
import org.trace.trackerproto.tracking.data.Track;
import org.trace.trackerproto.tracking.modules.location.FusedLocationModule;
import org.trace.trackerproto.tracking.storage.TrackInternalStorage;
import org.trace.trackerproto.tracking.storage.exceptions.UnableToStoreTrackException;

import java.util.List;


/**
 * Created by Rodrigo Lourenço on 17/02/2016.
 */
public class TRACETrackerService extends Service implements CollectorManager{

    private final String LOG_TAG = "TRACETrackerService";

    final Messenger mMessenger = new Messenger(new ClientHandler());


    //Location Tracking
    private FusedLocationModule locationModule;

    private TRACETracker mTracker;

    @Override
    public void onCreate() {
        super.onCreate();
        mTracker = TRACETracker.getTracker(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();
        return mMessenger.getBinder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(LOG_TAG, "onDestroy");


    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(LOG_TAG, "onUnbind");
        return super.onUnbind(intent);
    }


    @Override
    public boolean stopService(Intent name) {
        Log.d(LOG_TAG, "stopService");
        return super.stopService(name);

    }

    @Override
    public void storeLocation(Location location) {
        mTracker.storeLocation(location);
    }


    class ClientHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TRACETrackerOperations.TEST_OP:
                    Toast.makeText(getApplicationContext(), "Hello!", Toast.LENGTH_SHORT).show();
                    break;
                case TRACETrackerOperations.TRACK_LOCATION_ACTION:
                    mTracker.startLocationUpdates();
                    break;
                case TRACETrackerOperations.UNTRACK_LOCATION_ACTION:
                    mTracker.stopLocationUpdates();
                    break;
                case TRACETrackerOperations.TRACK_ACTIVITY_ACTION:
                    Toast.makeText(getApplicationContext(), "Start Tracking activity! (TODO)", Toast.LENGTH_SHORT).show();
                    break;
                case TRACETrackerOperations.UNTRACK_ACTIVITY_ACTION:
                    Toast.makeText(getApplicationContext(), "Stop Tracking activity! (TODO)", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    super.handleMessage(msg);
            }

        }
    }
}
