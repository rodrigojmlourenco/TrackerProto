package org.trace.trackerproto.tracking.storage.data;

import android.location.Location;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.location.DetectedActivity;
import com.google.gson.JsonObject;

import org.trace.trackerproto.Constants;
import org.trace.trackerproto.TrackerProto;
import org.trace.trackerproto.tracking.modules.activity.ActivityConstants;

import java.io.Serializable;


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
            jsonActivity.addProperty("type", ActivityConstants.getActivityString(TrackerProto.getInstance(), activity.getType()));
            jsonActivity.addProperty("confidence", activity.getConfidence());
        }

        this.activityMode = jsonActivity.toString();
    }

    public JsonObject getSerializableLocationAsJson(){
        JsonObject location = getMainAttributesAsJson();
        location.add(Constants.store.ATTRIBUTES, getSecondaryAttributesAsJson());
        return location;
    }

    public JsonObject getMainAttributesAsJson(){
        JsonObject attributes = new JsonObject();
        attributes.addProperty(Constants.store.LATITUDE, getLatitude());
        attributes.addProperty(Constants.store.LONGITUDE, getLongitude());
        attributes.addProperty(Constants.store.TIMESTAMP, getTime());
        return  attributes;
    }

    public JsonObject getSecondaryAttributesAsJson(){
        JsonObject attributes = new JsonObject();
        attributes.addProperty(Constants.store.attributes.ACCURACY, getAccuracy());
        attributes.addProperty(Constants.store.attributes.SPEED, getSpeed());
        attributes.addProperty(Constants.store.attributes.BEARING, getBearing());
        attributes.addProperty(Constants.store.attributes.ALTITUDE, getAltitude());
        attributes.addProperty(Constants.store.attributes.ELAPSED_NANOS, getElapsedRealtimeNanos());
        attributes.addProperty(Constants.store.attributes.PROVIDER, getProvider());
        attributes.addProperty(Constants.store.attributes.ACTIVITY, getActivityMode());
        return  attributes;
    }

    public void setSecondaryAttributes(JsonObject secondaryAttributes){

        if (secondaryAttributes.has(Constants.store.attributes.ACCURACY))
            setAccuracy(secondaryAttributes.get(Constants.store.attributes.ACCURACY).getAsFloat());

        if (secondaryAttributes.has(Constants.store.attributes.SPEED))
            setSpeed(secondaryAttributes.get(Constants.store.attributes.SPEED).getAsFloat());

        if (secondaryAttributes.has(Constants.store.attributes.BEARING))
            setBearing(secondaryAttributes.get(Constants.store.attributes.BEARING).getAsFloat());

        if (secondaryAttributes.has(Constants.store.attributes.ALTITUDE))
            setAltitude(secondaryAttributes.get(Constants.store.attributes.ALTITUDE).getAsFloat());

        if (secondaryAttributes.has(Constants.store.attributes.ELAPSED_NANOS)
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                setElapsedRealtimeNanos(secondaryAttributes.get(Constants.store.attributes.ELAPSED_NANOS).getAsLong());

        if (secondaryAttributes.has(Constants.store.attributes.PROVIDER))
            setProvider(secondaryAttributes.get(Constants.store.attributes.PROVIDER).getAsString());

        if (secondaryAttributes.has(Constants.store.attributes.ACTIVITY))
            activityMode = secondaryAttributes.get(Constants.store.attributes.ACTIVITY).getAsString();
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