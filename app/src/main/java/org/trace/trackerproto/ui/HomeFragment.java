package org.trace.trackerproto.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.trace.trackerproto.Constants;
import org.trace.trackerproto.R;
import org.trace.trackerproto.store.TRACEStoreApiClient;
import org.trace.trackerproto.store.TRACEStoreReceiver;
import org.trace.trackerproto.tracking.TRACETracker;

import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by Rodrigo Lourenço on 07/03/2016.
 */
public class HomeFragment extends Fragment implements EasyPermissions.PermissionCallbacks, TrackingFragment{


    private final String[] mSupportedBroadcastActions =
            new String[] {  Constants.BROADCAST_ACTION,
                    Constants.FIRST_TIME_BROADCAST};

    //UI Elements
    private Button startBtn, stopBtn;


    //State
    private boolean isTracking = false;

    //Permissions
    private String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateValuesFromBundle(savedInstanceState);

        toggleButtons(isBound, isTracking);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        //Registering the BroadcastReceiver for the TRACEStore intent service
        registerBroadcastReceiver(mSupportedBroadcastActions);



        //Setup UI and respective OnClickListeners
        this.startBtn   = (Button) rootView.findViewById(R.id.startBtn);
        this.stopBtn    = (Button) rootView.findViewById(R.id.stopBtn);



        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TRACEStoreApiClient.requestInitiateSession(getActivity());

                if(EasyPermissions.hasPermissions(getActivity(), perms)) {

                    client.startTracking();
                    isTracking = true;
                    toggleButtons(isBound, true);
                }else {
                    EasyPermissions.requestPermissions(
                            getActivity(),
                            getString(R.string.tracking_rationale),
                            Constants.Permissions.TRACKING, perms);
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

                    Toast.makeText(getActivity(), TRACEStoreApiClient.getSessionId(), Toast.LENGTH_SHORT).show();
                }
            }
        });



        return rootView;
    }



    @Override
    public void onStart() {
        super.onStart();

        Intent trackerService = new Intent(getActivity(), TRACETracker.class);
        trackerService.setFlags(Service. START_STICKY);
        getActivity().bindService(trackerService, mConnection, Context.BIND_AUTO_CREATE);
        isBound = true;
        toggleButtons(true, isTracking);
    }

    @Override
    public void onResume() {
        super.onResume();

        //Force Login if it's the first time
        if(TRACEStoreApiClient.isFirstTime(getActivity())){
            Intent forceLogin = new Intent(getActivity(), LoginActivity.class);
            startActivity(forceLogin);
        }else{
            TRACEStoreApiClient.requestLogin(getActivity(), "", "");
        }
    }

    @Override
    public void onDestroyView() {
        if(isBound){
            isBound = false;
            getActivity().unbindService(mConnection);
        }

        super.onDestroy();
    }

    /* Save State
    /* Save State
    /* Save State
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(Constants.TRACKER_SERVICE_BOUND_KEY, isBound);
        outState.putBoolean(Constants.TRACKER_SERVICE_TRACKING_KEY, isTracking);
        super.onSaveInstanceState(outState);
    }

    private void updateValuesFromBundle(Bundle inState){
        if(inState != null){

            if(inState.containsKey(Constants.REQUEST_LOCATION_UPDATES)){
                isTracking = inState.getBoolean(Constants.REQUEST_LOCATION_UPDATES);
            }

            if(inState.containsKey(Constants.TRACKER_SERVICE_BOUND_KEY))
                isBound = inState.getBoolean(Constants.TRACKER_SERVICE_BOUND_KEY);

            if(inState.containsKey(Constants.TRACKER_SERVICE_TRACKING_KEY))
                isTracking = inState.getBoolean(Constants.TRACKER_SERVICE_TRACKING_KEY);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if(savedInstanceState != null)
            updateValuesFromBundle(savedInstanceState);
    }



    /* UI
    /* UI
    /* UI
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    private void toggleButtons(boolean bound, boolean isTracking){
        if(!bound){
            startBtn.setEnabled(false);
            stopBtn.setEnabled(false);
        }else {
            startBtn.setEnabled(!isTracking);
            stopBtn.setEnabled(isTracking);
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(getActivity())
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    /* TRACE Store
    /* TRACE Store
    /* TRACE Store
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    private void registerBroadcastReceiver(String[] actions){

        if(actions.length <=0) return;


        IntentFilter filter = new IntentFilter(actions[0]);

        int i;
        for(i=1; i < actions.length; i++){
            filter.addAction(actions[i]);
        }

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(new TRACEStoreReceiver(), filter);
    }

    /* TRACE Tracker
    /* TRACE Tracker
    /* TRACE Tracker
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    boolean isBound = false;
    Messenger mService = null;

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


    /* Permissions
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }

    /* Tracking Fragment
    /* Tracking Fragment
    /* Tracking Fragment
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    @Override
    public boolean isTracking() {
        return isTracking;
    }

    @Override
    public void stopTracking() {
        if (client != null) {
            client.stopTracking();
            isTracking = false;
            toggleButtons(isBound, false);

            Toast.makeText(getActivity(), TRACEStoreApiClient.getSessionId(), Toast.LENGTH_SHORT).show();
        }
    }
}
