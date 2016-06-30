package org.trace.tracker.storage.data;


import android.location.Location;

public class TrackSummary {
    private String session;
    private boolean isClosed, isValid;
    private double elapsedTime, elapsedDistance;
    private long startTimestamp, stopTimestamp;

    public TrackSummary(){}

    public TrackSummary(String session, boolean isUploaded, boolean isValid){
        this.session = session;
        this.isClosed = isUploaded;
        this.isValid = isValid;
    }

    @Deprecated
    public String getSession() {
        return session;
    }

    @Deprecated
    public void setSession(String session) {
        this.session = session;
    }

    @Deprecated
    public boolean isClosed() {
        return isClosed;
    }

    @Deprecated
    public boolean isValid() {
        return isValid;
    }

    public double getElapsedTime() {
        return elapsedTime;
    }

    @Deprecated
    public void setElapsedTime(double elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public double getElapsedDistance() {
        return elapsedDistance;
    }

    public void setElapsedDistance(double elapsedDistance) {
        this.elapsedDistance = elapsedDistance;
    }

    public void setStartTimestamp(long startedAt) {
        this.startTimestamp = startedAt;
    }

    public void setStoppedTimestamp(long endedAt) {
        this.stopTimestamp = endedAt;
    }

    public long getStart() {
        return startTimestamp;
    }

    public long getStop() {
        return stopTimestamp;
    }

    private int modality;
    private int sensingType;
    public void setModality(int modality) {
        this.modality = modality;
    }

    public void setSensingType(int sensingType){
        this.sensingType = sensingType;
    }

    private Location fromLocation = null, toLocation = null;

    public void setFromLocation(TraceLocation location) {
        this.fromLocation = location;
    }

    public Location getFromLocation() {
        return fromLocation;
    }

    public void setFromLocation(Location location) {
        this.fromLocation = location;
    }

    public Location getToLocation() {
        return toLocation;
    }

    public void setToLocation(Location toLocation) {
        this.toLocation = toLocation;
    }

    private String semanticFromLocation = null, semanticToLocation = null;

    public String getSemanticToLocation() {
        return semanticToLocation;
    }

    public void setSemanticToLocation(String semanticToLocation) {
        this.semanticToLocation = semanticToLocation;
    }

    public String getSemanticFromLocation() {
        return semanticFromLocation;
    }

    public void setSemanticFromLocation(String semanticFromLocation) {
        this.semanticFromLocation = semanticFromLocation;
    }
}
