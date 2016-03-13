package org.trace.trackerproto;

import android.app.Application;
import android.content.Context;

/**
 * Created by Rodrigo Lourenço on 13/03/2016.
 */
public class TrackerProto extends Application {

    private static TrackerProto instance;

    public static TrackerProto getInstance() {
        return instance;
    }

    public static Context getContext(){
        return instance.getApplicationContext();
    }

    @Override
    public void onCreate() {
        instance = this;
        super.onCreate();
    }
}
