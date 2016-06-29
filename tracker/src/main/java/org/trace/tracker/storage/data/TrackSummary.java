package org.trace.tracker.storage.data;


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

}
