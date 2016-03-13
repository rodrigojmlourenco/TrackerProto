package org.trace.trackerproto.settings;

import com.google.android.gms.location.LocationRequest;
import com.google.gson.JsonObject;

import org.trace.trackerproto.tracking.filter.HeuristicBasedFilter;

/**
 * @author Rodrigo Lourenço
 * @version 0.0
 *
 * The TrackingProfile class defines the major settings values for both the tracking
 * and uploading schemes.
 */
public class TrackingProfile {

    //Fused Location Module
    private int locationDisplacementThreshold   = 2; //meters
    private long locationInterval               = 3500;
    private long locationFastInterval           = 1500;
    private int locationTrackingPriority        = LocationRequest.PRIORITY_HIGH_ACCURACY;
    private float locationMinimumAccuracy       = 40f;
    private float locationMaximumSpeed          = 55.56f;
    private HeuristicBasedFilter[] locationEnabledOutlierFilters = new HeuristicBasedFilter[]{};

    //Activity Recognition
    private long arInterval             = 3000;
    private int arMinimumConfidence     = 75;

    //Uploading
    private boolean wifiOnly    = false;
    private boolean onDemandOnly= false;

    public TrackingProfile(){}

    public TrackingProfile(JsonObject jsonProfile){
        loadFromJsonProfile(jsonProfile);
    }

    public int getLocationDisplacementThreshold() {
        return locationDisplacementThreshold;
    }

    public void setLocationDisplacementThreshold(int locationDisplacementThreshold) {
        this.locationDisplacementThreshold = locationDisplacementThreshold;
    }

    public long getLocationInterval() {
        return locationInterval;
    }

    public void setLocationInterval(long locationInterval) {
        this.locationInterval = locationInterval;
    }

    public long getLocationFastInterval() {
        return locationFastInterval;
    }

    public void setLocationFastInterval(long locationFastInterval) {
        this.locationFastInterval = locationFastInterval;
    }

    public int getLocationTrackingPriority() {
        return locationTrackingPriority;
    }

    public void setLocationTrackingPriority(int locationTrackingPriority) {
        this.locationTrackingPriority = locationTrackingPriority;
    }

    public float getLocationMinimumAccuracy() {
        return locationMinimumAccuracy;
    }

    public void setLocationMinimumAccuracy(float locationMinimumAccuracy) {
        this.locationMinimumAccuracy = locationMinimumAccuracy;
    }

    public void setLocationMaximumSpeed(float locationMaximumSpeed) {
        this.locationMaximumSpeed = locationMaximumSpeed;
    }

    public float getLocationMaximumSpeed() {
        return locationMaximumSpeed;
    }

    public HeuristicBasedFilter[] getLocationEnabledOutlierFilters() {
        return locationEnabledOutlierFilters;
    }

    public void setLocationEnabledOutlierFilters(HeuristicBasedFilter[] locationEnabledOutlierFilters) {
        this.locationEnabledOutlierFilters = locationEnabledOutlierFilters;
    }

    public long getArInterval() {
        return arInterval;
    }

    public void setActivityRecognitionInterval(long arInterval) {
        this.arInterval = arInterval;
    }

    public int getActivityMinimumConfidence() {
        return arMinimumConfidence;
    }

    public void setActivityMinimumConfidence(int minimumConfidence) {
        this.arMinimumConfidence = minimumConfidence;
    }

    public boolean isWifiOnly() {
        return wifiOnly;
    }

    public void setWifiOnly(boolean wifiOnly) {
        this.wifiOnly = wifiOnly;

    }

    public boolean isOnDemandOnly() {
        return onDemandOnly;
    }

    public void setOnDemandOnly(boolean onDemandOnly) {
        this.onDemandOnly = onDemandOnly;
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
        loadUpladingProfileFromJson((JsonObject) profile.get(Constants.UPLOADING));
    }

    private void loadLocationProfileFromJson(JsonObject locationProfile){
        locationTrackingPriority = locationProfile.get(Constants.LOCATION_PRIORITY).getAsInt();
        locationInterval = locationProfile.get(Constants.LOCATION_INTERVAL).getAsLong();
        locationFastInterval = locationProfile.get(Constants.LOCATION_FAST_INTERVAL).getAsLong();
        locationMinimumAccuracy = locationProfile.get(Constants.LOCATION_ACCURACY).getAsFloat();
        locationMaximumSpeed    = locationProfile.get(Constants.LOCATION_SPEED).getAsFloat();
        locationDisplacementThreshold = locationProfile.get(Constants.LOCATION_DISPLACEMENT_THRESHOLD).getAsInt();
    }

    private void loadActivityRecognitionProfileFromJson(JsonObject profile){
        arInterval = profile.get(Constants.ACTIVITY_RECOGNITION_INTERVAL).getAsLong();
        arMinimumConfidence = profile.get(Constants.ACTIVITY_RECOGNITION_CONFIDENCE).getAsInt();
    }

    private void loadUpladingProfileFromJson(JsonObject profile){
        wifiOnly = profile.get(Constants.UPLOADING_WIFI_ONLY).getAsBoolean();
        onDemandOnly = profile.get(Constants.UPLOADING_DEMAND_ONLY).getAsBoolean();
    }


    private JsonObject getJsonLocationTrackingProfile(){
        JsonObject locationTrackingProfile = new JsonObject();

        locationTrackingProfile.addProperty(Constants.LOCATION_PRIORITY, locationTrackingPriority);
        locationTrackingProfile.addProperty(Constants.LOCATION_INTERVAL, locationInterval);
        locationTrackingProfile.addProperty(Constants.LOCATION_FAST_INTERVAL, locationFastInterval);
        locationTrackingProfile.addProperty(Constants.LOCATION_ACCURACY, locationMinimumAccuracy);
        locationTrackingProfile.addProperty(Constants.LOCATION_SPEED, locationMaximumSpeed);
        locationTrackingProfile.addProperty(Constants.LOCATION_DISPLACEMENT_THRESHOLD, locationDisplacementThreshold);

        return locationTrackingProfile;
    }

    public JsonObject getJsonActivityRecognitionProfile(){
        JsonObject profile = new JsonObject();
        profile.addProperty(Constants.ACTIVITY_RECOGNITION_INTERVAL, arInterval);
        profile.addProperty(Constants.ACTIVITY_RECOGNITION_CONFIDENCE, arMinimumConfidence);
        return profile;
    }

    public JsonObject getJsonUploadingProfile(){
        JsonObject profile = new JsonObject();
        profile.addProperty(Constants.UPLOADING_WIFI_ONLY, wifiOnly);
        profile.addProperty(Constants.UPLOADING_DEMAND_ONLY, onDemandOnly);
        return profile;
    }

    public JsonObject getJsonTrackingProfile(){
        JsonObject trackingProfile = new JsonObject();

        trackingProfile.add(Constants.LOCATION, getJsonLocationTrackingProfile());
        trackingProfile.add(Constants.ACTIVITY_RECOGNITION, getJsonActivityRecognitionProfile());
        trackingProfile.add(Constants.UPLOADING, getJsonUploadingProfile());

        return trackingProfile;
    }





    @Override
    public String toString() {
        return getJsonTrackingProfile().toString();
    }

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

    }
}
