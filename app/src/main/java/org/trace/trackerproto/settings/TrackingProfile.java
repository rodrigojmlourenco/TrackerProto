package org.trace.trackerproto.settings;

import com.google.android.gms.location.LocationRequest;
import com.google.gson.JsonArray;
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
    private boolean locationDisplacementFilterEnabled = false;
    private float locationDisplacementThreshold = 2.5f; //meters
    private long locationInterval = 10000;
    private long locationFastInterval = 5000;
    private int locationTrackingPriority = LocationRequest.PRIORITY_HIGH_ACCURACY;
    private HeuristicBasedFilter[] locationEnabledOutlierFilters = new HeuristicBasedFilter[]{};

    //Activity Recognition
    private long arInterval     = 1000;
    //Uploading
    private boolean wifiOnly    = false;

    public TrackingProfile(){}

    public TrackingProfile(JsonObject jsonProfile){
        JsonObject locationProfile = (JsonObject) jsonProfile.get(Constants.LOCATION);
        JsonObject activityRecogProfile = (JsonObject) jsonProfile.get(Constants.ACTIVITY_RECOGNITION);
        JsonObject uploadingProfile = (JsonObject) jsonProfile.get(Constants.UPLOADING);

        //Setup Location Profile
        setLocationDisplacementFilterEnabled(locationProfile.get(Constants.LOCATION_DISPLACEMENT_ENABLED).getAsBoolean());
        setLocationDisplacementThreshold(locationProfile.get(Constants.LOCATION_DISPLACEMENT_THRESHOLD).getAsFloat());
        setLocationInterval(locationProfile.get(Constants.LOCATION_INTERVAL).getAsLong());
        setLocationFastInterval(locationProfile.get(Constants.LOCATION_FAST_INTERVAL).getAsLong());
        setLocationTrackingPriority(locationProfile.get(Constants.LOCATION_PRIORITIES).getAsInt());
        //TODO: setup the location outlier filter, some depend on constants;

        //Setup the Activity Recognition Profile
        setActivityRecognitionInterval(activityRecogProfile.get(Constants.ACTIVITY_RECOGNITION_INTERVAL).getAsLong());

        //Setup the Uploading Profile
        setWifiOnly(uploadingProfile.get(Constants.UPLOADING_WIFI_ONLY).getAsBoolean());

    }

    public boolean isLocationDisplacementFilterEnabled() {
        return locationDisplacementFilterEnabled;
    }

    public void setLocationDisplacementFilterEnabled(boolean locationDisplacementFilterEnabled) {
        this.locationDisplacementFilterEnabled = locationDisplacementFilterEnabled;
    }

    public float getLocationDisplacementThreshold() {
        return locationDisplacementThreshold;
    }

    public void setLocationDisplacementThreshold(float locationDisplacementThreshold) {
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

    public boolean isWifiOnly() {
        return wifiOnly;
    }

    public void setWifiOnly(boolean wifiOnly) {
        this.wifiOnly = wifiOnly;
    }

    private JsonObject getJsonLocationTrackingProfile(){
        JsonObject locationTrackingProfile = new JsonObject();
        JsonArray outlierFilters = new JsonArray();


        locationTrackingProfile.addProperty(Constants.LOCATION_INTERVAL, locationInterval);
        locationTrackingProfile.addProperty(Constants.LOCATION_FAST_INTERVAL, locationFastInterval);
        locationTrackingProfile.addProperty(Constants.LOCATION_PRIORITIES, locationTrackingPriority);
        locationTrackingProfile.addProperty(Constants.LOCATION_DISPLACEMENT_ENABLED, locationDisplacementFilterEnabled);
        locationTrackingProfile.addProperty(Constants.LOCATION_DISPLACEMENT_THRESHOLD, locationDisplacementThreshold);

        for(int i=0; i < locationEnabledOutlierFilters.length; i++)
            outlierFilters.add(locationEnabledOutlierFilters[i].getClass().getCanonicalName());

        locationTrackingProfile.add(Constants.LOCATION_OUTLIER_FILTERS, outlierFilters);

        return locationTrackingProfile;
    }

    public JsonObject getJsonActivityRecognitionProfile(){
        JsonObject profile = new JsonObject();
        profile.addProperty(Constants.ACTIVITY_RECOGNITION_INTERVAL, arInterval);
        return profile;
    }

    public JsonObject getJsonUploadingProfile(){
        JsonObject profile = new JsonObject();
        profile.addProperty(Constants.UPLOADING_WIFI_ONLY, wifiOnly);
        return profile;
    }

    public JsonObject getJsonTrackingProfile(){
        JsonObject trackingProfile = new JsonObject();

        trackingProfile.add(Constants.LOCATION, getJsonTrackingProfile());
        trackingProfile.add(Constants.ACTIVITY_RECOGNITION, getJsonActivityRecognitionProfile());
        trackingProfile.add(Constants.UPLOADING, getJsonUploadingProfile());

        return trackingProfile;
    }

    public interface Constants {
        public final String LOCATION = "location";
        public final String LOCATION_INTERVAL = "interval";
        public final String LOCATION_FAST_INTERVAL = "fastInterval";
        public final String LOCATION_PRIORITIES = "priority";
        public final String LOCATION_DISPLACEMENT_ENABLED = "displacementFilter";
        public final String LOCATION_DISPLACEMENT_THRESHOLD = "displacementThreshold";
        public final String LOCATION_OUTLIER_FILTERS = "outlierFilters";

        public final String ACTIVITY_RECOGNITION = "activity";
        public final String ACTIVITY_RECOGNITION_INTERVAL = "interval";

        public final String UPLOADING = "uploading";
        public final String UPLOADING_WIFI_ONLY = "wifyOnly";
    }
}
