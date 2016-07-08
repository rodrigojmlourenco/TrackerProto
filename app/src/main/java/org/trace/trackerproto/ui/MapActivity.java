package org.trace.trackerproto.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.GoogleApiClient;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.PathOverlay;
import org.trace.storeclient.TraceAuthenticationManager;
import org.trace.storeclient.cache.RouteCache;
import org.trace.storeclient.cache.exceptions.RouteIsIncompleteException;
import org.trace.storeclient.cache.exceptions.RouteNotFoundException;
import org.trace.storeclient.cache.exceptions.RouteNotParsedException;
import org.trace.storeclient.data.RouteWaypoint;
import org.trace.storeclient.exceptions.UserIsNotLoggedException;
import org.trace.trackerproto.ProtoConstants;
import org.trace.trackerproto.R;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class MapActivity extends Activity {

    private MapView mMap;

    //
    private List<RouteWaypoint> mTrack = null;
    private RouteCache mRouteCache;
    private TraceAuthenticationManager mAuthManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mRouteCache = RouteCache.getCacheInstance(getApplicationContext());

        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(this)
                //.enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .addApi(Auth.CREDENTIALS_API)
                .build();

        mAuthManager = TraceAuthenticationManager.getAuthenticationManager(this, mGoogleApiClient);

        //Step 1 - Load the track specified in the intent
        Intent i = getIntent();
        if(i != null && i.hasExtra(ProtoConstants.extras.TRACK_KEY_EXTRA)){
            final String session = i.getStringExtra(ProtoConstants.extras.TRACK_KEY_EXTRA);
            try {

                mTrack = mRouteCache.loadRouteTrace(mAuthManager.getAuthenticationToken(), session);

            } catch (RouteNotFoundException e) {
                e.printStackTrace();
            } catch (RouteNotParsedException e) {
                e.printStackTrace();
            } catch (UserIsNotLoggedException e) {
                e.printStackTrace();
            }catch (RouteIsIncompleteException e1){
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mTrack = mRouteCache.loadRouteTrace(mAuthManager.getAuthenticationToken(), session);
                            drawRoute();
                        } catch (RouteNotFoundException e) {
                            e.printStackTrace();
                        } catch (RouteIsIncompleteException e) {
                            e.printStackTrace();
                        } catch (RouteNotParsedException e) {
                            e.printStackTrace();
                        } catch (UserIsNotLoggedException e) {
                            e.printStackTrace();
                        }
                    }
                }, TimeUnit.SECONDS.toMillis(3));
            }
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

        GeoPoint start  = new GeoPoint(mTrack.get(0).getAsLocation()),
                end    = new GeoPoint(mTrack.get(mTrack.size()-1).getAsLocation());

        for(RouteWaypoint location : mTrack)
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




    /*
    private void drawRouteWithRoadManager(){
        RoadManager roadManager = new OSRMRoadManager(this);

        ArrayList<GeoPoint> waypoints = new ArrayList<>();

        GeoPoint start  = new GeoPoint(mTrack.getFromLocation()),
                end    = new GeoPoint(mTrack.getToLocation());

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
    */
}
