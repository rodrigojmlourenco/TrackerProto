package org.trace.tracker.settings;

import com.google.android.gms.location.LocationRequest;
import com.google.gson.JsonObject;

import org.trace.tracker.filter.HeuristicBasedFilter;

/**
 * @author Rodrigo Lourenço
 * @version 0.0
 *
 * The TrackingProfile class defines the major settings values for both the tracking
 * and uploading schemes.
 * <br>
 * <emph>Notes:</emph>
 * <ul>
 *     <li>This should only contain the tracking settings</li>
 * </ul>
 * <br>
 * <emph>TODOs:</emph>
 * <ul>
 *     <li>
 *         Because there are so many configuration possibilities this class should be more flexible.
 *         For instance, it could employ a key value approach
 *     </li>
 * </ul>
 */
public class TrackingProfile {

    //Fused TraceLocationModule
    /** The distance that should be traversed before any other location are accepted. Minimum 2m*/
    private int locationDisplacementThreshold   = 2; //meters

    /** The sampling rate employed in location tracking. Default is 3.5s*/
    private long locationInterval               = 3500;

    /** The fastest acceptable interval for location updates. Default is 1.5s */
    private long locationFastInterval           = 1500;

    /** The priority given for location updates (Accuracy vs Power). Default is High Accuracy */
    private int locationTrackingPriority        = LocationRequest.PRIORITY_HIGH_ACCURACY;

    /** Minimum accuracy required for a location to be acceptable. Default is 40m*/
    private float locationMinimumAccuracy       = 40f;

    /** Maximum acceptable speed for a location. Default is 200Km/h */
    private float locationMaximumSpeed          = 55.56f;

    private HeuristicBasedFilter[] locationEnabledOutlierFilters = new HeuristicBasedFilter[]{};

    //Activity Recognition
    /** The sampling rate employed in activity mode tracking. Default is 3s*/
    private long activityInterval = 3000;

    /** The minimum acceptable confidence for activity tracking. Default is 75% */
    private int activityMinimumConfidence = 75;

    private boolean isActiveOutlierRemoval = true;

    /**
     * Creates a new TrackingProfile with the default values.
     */
    public TrackingProfile(){}

    /**
     * Create a new TrackingProfile given a JsonObject that contains the key-value pairs.
     * @param jsonProfile The profile as a JsonObject.
     */
    public TrackingProfile(JsonObject jsonProfile){
        loadFromJsonProfile(jsonProfile);
    }

    /**
     * @return The location displacement threshold.
     */
    public int getLocationDisplacementThreshold() {
        return locationDisplacementThreshold;
    }

    /**
     * Sets the location displacement threshold.
     * @param locationDisplacementThreshold
     */
    public void setLocationDisplacementThreshold(int locationDisplacementThreshold) {
        this.locationDisplacementThreshold = locationDisplacementThreshold;
    }

    /**
     * @return The location sampling interval.
     */
    public long getLocationInterval() {
        return locationInterval;
    }

    /**
     * Sets the location sampling interval.
     * @param locationInterval
     */
    public void setLocationInterval(long locationInterval) {
        this.locationInterval = locationInterval;
    }

    /**
     * @return The location fastest sampling interval.
     */
    public long getLocationFastInterval() {
        return locationFastInterval;
    }

    /**
     * Sets the location fastest sampling interval.
     * @param locationFastInterval
     */
    public void setLocationFastInterval(long locationFastInterval) {
        this.locationFastInterval = locationFastInterval;
    }

    /**
     * @return The location's tracking priority, i.e. Accuracy vs Power
     * @See LocationRequest
     */
    public int getLocationTrackingPriority() {
        return locationTrackingPriority;
    }

    /**
     * Sets the location tracking priority.
     * @param locationTrackingPriority
     */
    public void setLocationTrackingPriority(int locationTrackingPriority) {
        this.locationTrackingPriority = locationTrackingPriority;
    }

    /**
     * @return The minimum acceptable accuracy.
     */
    public float getLocationMinimumAccuracy() {
        return locationMinimumAccuracy;
    }

    /**
     * Sets the minimum acceptable accuracy.
     * @param locationMinimumAccuracy
     */
    public void setLocationMinimumAccuracy(float locationMinimumAccuracy) {
        this.locationMinimumAccuracy = locationMinimumAccuracy;
    }

    /**
     * Sets the location's maximum acceptable speed.
     * @param locationMaximumSpeed
     */
    public void setLocationMaximumSpeed(float locationMaximumSpeed) {
        this.locationMaximumSpeed = locationMaximumSpeed;
    }


    /**
     * Returns the maximum acceptable speed.
     * @return
     */
    public float getLocationMaximumSpeed() {
        return locationMaximumSpeed;
    }


    public boolean isActiveOutlierRemoval() {
        return isActiveOutlierRemoval;
    }

    public void activateOutlierRemoval(boolean active) {
        this.isActiveOutlierRemoval = active;
    }

    /**
     * @return The activity mode tracking sampling rate.
     */
    public long getActivityInterval() {
        return activityInterval;
    }

    /**
     * Sets the activity mode tracking interval.
     * @param arInterval
     */
    public void setActivityRecognitionInterval(long arInterval) {
        this.activityInterval = arInterval;
    }

    /**
     * @return The activity mode minimum confidence.
     */
    public int getActivityMinimumConfidence() {
        return activityMinimumConfidence;
    }

    /**
     * Sets the activity mode minimum confidence.
     * @param minimumConfidence
     */
    public void setActivityMinimumConfidence(int minimumConfidence) {
        this.activityMinimumConfidence = minimumConfidence;
    }




    /* JSON Handling
    /* JSON Handling
    /* JSON Handling
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    private void loadFromJsonProfile(JsonObject profile){
        loadLocationProfileFromJson((JsonObject) profile.get(Constants.LOCATION));
        loadActivityRecognitionProfileFromJson((JsonObject) profile.get(Constants.ACTIVITY_RECOGNITION));
    }

    private void loadLocationProfileFromJson(JsonObject locationProfile){
        locationTrackingPriority = locationProfile.get(Constants.LOCATION_PRIORITY).getAsInt();
        locationInterval = locationProfile.get(Constants.LOCATION_INTERVAL).getAsLong();
        locationFastInterval = locationProfile.get(Constants.LOCATION_FAST_INTERVAL).getAsLong();
        locationMinimumAccuracy = locationProfile.get(Constants.LOCATION_ACCURACY).getAsFloat();
        locationMaximumSpeed    = locationProfile.get(Constants.LOCATION_SPEED).getAsFloat();
        locationDisplacementThreshold = locationProfile.get(Constants.LOCATION_DISPLACEMENT_THRESHOLD).getAsInt();
        isActiveOutlierRemoval = locationProfile.get(Constants.OUTLIER_REMOVAL).getAsBoolean();
    }

    private void loadActivityRecognitionProfileFromJson(JsonObject profile){
        activityInterval = profile.get(Constants.ACTIVITY_RECOGNITION_INTERVAL).getAsLong();
        activityMinimumConfidence = profile.get(Constants.ACTIVITY_RECOGNITION_CONFIDENCE).getAsInt();
    }


    private JsonObject getJsonLocationTrackingProfile(){
        JsonObject locationTrackingProfile = new JsonObject();

        locationTrackingProfile.addProperty(Constants.LOCATION_PRIORITY, locationTrackingPriority);
        locationTrackingProfile.addProperty(Constants.LOCATION_INTERVAL, locationInterval);
        locationTrackingProfile.addProperty(Constants.LOCATION_FAST_INTERVAL, locationFastInterval);
        locationTrackingProfile.addProperty(Constants.LOCATION_ACCURACY, locationMinimumAccuracy);
        locationTrackingProfile.addProperty(Constants.LOCATION_SPEED, locationMaximumSpeed);
        locationTrackingProfile.addProperty(Constants.LOCATION_DISPLACEMENT_THRESHOLD, locationDisplacementThreshold);
        locationTrackingProfile.addProperty(Constants.OUTLIER_REMOVAL, isActiveOutlierRemoval);

        return locationTrackingProfile;
    }

    /**
     * Returns the activity recognition settings as a JsonObject
     * @return
     */
    public JsonObject getJsonActivityRecognitionProfile(){
        JsonObject profile = new JsonObject();
        profile.addProperty(Constants.ACTIVITY_RECOGNITION_INTERVAL, activityInterval);
        profile.addProperty(Constants.ACTIVITY_RECOGNITION_CONFIDENCE, activityMinimumConfidence);
        return profile;
    }

    /**
     * Returns the location tracking settings as a JsonObject
     * @return
     */
    public JsonObject getJsonTrackingProfile(){
        JsonObject trackingProfile = new JsonObject();

        trackingProfile.add(Constants.LOCATION, getJsonLocationTrackingProfile());
        trackingProfile.add(Constants.ACTIVITY_RECOGNITION, getJsonActivityRecognitionProfile());

        return trackingProfile;
    }



    @Override
    public String toString() {
        return getJsonTrackingProfile().toString();
    }

    /**
     * Keys corresponding to the key-value pairs.
     */
    public interface Constants {
        String LOCATION = "location";
        String LOCATION_INTERVAL        = "interval";
        String LOCATION_FAST_INTERVAL   = "fastInterval";
        String LOCATION_PRIORITY        = "priority";
        String LOCATION_ACCURACY        = "accuracy";
        String LOCATION_SPEED           = "speed";
        String LOCATION_SATELLITES      = "satellites";

        String LOCATION_DISPLACEMENT_THRESHOLD = "displacementThreshold";
        String LOCATION_OUTLIER_FILTERS = "outlierFilters";

        String ACTIVITY_RECOGNITION = "activity";
        String ACTIVITY_RECOGNITION_INTERVAL = "interval";
        String ACTIVITY_RECOGNITION_CONFIDENCE = "confidence";

        String UPLOADING = "uploading";
        String UPLOADING_WIFI_ONLY = "wifyOnly";
        String UPLOADING_DEMAND_ONLY = "onlyOnDemand";

        String OUTLIER_REMOVAL = "removeOutliers";
    }
}
