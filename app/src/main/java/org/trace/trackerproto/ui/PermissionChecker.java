package org.trace.trackerproto.ui;

/**
 * Created by Rodrigo Lourenço on 12/02/2016.
 */
public interface PermissionChecker {

    public void checkForLocationPermissions();

    public boolean hasLocationPermissions();
}
