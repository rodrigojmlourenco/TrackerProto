package org.trace.trackerproto.tracking.data;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.location.DetectedActivity;

import org.trace.trackerproto.tracking.modules.activity.ActivityConstants;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * @version 1.0
 * @author Rodrigo Lourenço
 */
public class Track implements Serializable, Parcelable{

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

        isLocalOnly = true;
    }

    protected Track(Parcel in) {
        sessionId = in.readString();
        startTime = in.readLong();
        stopTime = in.readLong();
        elapsedDistance = in.readDouble();
        isLocalOnly = in.readByte() != 0;
        tracedTrack = new LinkedList<>();
        in.readTypedList(tracedTrack, SerializableLocation.CREATOR);
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

        SerializableLocation og = tracedTrack.getLast();
        Location aux = new Location(og.getProvider());
        aux.setLatitude(og.getLatitude());
        aux.setLongitude(og.getLongitude());

        elapsedDistance += aux.distanceTo(location);

        tracedTrack.add(new SerializableLocation(location));
    }

    public void addTracedLocation(Location location, String activity){

        stopTime = location.getTime();

        SerializableLocation og = tracedTrack.getLast();
        Location aux = new Location(og.getProvider());
        aux.setLatitude(og.getLatitude());
        aux.setLongitude(og.getLongitude());

        elapsedDistance += aux.distanceTo(location);

        SerializableLocation loc = new SerializableLocation(location);
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
        dest.writeTypedList(tracedTrack);
    }
}
