package org.trace.trackerproto.ui;

import android.app.Fragment;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.trace.storeclient.TRACEStore;
import org.trace.storeclient.TRACEStoreReceiver;
import org.trace.tracker.TRACETrackerService;
import org.trace.tracker.Tracker;
import org.trace.tracker.TrackingConstants;
import org.trace.trackerproto.R;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class HomeFragment extends Fragment implements TrackingFragment, MapViewFragment{


    private final String[] mSupportedBroadcastActions =
            new String[] {  TrackingConstants.BROADCAST_ACTION,
                    TrackingConstants.FIRST_TIME_BROADCAST};

    //UI Elements
    private ImageButton lastLocationBtn, toggleTrackingBtn;

    //State
    boolean isBound = false;
    boolean isTracking = false;

    //Location Listening
    private Location mCurrentLocation;
    private BroadcastReceiver mLocationReceiver;


    //TODO: TESTING
    private Tracker mTracker;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        updateValuesFromBundle(savedInstanceState);

        toggleButtons(isBound, isTracking);

        mLocationReceiver = new BroadcastReceiver(){

            @Override
            public void onReceive(Context context, Intent intent) {
                mCurrentLocation = intent.getParcelableExtra(TrackingConstants.tracker.BROADCAST_LOCATION_EXTRA);


                if(mCurrentLocation!=null)
                    focusOnMap(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                else
                    scheduleFocusTask(1);

            }
        };

        IntentFilter locationFilter = new IntentFilter();
        locationFilter.addAction(TrackingConstants.tracker.BROADCAST_LOCATION_ACTION);

        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(mLocationReceiver, locationFilter);


        mLocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        setupOSMDroidMap();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        //Registering the BroadcastReceiver for the TRACEStore intent service
        registerBroadcastReceiver(mSupportedBroadcastActions);

        //Setup UI and respective OnClickListeners
        this.lastLocationBtn    = (ImageButton) rootView.findViewById(R.id.lastLocationBtn);
        this.toggleTrackingBtn  = (ImageButton) rootView.findViewById(R.id.toggleTrackingBtn);
        this.lastLocationBtn.setBackgroundResource(R.drawable.responsive_location_bg);

        lastLocationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isGPSEnabled()) {

                    if (EasyPermissions.hasPermissions(getActivity(), TrackingConstants.permissions.TRACKING_PERMISSIONS))
                        TRACETrackerService.Client.getLastLocation(mService);
                    else
                        EasyPermissions.requestPermissions(
                                getActivity(),
                                getString(R.string.tracking_rationale),
                                TrackingConstants.permissions.FOCUS_ON_MAP,
                                TrackingConstants.permissions.TRACKING_PERMISSIONS);
                }else{
                    buildAlertMessageNoGps();
                }
            }
        });

        toggleTrackingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isGPSEnabled()) {

                    if (EasyPermissions.hasPermissions(getActivity(), TrackingConstants.permissions.TRACKING_PERMISSIONS)) {
                        if (!isTracking()) {
                            startTrackingOnClick();
                            Toast.makeText(getActivity(), getString(R.string.started_tracking), Toast.LENGTH_SHORT).show();
                        } else {
                            stopTrackingOnClick();
                            Toast.makeText(getActivity(), getString(R.string.stoped_tracking), Toast.LENGTH_SHORT).show();
                            ((TrackCountListener)getActivity()).updateTrackCount();
                        }
                    }else{
                        EasyPermissions.requestPermissions(
                                getActivity(),
                                getString(R.string.tracking_rationale),
                                TrackingConstants.permissions.TRACKING,
                                TrackingConstants.permissions.TRACKING_PERMISSIONS);
                    }
                } else
                    buildAlertMessageNoGps();

            }
        });

        return rootView;
    }


    @Override
    public void onStart() {
        super.onStart();

        Intent trackerService = new Intent(getActivity(), TRACETrackerService.class);
        trackerService.setFlags(Service.START_STICKY);
        getActivity().bindService(trackerService, mConnection, Context.BIND_AUTO_CREATE);
        isBound = true;
        toggleButtons(true, isTracking);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if(isBound){
            isBound = false;
            getActivity().unbindService(mConnection);
        }

        super.onDestroyView();
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
        outState.putBoolean(FragmentStateKeys.SERVICE_BOUND_KEY, isBound);
        outState.putBoolean(FragmentStateKeys.TRACKING_STATE_KEY, isTracking);
        outState.putInt(FragmentStateKeys.MARKER_INDEX_KEY, -1);
        //outState.putInt(FragmentStateKeys.MARKER_INDEX_KEY, mMarkerIndex);
        if(mCurrentLocation != null) outState.putParcelable(FragmentStateKeys.LAST_LOCATION_KEY, mCurrentLocation);

        super.onSaveInstanceState(outState);
    }

    private void updateValuesFromBundle(Bundle inState){
        if(inState != null){

            if(inState.containsKey(FragmentStateKeys.SERVICE_BOUND_KEY))
                isBound = inState.getBoolean(FragmentStateKeys.SERVICE_BOUND_KEY);

            if(inState.containsKey(FragmentStateKeys.TRACKING_STATE_KEY))
                isTracking = inState.getBoolean(FragmentStateKeys.TRACKING_STATE_KEY);

            if(inState.containsKey(FragmentStateKeys.LAST_LOCATION_KEY))
                mCurrentLocation = inState.getParcelable(FragmentStateKeys.LAST_LOCATION_KEY);


            /* TODO: handle after osmdroid migration
            if(inState.containsKey(FragmentStateKeys.MARKER_INDEX_KEY))
                mMarkerIndex = inState.getInt(FragmentStateKeys.MARKER_INDEX_KEY);
            */

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
            toggleTrackingBtn.setEnabled(false);
        }else {
            toggleTrackingBtn.setEnabled(true);

            if (isTracking)
                toggleTrackingBtn.setBackgroundResource(R.drawable.responsive_stop_bg);
            else
                toggleTrackingBtn.setBackgroundResource(R.drawable.responsive_start_bg);
        }
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

    /* TRACE TRACETracker
    /* TRACE TRACETracker
    /* TRACE TRACETracker
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */


    Messenger mService = null;

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService= new Messenger(service);
            mTracker = Tracker.getInstance(getActivity(), mService); //TODO: testing
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService= null;
            isBound = false;
        }
    };


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
    public void startTracking() {
        startTrackingOnClick();
    }

    @Override
    public void stopTracking() {

        TRACETrackerService.Client.stopTracking(mService);
        isTracking = false;
        toggleButtons(isBound, false);

        Toast.makeText(getActivity(), TRACEStore.Client.getSessionId(), Toast.LENGTH_SHORT).show();

    }

    @Override
    public void focusOnCurrentLocation() {
        scheduleFocusTask(0);
    }


    private void startTrackingOnClick(){

        if (EasyPermissions.hasPermissions(getActivity(), TrackingConstants.permissions.TRACKING_PERMISSIONS)) {

            //TRACETrackerService.Client.startTracking(mService); TODO: TESTING
            mTracker.startTracking();
            isTracking = true;
            toggleButtons(isBound, true);

        } else {
            EasyPermissions.requestPermissions(
                    getActivity(),
                    getString(R.string.tracking_rationale),
                    TrackingConstants.permissions.TRACKING,
                    TrackingConstants.permissions.TRACKING_PERMISSIONS);
        }
    }

    private void stopTrackingOnClick(){
        SessionHandler handler = (SessionHandler)getActivity();
        handler.teardownTrackingSession();
        //TRACETrackerService.Client.stopTracking(mService); TODO: TESTING
        mTracker.stopTracking();
        isTracking = false;
        toggleButtons(isBound, false);
    }


    /* OSM Droid Maps
    /* OSM Droid Maps
    /* OSM Droid Maps
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    public final static GeoPoint CENTER_MAP = new GeoPoint(38.7368192, -9.138705);

    private MapView osmMapView;


    private void setupOSMDroidMap(){

        if(EasyPermissions.hasPermissions(getActivity(), TrackingConstants.permissions.EXTERNAL_STORAGE_PERMISSIONS)) {
            osmMapView = (MapView) getActivity().findViewById(R.id.mapContainerLayout);
            osmMapView.setTileSource(TileSourceFactory.MAPNIK);
            osmMapView.setMultiTouchControls(true);

            IMapController mapController = osmMapView.getController();
            mapController.setZoom(12);
            GeoPoint startPoint = CENTER_MAP;
            mapController.setCenter(startPoint);
        }else{
            EasyPermissions.requestPermissions(
                    getActivity(),
                    getString(R.string.export_rationale),
                    TrackingConstants.permissions.DRAW_MAPS,
                    TrackingConstants.permissions.EXTERNAL_STORAGE_PERMISSIONS);
        }
    }

    @AfterPermissionGranted(TrackingConstants.permissions.DRAW_MAPS)
    private void redrawOSMDroidMap(){
        Log.e("PERMISSIONS!", "@HomeFragment redraw the map");
    }

    private void focusOnMap(double latitude, double longitude){

        GeoPoint center = new GeoPoint(latitude, longitude);

        IMapController mapController = osmMapView.getController();
        mapController.setZoom(20);
        mapController.setCenter(center);

        osmMapView.invalidate();


        //TESTING
        TRACEStore.Client.fetchShopsWithRewards(getActivity(), latitude, longitude, 5);
    }


    /* Devices Management
    /* Devices Management
    /* Devices Management
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    private LocationManager mLocationManager;

    private boolean isGPSEnabled(){
        return mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void buildAlertMessageNoGps() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getString(R.string.gps_enable_rationale))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });

        final AlertDialog alert = builder.create();
        alert.show();
    }


    /* Devices Management
    /* Devices Management
    /* Devices Management
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    private final ScheduledExecutorService mMarkerWorker =
            Executors.newSingleThreadScheduledExecutor();

    private void scheduleFocusTask(int time){
        Log.d("HOME", "schedueling focus task");
        mMarkerWorker.schedule(new FocusTask(), time, TimeUnit.SECONDS);
    }

    private class FocusTask implements Runnable {

        @Override
        public void run() {
            TRACETrackerService.Client.getLastLocation(mService);
        }
    }


    /* Map View Fragment
    /* Map View Fragment
    /* Map View Fragment
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    @Override
    public void cleanMap() {
        /*if(this.mapView != null)
            this.mapView.destroyAll();*/
    }

    @Override
    public void redrawMap() {
        setupOSMDroidMap();
    }

    /* State Keys
    /* State Keys
    /* State Keys
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    public interface FragmentStateKeys {
        String MARKER_INDEX_KEY = "MARKER_INDEX_KEY";
        String SERVICE_BOUND_KEY ="BOUND_TRACKER_SERVICE";
        String TRACKING_STATE_KEY = "TRACKING_TRACKER_SERVICE";
        String LAST_LOCATION_KEY = "LAST_LOCATION";
    }
}
