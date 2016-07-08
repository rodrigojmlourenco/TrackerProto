package org.trace.tracker.storage.data;

import android.location.Location;
import android.os.Build;
import android.os.Parcel;

import com.google.android.gms.location.DetectedActivity;
import com.google.gson.JsonObject;

import org.trace.tracker.modules.activity.ActivityRecognitionModule;


public class TraceLocation extends Location{

    private String activityMode;
    private boolean isCorner;

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
        location.addProperty(Attributes.ATTRIBUTES, getSecondaryAttributesAsJson().toString());
        return location;
    }

    public JsonObject getMainAttributesAsJson(){
        JsonObject attributes = new JsonObject();
        attributes.addProperty(Attributes.LATITUDE, getLatitude());
        attributes.addProperty(Attributes.LONGITUDE, getLongitude());
        attributes.addProperty(Attributes.TIMESTAMP, getTime());
        return  attributes;
    }

    public JsonObject getSecondaryAttributesAsJson(){
        JsonObject attributes = new JsonObject();
        attributes.addProperty(SecondaryAttributes.ACCURACY, getAccuracy());
        attributes.addProperty(SecondaryAttributes.SPEED, getSpeed());
        attributes.addProperty(SecondaryAttributes.BEARING, getBearing());
        attributes.addProperty(SecondaryAttributes.ALTITUDE, getAltitude());
        attributes.addProperty(SecondaryAttributes.ELAPSED_NANOS, getElapsedRealtimeNanos());
        attributes.addProperty(SecondaryAttributes.PROVIDER, getProvider());
        attributes.addProperty(SecondaryAttributes.ACTIVITY, getActivityMode());
        return  attributes;
    }

    public void setSecondaryAttributes(JsonObject secondaryAttributes){

        if (secondaryAttributes.has(SecondaryAttributes.ACCURACY))
            setAccuracy(secondaryAttributes.get(SecondaryAttributes.ACCURACY).getAsFloat());

        if (secondaryAttributes.has(SecondaryAttributes.SPEED))
            setSpeed(secondaryAttributes.get(SecondaryAttributes.SPEED).getAsFloat());

        if (secondaryAttributes.has(SecondaryAttributes.BEARING))
            setBearing(secondaryAttributes.get(SecondaryAttributes.BEARING).getAsFloat());

        if (secondaryAttributes.has(SecondaryAttributes.ALTITUDE))
            setAltitude(secondaryAttributes.get(SecondaryAttributes.ALTITUDE).getAsFloat());

        if (secondaryAttributes.has(SecondaryAttributes.ELAPSED_NANOS)
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                setElapsedRealtimeNanos(secondaryAttributes.get(SecondaryAttributes.ELAPSED_NANOS).getAsLong());

        if (secondaryAttributes.has(SecondaryAttributes.PROVIDER))
            setProvider(secondaryAttributes.get(SecondaryAttributes.PROVIDER).getAsString());

        if (secondaryAttributes.has(SecondaryAttributes.ACTIVITY))
            activityMode = secondaryAttributes.get(SecondaryAttributes.ACTIVITY).getAsString();
    }

    /**
     * Check if the current position vs the last position is a corner (heading change of 30 degrees)
     * @author Miley
     * @param lastPositionBearing
     * @return
     */
    public boolean isCorner(float lastPositionBearing) {

        if (getSpeed() < 0.5)
            return false;
        if (getBearing() < 0)
            return false;


        float diffHeading = Math.abs(lastPositionBearing - getBearing());

        if (diffHeading > 180)
            diffHeading = 360 - diffHeading;

        if (diffHeading > 30)
            return true;

        return false;
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

    /*
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    public interface Attributes {
        String  LATITUDE    = "latitude",
                LONGITUDE   = "longitude",
                TIMESTAMP   = "timestamp",
                ATTRIBUTES  = "attributes";
    }

    public interface SecondaryAttributes {
        String  ACTIVITY= "activity",
                BEARING = "bearing",
                ALTITUDE= "altitude",
                SPEED   = "speed",
                ACCURACY= "accuracy",
                PROVIDER= "provider",
                ELAPSED_NANOS = "elapsedNanos";
    }


}