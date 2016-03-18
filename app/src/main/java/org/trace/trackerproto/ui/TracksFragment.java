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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.trace.tracking.Constants;
import org.trace.trackerproto.R;
import org.trace.tracking.store.TRACEStoreApiClient;
import org.trace.tracking.storage.GPXTrackWriter;
import org.trace.tracking.storage.PersistentTrackStorage;
import org.trace.tracking.storage.data.SimplifiedTrack;
import org.trace.tracking.storage.data.Track;

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

    //Storage
    private PersistentTrackStorage mTrackStorage;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_tracks, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTrackStorage = new PersistentTrackStorage(getActivity());

        List<SimplifiedTrack> simplifiedTracks = mTrackStorage.getTracksSessions();
        String[] tracks = new String[simplifiedTracks.size()];

        for(int i=0; i < simplifiedTracks.size(); i++) //Remove the file prefix
            tracks[i] = simplifiedTracks.get(i).getSession();

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
            EasyPermissions.requestPermissions(this, getString(R.string.export_rationale), Constants.permissions.EXTERNAL_STORAGE, perms);
            feedback = getString(R.string.try_again);
        }else {

            Track track;

            track = mTrackStorage.getTrack(sessionId);
            feedback = GPXTrackWriter.exportAsGPX(getActivity(), track);
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
                Track t = mTrackStorage.getTrack(values[i]);
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
            sessionView.setText(t.getSessionId());
            timeView.setText(df.format(t.getElapsedTime())+getString(R.string.millis));
            distanceView.setText(df.format(t.getTravelledDistance()) + getString(R.string.meters));

            rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (EasyPermissions.hasPermissions(getActivity(), perms)) {
                        Toast.makeText(context, tracks.get(values.get(position)).getSessionId(), Toast.LENGTH_LONG).show();

                        Intent maps = new Intent(context, MapActivity.class);
                        maps.putExtra(Constants.TRACK_KEY, values.get(position));
                        context.startActivity(maps);

                    } else {
                        EasyPermissions.requestPermissions(
                                getActivity(),
                                getString(R.string.export_rationale),
                                Constants.permissions.EXTERNAL_STORAGE, perms);
                    }

                }
            });

            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {


                    String handle = values.get(position);
                    String sessionId = tracks.get(values.get(position)).getSessionId();

                    mTrackStorage.deleteTrackById(sessionId);

                    ((TrackCountListener)getActivity()).updateTrackCount();

                    remove(handle);
                }
            });

            uploadBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if(isNetworkConnected()) {
                        Track track = mTrackStorage.getTrack(values.get(position));
                        TRACEStoreApiClient.uploadWholeTrack(context, track);
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
