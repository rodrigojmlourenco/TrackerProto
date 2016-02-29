package org.trace.trackerproto.ui;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.trace.trackerproto.Constants;
import org.trace.trackerproto.R;
import org.trace.trackerproto.tracking.data.SerializableLocation;
import org.trace.trackerproto.tracking.data.Track;
import org.trace.trackerproto.tracking.storage.TrackInternalStorage;
import org.trace.trackerproto.tracking.storage.exceptions.UnableToLoadStoredTrackException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MapActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final String MAPFILE = "lisbon.map";

    private MapView mapView;
    private TileCache tileCache;
    private TileRendererLayer tileRendererLayer;

    private Track mTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidGraphicFactory.createInstance(this.getApplication());

        this.mapView = new MapView(this);
        setContentView(this.mapView);

        this.mapView.setClickable(true);
        this.mapView.getMapScaleBar().setVisible(true);
        this.mapView.setBuiltInZoomControls(true);
        this.mapView.getMapZoomControls().setZoomLevelMin((byte) 10);
        this.mapView.getMapZoomControls().setZoomLevelMax((byte) 20);

        // create a tile cache of suitable size
        this.tileCache = AndroidUtil.createTileCache(this, "mapcache",
                mapView.getModel().displayModel.getTileSize(), 1f,
                this.mapView.getModel().frameBufferModel.getOverdrawFactor());

        Intent i = getIntent();
        if(i != null && i.hasExtra(Constants.TRACK_KEY)){
            try {
                mTrack = TrackInternalStorage.loadTracedTrack(this, i.getStringExtra(Constants.TRACK_KEY));
            } catch (UnableToLoadStoredTrackException e) {
                e.printStackTrace();
                mTrack = null;
            }
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        if(mTrack!=null){
            SerializableLocation start = mTrack.getStartPosition();
            this.mapView.getModel().mapViewPosition.setCenter(new LatLong(start.getLatitude(), start.getLongitude()));
            this.mapView.getModel().mapViewPosition.setZoomLevel((byte) 17);
        }else {
            this.mapView.getModel().mapViewPosition.setCenter(new LatLong(38.7368192, -9.138705));
            this.mapView.getModel().mapViewPosition.setZoomLevel((byte) 12);
        }

        // tile renderer layer using internal render theme
        File map = getMapFile();
        if(map == null) return;
        MapDataStore mapDataStore = new MapFile(getMapFile());
        this.tileRendererLayer = new TileRendererLayer(tileCache, mapDataStore,
                this.mapView.getModel().mapViewPosition, false, true, AndroidGraphicFactory.INSTANCE);
        tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);

        // only once a layer is associated with a mapView the rendering starts
        this.mapView.getLayerManager().getLayers().add(tileRendererLayer);

        if(mTrack!=null) { //Draw track
            Paint paint = AndroidGraphicFactory.INSTANCE.createPaint();
            paint.setColor(Color.RED);
            paint.setStrokeWidth(15f);
            paint.setStyle(Style.STROKE);

            // instantiating the polyline object
            Polyline polyline = new Polyline(paint, AndroidGraphicFactory.INSTANCE);

            // set lat lng for the polyline
            List<LatLong> coordinateList = polyline.getLatLongs();
            for(SerializableLocation loc : mTrack.getTracedTrack())
                coordinateList.add(new LatLong(loc.getLatitude(), loc.getLongitude()));


            // adding the layer to the mapview
            mapView.getLayerManager().getLayers().add(polyline);

            //TODO: inserir markers
        }

    }




    @AfterPermissionGranted(Constants.Permissions.EXTERNAL_STORAGE)
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private File getMapFile() {

        String[] perms = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if(!EasyPermissions.hasPermissions(this, perms)){
            EasyPermissions.requestPermissions(this, "Some some", Constants.Permissions.EXTERNAL_STORAGE, perms);
            finish();
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


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }
}
