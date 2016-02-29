package org.trace.trackerproto.tracking.filter;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import org.trace.trackerproto.tracking.utils.LocationUtils;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;


public class HeuristicBasedFilter {


    private LinkedList<HeuristicRule> heuristics;

    public HeuristicBasedFilter(){
        heuristics = new LinkedList<>();
    }

    public void addNewHeuristic(HeuristicRule rule){
        this.heuristics.add(rule);
    }

    public boolean isValidLocation(Location location){

        for(HeuristicRule r : heuristics)
            if(r.isOutlier(location))
                return false;

        return true;
    }


    public interface HeuristicRule {

        String LOG_TAG = "Outlier";

        boolean isOutlier(Location location);

        boolean isOutlier(Location location, Location previous);
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
        public boolean isOutlier(Location location) {

            boolean isOutlier = location.getAccuracy() > accuracyThreshold;

            if(isOutlier) Log.e(LOG_TAG, "Too inaccurate");

            return  isOutlier;
        }

        @Override
        public boolean isOutlier(Location location, Location previous) {
            return  isOutlier(location);
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
        public boolean isOutlier(Location location) {

            boolean isOutlier = location.getSpeed() > speedThreshold;

            if(isOutlier) Log.e(LOG_TAG, "Too fast");

            return isOutlier;
        }

        @Override
        public boolean isOutlier(Location location, Location previous) {
            return  isOutlier(location);
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
        public boolean isOutlier(Location location) {

            boolean isOutlier;
            Bundle extras = location.getExtras();

            if(extras == null) return false;

            isOutlier = extras.getInt("satellites", minSatellites) < minSatellites;

            if(isOutlier) Log.e(LOG_TAG, "Not enough satellites");

            return isOutlier;
        }

        @Override
        public boolean isOutlier(Location location, Location previous) {
            return  isOutlier(location);
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
        public boolean isOutlier(Location location) {
            return false;
        }

        @Override
        public boolean isOutlier(Location location, Location previous) {
            long timeDeltaNanos = location.getElapsedRealtimeNanos() - previous.getElapsedRealtimeNanos();
            float travelledDistance = previous.distanceTo(location);

            float speedMS = travelledDistance / (TimeUnit.SECONDS.convert(timeDeltaNanos, TimeUnit.DAYS.NANOSECONDS));

            return speedMS > speedThreshold;
        }
    }

    /**
     * A location is considered to be an outlier if its accuracy, or error margin
     * overlaps the accuracy of the previously registered location, and the first presents
     * a lower accuracy.
     */
    public static class OverlappingLocationHeuristicRule implements HeuristicRule {

        @Override
        public boolean isOutlier(Location location) {
            return false;
        }

        @Override
        public boolean isOutlier(Location location, Location previous) {
            return LocationUtils.areOverlappingLocations(previous,location);
        }
    }
}
