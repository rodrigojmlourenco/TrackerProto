package org.trace.trackerproto.ui;

import android.Manifest;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.trace.trackerproto.Constants;
import org.trace.trackerproto.R;
import org.trace.trackerproto.store.TRACEStoreApiClient;
import org.trace.trackerproto.tracking.data.Track;
import org.trace.trackerproto.tracking.storage.TrackInternalStorage;
import org.trace.trackerproto.tracking.storage.exceptions.UnableToLoadStoredTrackException;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public class TrackListActivity extends ListActivity implements EasyPermissions.PermissionCallbacks {

    TrackItemAdapter mAdapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<String> trackFiles = TrackInternalStorage.listStoredTracks(this);
        String[] tracks = new String[trackFiles.size()];

        for(int i=0; i < trackFiles.size(); i++) //Remove the file prefix
            tracks[i] = trackFiles.get(i).replace("track_", "");

        mAdapter = new TrackItemAdapter(this, tracks);
        setListAdapter(mAdapter);

    }

    private String[] fetchTracks(){
        List<String> trackFiles = TrackInternalStorage.listStoredTracks(this);
        String[] tracks = new String[trackFiles.size()];

        for(int i=0; i < trackFiles.size(); i++) //Remove the file prefix
            tracks[i] = trackFiles.get(i).replace("track_", "");

        return tracks;
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
        finish();
    }


    private void exportTrack(String sessionId){

        String feedback;
        String[] perms = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if(!EasyPermissions.hasPermissions(TrackListActivity.this, perms)) {
            EasyPermissions.requestPermissions(this, "Some some", Constants.Permissions.EXTERNAL_STORAGE, perms);
            feedback = "Try again.";
        }else {

            Track track;


            try {
                track = TrackInternalStorage.loadTracedTrack(this, sessionId);
                feedback = TrackInternalStorage.exportAsGPX(this, track);
            } catch (UnableToLoadStoredTrackException e) {
                e.printStackTrace();
                feedback = "Unable to find the track";
            }
        }

        Toast.makeText(this, feedback, Toast.LENGTH_SHORT).show();
    }

    private class TrackItemAdapter extends ArrayAdapter<String>{

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
                try {
                    Track t = TrackInternalStorage.loadTracedTrack(context, values[i]);
                    tracks.put(values[i], t);
                } catch (UnableToLoadStoredTrackException e) {
                    e.printStackTrace();
                }
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
            timeView.setText(df.format(t.getElapsedTime())+"ms");
            distanceView.setText(df.format(t.getTravelledDistance()) + "m");

            rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(context, tracks.get(values.get(position)).getSessionId(), Toast.LENGTH_LONG).show();

                    Intent maps = new Intent(context, MapActivity.class);
                    maps.putExtra(Constants.TRACK_KEY, values.get(position));
                    context.startActivity(maps);

                }
            });

            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String handle = values.get(position);
                    String sessionId = tracks.get(values.get(position)).getSessionId();
                    TrackInternalStorage.removeStoreTrack(context, handle);
                    remove(handle);
                }
            });

            uploadBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String sessionId = tracks.get(values.get(position)).getSessionId();


                    Track track;

                    try {

                        track = TrackInternalStorage.loadTracedTrack(context, values.get(position));
                        TRACEStoreApiClient.uploadWholeTrack(context, track);

                    } catch (UnableToLoadStoredTrackException e) {
                        e.printStackTrace();
                        Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
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
}
