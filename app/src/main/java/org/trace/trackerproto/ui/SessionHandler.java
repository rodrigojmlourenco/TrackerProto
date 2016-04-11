package org.trace.trackerproto.ui;


public interface SessionHandler {

    void updateTrackingSession();

    void teardownTrackingSession();

    String getSessionIdentifier();

    boolean isValidSession();
}
