package org.trace.trackerproto.tracking.modules;

import android.location.Location;

/**
 * Created by Rodrigo Lourenço on 18/02/2016.
 */
public interface ModuleInterface {

    public void startTracking(long millis);

    public void stopTracking();

    public void dump();

    public Location getLastLocation();
}
