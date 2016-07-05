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
    private List<TraceLocation> tracedTrack;
    private double averageSpeed, medianSpeed, topSpeed;

    @Deprecated
    private boolean isLocalOnly;
    @Deprecated
    private boolean isValid = false;

    public Track(){
        tracedTrack = new LinkedList<>();
        isLocalOnly = true;

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
        setElapsedDistance(summary.getElapsedDistance());
        setModality(summary.getModality());
        setSensingType(summary.getSensingType());

        tracedTrack = new ArrayList<>();
    }


    protected Track(Parcel in) {
        super(in);
        trackId = in.readString();
        tracedTrack = in.createTypedArrayList(TraceLocation.CREATOR);
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
        dest.writeTypedList(tracedTrack);
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


    public String getTrackId() {
        return trackId;
    }

    public List<TraceLocation> getTracedTrack() {
        return tracedTrack;
    }


    public void setTrackId(String trackId) {
        this.trackId = trackId;
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

    public void updateSpeeds(){ //TODO: there should be a better solution

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
