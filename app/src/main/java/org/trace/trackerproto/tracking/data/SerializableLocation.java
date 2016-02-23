package org.trace.trackerproto.tracking.data;

import android.location.Location;

import java.io.Serializable;

/**
 * Created by Rodrigo Lourenço on 22/02/2016.
 */
public class SerializableLocation implements Serializable{

    private double latitude, longitude, altitude;
    private long timestamp, elapsedNanos;
    private float accuracy, speed, bearing;
    private String provider;

    public SerializableLocation(){}

    public SerializableLocation(Location location){
        this.latitude = location.getLatitude();
        this.longitude= location.getLongitude();
        this.accuracy = location.getAccuracy();

        this.altitude = location.getAltitude();
        this.speed    = location.getSpeed();
        this.bearing  = location.getBearing();

        this.provider = location.getProvider();
    }

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
}
