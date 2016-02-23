package org.trace.trackerproto.tracking;

import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

/**
 * Created by Rodrigo Lourenço on 17/02/2016.
 */
public class TRACETrackerClient {

    private final Messenger mService;

    public TRACETrackerClient(Messenger messenger){
        this.mService = messenger;
    }

    public void testService(){

        Message msg = Message.obtain(null, TRACETrackerOperations.TEST_OP, 0, 0);

        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void startTrackingLocation(int precision, int energy){

        Message msg = Message.obtain(null, TRACETrackerOperations.TRACK_LOCATION_ACTION, precision, energy);

        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void stopTrackingLocation(){

        Message msg = Message.obtain(null, TRACETrackerOperations.UNTRACK_LOCATION_ACTION, 0, 0);

        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void startTrackingActivity(){

        Message msg = Message.obtain(null, TRACETrackerOperations.TRACK_ACTIVITY_ACTION, 0, 0);

        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void stopTrackingActivity(){

        Message msg = Message.obtain(null, TRACETrackerOperations.UNTRACK_ACTIVITY_ACTION, 0, 0);

        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void storeSession(String session){

    }
}
