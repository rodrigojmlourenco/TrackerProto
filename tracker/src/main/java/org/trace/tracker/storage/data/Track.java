package org.trace.tracker.storage.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.apache.commons.math3.stat.descriptive.rank.Median;

import java.util.LinkedList;

//import org.apache.commons.math3.stat.descriptive.moment.Mean;
//import org.apache.commons.math3.stat.descriptive.rank.Max;
//import org.apache.commons.math3.stat.descriptive.rank.Median;

/**
 * @version 1.0
 * @author Rodrigo Lourenço
 */
public class Track implements Parcelable{

    private String sessionId;
    private long startTime, stopTime;
    private LinkedList<TraceLocation> tracedTrack;
    private double elapsedDistance;
    private double averageSpeed, medianSpeed, topSpeed;

    private boolean isLocalOnly;
    private boolean isValid = false;

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

        updateSpeeds();
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

    public void addTracedLocation(TraceLocation location){

        if(tracedTrack.isEmpty())
            startTime = location.getTime();

        stopTime = location.getTime();
        //TODO: update elapsedDistance;
        tracedTrack.add(location);
    }

    public String getSessionId() {
        return sessionId;
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

    public boolean isValid() {
        return isValid;
    }

    public void setIsValid(boolean isValid) {
        this.isValid = isValid;
    }

    public void setTravelledDistance(double distance){
        this.elapsedDistance = distance;
    }

    public double getAverageSpeed() {
        return averageSpeed;
    }

    public double getMedianSpeed() {
        return medianSpeed;
    }

    public double getTopSpeed() {
        return topSpeed;
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

    private void updateSpeeds(){

        double[] measuredSpeeds = new double[tracedTrack.size()];

        for(int i=0; i < tracedTrack.size(); i++){
            measuredSpeeds[i] = tracedTrack.get(i).getSpeed();
        }

        Mean mean = new Mean();
        Median median = new Median();
        Max max = new Max();

        averageSpeed    = (mean.evaluate(measuredSpeeds)    *3600)/1000; //Km/h
        medianSpeed     = (median.evaluate(measuredSpeeds)  *3600)/1000; //Km/h
        topSpeed        = (max.evaluate(measuredSpeeds)     *3600)/1000; //Km/h

    }


    public JsonObject toJson(){
        JsonObject traceTrack = new JsonObject();
        JsonArray track = new JsonArray();

        for(TraceLocation location : tracedTrack)
            track.add(location.getSerializableLocationAsJson());

        traceTrack.addProperty("start", getStartTimestamp());
        traceTrack.addProperty("end", getEndTimestamp());
        traceTrack.addProperty("elapsedTime", getElapsedTime());
        traceTrack.addProperty("distance", getTravelledDistance());
        traceTrack.addProperty("topSpeed", getTopSpeed());
        traceTrack.addProperty("avgSpeed", getAverageSpeed());
        traceTrack.addProperty("medianSpeed", getMedianSpeed());

        traceTrack.add("track", track);

        return traceTrack;
    }

    public long getStartTimestamp(){
        return tracedTrack.getFirst().getTime();
    }

    public long getEndTimestamp(){
        return tracedTrack.getLast().getTime();
    }
}
