package org.trace.trackerproto.tracking.data;

import android.location.Location;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * @version 1.0
 * @author Rodrigo Lourenço
 */
public class Track implements Serializable{

    private String sessionId;
    private long startTime, stopTime;
    private LinkedList<SerializableLocation> tracedTrack;
    private double elapsedDistance;

    private boolean isLocalOnly;

    public Track(String sessionId, Location location){

        this.sessionId = sessionId;

        tracedTrack = new LinkedList<>();

        startTime = location.getTime();
        stopTime  = location.getTime();

        elapsedDistance = 0;

        tracedTrack.add(new SerializableLocation(location));
    }

    public void addTracedLocation(Location location){

        stopTime = location.getTime();

        SerializableLocation og = tracedTrack.getLast();
        Location aux = new Location(og.getProvider());
        aux.setLatitude(og.getLatitude());
        aux.setLongitude(og.getLongitude());

        elapsedDistance += aux.distanceTo(location);

        tracedTrack.add(new SerializableLocation(location));
    }

    public String getSessionId() {
        return sessionId;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getStopTime() {
        return stopTime;
    }

    public LinkedList<SerializableLocation> getTracedTrack() {
        return tracedTrack;
    }

    public double getTravelledDistance() {
        return elapsedDistance;
    }

    public long getElapsedTime(){
        return stopTime-startTime;
    }

    public void upload(){
        isLocalOnly = false;
    }

    public boolean isLocalOnly(){
        return isLocalOnly;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public SerializableLocation getStartPosition(){
        return tracedTrack.getFirst();
    }

    public SerializableLocation getFinalPosition(){
        return tracedTrack.getLast();
    }
}
