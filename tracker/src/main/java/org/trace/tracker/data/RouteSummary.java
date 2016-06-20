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

package org.trace.tracker.data;

import android.os.Handler;

import org.trace.tracker.storage.data.TraceLocation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by Rodrigo Lourenço on 20/06/2016.
 */
public class RouteSummary {

    private long startTime, endTime;
    private int modality;
    private int sensingType;
    private double travelledDistance;
    private List<TraceLocation> route;
    private Dictionary<String, String> semanticStartLocation, semanticEndLocation;

    private SimpleDateFormat format = new SimpleDateFormat("hh:mm:ss dd/MM/yyyy");

    public RouteSummary(long start, long end, int modality){
        this.startTime = start;
        this.endTime = end;
        this.travelledDistance = 0;
        this.modality = modality;

        this.route = new ArrayList<>();

        this.semanticStartLocation = new Hashtable<>();
        this.semanticEndLocation = new Hashtable<>();
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getModality() {
        return modality;
    }

    public void setModality(int modality) {
        this.modality = modality;
    }

    public int getSensingType() {
        return sensingType;
    }

    public void setSensingType(int sensingType) {
        this.sensingType = sensingType;
    }

    public double getTravelledDistance() {

        if(route.isEmpty()) return 0;

        return travelledDistance;
    }

    public Dictionary<String, String> getSemanticStartLocation() {
        return semanticStartLocation;
    }

    public void setStartLocation(TraceLocation location) {
        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                //TODO: needs a context
            }
        });
    }

    public Dictionary<String, String> getSemanticEndLocation() {
        return semanticEndLocation;
    }

    public void setEndLocation(TraceLocation location) {
        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                //TODO: needs a context
            }
        });
    }

    public List<TraceLocation> getRoute(){
        return getRoute();
    }

    public void addRouteWayPoint(TraceLocation wayPoint){

        if(!route.isEmpty()){
            travelledDistance += wayPoint.distanceTo(route.get(route.size()-1));
        }

        route.add(wayPoint);
    }
}
