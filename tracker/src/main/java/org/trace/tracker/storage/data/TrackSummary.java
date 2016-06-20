package org.trace.tracker.storage.data;

/**
 * Created by Rodrigo Lourenço on 11/03/2016.
 */
public class TrackSummary {
    private String session;
    private boolean isClosed, isValid;
    private double elapsedTime, elapsedDistance;

    public TrackSummary(){}

    public TrackSummary(String session, boolean isUploaded, boolean isValid){
        this.session = session;
        this.isClosed = isUploaded;
        this.isValid = isValid;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public boolean isValid() {
        return isValid;
    }

    public double getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(double elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public double getElapsedDistance() {
        return elapsedDistance;
    }

    public void setElapsedDistance(double elapsedDistance) {
        this.elapsedDistance = elapsedDistance;
    }
}
