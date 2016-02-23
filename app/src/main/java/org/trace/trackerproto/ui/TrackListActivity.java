package org.trace.trackerproto.ui;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.trace.trackerproto.Constants;
import org.trace.trackerproto.R;
import org.trace.trackerproto.tracking.data.Track;
import org.trace.trackerproto.tracking.storage.TrackInternalStorage;
import org.trace.trackerproto.tracking.storage.exceptions.UnableToLoadStoredTrackException;

import java.util.HashMap;
import java.util.List;

public class TrackListActivity extends ListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<String> trackFiles = TrackInternalStorage.listStoredTracks(this);
        String[] tracks = new String[trackFiles.size()];

        for(int i=0; i < trackFiles.size(); i++) //Remove the file prefix
            tracks[i] = trackFiles.get(i).replace("track_", "");




        TrackItemAdapter adapter = new TrackItemAdapter(this, tracks);
        setListAdapter(adapter);

    }

    private class TrackItemAdapter extends ArrayAdapter<String>{

        private Context context;
        private String[] values;
        private HashMap<Integer, Track> tracks;

        public TrackItemAdapter(Context context, String[] values) {
            super(context, R.layout.track_item, values);

            tracks = new HashMap<>();

            this.context = context;
            this.values = values;

            int i;
            for(i=0; i < values.length; i++){
                try {
                    tracks.put(i, TrackInternalStorage.loadTracedTrack(context, values[i]));
                } catch (UnableToLoadStoredTrackException e) {
                    e.printStackTrace();
                }
            }

        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater =
                    (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View rowView = inflater.inflate(R.layout.track_item, parent, false);
            TextView sessionView, timeView, distanceView;

            sessionView = (TextView) rowView.findViewById(R.id.sessionIdView);
            timeView    = (TextView) rowView.findViewById(R.id.elapsedTimeView);
            distanceView= (TextView) rowView.findViewById(R.id.travelledDistanceView);


            Track t = tracks.get(position);
            sessionView.setText(t.getSessionId());
            timeView.setText(String.valueOf(t.getElapsedTime())+"ms");
            distanceView.setText(String.valueOf(t.getTravelledDistance())+"m");



            rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(context, tracks.get(position).getSessionId(), Toast.LENGTH_LONG).show();

                    Intent maps = new Intent(context, MapActivity.class);
                    maps.putExtra(Constants.TRACK_KEY, tracks.get(position).getSessionId());
                    context.startActivity(maps);

                }
            });

            return rowView;
        }


    }
}
