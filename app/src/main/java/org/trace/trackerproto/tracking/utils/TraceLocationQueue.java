package org.trace.trackerproto.tracking.utils;

import org.trace.trackerproto.store.TRACEStoreApiClient;
import org.trace.trackerproto.tracking.filter.HeuristicBasedFilter;
import org.trace.trackerproto.tracking.storage.PersistentTrackStorage;
import org.trace.trackerproto.tracking.storage.data.TraceLocation;

import java.util.LinkedList;

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
public class TraceLocationQueue {

    public static final int QUEUE_MAX_SIZE = 3;

    private final Object mLock = new Object();
    private LinkedList<TraceLocation> mLocationQueue;

    private final PersistentTrackStorage mTrackStorage;

    private final HeuristicBasedFilter mOutlierFilter;

    private double elapsedTime, travelledDistance;

    public TraceLocationQueue(PersistentTrackStorage trackStorage, HeuristicBasedFilter filter){
        this.mLocationQueue = new LinkedList<>();
        this.mTrackStorage  = trackStorage;
        this.mOutlierFilter = filter;

        this.elapsedTime        = 0;
        this.travelledDistance  = 0;
    }

    /**
     * Retrieves the most current location, in the location queue.
     * @return The most recent TraceLocation
     */
    public TraceLocation getCurrentLocation(){
        synchronized (mLock){
            return mLocationQueue.getLast();
        }
    }


    /**
     * Adds a new location to the location queue. This location will also be subject
     * to the specified, upon creation, outlier detection filters.
     *
     * @param location The new TraceLocation
     */
    public void addLocation(TraceLocation location){

        //Step 1 - Run the outlier filters
        //TODO:Run the outlier filters

        //Step 2 -  Add the location to the queue
        //          If the queue's size has reached the maximum size, store the first
        TraceLocation store = null, aux;
        synchronized (mLock){

            if(!mLocationQueue.isEmpty()){
                aux = mLocationQueue.getLast();
                travelledDistance += aux.distanceTo(location);
                elapsedTime += location.getTime() - aux.getTime();
            }

            if(mLocationQueue.size() >= QUEUE_MAX_SIZE)
                store = mLocationQueue.removeFirst();

            mLocationQueue.add(location);
        }

        if(store != null)
            mTrackStorage.storeLocation(
                    store,
                    TRACEStoreApiClient.getSessionId(),
                    TRACEStoreApiClient.isValidSession());
    }

    /**
     * If there are any remaining location, which have not yet been stored,
     * this method removes those locations from the queue and stores them in
     * persistent memory.
     */
    public void clearAndStoreQueue(){
        synchronized (mLock) {
            do {

                mTrackStorage.storeLocation(
                        mLocationQueue.removeFirst(),
                        TRACEStoreApiClient.getSessionId(),
                        TRACEStoreApiClient.isValidSession());

            }while (!mLocationQueue.isEmpty());
        }
    }

    public double getElapsedTime() {
        return elapsedTime;
    }

    public double getTravelledDistance() {
        return travelledDistance;
    }
}
