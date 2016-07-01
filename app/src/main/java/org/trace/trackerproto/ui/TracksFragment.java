package org.trace.trackerproto.ui;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
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

import org.trace.storeclient.TRACEStore;
import org.trace.tracker.TRACETrackerService;
import org.trace.tracker.TrackingConstants;
import org.trace.tracker.storage.data.TrackSummary;
import org.trace.tracker.storage.data.Track;
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

        /* Old Version Using PersistentTrackStorage
        mTrackStorage = new PersistentTrackStorage(getActivity());

        List<TrackSummary> simplifiedTracks = mTrackStorage.getTracksSessions();
        String[] tracks = new String[simplifiedTracks.size()];
        */
        //new Version
        List<TrackSummary> simplifiedTracks = TRACETrackerService.Client.getAllStoredTracks(getActivity());
        String[] tracks = new String[simplifiedTracks.size()];

        for(int i=0; i < simplifiedTracks.size(); i++) //Remove the file prefix
            tracks[i] = simplifiedTracks.get(i).getTrackId();

        mAdapter = new TrackItemAdapter(getActivity(), tracks);
        ListView list = (ListView) getView().findViewById(R.id.trackListView);
        list.setAdapter(mAdapter);

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
            EasyPermissions.requestPermissions(this, getString(R.string.export_rationale), TrackingConstants.permissions.EXTERNAL_STORAGE, perms);
            feedback = getString(R.string.try_again);
        }else {

            Track track;

            track = TRACETrackerService.Client.getStoredTrack(getActivity(), sessionId);
            feedback = TRACETrackerService.Client.exportStoredTrackToExternalMemory(getActivity(), track);
        }

        Toast.makeText(getActivity(), feedback, Toast.LENGTH_SHORT).show();
    }

    private class TrackItemAdapter extends ArrayAdapter<String> {

        private Context context;
        private ArrayList<String> values;
        private HashMap<String, Track> tracks;

        public TrackItemAdapter(Context context, String[] values) {
            super(context, R.layout.track_item, values);

            tracks = new HashMap<>();

            this.context = context;
            this.values = new ArrayList<>(Arrays.asList(values));

            int i;
            for(i=0; i < values.length; i++){
                //Track t = mTrackStorage.getTrack(values[i]);
                Track t = TRACETrackerService.Client.getStoredTrack(getActivity(), values[i]);
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
            final Track t = tracks.get(values.get(position));
            try {
                sessionView.setText(t.getTrackId());
            }catch (NullPointerException e){
                sessionView.setText("Unknown session");
            }

            try{
                timeView.setText(df.format(t.getElapsedTime())+getString(R.string.millis));
            }catch (NullPointerException e){
                timeView.setText("??"+getString(R.string.millis));
            }

            try{
                distanceView.setText(df.format(t.getTravelledDistance()) + getString(R.string.meters));
            }catch (NullPointerException e){
                distanceView.setText("??" + getString(R.string.meters));
            }


            rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (EasyPermissions.hasPermissions(getActivity(), perms)) {
                        Toast.makeText(context, tracks.get(values.get(position)).getTrackId(), Toast.LENGTH_LONG).show();


                        Intent maps = new Intent(context, MapActivity.class);
                        maps.putExtra(ProtoConstants.extras.TRACK_KEY_EXTRA, values.get(position));
                        context.startActivity(maps);

                    } else {
                        EasyPermissions.requestPermissions(
                                getActivity(),
                                getString(R.string.export_rationale),
                                TrackingConstants.permissions.EXTERNAL_STORAGE, perms);
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
                                    String handle = values.get(position);
                                    String sessionId = tracks.get(values.get(position)).getTrackId();

                                    TRACETrackerService.Client.deleteStoredTrack(getActivity(), sessionId);

                                    ((TrackCountListener) getActivity()).updateTrackCount();

                                    remove(handle);
                                }
                            })
                            .setNegativeButton(R.string.no, null)
                            .show();
                }
            });

            uploadBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if(isNetworkConnected()) {
                        //Track track = mTrackStorage.getTrack(values.get(position));
                        Track track = TRACETrackerService.Client.getStoredTrack(getActivity(), values.get(position));

                        if(track == null){
                            Log.e("UPLOAD", "The session is not being updated in the activity!"); //TODO
                            return;
                        }

                        TRACEStore.Client.uploadTrack(context, ((MainActivity) getActivity()).getAuthenticationToken(), track.toJson()); //TODO: refactorizar
                    }else
                        buildAlertMessageNoConnectivity();

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
}
