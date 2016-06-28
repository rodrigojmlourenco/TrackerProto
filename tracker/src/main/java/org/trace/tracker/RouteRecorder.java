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

package org.trace.tracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.DetectedActivity;

import org.trace.tracker.data.RouteSummary;
import org.trace.tracker.storage.data.TraceLocation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by Rodrigo Lourenço on 20/06/2016.
 */
public class RouteRecorder {

    private static final String LOG_TAG = "RouteRecorder";

    //State
    private boolean isPaused = false;
    private boolean isRecording = false;
    private boolean isAutomatic;
    private boolean silentStart;
    private RouteSummary mRouteSummary;

    private static RouteRecorder ROUTE_RECORDER = null;
    private boolean isFinishLock;


    private RouteRecorder(){

        //Position Handlers
        onPositionChanged = new OnPositionChangeHandler();
        onModalityChanged = new OnModalityChangeHandler();
        //TODO: register Receiver

        //Timers
        mIdleTimer = mExecutorService.schedule(new IdleElapsed(), 20, TimeUnit.MINUTES); //TODO: should not be hardcoded

    }

    public static RouteRecorder getInstance(){
        synchronized (RouteRecorder.class){
            if(ROUTE_RECORDER == null) ROUTE_RECORDER = new RouteRecorder();
        }

        return ROUTE_RECORDER;
    }

    /* Route Recorder Client Functions
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    public void startRoute(int modality, boolean automatic, boolean silent){

        if(isRecording){
            //TODO: check if failed first
            return;
        }else
            logStartRecording();

        //Set RouteRecorder State
        isRecording = true;
        isAutomatic = automatic;
        silentStart = silent;
        //TODO: finishLock (boolean)?


        if(isAutomatic){
            //TODO: analyze and copycat
        }

        //Timers
        resetIdleTimer();

        long time = System.currentTimeMillis();
        mRouteSummary = new RouteSummary(time, time, modality);

    }

    public void pause(){

        if(isPaused) return;

        isPaused = true;
        stopIdleTimer();
        //TODO: stop location tracking;
    }

    public void resume(){

        if(!isPaused) return;

        isPaused = false;
        startIdleTimer();
        //TODO: start location tracking;
    }

    public void stopRoute(boolean isManual){

        //TODO: why this?
        if(isFinishLock){
            return;
        }

        isFinishLock = false;

        //TODO: why this?
        if(!isRecording){
            isFinishLock = false;
            return;
        }
        isRecording = false;


        if(isAutomatic && mRouteSummary.getTravelledDistance() <= 250){ /*TODO: should not be hardcoded */
            //If the route was automatically started and less than 250 meters where travelled
            //then the route should be discarded.
            return;
        }

        if(isManual){
            mRouteSummary.setSensingType(1);
            //TODO: save the route summary to the database? why only now?
        }

        //TODO: stop the location tracking

        //TODO: stop the modality tracking

        //TODO: set the end location (this has to be done async)

        //TODO: they create some kind of polyline (starting in line 566)


        //Step X - If has WiFi then upload to the server (DEIXA DE SER FEITO AQUI)
    }


    /* Logging
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    private SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss dd/MM/yyyy");

    private void logStartRecording(){
        Log.i(LOG_TAG, "Started recording @"+sdf.format(new Date(System.currentTimeMillis())));
    }

    private void logPosition(TraceLocation location){

    }

    private void logAbnormalBehaviour(String abnormalBehaviour) {
        Log.e(LOG_TAG, abnormalBehaviour);
    }


    /* Position Change Handlers
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    private OnPositionChangeHandler onPositionChanged;

    public class OnPositionChangeHandler extends BroadcastReceiver{

        private TraceLocation lastPosition = null;

        @Override
        public void onReceive(Context context, Intent intent) {
            //TODO: onPositionChanged

            TraceLocation position;
            double currentDistance = -1;

            if(intent.hasExtra(org.trace.tracker.TrackingConstants.tracker.LOCATION_EXTRA))
                position = intent.getParcelableExtra(org.trace.tracker.TrackingConstants.tracker.LOCATION_EXTRA);
            else
                return;


            if(!isRecording){
                logAbnormalBehaviour("Not recording but location was received. "+position.toString());
                return;
            }

            logPosition(position);

            //Mathematically correct the values of new incoherent locations
            if(position.getSpeed() <= 0 && lastPosition != null){
                double distance = position.distanceTo(lastPosition);
                long elapsedTime= TimeUnit.MILLISECONDS.toSeconds(position.getTime() - lastPosition.getTime());

                if(distance == 0 || elapsedTime == 0)
                    position.setSpeed(0);
                else
                    position.setSpeed((float) (distance / elapsedTime));

                if(position.getLatitude() != 0 && position.getLongitude() != 0
                        && lastPosition.getLatitude() != 0 && lastPosition.getLongitude() != 0) //TODO: why verify this
                position.setBearing(position.bearingTo(lastPosition));
            }


            //TODO: let the updateLocation handler know about the position change

            //Pull the last known activity and update it
            position.setActivityMode(onModalityChanged.getCurrentActivity());


            if(isAcceptableAccuracy(position)){

                //TODO: handling a silent start?

                if(lastPosition == null) { //1st recorded position

                    //Save the route summary in a database with the new point

                    resetIdleTimer();
                    lastPosition = position;

                }else if(isInterestingNewPosition(position)){ //This is a new location

                    //Update the route summary in a database with the new point

                    resetIdleTimer();
                    lastPosition = position;
                }

            }
        }


        private boolean isAcceptableAccuracy(TraceLocation position){
            return position.getAccuracy() <= 50; //TODO: should not be hardcoded

        }

        private boolean isInterestingNewPosition(TraceLocation position) {
            if (lastPosition == null)
                return false;
            else
                return lastPosition.distanceTo(position) > 100 /* TODO: should not be hardcoded */
                        || position.isCorner(lastPosition.getBearing()); /*TODO: may cause infinite interesting points */
        }

    }


    /* Modality Change Handlers
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    private OnModalityChangeHandler onModalityChanged;

    public class OnModalityChangeHandler extends BroadcastReceiver {

        private DetectedActivity currentActivity;

        @Override
        public void onReceive(Context context, Intent intent) {
            //TODO: motionActivityHandler
        }

        public DetectedActivity getCurrentActivity(){
            return currentActivity;
        }

        public int getCurrentActivityType(){
            //TODO;
            return -1;
        }
    }
    /* Timers
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    private ScheduledFuture mIdleTimer;
    private ScheduledExecutorService mExecutorService = Executors.newScheduledThreadPool(1);


    private void startIdleTimer(){
        mIdleTimer = mExecutorService.schedule(new IdleElapsed(), 20, TimeUnit.MINUTES);
    }
    private void stopIdleTimer(){
        mIdleTimer.cancel(true);
    }

    private void resetIdleTimer(){
        stopIdleTimer();
        startIdleTimer();
    }

    private class IdleElapsed implements Runnable {
        @Override
        public void run() {

            if(!isRecording) return;

            //TODO: analyze and copycat
        }
    }
}
