package org.trace.trackerproto.tracking;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.LocationListener;

import org.trace.trackerproto.Constants;

public class TRACETracker extends Service implements CollectorManager, LocationListener{

    private final String LOG_TAG = "TRACETracker";

    private Tracker mTracker;
    final Messenger mMessenger = new Messenger(new ClientHandler());

    @Override
    public void onCreate() {
        super.onCreate();
        mTracker = Tracker.getTracker(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        Log.d(LOG_TAG, "onBind");

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mTracker, new IntentFilter(Constants.COLLECT_ACTION));

        registerReceiver(mTracker, new IntentFilter(Constants.COLLECT_ACTION));

        return mMessenger.getBinder();
    }




    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(LOG_TAG, "onUnbind");

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mTracker);

        unregisterReceiver(mTracker);

        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(LOG_TAG, "onDestroy");
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



    private void startTracking(){
        mTracker.updateSettings();
        mTracker.startLocationUpdates();
        mTracker.startActivityUpdates();
    }

    private void stopTracking(){
        mTracker.stopLocationUpdates();
        mTracker.stopActivityUpdates();
    }

    private void broadcastCurrentLocation(){

        Location location = mTracker.getCurrentLocation();

        Log.d("SERVICE", "Broadcasting action :" + Constants.BROADCAST_LOCATION_ACTION + " with location " + String.valueOf(location));

        Intent locationBroadcast = new Intent();
        locationBroadcast.setAction(Constants.BROADCAST_LOCATION_ACTION);
        locationBroadcast.putExtra(Constants.BROADCAST_LOCATION_EXTRA, location);
        LocalBroadcastManager.getInstance(this).sendBroadcast(locationBroadcast);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("LOC", String.valueOf(location));
    }

    class ClientHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case TRACETrackerOperations.TRACK_ACTION:
                case TRACETrackerOperations.TRACK_LOCATION_ACTION:
                    startTracking();
                    break;
                case TRACETrackerOperations.UNTRACK_ACTION:
                case TRACETrackerOperations.UNTRACK_LOCATION_ACTION:
                    stopTracking();
                    break;
                case TRACETrackerOperations.TRACK_ACTIVITY_ACTION:
                    Toast.makeText(getApplicationContext(), "Start Tracking activity! (TODO)", Toast.LENGTH_SHORT).show();
                    break;
                case TRACETrackerOperations.UNTRACK_ACTIVITY_ACTION:
                    Toast.makeText(getApplicationContext(), "Stop Tracking activity! (TODO)", Toast.LENGTH_SHORT).show();
                    break;
                case TRACETrackerOperations.LAST_LOCATION_ACTION:
                    broadcastCurrentLocation();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    public interface TRACETrackerOperations {

        int TRACK_LOCATION_ACTION   = 2;
        int UNTRACK_LOCATION_ACTION = 3;

        int TRACK_ACTIVITY_ACTION   = 5;
        int UNTRACK_ACTIVITY_ACTION = 6;

        int TRACK_ACTION            = 7;
        int UNTRACK_ACTION          = 8;

        int LAST_LOCATION_ACTION    = 9;
    }

    /**
     *
     */
    public static class TRACETrackerClient {

        private final Messenger mService;

        public TRACETrackerClient(Messenger messenger){
            this.mService = messenger;
        }

        private void sendRequest(Message msg){
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        public void startTracking(){
            Message msg = Message.obtain(null, TRACETrackerOperations.TRACK_ACTION);
            sendRequest(msg);
        }

        public void stopTracking(){
            Message msg = Message.obtain(null, TRACETrackerOperations.UNTRACK_ACTION);
            sendRequest(msg);
        }

        public void getLastLocation(){
            Message msg = Message.obtain(null, TRACETrackerOperations.LAST_LOCATION_ACTION);
            sendRequest(msg);
        }
    }
}
