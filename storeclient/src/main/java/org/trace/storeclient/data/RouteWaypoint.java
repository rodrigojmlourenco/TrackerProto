/*
 * Copyright (c) 2016 Rodrigo Lourenço, Miguel Costa, Paulo Ferreira, João Barreto @  INESC-ID.
 *
 * This file is part of TRACE.
 *
 * TRACE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TRACE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TRACE.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.trace.storeclient.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.JsonObject;

public class RouteWaypoint implements Parcelable{

    private String session;
    private long timestamp;
    private double latitude;
    private double longitude;
    private String attributes;

    public RouteWaypoint(){}

    public RouteWaypoint(String session, JsonObject jsonWaypoint){
        this.session = session;
        this.timestamp = jsonWaypoint.get("timestamp").getAsLong();
        this.latitude = jsonWaypoint.get("latitude").getAsDouble();
        this.longitude = jsonWaypoint.get("longitude").getAsDouble();

        try {
            this.attributes = jsonWaypoint.get("attributes").getAsString();
        }catch (UnsupportedOperationException e){
            this.attributes = "";
        }
    }

    public RouteWaypoint(Parcel in) {
        session = in.readString();
        timestamp = in.readLong();
        latitude = in.readDouble();
        longitude = in.readDouble();
        attributes = in.readString();
    }

    public static final Creator<RouteWaypoint> CREATOR = new Creator<RouteWaypoint>() {
        @Override
        public RouteWaypoint createFromParcel(Parcel in) {
            return new RouteWaypoint(in);
        }

        @Override
        public RouteWaypoint[] newArray(int size) {
            return new RouteWaypoint[size];
        }
    };

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(session);
        dest.writeLong(timestamp);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeString(attributes);
    }

    public JsonObject toJson(){
        JsonObject jWaypoint = new JsonObject();
        jWaypoint.addProperty(Attributes.timestamp, timestamp);
        jWaypoint.addProperty(Attributes.latitude, latitude);
        jWaypoint.addProperty(Attributes.longitude, longitude);
        jWaypoint.addProperty(Attributes.attributes, attributes);
        return jWaypoint;
    }

    public interface Attributes {
        String timestamp = "timestamp",
                latitude = "latitude",
                longitude = "longitude",
                attributes = "attributes";
    }
}
