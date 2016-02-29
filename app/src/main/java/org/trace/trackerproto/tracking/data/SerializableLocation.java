package org.trace.trackerproto.tracking.data;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;


public class SerializableLocation implements Serializable, Parcelable{

    private double latitude, longitude, altitude;
    private long timestamp, elapsedNanos;
    private float accuracy, speed, bearing;
    private String provider;

    private String activityMode;

    public SerializableLocation(){}

    public SerializableLocation(Location location){
        this.latitude = location.getLatitude();
        this.longitude= location.getLongitude();
        this.accuracy = location.getAccuracy();

        this.altitude = location.getAltitude();
        this.speed    = location.getSpeed();
        this.bearing  = location.getBearing();

        this.provider = location.getProvider();

        this.timestamp = location.getTime();
        this.elapsedNanos = location.getElapsedRealtimeNanos();
    }

    protected SerializableLocation(Parcel in) {
        latitude = in.readDouble();
        longitude = in.readDouble();
        altitude = in.readDouble();
        timestamp = in.readLong();
        elapsedNanos = in.readLong();
        accuracy = in.readFloat();
        speed = in.readFloat();
        bearing = in.readFloat();
        provider = in.readString();
        activityMode = in.readString();
    }

    public static final Creator<SerializableLocation> CREATOR = new Creator<SerializableLocation>() {
        @Override
        public SerializableLocation createFromParcel(Parcel in) {
            return new SerializableLocation(in);
        }

        @Override
        public SerializableLocation[] newArray(int size) {
            return new SerializableLocation[size];
        }
    };

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getElapsedNanos() {
        return elapsedNanos;
    }

    public void setElapsedNanos(long elapsedNanos) {
        this.elapsedNanos = elapsedNanos;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getBearing() {
        return bearing;
    }

    public void setBearing(float bearing) {
        this.bearing = bearing;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }


    public String getActivityMode() {
        return activityMode;
    }

    public void setActivityMode(String activityMode) {
        this.activityMode = activityMode;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeDouble(altitude);
        dest.writeLong(timestamp);
        dest.writeLong(elapsedNanos);
        dest.writeFloat(accuracy);
        dest.writeFloat(speed);
        dest.writeFloat(bearing);
        dest.writeString(provider);
        dest.writeString(activityMode);
    }
}
