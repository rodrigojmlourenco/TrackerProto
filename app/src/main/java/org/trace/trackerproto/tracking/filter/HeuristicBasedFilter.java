package org.trace.trackerproto.tracking.filter;

import android.os.Bundle;
import android.util.Log;

import org.trace.trackerproto.tracking.storage.data.TraceLocation;
import org.trace.trackerproto.tracking.utils.LocationUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;


public class HeuristicBasedFilter {


    private ArrayList<HeuristicRule> heuristics;
    private HashMap<Class, Integer> heuristicsMap;

    public HeuristicBasedFilter(){
        heuristics = new ArrayList<>();
        heuristicsMap = new HashMap<>();
    }

    public void addNewHeuristic(HeuristicRule rule){
        this.heuristics.add(rule);
        this.heuristicsMap.put(rule.getClass(), heuristics.indexOf(rule));
    }

    public boolean isValidLocation(TraceLocation location){

        for(HeuristicRule r : heuristics)
            if(r.isOutlier(location))
                return false;

        return true;
    }

    public boolean isValidLocation(TraceLocation current, TraceLocation previous){

        for(HeuristicRule r : heuristics)
            if(r.isOutlier(current, previous))
                return false;

        return true;
    }

    public void updateHeuristic(HeuristicRule rule){
        int index = heuristicsMap.get(rule.getClass());
        heuristics.remove(index);
        heuristics.add(index, rule);
    }

    public interface HeuristicRule {

        String LOG_TAG = "Outlier";

        boolean isOutlier(TraceLocation location);

        boolean isOutlier(TraceLocation location, TraceLocation previous);

        boolean isOutlier(TraceLocation current, TraceLocation previous, TraceLocation earlierThan);
    }



    /**
     * Considers locations to be outliers whenever the location's accuracy
     * is over the specified accuracy threshold.
     */
    public static class AccuracyBasedHeuristicRule implements HeuristicRule{


        private float accuracyThreshold = 50;

        public AccuracyBasedHeuristicRule(float accuracyThreshold){
            this.accuracyThreshold = accuracyThreshold;
        }

        @Override
        public boolean isOutlier(TraceLocation location) {

            boolean isOutlier = location.getAccuracy() > accuracyThreshold;

            if(isOutlier) Log.e(LOG_TAG, "Too inaccurate");

            return  isOutlier;
        }

        @Override
        public boolean isOutlier(TraceLocation location, TraceLocation previous) {
            return  isOutlier(location);
        }

        @Override
        public boolean isOutlier(TraceLocation current, TraceLocation previous, TraceLocation earlierThan) {
            return isOutlier(current);
        }
    }


    /**
     * Considers locations to be outliers whenever the location's speed
     * is over the specified speed threshold.
     */
    public static class SpeedBasedHeuristicRule implements HeuristicRule {

        private float speedThreshold = 50;

        public SpeedBasedHeuristicRule(float speedThreshold){
            this.speedThreshold = speedThreshold;
        }

        @Override
        public boolean isOutlier(TraceLocation location) {

            boolean isOutlier = location.getSpeed() > speedThreshold;

            if(isOutlier) Log.e(LOG_TAG, "Too fast");

            return isOutlier;
        }

        @Override
        public boolean isOutlier(TraceLocation location, TraceLocation previous) {
            return  isOutlier(location);
        }

        @Override
        public boolean isOutlier(TraceLocation current, TraceLocation previous, TraceLocation earlierThan) {
            return isOutlier(current);
        }
    }

    /**
     * Considers locations to be outliers whenever the location's was pinpointed
     * by a GPS provider and with less than a specific number of satellites;
     */
    public static class SatelliteBasedHeuristicRule implements HeuristicRule {

        private int minSatellites = 4;

        public SatelliteBasedHeuristicRule(int minSatellites){
            this.minSatellites = minSatellites;
        }

        @Override
        public boolean isOutlier(TraceLocation location) {

            boolean isOutlier;
            Bundle extras = location.getExtras();

            if(extras == null) return false;

            isOutlier = extras.getInt("satellites", minSatellites) < minSatellites;

            if(isOutlier) Log.e(LOG_TAG, "Not enough satellites");

            return isOutlier;
        }

        @Override
        public boolean isOutlier(TraceLocation location, TraceLocation previous) {
            return  isOutlier(location);
        }

        @Override
        public boolean isOutlier(TraceLocation current, TraceLocation previous, TraceLocation earlierThan) {
            return isOutlier(current);
        }
    }

    /**
     * Similar to the SpeedBasedHeuristicRule, however, the speed in m/s is calculated
     * given the time it took to get from the current location, to the previously
     * registered one.
     */
    public static class CalculatedSpeedBasedHeuristicRule implements HeuristicRule {

        private final float speedThreshold;

        public CalculatedSpeedBasedHeuristicRule(float threshold){
            this.speedThreshold = threshold;
        }

        @Override
        public boolean isOutlier(TraceLocation location) {

            throw new UnsupportedOperationException("CalculatedSpeedBasedHeuristicRule can only be applied with at least two locations.");
        }

        @Override
        public boolean isOutlier(TraceLocation location, TraceLocation previous) {
            boolean isOutlier;
            long timeDeltaNanos = location.getElapsedRealtimeNanos() - previous.getElapsedRealtimeNanos();
            float travelledDistance = previous.distanceTo(location);

            float speedMS = travelledDistance / (TimeUnit.SECONDS.convert(timeDeltaNanos, TimeUnit.DAYS.NANOSECONDS));

            isOutlier = speedMS > speedThreshold;

            if(isOutlier)
                Log.i(LOG_TAG, "Calculated speed is too high");

            return isOutlier;
        }

        @Override
        public boolean isOutlier(TraceLocation current, TraceLocation previous, TraceLocation earlierThanPrevious) {
            return isOutlier(current, previous);
        }
    }

    /**
     * A location is considered to be an outlier if its accuracy, or error margin
     * overlaps the accuracy of the previously registered location, and the first presents
     * a lower accuracy.
     */
    public static class OverlappingLocationHeuristicRule implements HeuristicRule {

        @Override
        public boolean isOutlier(TraceLocation location) {
            throw new UnsupportedOperationException(OverlappingLocationHeuristicRule.class.getSimpleName()+" can only be applied with at least two locations.");

        }

        @Override
        public boolean isOutlier(TraceLocation location, TraceLocation previous) {
            boolean isOutlier =
                    LocationUtils.areOverlappingLocations(previous,location)
                        && location.getAccuracy() > previous.getAccuracy();



            if(isOutlier)
                Log.i(LOG_TAG, "Overlapping locations");

            return isOutlier;
        }

        @Override
        public boolean isOutlier(TraceLocation current, TraceLocation previous, TraceLocation earlierThan) {
            boolean isOutlier =
                    LocationUtils.areOverlappingLocations(previous,current)
                            && current.getAccuracy() > previous.getAccuracy();



            if(isOutlier)
                Log.i(LOG_TAG, "Overlapping locations");

            return isOutlier;
        }
    }

    public static class UnrealisticPassThroughSpeed implements HeuristicRule{

        private String errorMessage =
                getClass().getSimpleName()+" can only be applied with at least three known locations.";

        @Override
        public boolean isOutlier(TraceLocation location) {
            throw new UnsupportedOperationException(errorMessage);
        }

        @Override
        public boolean isOutlier(TraceLocation location, TraceLocation previous) {
            throw new UnsupportedOperationException(errorMessage);
        }

        @Override
        public boolean isOutlier(TraceLocation current, TraceLocation previous, TraceLocation earlierThan) {
            return false;
        }
    }

    //TODO: converter as locations em TraceLocations
    //TODO: os ultimos 2 devem ser casos especias uma vez que não correspondem necessáriamente ao modelo dos restantes.!!!
}
