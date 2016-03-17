package org.trace.trackerproto.tracking.filter;

import android.util.Log;

import org.trace.trackerproto.tracking.storage.data.TraceLocation;


public class UnrealisticPassThroughSpeedOutlierFilter implements HeuristicBasedFilter.HeuristicRule{

    private double threshold = 50; // m/s

    public UnrealisticPassThroughSpeedOutlierFilter(){}

    public UnrealisticPassThroughSpeedOutlierFilter(double threshold){
        this.threshold = threshold;
    }

    @Override
    public boolean isOutlier(TraceLocation location) {
        throw new UnsupportedOperationException(UnrealisticPassThroughSpeedOutlierFilter.class.getSimpleName());
    }

    @Override
    public boolean isOutlier(TraceLocation location, TraceLocation previous) {
        throw new UnsupportedOperationException(UnrealisticPassThroughSpeedOutlierFilter.class.getSimpleName());
    }

    @Override
    public boolean isOutlier(TraceLocation current, TraceLocation previous, TraceLocation earlierThan) {
        boolean isOutlier;
        double travelledDistance, elapsedTime;

        travelledDistance = earlierThan.distanceTo(previous) + previous.distanceTo(current);

        elapsedTime = current.getElapsedRealtimeNanos() - earlierThan.getElapsedRealtimeNanos();
        elapsedTime = elapsedTime / 1000000000.0;

        isOutlier = (travelledDistance / elapsedTime) > threshold;

        if(isOutlier)
            Log.e(LOG_TAG, "Unrealistic pass through speed.");

        return isOutlier;
    }
}
