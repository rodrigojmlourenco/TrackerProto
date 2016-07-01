package org.trace.tracker.storage.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.JsonObject;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.apache.commons.math3.stat.descriptive.rank.Median;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * @version 1.0
 * @author Rodrigo Lourenço
 *
 * TODO: the Parcel does not deal properly with the TrackSummaryFields
 */
public class Track extends TrackSummary implements Parcelable{

    private String trackId;
    private long startTime, stopTime;
    private List<TraceLocation> tracedTrack;
    private double elapsedDistance;
    private double averageSpeed, medianSpeed, topSpeed;

    @Deprecated
    private boolean isLocalOnly;
    @Deprecated
    private boolean isValid = false;

    public Track(){
        tracedTrack = new LinkedList<>();
        isLocalOnly = true;
        elapsedDistance = 0;

        this.tracedTrack = new ArrayList<>();
    }

    public Track(TrackSummary summary){

        setTrackId(summary.getTrackId());
        setStartTimestamp(summary.getStart());
        setStoppedTimestamp(summary.getStop());
        setFromLocation(summary.getFromLocation());
        setToLocation(summary.getToLocation());
        setSemanticFromLocation(summary.getSemanticFromLocation());
        setSemanticToLocation(summary.getSemanticToLocation());
        setTravelledDistance(summary.getElapsedDistance());
        setModality(summary.getModality());
        setSensingType(summary.getSensingType());

        tracedTrack = new ArrayList<>();
    }


    protected Track(Parcel in) {
        super(in);
        trackId = in.readString();
        startTime = in.readLong();
        stopTime = in.readLong();
        tracedTrack = in.createTypedArrayList(TraceLocation.CREATOR);
        elapsedDistance = in.readDouble();
        averageSpeed = in.readDouble();
        medianSpeed = in.readDouble();
        topSpeed = in.readDouble();
        isLocalOnly = in.readByte() != 0;
        isValid = in.readByte() != 0;

        this.tracedTrack = new ArrayList<>();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(trackId);
        dest.writeLong(startTime);
        dest.writeLong(stopTime);
        dest.writeTypedList(tracedTrack);
        dest.writeDouble(elapsedDistance);
        dest.writeDouble(averageSpeed);
        dest.writeDouble(medianSpeed);
        dest.writeDouble(topSpeed);
        dest.writeByte((byte) (isLocalOnly ? 1 : 0));
        dest.writeByte((byte) (isValid ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
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

    public void setTracedTrack(List<TraceLocation> trace){
        this.tracedTrack = trace;
    }

    public void addTracedLocation(TraceLocation location){

        if(tracedTrack.isEmpty())
            startTime = location.getTime();

        stopTime = location.getTime();
        tracedTrack.add(location);
    }

    public String getTrackId() {
        return trackId;
    }

    public List<TraceLocation> getTracedTrack() {
        return tracedTrack;
    }

    public double getTravelledDistance() {
        return elapsedDistance;
    }

    public long getElapsedTime(){
        return stopTime-startTime;
    }

    public void setTrackId(String trackId) {
        this.trackId = trackId;
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
        throw new UnsupportedOperationException("toJson@Track");
    }

}
