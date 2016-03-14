package org.trace.trackerproto.tracking.storage.data;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * @version 1.0
 * @author Rodrigo Lourenço
 */
public class Track implements Serializable, Parcelable{

    private String sessionId;
    private long startTime, stopTime;
    private LinkedList<TraceLocation> tracedTrack;
    private double elapsedDistance;

    private boolean isLocalOnly;
    private boolean isValid = false;

    public Track(String sessionId, Location location){

        this.sessionId = sessionId;

        tracedTrack = new LinkedList<>();

        startTime = location.getTime();
        stopTime  = location.getTime();

        elapsedDistance = 0;

        tracedTrack.add(new TraceLocation(location));

        isLocalOnly = true;

    }

    public Track(){
        tracedTrack = new LinkedList<>();
        isLocalOnly = true;
        elapsedDistance = 0;
    }

    protected Track(Parcel in) {
        sessionId = in.readString();
        startTime = in.readLong();
        stopTime = in.readLong();
        elapsedDistance = in.readDouble();
        isLocalOnly = in.readByte() != 0;
        isValid = in.readByte() != 0;
        tracedTrack = new LinkedList<>();
        in.readTypedList(tracedTrack, TraceLocation.CREATOR);
    }

    public static final Creator<Track> CREATOR = new Creator<Track>() {
        @Override
        public Track createFromParcel(Parcel in) {
            return new Track(in);
        }

        @Override
        public Track[] newArray(int size) {
            return new Track[size];
        }
    };

    public void addTracedLocation(Location location){

        stopTime = location.getTime();

        TraceLocation og = tracedTrack.getLast();
        Location aux = new Location(og.getProvider());
        aux.setLatitude(og.getLatitude());
        aux.setLongitude(og.getLongitude());

        elapsedDistance += aux.distanceTo(location);

        tracedTrack.add(new TraceLocation(location));
    }

    public void addTracedLocation(TraceLocation location){

        if(tracedTrack.isEmpty())
            startTime = location.getTime();

        stopTime = location.getTime();
        //TODO: update elapsedDistance;
        tracedTrack.add(location);
    }

    public void addTracedLocation(Location location, String activity){

        stopTime = location.getTime();

        TraceLocation og = tracedTrack.getLast();
        Location aux = new Location(og.getProvider());
        aux.setLatitude(og.getLatitude());
        aux.setLongitude(og.getLongitude());

        elapsedDistance += aux.distanceTo(location);

        TraceLocation loc = new TraceLocation(location);
        loc.setActivityMode(activity);

        tracedTrack.add(loc);
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

    public LinkedList<TraceLocation> getTracedTrack() {
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

    public TraceLocation getStartPosition(){
        return tracedTrack.getFirst();
    }

    public TraceLocation getFinalPosition(){
        return tracedTrack.getLast();
    }


    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setStopTime(long stopTime) {
        this.stopTime = stopTime;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setIsValid(boolean isValid) {
        this.isValid = isValid;
    }

    public void setTravelledDistance(double distance){
        this.elapsedDistance = distance;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(sessionId);
        dest.writeLong(startTime);
        dest.writeLong(stopTime);
        dest.writeDouble(elapsedDistance);
        dest.writeByte((byte) (isLocalOnly ? 1 : 0));
        dest.writeByte((byte) (isValid ? 1 : 0));
        dest.writeTypedList(tracedTrack);
    }
}
