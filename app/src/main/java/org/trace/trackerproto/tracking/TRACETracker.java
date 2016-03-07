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

import org.trace.trackerproto.Constants;

public class TRACETracker extends Service implements CollectorManager{

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
        Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();

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


    class ClientHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TRACETrackerOperations.TEST_OP:
                    Toast.makeText(getApplicationContext(), "Hello!", Toast.LENGTH_SHORT).show();
                    break;
                case TRACETrackerOperations.TRACK_ACTION:
                case TRACETrackerOperations.TRACK_LOCATION_ACTION:
                    mTracker.updateSettings();
                    mTracker.startLocationUpdates();
                    mTracker.startActivityUpdates();
                    break;
                case TRACETrackerOperations.UNTRACK_ACTION:
                case TRACETrackerOperations.UNTRACK_LOCATION_ACTION:
                    mTracker.stopLocationUpdates();
                    mTracker.stopActivityUpdates();
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

    public interface TRACETrackerOperations {
        int TEST_OP                 = 1;

        int TRACK_LOCATION_ACTION   = 2;
        int UNTRACK_LOCATION_ACTION = 3;

        int TRACK_ACTIVITY_ACTION   = 5;
        int UNTRACK_ACTIVITY_ACTION = 6;

        int TRACK_ACTION            = 7;
        int UNTRACK_ACTION          = 8;
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

        public void testService(){
            Message msg = Message.obtain(null, TRACETrackerOperations.TEST_OP, 0, 0);
            sendRequest(msg);
        }

        public void startTracking(){
            Message msg = Message.obtain(null, TRACETrackerOperations.TRACK_ACTION);
            sendRequest(msg);
        }

        public void stopTracking(){
            Message msg = Message.obtain(null, TRACETrackerOperations.UNTRACK_ACTION);
            sendRequest(msg);
        }

        @Deprecated
        public void startTrackingLocation(int precision, int energy){
            Message msg = Message.obtain(null, TRACETrackerOperations.TRACK_LOCATION_ACTION, precision, energy);
            sendRequest(msg);
        }

        @Deprecated
        public void stopTrackingLocation(){
            Message msg = Message.obtain(null, TRACETrackerOperations.UNTRACK_LOCATION_ACTION, 0, 0);
            sendRequest(msg);
        }

        @Deprecated
        public void startTrackingActivity(){
            Message msg = Message.obtain(null, TRACETrackerOperations.TRACK_ACTIVITY_ACTION, 0, 0);
            sendRequest(msg);
        }

        @Deprecated
        public void stopTrackingActivity(){
            Message msg = Message.obtain(null, TRACETrackerOperations.UNTRACK_ACTIVITY_ACTION, 0, 0);
            sendRequest(msg);
        }

        public void storeSession(String session){

        }
    }
}
