package org.trace.trackerproto.ui;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.trace.trackerproto.Constants;
import org.trace.trackerproto.R;
import org.trace.trackerproto.store.TRACEStoreApiClient;
import org.trace.trackerproto.store.TRACEStoreReceiver;
import org.trace.trackerproto.tracking.TRACETracker;
import org.trace.trackerproto.tracking.Tracker;

public class MainActivity extends AppCompatActivity
        implements  PermissionChecker {

    private Location mLastLocation = null;

    private final String[] mSupportedBroadcastActions =
            new String[] {  Constants.BROADCAST_ACTION,
                            Constants.FIRST_TIME_BROADCAST};

    //UI Elements
    private Button startBtn, stopBtn, mapsBtn, tracksBtn, settingsBtn;
    private TextView locationTxtView;


    //State
    private boolean isTracking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Registering the BroadcastReceiver for the TRACEStore intent service
        registerBroadcastReceiver(mSupportedBroadcastActions);

        updateValuesFromBundle(savedInstanceState);

        //Setup UI and respective OnClickListeners
        this.startBtn   = (Button) findViewById(R.id.startBtn);
        this.stopBtn    = (Button) findViewById(R.id.stopBtn);
        this.settingsBtn= (Button) findViewById(R.id.settingsBtn);
        this.locationTxtView = (TextView) findViewById(R.id.locationIn);


        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                checkForLocationPermissions();

                if(client!=null && hasLocationPermissions()) {

                    TRACEStoreApiClient.requestInitiateSession(MainActivity.this);

                    client.startTracking();
                    isTracking = true;
                    toggleButtons(isBound, true);
                }
            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (client != null) {
                    client.stopTracking();
                    isTracking = false;
                    toggleButtons(isBound, false);

                    Toast.makeText(MainActivity.this, TRACEStoreApiClient.getSessionId(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        settingsBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Intent settingsActivity = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settingsActivity);
            }
        });

        //TEST - Maps Forge
        mapsBtn = (Button) findViewById(R.id.mapBtn);
        mapsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, MapActivity.class);
                startActivity(i);
            }
        });


        //Test - Tracks List
        tracksBtn = (Button) findViewById(R.id.tracksBtn);
        tracksBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, TrackListActivity.class);
                startActivity(i);
            }
        });
    }


    private void toggleButtons(boolean bound, boolean isTracking){
        if(!bound){
            startBtn.setEnabled(false);
            stopBtn.setEnabled(false);
        }else {
            startBtn.setEnabled(!isTracking);
            stopBtn.setEnabled(isTracking);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Force Login if it's the first time
        if(TRACEStoreApiClient.isFirstTime(this)){
            Intent forceLogin = new Intent(this, LoginActivity.class);
            startActivity(forceLogin);
        }else{
            TRACEStoreApiClient.requestLogin(this, "", "");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent trackerService = new Intent(getApplicationContext(), TRACETracker.class);
        trackerService.setFlags(Service. START_STICKY);
        bindService(trackerService, mConnection, Context.BIND_AUTO_CREATE);
        isBound = true;
        toggleButtons(true, isTracking);

    }

    @Override
    protected void onDestroy() {
        if(isBound){
            isBound = false;
            unbindService(mConnection);
        }

        if(isFinishing()){
            TRACEStoreApiClient.requestLogout(this);

            if(isTracking) {
                client.stopTracking();
                isTracking = false;
            }
        }

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(Constants.TRACKER_SERVICE_BOUND_KEY, isBound);
        outState.putBoolean(Constants.TRACKER_SERVICE_TRACKING_KEY, isTracking);
        outState.putParcelable(Constants.LAST_LOCATION_KEY, mLastLocation);
        super.onSaveInstanceState(outState);
    }

    private void updateValuesFromBundle(Bundle inState){
        if(inState != null){

            if(inState.containsKey(Constants.LAST_LOCATION_KEY) && inState.get(Constants.LAST_LOCATION_KEY) != null)
                locationTxtView.setText(inState.get(Constants.LAST_LOCATION_KEY).toString());

            
            if(inState.containsKey(Constants.REQUEST_LOCATION_UPDATES)){
                isTracking = inState.getBoolean(Constants.REQUEST_LOCATION_UPDATES);
            }

            if(inState.containsKey(Constants.TRACKER_SERVICE_BOUND_KEY))
                isBound = inState.getBoolean(Constants.TRACKER_SERVICE_BOUND_KEY);

            if(inState.containsKey(Constants.TRACKER_SERVICE_TRACKING_KEY))
                isTracking = inState.getBoolean(Constants.TRACKER_SERVICE_TRACKING_KEY);
        }
    }

    private void registerBroadcastReceiver(String[] actions){

        if(actions.length <=0) return;


        IntentFilter filter = new IntentFilter(actions[0]);

        int i;
        for(i=1; i < actions.length; i++){
            filter.addAction(actions[i]);
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(new TRACEStoreReceiver(), filter);
    }


    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void checkForLocationPermissions() {
        int coarsePermission, finePermission;

        finePermission  = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        coarsePermission= ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);

        if(finePermission != PackageManager.PERMISSION_GRANTED || coarsePermission != PackageManager.PERMISSION_GRANTED){

            if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
                    || shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {

                showMessageOKCancel("TRACE needs access to your location to track your movements",
                        new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.e("PERMISSIONS", String.valueOf(which));
                            }
                        });
                return;
            }

            //TODO: should provide some rationale
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    Constants.TRACE_LOC_PERMISSIONS);

        }
    }

    @Override
    public boolean hasLocationPermissions() {
        int finePermission  = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int coarsePermission= ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);

        return finePermission == PackageManager.PERMISSION_GRANTED
                && coarsePermission == PackageManager.PERMISSION_GRANTED;
    }


    //TESTING - TRACETracker
    Messenger mService = null;
    boolean isBound = false;

    private TRACETracker.TRACETrackerClient client = null;



    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService= new Messenger(service);
            client  = new TRACETracker.TRACETrackerClient(mService);
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService= null;
            client  = null;
            isBound = false;
        }
    };
}
