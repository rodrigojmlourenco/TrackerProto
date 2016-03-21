package org.trace.trackerproto.ui;

public interface TrackingFragment {

    boolean isTracking();

    void stopTracking();

    void startTracking();

    void focusOnCurrentLocation();
}
