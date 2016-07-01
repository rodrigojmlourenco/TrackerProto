package org.trace.tracker.storage;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.trace.tracker.storage.data.TraceLocation;
import org.trace.tracker.storage.data.Track;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GPXTrackWriter {


    public static final String GPX_EXTENSION = ".gpx";

    public void writeGPXToInternalStorage(Context context, String sessionId, String gpx){

        try {
            FileOutputStream fos = context.openFileOutput(sessionId+GPX_EXTENSION, Context.MODE_PRIVATE);
            fos.write(gpx.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getAdditionalInfo(TraceLocation location){
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("speed", location.getSpeed());
        jsonObject.addProperty("accuracy", location.getAccuracy());
        jsonObject.addProperty("provider", location.getProvider());
        jsonObject.addProperty("bearing", location.getBearing());
        jsonObject.addProperty("activity", location.getActivityMode());
        return gson.toJson(jsonObject);
    }

    private static String locationToGpx(TraceLocation location){

        String gpx;
        double latitude, longitude;
        DateFormat df = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
        latitude = location.getLatitude();
        longitude= location.getLongitude();


        gpx ="\t\t\t<trkpt lat=\""+latitude+"\" lon=\""+longitude+"\">\n";
        gpx+="\t\t\t\t<ele>"+location.getAltitude()+"</ele>\n";
        gpx+="\t\t\t\t<time>"+df.format(new Date(location.getTime()))+"</time>\n";
        gpx+="\t\t\t\t<cmt>"+getAdditionalInfo(location)+"</cmt>\n";
        gpx+="\t\t\t</trkpt>";

        return gpx;
    }

    private static boolean isEmptyFile(File f){
        boolean isEmpty = false;

        try {
            BufferedReader bf = new BufferedReader(new FileReader(f));
            isEmpty = (bf.readLine()==null);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return isEmpty;
    }

    private static String trackToGPXFile(Track t, File file){

        boolean error = false;
        String response = "";

        String gpx = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        gpx += "<gpx>\n\t<trk><name>"+t.getTrackId()+"</name>\t\t<trkseg>\n";

        for(TraceLocation location : t.getTracedTrack())
            gpx += locationToGpx(location)+"\n";

        gpx+="\t\t</trkseg>\t</trk>\n</gpx>";

        FileWriter fw = null;

        try {
            fw = new FileWriter(file);
            fw.write(gpx);
            response = t.getTrackId() + " exported to 'Documents'";
        } catch (IOException e) {
            e.printStackTrace();
            error = true;
        }finally {

            if(fw != null) try {
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }


            boolean deleted = false;
            if(error) {
                response = "Unable to export the file";
                Log.e("GPX", "Unable to export "+t.getTrackId()+", deleting the file");

                while (!deleted)
                    deleted = file.delete();
            }
        }

        return response;
    }

    public static String exportAsGPX(Context context, final Track track) {

        String response;
        final File gpxTrack =
                new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOCUMENTS),
                        "gpx_"+track.getTrackId()+".gpx");

        if(gpxTrack.exists() && !isEmptyFile(gpxTrack)) {
            response = "Already exported, skipping its creation";
            Log.e("STORAGE", "Already exported, skipping its creation");
        }else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    trackToGPXFile(track, gpxTrack);
                }
            }).start();

            //TODO: a track pode não ter sido exported com sucesso!
            return "Track successfully exported to your documents folder.";
        }



        return response;
    }
}
