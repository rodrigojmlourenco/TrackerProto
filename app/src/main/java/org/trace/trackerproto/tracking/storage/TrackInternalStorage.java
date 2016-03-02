package org.trace.trackerproto.tracking.storage;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONObject;
import org.trace.trackerproto.tracking.data.SerializableLocation;
import org.trace.trackerproto.tracking.data.Track;
import org.trace.trackerproto.tracking.storage.exceptions.UnableToLoadStoredTrackException;
import org.trace.trackerproto.tracking.storage.exceptions.UnableToStoreTrackException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author Rodrigo Lourenço
 * @version 1.0
 *
 * Created by Rodrigo Lourenço on 22/02/2016.
 */
public class TrackInternalStorage {

    private static final String BASE_DIR = "tracks/";
    private static final String PREFIX = "track_";

    public static void storeTracedTrack(Context context, String sessionId, Track track) throws UnableToStoreTrackException {


        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        String filename = String.valueOf(System.currentTimeMillis());

        try {
            fos = context.openFileOutput(PREFIX + filename, Context.MODE_PRIVATE);
            oos = new ObjectOutputStream(fos);

            oos.writeObject(track);

        } catch (IOException e) {
            e.printStackTrace();
            throw new UnableToStoreTrackException(e.getMessage());
        } finally {
            try {
                if(oos != null){

                    oos.close();

                    if(fos != null)
                        fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Track loadTracedTrack(Context context, String sessionId) throws UnableToLoadStoredTrackException {

        Track track;

        try {
            FileInputStream fis = context.openFileInput(PREFIX + sessionId);
            ObjectInputStream ois = new ObjectInputStream(fis);

            track = (Track) ois.readObject();

            ois.close();
            fis.close();

        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            throw new UnableToLoadStoredTrackException(e.getMessage());
        }

        return track;
    }

    public static List<String> listStoredTracks(Context context){

        List<String> files = new ArrayList<>();

        for(String fn : Arrays.asList(context.fileList())){
            if(fn.startsWith(PREFIX))
                files.add(fn);
        }

        return files;
    }

    public static void removeStoreTrack(Context context, String sessionId){
        context.deleteFile(PREFIX + sessionId);
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
