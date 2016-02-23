package org.trace.trackerproto.tracking.storage;

import android.content.Context;

import org.trace.trackerproto.tracking.data.Track;
import org.trace.trackerproto.tracking.storage.exceptions.UnableToLoadStoredTrackException;
import org.trace.trackerproto.tracking.storage.exceptions.UnableToStoreTrackException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Arrays;
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

        //TODO: this should not be necessary
        if(sessionId == null || sessionId.isEmpty())
            sessionId = "local_"+String.valueOf(Math.random());


        try {
            FileOutputStream fos = context.openFileOutput(PREFIX+sessionId, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(track);

            oos.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new UnableToStoreTrackException(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            throw new UnableToStoreTrackException(e.getMessage());
        }
    }

    public static Track loadTracedTrack(Context context, String sessionId) throws UnableToLoadStoredTrackException {

        Track track = null;

        try {
            FileInputStream fis = context.openFileInput(PREFIX+sessionId);
            ObjectInputStream ois = new ObjectInputStream(fis);

            track = (Track) ois.readObject();

            ois.close();
            fis.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new UnableToLoadStoredTrackException(e.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new UnableToLoadStoredTrackException(e.getMessage());
        } catch (OptionalDataException e) {
            e.printStackTrace();
            throw new UnableToLoadStoredTrackException(e.getMessage());
        } catch (StreamCorruptedException e) {
            e.printStackTrace();
            throw new UnableToLoadStoredTrackException(e.getMessage());
        } catch (IOException e) {
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
}
