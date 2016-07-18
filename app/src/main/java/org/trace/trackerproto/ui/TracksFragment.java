package org.trace.trackerproto.ui;

import android.Manifest;
import android.app.Fragment;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.trace.storeclient.cache.RouteCache;
import org.trace.storeclient.cache.exceptions.UnableToCreateRouteCopyException;
import org.trace.storeclient.data.Route;
import org.trace.storeclient.data.RouteWaypoint;
import org.trace.tracker.Tracker;
import org.trace.tracker.TrackerActivity;
import org.trace.tracker.TrackerService;
import org.trace.tracker.permissions.PermissionsConstants;
import org.trace.tracker.storage.data.TraceLocation;
import org.trace.tracker.storage.data.Track;
import org.trace.tracker.storage.data.TrackSummary;
import org.trace.trackerproto.ProtoConstants;
import org.trace.trackerproto.R;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

//TODO: migrar na seriablizable storage para a persistant storage
public class TracksFragment extends Fragment implements EasyPermissions.PermissionCallbacks{

    TrackItemAdapter mAdapter;
    private RouteCache mRouteCache;

    //Permissions
    private String[] perms = {  Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tracks, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        /* Old Version Using PersistentTrackStorage *
        mTrackStorage = new PersistentTrackStorage(getActivity());

        List<TrackSummary> simplifiedTracks = mTrackStorage.getTracksSessions();
        String[] tracks = new String[simplifiedTracks.size()];
        *
        //new Version

        List<TrackSummary> simplifiedTracks = mTracker.getAllTracedTracks();
        String[] tracks = new String[simplifiedTracks.size()];

        for(int i=0; i < simplifiedTracks.size(); i++) //Remove the file prefix
            tracks[i] = simplifiedTracks.get(i).getTrackId();


        /*
        //Version 2.0 - RouteCache
        mRouteCache = RouteCache.getCacheInstance(getActivity().getApplicationContext());
        List<RouteSummary> routeSummaries = mRouteCache.loadRoutes(null);
        String[] tracks = new String[routeSummaries.size()];

        for(int i=0; i < routeSummaries.size(); i++) //Remove the file prefix
            tracks[i] = routeSummaries.get(i).getSession();

        mAdapter = new TrackItemAdapter(getActivity(), tracks);
        ListView list = (ListView) getView().findViewById(R.id.trackListView);
        list.setAdapter(mAdapter);
        */

        mConnectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }


    private void exportTrack(String sessionId){

        String feedback;

        if(!EasyPermissions.hasPermissions(getActivity(), perms)) {
            EasyPermissions.requestPermissions(this, getString(R.string.export_rationale), PermissionsConstants.EXTERNAL_STORAGE, perms);
            feedback = getString(R.string.try_again);
        }else {
            //Track track;
            //track = TRACETrackerService.Client.getStoredTrack(getActivity(), sessionId);
            //feedback = TRACETrackerService.Client.exportStoredTrackToExternalMemory(getActivity(), track);
            feedback = "DEPRECATED : exportStoredTrackToExternalMemory@TRACETrackerService.Client";
        }

        Toast.makeText(getActivity(), feedback, Toast.LENGTH_SHORT).show();
    }

    private class TrackItemAdapter extends ArrayAdapter<String> {

        private Context context;
        private ArrayList<String> values;
        //private HashMap<String, RouteSummary> tracks;
        private HashMap<String, TrackSummary> tracks;

        public TrackItemAdapter(Context context, String[] values) {
            super(context, R.layout.track_item, values);

            tracks = new HashMap<>();

            this.context = context;
            this.values = new ArrayList<>(Arrays.asList(values));

            int i;
            for(i=0; i < values.length; i++){
                //Track t = mTrackStorage.getTrack_DEPRECATED(values[i]);
                //Track t = TRACETrackerService.Client.getStoredTrack(getActivity(), values[i]);
                /*
                try {
                    RouteSummary summary = mRouteCache.loadRoute(null, values[i]);
                    tracks.put(values[i], summary);
                } catch (RouteNotFoundException e) {
                    e.printStackTrace();
                }
                */
                Track t = mTracker.getTracedTrack(values[i]);
                tracks.put(values[i], t);
            }

            setNotifyOnChange(true);
        }

        @Override
        public int getCount() {
            if(values != null)
                return values.size();
            else
                return 0;
        }

        @Override
        public void remove(String object) {
            if(values != null && !values.isEmpty()){
                values.remove(object);
                notifyDataSetChanged();
            }
        }


        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater =
                    (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View rowView = inflater.inflate(R.layout.track_item, parent, false);
            TextView sessionView, timeView, distanceView;
            ImageButton deleteBtn, uploadBtn, exportBtn;


            sessionView = (TextView) rowView.findViewById(R.id.sessionIdView);
            timeView    = (TextView) rowView.findViewById(R.id.elapsedTimeView);
            distanceView= (TextView) rowView.findViewById(R.id.travelledDistanceView);

            deleteBtn = (ImageButton) rowView.findViewById(R.id.trackDeleteBtn);
            uploadBtn = (ImageButton) rowView.findViewById(R.id.trackUploadBtn);
            exportBtn = (ImageButton) rowView.findViewById(R.id.trackExportBtn);

            DecimalFormat df = new DecimalFormat("#.0");
            //final RouteSummary t = tracks.get(values.get(position));
            final TrackSummary t = tracks.get(values.get(position));
            try {
                //sessionView.setText(t.getSession());
                sessionView.setText(t.getTrackId());
            }catch (NullPointerException e){
                sessionView.setText("Unknown session");
            }

            try{
                timeView.setText(df.format(t.getElapsedTime())+getString(R.string.seconds));
            }catch (NullPointerException e){
                timeView.setText("??"+getString(R.string.millis));
            }

            try{
                distanceView.setText(df.format(t.getElapsedDistance()) + getString(R.string.meters));
            }catch (NullPointerException e){
                distanceView.setText("??" + getString(R.string.meters));
            }


            rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (EasyPermissions.hasPermissions(getActivity(), perms)) {
                        //Toast.makeText(context, tracks.get(values.get(position)).getSession(), Toast.LENGTH_LONG).show();
                        Toast.makeText(context, tracks.get(values.get(position)).getTrackId(), Toast.LENGTH_LONG).show();

                        Intent maps = new Intent(context, MapActivity.class);
                        maps.putExtra(ProtoConstants.extras.TRACK_KEY_EXTRA, values.get(position));
                        context.startActivity(maps);

                    } else {
                        EasyPermissions.requestPermissions(
                                getActivity(),
                                getString(R.string.export_rationale),
                                PermissionsConstants.EXTERNAL_STORAGE, perms);
                    }

                }
            });

            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    new android.app.AlertDialog.Builder(getActivity())
                            .setTitle("Delete track")
                            .setMessage("Are you sure you want to delete this track?")
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    /*
                                    String handle = values.get(position);
                                    String sessionId = tracks.get(values.get(position)).getTrackId();

                                    TRACETrackerService.Client.deleteStoredTrack(getActivity(), sessionId);


                                    ((TrackCountListener) getActivity()).updateTrackCount();

                                    remove(handle);
                                    */
                                    Toast.makeText(getActivity(), "DEPRECATED", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton(R.string.no, null)
                            .show();
                }
            });

            uploadBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    // if(isNetworkConnected()) {

                    Tracker rr = ((MainActivity)getActivity()).getRouteRecorder();
                    String index = values.get(position);
                    Track t = rr.getTracedTrack(index);
                    t.updateSpeeds();

                    Log.d("TEST", t.toString());

                    Route route = new Route();
                    route.setSession(t.getTrackId());
                    route.setStartedAt(t.getStart());
                    route.setEndedAt(t.getStop());
                    route.setElapsedDistance(t.getElapsedDistance());
                    route.setPoints(t.getTracedTrack().size());
                    route.setModality(t.getModality());
                    route.setAvgSpeed((float) t.getAverageSpeed());
                    route.setTopSpeed((float) t.getTopSpeed());

                    List<RouteWaypoint> points = new ArrayList<RouteWaypoint>();
                    for(TraceLocation location : t.getTracedTrack()){
                        points.add(new RouteWaypoint(t.getTrackId(), location.getSerializableLocationAsJson()));
                    }
                    route.setTrace(points);


                    RouteCache cache = RouteCache.getCacheInstance(getActivity().getApplicationContext());

                    try {
                        cache.saveRoute(((MainActivity) getActivity()).getAuthenticationToken(), route);
                        ((TrackerActivity)getActivity()).getTracker().deleteTracedTrack(t.getTrackId());
                        remove(index);

                    } catch (UnableToCreateRouteCopyException e) {
                        e.printStackTrace();
                    }
                }
            });

            exportBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    exportTrack(values.get(position));
                }
            });

            return rowView;
        }
    }

    /* Devices Management
    /* Devices Management
    /* Devices Management
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    private ConnectivityManager mConnectivityManager;

    private boolean isNetworkConnected(){
        return mConnectivityManager.getActiveNetworkInfo() != null;
    }

    private void buildAlertMessageNoConnectivity() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getString(R.string.network_enable_rationale))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(Settings.ACTION_SETTINGS));
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

    private Tracker mTracker;
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TrackerService.CustomBinder binder = (TrackerService.CustomBinder) service;
            mTracker = binder.getService();

            List<TrackSummary> simplifiedTracks = mTracker.getAllTracedTracks();
            String[] tracks = new String[simplifiedTracks.size()];

            for(int i=0; i < simplifiedTracks.size(); i++) //Remove the file prefix
                tracks[i] = simplifiedTracks.get(i).getTrackId();

            mAdapter = new TrackItemAdapter(getActivity(), tracks);
            ListView list = (ListView) getView().findViewById(R.id.trackListView);
            list.setAdapter(mAdapter);
            list.invalidate();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mTracker = null;
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        Intent trackerService = new Intent(getActivity(), TrackerService.class);
        trackerService.setFlags(Service.START_STICKY);
        getActivity().bindService(trackerService, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        getActivity().unbindService(mConnection);
        super.onStop();
    }
}
