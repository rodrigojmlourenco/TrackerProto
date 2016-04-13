package org.trace.tracking.tracker.filter;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.trace.tracking.TrackingConstants;
import org.trace.tracking.tracker.storage.data.TraceLocation;

import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * Specialized queue designed to hold TraceLocation objects and ease the application of certain
 * heuristic-based outlier detection filters. Additionally, this queue is also designed to account
 * for the possibility of asynchronous accesses to said queue.
 *
 * Finally, this queue also holds some additional metadata information regarding the traced track,
 * namely the elapsed time and travelled distance.
 *
 * @see TraceLocation
 *
 * @author Rodrigo Lourenço
 * @version 1.0
 */
public class OutlierFilteringLocationQueue {

    public static final int QUEUE_MAX_SIZE = 2;
    private static final String LOG_TAG = "Outlier";

    private final Object mLock = new Object();
    private LinkedList<TraceLocation> mLocationQueue;

    private final Context mContext;

    private HeuristicBasedFilter mOutlierFilter;
    private UnrealisticPassThroughSpeedOutlierFilter mUnrealisticPassThroughSpeedOutlierFilter;


    public OutlierFilteringLocationQueue(Context context){
        this.mLocationQueue = new LinkedList<>();

        this.mContext = context;

        this.mOutlierFilter = new HeuristicBasedFilter();
        this.mUnrealisticPassThroughSpeedOutlierFilter = new UnrealisticPassThroughSpeedOutlierFilter();
    }



    public void addHeuristicRule(HeuristicBasedFilter.HeuristicRule rule){
        this.mOutlierFilter.addNewHeuristic(rule);
    }

    public void clearHeuristicRules(){
        this.mOutlierFilter = new HeuristicBasedFilter();
    }

    public void updateHeuristicRule(HeuristicBasedFilter.HeuristicRule rule){
        mOutlierFilter.updateHeuristic(rule);
    }

    public void addSpecialHeuristicRule(){
        //TODO:
    }


    /**
     * Adds a new location to the location queue. This location will also be subject
     * to the specified, upon creation, outlier detection filters.
     *
     * @param location The new TraceLocation
     */
    public void addLocation(TraceLocation location){

        TraceLocation previous;

        //Step 1 - Validate the location
        //Step 1a - Run the simple outlier filters
        try {
            previous = mLocationQueue.getLast();

            if(!mOutlierFilter.isValidLocation(location,previous))
                return;

        }catch (NoSuchElementException e){
            try {
                if (!mOutlierFilter.isValidLocation(location))
                    return;
            }catch (UnsupportedOperationException e1){
                Log.i(LOG_TAG, e1.getMessage());
            }
        }

        //Step 1b - Run the complex filters (i.e. the TripZoom filters)
        //These have to be directly handled in the code, as they do not necessarily imply the the
        //current location is the outlier, but previous location may be as well.
        synchronized (mLock){
            if(mLocationQueue.size() >= QUEUE_MAX_SIZE){

                int index = mLocationQueue.size() - 1;

                if(mUnrealisticPassThroughSpeedOutlierFilter.isOutlier(
                        location,
                        mLocationQueue.get(index),
                        mLocationQueue.get(index-1))){

                    mLocationQueue.remove(index);
                }
            }
        }

        Log.i(LOG_TAG, "Location was accepted as valid... "+location.toString());


        //Step 2 -  Add the location to the queue
        //          If the queue's size has reached the maximum size, store the first
        TraceLocation validLocation = null;
        synchronized (mLock){

            if(mLocationQueue.size() >= QUEUE_MAX_SIZE)
                validLocation = mLocationQueue.removeFirst();

            mLocationQueue.addLast(location);
        }

        if(validLocation!=null)
            broadcastLocation(validLocation);
    }


    private void broadcastLocation(TraceLocation location){
        Intent localIntent = new Intent(TrackingConstants.tracker.COLLECT_LOCATIONS_ACTION);
        localIntent.putExtra(TrackingConstants.tracker.LOCATION_EXTRA, location);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(localIntent);
    }

    /**
     * Removes any location still stored in the queue.
     */
    public void clearQueue(){
        synchronized (mLock){
            mLocationQueue.clear();
        }
    }

    /**
     * If there are any remaining location, which have not yet been stored,
     * this method removes those locations from the queue and stores them in
     * persistent memory.
     * <br>
     * Additionally, this method also updates the travelled distance and elapsed time
     * associated with this specific tracking session.
     */
    public void clearAndStoreQueue(){
        synchronized (mLock) {

            if(mLocationQueue.isEmpty())
                return;

            do {

                broadcastLocation(mLocationQueue.removeFirst());


            }while (!mLocationQueue.isEmpty());
        }
    }
}
