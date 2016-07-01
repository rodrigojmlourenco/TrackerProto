package org.trace.tracker.storage.data;


import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

public class TrackSummary implements Parcelable{

    private String trackId;
    private double elapsedDistance;
    private long startTimestamp, stopTimestamp;
    private int modality;
    private int sensingType;
    private String semanticFromLocation = null, semanticToLocation = null;
    private Location fromLocation = null, toLocation = null;

    public TrackSummary(){}

    public TrackSummary(String trackId){
        this.trackId = trackId;
    }

    protected TrackSummary(Parcel in) {
        trackId = in.readString();
        elapsedDistance = in.readDouble();
        startTimestamp = in.readLong();
        stopTimestamp = in.readLong();
        modality = in.readInt();
        sensingType = in.readInt();
        semanticFromLocation = in.readString();
        semanticToLocation = in.readString();
        fromLocation = in.readParcelable(Location.class.getClassLoader());
        toLocation = in.readParcelable(Location.class.getClassLoader());
    }

    public static final Creator<TrackSummary> CREATOR = new Creator<TrackSummary>() {
        @Override
        public TrackSummary createFromParcel(Parcel in) {
            return new TrackSummary(in);
        }

        @Override
        public TrackSummary[] newArray(int size) {
            return new TrackSummary[size];
        }
    };

    public String getTrackId() {
        return trackId;
    }

    public void setTrackId(String trackId) {
        this.trackId = trackId;
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


    public void setModality(int modality) {
        this.modality = modality;
    }

    public void setSensingType(int sensingType){
        this.sensingType = sensingType;
    }



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

    public int getModality() {
        return modality;
    }

    public int getSensingType() {
        return sensingType;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(trackId);
        dest.writeDouble(elapsedDistance);
        dest.writeLong(startTimestamp);
        dest.writeLong(stopTimestamp);
        dest.writeInt(modality);
        dest.writeInt(sensingType);
        dest.writeString(semanticFromLocation);
        dest.writeString(semanticToLocation);
        dest.writeParcelable(fromLocation, flags);
        dest.writeParcelable(toLocation, flags);
    }
}
