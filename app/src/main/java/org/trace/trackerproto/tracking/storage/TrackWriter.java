package org.trace.trackerproto.tracking.storage;

import android.content.Context;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Rodrigo Lourenço on 19/02/2016.
 */
public class TrackWriter {


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
}
