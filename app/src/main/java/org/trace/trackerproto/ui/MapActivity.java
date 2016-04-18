package org.trace.trackerproto.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.PathOverlay;
import org.trace.tracker.storage.PersistentTrackStorage;
import org.trace.tracker.storage.data.TraceLocation;
import org.trace.tracker.storage.data.Track;
import org.trace.trackerproto.ProtoConstants;
import org.trace.trackerproto.R;

import java.util.ArrayList;

public class MapActivity extends Activity {

    private MapView mMap;

    //
    private Track mTrack;
    private PersistentTrackStorage mTrackStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mTrackStorage = new PersistentTrackStorage(this);

        //Step 1 - Load the track specified in the intent
        Intent i = getIntent();
        if(i != null && i.hasExtra(ProtoConstants.extras.TRACK_KEY_EXTRA)){
            String trackId = i.getStringExtra(ProtoConstants.extras.TRACK_KEY_EXTRA);
            mTrack = mTrackStorage.getTrack(trackId);
        }else{
            mTrack = null;
        }


        mMap = (MapView) findViewById(R.id.map);
        mMap.setTileSource(TileSourceFactory.MAPNIK);

        //mMap.setBuiltInZoomControls(true);
        mMap.setMultiTouchControls(true);

        drawRoute();

    }

    @SuppressWarnings("deprecation")
    private void drawRoute(){
        if(mTrack == null) return;

        PathOverlay pathOverlay = new PathOverlay(Color.BLUE, this);

        GeoPoint start  = new GeoPoint(mTrack.getTracedTrack().getFirst()),
                 end    = new GeoPoint(mTrack.getTracedTrack().getLast());

        for(TraceLocation location : mTrack.getTracedTrack())
            pathOverlay.addPoint(new GeoPoint(location.getLatitude(), location.getLongitude()));

        Paint pPaint = pathOverlay.getPaint();
        pPaint.setStrokeWidth(5);
        pathOverlay.setPaint(pPaint);

        IMapController mapController = mMap.getController();
        mapController.setZoom(19);
        mapController.setCenter(start);

        mMap.getOverlays().add(pathOverlay);
        mMap.invalidate();
    }





    private void drawRouteWithRoadManager(){
        RoadManager roadManager = new OSRMRoadManager(this);

        ArrayList<GeoPoint> waypoints = new ArrayList<>();

        GeoPoint start  = new GeoPoint(mTrack.getTracedTrack().getFirst()),
                end    = new GeoPoint(mTrack.getTracedTrack().getLast());

        for(TraceLocation location : mTrack.getTracedTrack())
            waypoints.add(new GeoPoint(location.getLatitude(), location.getLongitude()));

        Road road = roadManager.getRoad(waypoints);
        Polyline roadOverlay = RoadManager.buildRoadOverlay(road, this);

        IMapController mapController = mMap.getController();
        mapController.setZoom(25);
        mapController.setCenter(start);

        mMap.getOverlays().add(roadOverlay);
        mMap.invalidate();
    }

}
