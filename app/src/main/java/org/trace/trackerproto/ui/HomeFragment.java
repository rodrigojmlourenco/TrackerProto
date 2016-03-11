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
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.trace.trackerproto.Constants;
import org.trace.trackerproto.R;
import org.trace.trackerproto.store.TRACEStoreApiClient;
import org.trace.trackerproto.store.TRACEStoreReceiver;
import org.trace.trackerproto.tracking.TRACETracker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by Rodrigo Lourenço on 07/03/2016.
 */
public class HomeFragment extends Fragment
        implements EasyPermissions.PermissionCallbacks, TrackingFragment, MapViewFragment{


    private final String[] mSupportedBroadcastActions =
            new String[] {  Constants.BROADCAST_ACTION,
                    Constants.FIRST_TIME_BROADCAST};

    //UI Elements
    private ImageButton lastLocationBtn, toggleTrackingBtn;
    private FrameLayout mapLayout;

    //State
    boolean isBound = false;
    boolean isTracking = false;




    //Location Listening
    private Location mCurrentLocation;
    private BroadcastReceiver mLocationReceiver;


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        updateValuesFromBundle(savedInstanceState);

        toggleButtons(isBound, isTracking);

        mLocationReceiver = new BroadcastReceiver(){

            @Override
            public void onReceive(Context context, Intent intent) {
                mCurrentLocation = intent.getParcelableExtra(Constants.BROADCAST_LOCATION_EXTRA);

                if(mCurrentLocation!=null)
                    focusOnMap(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                else
                    scheduleFocusTask(1);
            }
        };

        IntentFilter locationFilter = new IntentFilter();
        locationFilter.addAction(Constants.BROADCAST_LOCATION_ACTION);

        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(mLocationReceiver, locationFilter);


        mLocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        setupForgeMaps();


    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        //Registering the BroadcastReceiver for the TRACEStore intent service
        registerBroadcastReceiver(mSupportedBroadcastActions);

        //Setup UI and respective OnClickListeners
        this.mapLayout          = (FrameLayout) rootView.findViewById(R.id.mapContainerLayout);
        this.lastLocationBtn    = (ImageButton) rootView.findViewById(R.id.lastLocationBtn);
        this.toggleTrackingBtn  = (ImageButton) rootView.findViewById(R.id.toggleTrackingBtn);
        this.lastLocationBtn.setBackgroundResource(R.drawable.responsive_location_bg);

        lastLocationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isGPSEnabled()) {

                    if (EasyPermissions.hasPermissions(getActivity(), Constants.permissions.TRACKING_PERMISSIONS))
                        client.getLastLocation();
                    else
                        EasyPermissions.requestPermissions(
                                getActivity(),
                                getString(R.string.tracking_rationale),
                                Constants.permissions.TRACKING,
                                Constants.permissions.TRACKING_PERMISSIONS);
                }else{
                    buildAlertMessageNoGps();
                }
            }

        });

        toggleTrackingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isGPSEnabled()) {

                    if (!isTracking()) {
                        startTrackingOnClick();
                        Toast.makeText(getActivity(), getString(R.string.started_tracking), Toast.LENGTH_SHORT).show();
                    } else {
                        stopTrackingOnClick();
                        Toast.makeText(getActivity(), getString(R.string.stoped_tracking), Toast.LENGTH_SHORT).show();
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

        Intent trackerService = new Intent(getActivity(), TRACETracker.class);
        trackerService.setFlags(Service.START_STICKY);
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
    public void onStop() {
        super.onStop();
        //this.mapView.destroyAll();
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
        outState.putBoolean(Constants.home.SERVICE_BOUND_KEY, isBound);
        outState.putBoolean(Constants.home.TRACKING_STATE_KEY, isTracking);
        outState.putInt(Constants.home.MARKER_INDEX_KEY, -1);
        //outState.putInt(Constants.home.MARKER_INDEX_KEY, mMarkerIndex);
        if(mCurrentLocation != null) outState.putParcelable(Constants.home.LAST_LOCATION_KEY, mCurrentLocation);

        super.onSaveInstanceState(outState);
    }

    private void updateValuesFromBundle(Bundle inState){
        if(inState != null){

            if(inState.containsKey(Constants.home.SERVICE_BOUND_KEY))
                isBound = inState.getBoolean(Constants.home.SERVICE_BOUND_KEY);

            if(inState.containsKey(Constants.home.TRACKING_STATE_KEY))
                isTracking = inState.getBoolean(Constants.home.TRACKING_STATE_KEY);

            if(inState.containsKey(Constants.home.LAST_LOCATION_KEY))
                mCurrentLocation = inState.getParcelable(Constants.home.LAST_LOCATION_KEY);


            if(inState.containsKey(Constants.home.MARKER_INDEX_KEY))
                mMarkerIndex = inState.getInt(Constants.home.MARKER_INDEX_KEY);

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

    /* TRACE Tracker
    /* TRACE Tracker
    /* TRACE Tracker
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */


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


    private void startTrackingOnClick(){
        TRACEStoreApiClient.requestInitiateSession(getActivity());

        if (EasyPermissions.hasPermissions(getActivity(), Constants.permissions.TRACKING_PERMISSIONS)) {

            client.startTracking();
            isTracking = true;
            toggleButtons(isBound, true);
        } else {
            EasyPermissions.requestPermissions(
                    getActivity(),
                    getString(R.string.tracking_rationale),
                    Constants.permissions.TRACKING,
                    Constants.permissions.TRACKING_PERMISSIONS);
        }
    }

    private void stopTrackingOnClick(){
        if (client != null) {
            client.stopTracking();
            isTracking = false;
            toggleButtons(isBound, false);
        }
    }


    /* Tracking Fragment
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    private static final String MAPFILE = "lisbon.map";

    private MapView mapView;
    private TileCache tileCache;
    private TileRendererLayer tileRendererLayer;

    private void setupForgeMaps(){

        AndroidGraphicFactory.createInstance(getActivity().getApplication());

        this.mapView = new MapView(getActivity());
        mapLayout.addView(this.mapView);

        this.mapView.setClickable(true);
        this.mapView.getMapScaleBar().setVisible(true);
        this.mapView.setBuiltInZoomControls(false);
        this.mapView.getMapZoomControls().setZoomLevelMin((byte) 10);
        this.mapView.getMapZoomControls().setZoomLevelMax((byte) 20);

        // create a tile cache of suitable size
        this.tileCache = AndroidUtil.createTileCache(getActivity(), "mapcache",
                mapView.getModel().displayModel.getTileSize(), 1f,
                this.mapView.getModel().frameBufferModel.getOverdrawFactor());


        int zoom;
        LatLong pinpoint;
        Marker marker = null;
        if(mCurrentLocation == null){
            pinpoint = new LatLong(38.7368192, -9.138705);
            zoom = 12;
        }else{
            pinpoint = new LatLong(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
            zoom = 16;
        }


        this.mapView.getModel().mapViewPosition.setCenter(pinpoint);
        this.mapView.getModel().mapViewPosition.setZoomLevel((byte) zoom);

        File map;

        try {
            map = getMapFile();
            if (map == null) return;
        }catch (FileNotFoundException e){ //TODO: create better exception
            e.printStackTrace();
            return;
        }


        MapDataStore mapDataStore = new MapFile(map);
        this.tileRendererLayer = new TileRendererLayer(tileCache, mapDataStore,
                this.mapView.getModel().mapViewPosition, false, true, AndroidGraphicFactory.INSTANCE);
        tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);

        // only once a layer is associated with a mapView the rendering starts
        this.mapView.getLayerManager().getLayers().add(tileRendererLayer);

        if(marker != null)
            this.mapView.getLayerManager().getLayers().add(marker);

        this.mapView.getMapZoomControls().hide();

        if (mCurrentLocation!=null)
            focusOnMap(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());

    }

    private int mMarkerIndex = -1;
    private void focusOnMap(double latitude, double longitude){

        if(getActivity()== null || (getActivity() != null && getActivity().isFinishing())) return;

        LatLong position =  new LatLong(latitude, longitude);

        //Center
        this.mapView.getModel().mapViewPosition.setCenter(position);
        this.mapView.getModel().mapViewPosition.setZoomLevel((byte) 16);

        //Add Marker
        try {
            if (mMarkerIndex != -1)
                this.mapView.getLayerManager().getLayers().remove(mMarkerIndex);
        }catch (ArrayIndexOutOfBoundsException e){
            e.printStackTrace();
        }

        Marker marker = generateMarker(position);

        if(marker != null){
            this.mapView.getLayerManager().getLayers().add(marker);
            mMarkerIndex = this.mapView.getLayerManager().getLayers().indexOf(marker);
        }
    }

    private File getMapFile() throws  FileNotFoundException{

        if(!EasyPermissions.hasPermissions(getActivity(), Constants.permissions.EXTERNAL_STORAGE_PERMISSIONS)){
            EasyPermissions.requestPermissions(
                    getActivity(),
                    "Some some",
                    Constants.permissions.EXTERNAL_STORAGE,
                    Constants.permissions.EXTERNAL_STORAGE_PERMISSIONS);
        }else {

            File file;
            InputStream inputStream = null;
            OutputStream outputStream = null;

            file = new File(Environment.getExternalStorageDirectory(), MAPFILE);

            if (!file.exists()) {


                try {
                    inputStream = getResources().openRawResource(R.raw.portugal);
                    outputStream = new FileOutputStream(file);

                    int read = 0;
                    byte[] bytes = new byte[1024];


                    while ((read = inputStream.read(bytes)) != -1) {
                        outputStream.write(bytes, 0, read);
                    }

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (outputStream != null) {
                        try {
                            // outputStream.flush();
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }

            return new File(Environment.getExternalStorageDirectory(), MAPFILE);
        }

        return null;
    }

    private Marker generateMarker(LatLong position){

        Drawable drawable = ContextCompat.getDrawable(getActivity(), R.drawable.ic_pin3);

        Bitmap icon = drawable==null? null : AndroidGraphicFactory.convertToBitmap(drawable);

        return icon == null ? null : new Marker(position, icon, 1 * icon.getWidth() / 2, -1 * icon.getHeight() / 2);


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

    @Override
    public void cleanMap() {
        if(this.mapView != null)
            this.mapView.destroyAll();
    }


    private class FocusTask implements Runnable {

        @Override
        public void run() {
            client.getLastLocation();
        }
    }

}
