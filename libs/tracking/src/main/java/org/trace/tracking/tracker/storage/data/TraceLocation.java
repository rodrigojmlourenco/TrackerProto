package org.trace.tracking.tracker.storage.data;

import android.location.Location;
import android.os.Build;
import android.os.Parcel;

import com.google.android.gms.location.DetectedActivity;
import com.google.gson.JsonObject;

import org.trace.tracking.TrackingConstants;
import org.trace.tracking.tracker.modules.activity.ActivityRecognitionModule;


public class TraceLocation extends Location{

    private String activityMode;

    public TraceLocation(){
        super("unknown");
    }

    public TraceLocation(Location location){
        super(location);
        this.activityMode = "unknown";

    }


    protected TraceLocation(Parcel in) {

        super("unknown");

        double latitude  = in.readDouble();
        double longitude = in.readDouble();
        double altitude  = in.readDouble();
        long time = in.readLong();
        long elapsedNanos = in.readLong();
        float accuracy  = in.readFloat();
        float speed     = in.readFloat();
        float bearing   = in.readFloat();
        String provider = in.readString();
        String activity = in.readString();

        setLatitude(latitude);
        setLongitude(longitude);
        setTime(time);
        setAccuracy(accuracy);
        setBearing(bearing);
        setAltitude(altitude);
        setSpeed(speed);
        setProvider(provider);
        setActivityMode(activity);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            setElapsedRealtimeNanos(elapsedNanos);
    }

    public static final Creator<TraceLocation> CREATOR = new Creator<TraceLocation>() {
        @Override
        public TraceLocation createFromParcel(Parcel in) {
            return new TraceLocation(in);
        }

        @Override
        public TraceLocation[] newArray(int size) {
            return new TraceLocation[size];
        }
    };

    @Override
    public long getElapsedRealtimeNanos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return super.getElapsedRealtimeNanos();
        }else
            return super.getTime() * 1000000;
    }


    public String getActivityMode() {
        return activityMode  == null ? "null" : activityMode;
    }

    public void setActivityMode(String activityMode) {
        this.activityMode = activityMode;
    }

    public void setActivityMode(DetectedActivity activity){

        JsonObject jsonActivity = new JsonObject();
        if(activity == null){
            jsonActivity.addProperty("type", "unknown");
            jsonActivity.addProperty("confidence", 100);
        }else{
            jsonActivity.addProperty("type", ActivityRecognitionModule.getActivityString(activity.getType()));
            jsonActivity.addProperty("confidence", activity.getConfidence());
        }

        this.activityMode = jsonActivity.toString();
    }

    public JsonObject getSerializableLocationAsJson(){
        JsonObject location = getMainAttributesAsJson();
        location.addProperty(TrackingConstants.location.ATTRIBUTES, getSecondaryAttributesAsJson().toString());
        return location;
    }

    public JsonObject getMainAttributesAsJson(){
        JsonObject attributes = new JsonObject();
        attributes.addProperty(TrackingConstants.location.LATITUDE, getLatitude());
        attributes.addProperty(TrackingConstants.location.LONGITUDE, getLongitude());
        attributes.addProperty(TrackingConstants.location.TIMESTAMP, getTime());
        return  attributes;
    }

    public JsonObject getSecondaryAttributesAsJson(){
        JsonObject attributes = new JsonObject();
        attributes.addProperty(TrackingConstants.location.attributes.ACCURACY, getAccuracy());
        attributes.addProperty(TrackingConstants.location.attributes.SPEED, getSpeed());
        attributes.addProperty(TrackingConstants.location.attributes.BEARING, getBearing());
        attributes.addProperty(TrackingConstants.location.attributes.ALTITUDE, getAltitude());
        attributes.addProperty(TrackingConstants.location.attributes.ELAPSED_NANOS, getElapsedRealtimeNanos());
        attributes.addProperty(TrackingConstants.location.attributes.PROVIDER, getProvider());
        attributes.addProperty(TrackingConstants.location.attributes.ACTIVITY, getActivityMode());
        return  attributes;
    }

    public void setSecondaryAttributes(JsonObject secondaryAttributes){

        if (secondaryAttributes.has(TrackingConstants.location.attributes.ACCURACY))
            setAccuracy(secondaryAttributes.get(TrackingConstants.location.attributes.ACCURACY).getAsFloat());

        if (secondaryAttributes.has(TrackingConstants.location.attributes.SPEED))
            setSpeed(secondaryAttributes.get(TrackingConstants.location.attributes.SPEED).getAsFloat());

        if (secondaryAttributes.has(TrackingConstants.location.attributes.BEARING))
            setBearing(secondaryAttributes.get(TrackingConstants.location.attributes.BEARING).getAsFloat());

        if (secondaryAttributes.has(TrackingConstants.location.attributes.ALTITUDE))
            setAltitude(secondaryAttributes.get(TrackingConstants.location.attributes.ALTITUDE).getAsFloat());

        if (secondaryAttributes.has(TrackingConstants.location.attributes.ELAPSED_NANOS)
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                setElapsedRealtimeNanos(secondaryAttributes.get(TrackingConstants.location.attributes.ELAPSED_NANOS).getAsLong());

        if (secondaryAttributes.has(TrackingConstants.location.attributes.PROVIDER))
            setProvider(secondaryAttributes.get(TrackingConstants.location.attributes.PROVIDER).getAsString());

        if (secondaryAttributes.has(TrackingConstants.location.attributes.ACTIVITY))
            activityMode = secondaryAttributes.get(TrackingConstants.location.attributes.ACTIVITY).getAsString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(getLatitude());
        dest.writeDouble(getLongitude());
        dest.writeDouble(getAltitude());
        dest.writeLong(getTime());
        dest.writeLong(getElapsedRealtimeNanos());
        dest.writeFloat(getAccuracy());
        dest.writeFloat(getSpeed());
        dest.writeFloat(getBearing());
        dest.writeString(getProvider());
        dest.writeString(activityMode);
    }
}