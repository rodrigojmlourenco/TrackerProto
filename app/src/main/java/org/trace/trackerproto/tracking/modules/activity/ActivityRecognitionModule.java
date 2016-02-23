package org.trace.trackerproto.tracking.modules.activity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;

import org.trace.trackerproto.*;
import org.trace.trackerproto.tracking.exceptions.GoogleApiClientDisconnectedException;
import org.trace.trackerproto.tracking.modules.ModuleInterface;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Rodrigo Lourenço on 17/02/2016.
 */
public class ActivityRecognitionModule implements ModuleInterface{

    protected final static String LOG_TAG = "ActivityRecogModule";

    private Context mContext;
    private GoogleApiClient mGoogleApiClient;
    private BroadcastReceiver mActivityReceiver;
    private PendingIntent mActivityRecogIntent = null;

    private boolean isTracking = false;

    private Queue<SimpleDetectedActivity> activities;

    public ActivityRecognitionModule(Context ctx, GoogleApiClient googleApiClient){

        if(!googleApiClient.isConnected())
            throw new GoogleApiClientDisconnectedException();

        this.mContext = ctx;
        this.mGoogleApiClient = googleApiClient;

        this.activities = new LinkedList<>();

        this.mActivityReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Add current time
                Calendar rightNow = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("h:mm:ss a");
                String strDate = sdf.format(rightNow.getTime());;

                String activity = intent.getStringExtra("activity");
                int confidence  = intent.getExtras().getInt("confidence");

                String v =  strDate + " " +
                        activity + " " +
                        "Confidence : " + confidence + "\n";

                Log.i(LOG_TAG, v);

                activities.add(new SimpleDetectedActivity(activity, confidence));
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.BROADCAST_ACTION);
        mContext.registerReceiver(mActivityReceiver, filter);
    }

    public void startTracking(long interval){

        Intent i = new Intent(mContext, ActivityRecognitionHandler.class);
        mActivityRecogIntent = PendingIntent.getService(mContext, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        ActivityRecognition.ActivityRecognitionApi
                .requestActivityUpdates(mGoogleApiClient, interval, mActivityRecogIntent);

        isTracking = true;
    }

    public void stopTracking(){

        if(!isTracking) return;

        ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, mActivityRecogIntent);

        isTracking = false;

    }

    @Override
    public void dump() {
        String info = new String();

        for(SimpleDetectedActivity activity : activities){
            info += activity.toString()+"\n";
        }

        Log.i(LOG_TAG, info);
    }

    @Override
    public Location getLastLocation() {
        return null;
    }
}
