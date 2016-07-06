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

import java.util.concurrent.TimeUnit;

public class RouteSummary implements Parcelable{

    private String session;
    private long startedAt;
    private long endedAt;
    private double elapsedDistance;
    private float avgSpeed;
    private float topSpeed;
    private int points;
    private int modality;
    private String from ="", to = "";

    public RouteSummary(){}

    public RouteSummary(JsonObject summary){
        session = summary.get(Attributes.session).getAsString();
        startedAt = summary.get(Attributes.startedAt).getAsLong();
        endedAt = summary.get(Attributes.endedAt).getAsLong();
        elapsedDistance = summary.get(Attributes.elapsedDistance).getAsDouble();
        points = summary.get(Attributes.points).getAsInt();
        modality = summary.get(Attributes.modality).getAsInt();
        avgSpeed = summary.get(Attributes.avgSpeed).getAsFloat();
        topSpeed = summary.get(Attributes.topSpeed).getAsFloat();
        //from = summary.get(Attributes.from).getAsString();
        //to = summary.get(Attributes.to).getAsString();
    }


    protected RouteSummary(Parcel in) {
        session = in.readString();
        startedAt = in.readLong();
        endedAt = in.readLong();
        elapsedDistance = in.readDouble();
        avgSpeed = in.readFloat();
        topSpeed = in.readFloat();
        points = in.readInt();
        modality = in.readInt();
        from = in.readString();
        to = in.readString();
    }

    public static final Creator<RouteSummary> CREATOR = new Creator<RouteSummary>() {
        @Override
        public RouteSummary createFromParcel(Parcel in) {
            return new RouteSummary(in);
        }

        @Override
        public RouteSummary[] newArray(int size) {
            return new RouteSummary[size];
        }
    };

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public long getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(long endedAt) {
        this.endedAt = endedAt;
    }

    public int getElapsedTime() {
        return (int) TimeUnit.SECONDS.convert(endedAt - startedAt, TimeUnit.MILLISECONDS);
    }



    public double getElapsedDistance() {
        return elapsedDistance;
    }

    public void setElapsedDistance(double elapsedDistance) {
        this.elapsedDistance = elapsedDistance;
    }

    public float getAvgSpeed() {
        return avgSpeed;
    }

    public void setAvgSpeed(float avgSpeed) {
        this.avgSpeed = avgSpeed;
    }

    public float getTopSpeed() {
        return topSpeed;
    }

    public void setTopSpeed(float topSpeed) {
        this.topSpeed = topSpeed;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getModality() {
        return modality;
    }

    public void setModality(int modality) {
        this.modality = modality;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(session);
        dest.writeLong(startedAt);
        dest.writeLong(endedAt);
        dest.writeDouble(elapsedDistance);
        dest.writeFloat(avgSpeed);
        dest.writeFloat(topSpeed);
        dest.writeInt(points);
        dest.writeInt(modality);
        dest.writeString(from);
        dest.writeString(to);
    }

    private interface Attributes {
        String session = "session";
        String startedAt = "startedAt";
        String endedAt = "endedAt";
        String elapsedTime = "elapsedTime";
        String elapsedDistance = "elapsedDistance";
        String points = "points";
        String modality = "modality";
        String avgSpeed = "avgSpeed";
        String topSpeed = "topSpeed";
        String from = "from";
        String to = "to";
    }
}
