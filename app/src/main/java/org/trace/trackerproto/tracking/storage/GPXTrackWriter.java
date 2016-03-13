package org.trace.trackerproto.tracking.storage;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.trace.trackerproto.tracking.storage.data.SerializableLocation;
import org.trace.trackerproto.tracking.storage.data.Track;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getAdditionalInfo(SerializableLocation location){
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("speed", location.getSpeed());
        jsonObject.addProperty("accuracy", location.getAccuracy());
        jsonObject.addProperty("provider", location.getProvider());
        jsonObject.addProperty("bearing", location.getBearing());
        jsonObject.addProperty("activity", location.getActivityMode());
        return gson.toJson(jsonObject);
    }

    private static String locationToGpx(SerializableLocation location){

        String gpx;
        double latitude, longitude;
        DateFormat df = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
        latitude = location.getLatitude();
        longitude= location.getLongitude();


        gpx ="\t\t\t<trkpt lat=\""+latitude+"\" lon=\""+longitude+"\">\n";
        gpx+="\t\t\t\t<ele>"+location.getAltitude()+"</ele>\n";
        gpx+="\t\t\t\t<time>"+df.format(new Date(location.getTimestamp()))+"</time>\n";
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
        gpx += "<gpx>\n\t<trk><name>"+t.getSessionId()+"</name>\t\t<trkseg>\n";

        for(SerializableLocation location : t.getTracedTrack())
            gpx += locationToGpx(location)+"\n";

        gpx+="\t\t</trkseg>\t</trk>\n</gpx>";

        FileWriter fw = null;

        try {
            fw = new FileWriter(file);
            fw.write(gpx);
            response = t.getSessionId() + " exported to 'Documents'";
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
                Log.e("GPX", "Unable to export "+t.getSessionId()+", deleting the file");

                while (!deleted)
                    deleted = file.delete();
            }
        }

        return response;
    }

    public static String exportAsGPX(Context context, Track track) {

        String response;
        File gpxTrack =
                new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOCUMENTS),
                        "gpx_"+track.getSessionId()+".gpx");

        if(gpxTrack.exists() && !isEmptyFile(gpxTrack)) {
            response = "Already exported, skipping its creation";
            Log.e("STORAGE", "Already exported, skipping its creation");
        }else
            response = trackToGPXFile(track, gpxTrack);

        return response;
    }
}
