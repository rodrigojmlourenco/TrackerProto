package org.trace.modalityrecognizer.data;

import com.google.android.gms.location.DetectedActivity;
import com.google.gson.JsonObject;

/**
 * A TraceModality object is characterized by the user's modality, the confidence associated with
 * that specific modality and the time at which the modality was recognized.
 * <br><br>
 * The library was designed to support a specific set of modalities.
 *
 * @see Modality
 */
public class TraceModality {

    private Modality modality;
    private int confidence;
    private long timestamp;

    public TraceModality(){}

    public TraceModality(Modality modality, int confidence, long timestamp){
        this.modality   = modality;
        this.confidence = confidence;
        this.timestamp  = timestamp;

    }

    public TraceModality(int activity, int confidence, long timestamp){
        this.modality   = activityToModality(activity);
        this.confidence = confidence;
        this.timestamp  = timestamp;
    }

    private Modality activityToModality(int activity){

        switch (activity){
            case DetectedActivity.TILTING:
            case DetectedActivity.UNKNOWN:
                return Modality.Unknown;
            case DetectedActivity.ON_BICYCLE:
                return Modality.Cycling;
            case DetectedActivity.IN_VEHICLE:
                return Modality.Car;
            case DetectedActivity.ON_FOOT:
            case DetectedActivity.WALKING:
                return Modality.Walking;
            case DetectedActivity.RUNNING:
                return Modality.Running;
            case DetectedActivity.STILL:
                return Modality.Stationary;

        }

        return Modality.Unknown;
    }

    public Modality getModality() {
        return modality;
    }

    public void setModality(Modality modality) {
        this.modality = modality;
    }

    public int getConfidence() {
        return confidence;
    }

    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public JsonObject toJson(){
        JsonObject object = new JsonObject();

        object.addProperty("modality", modality.ordinal());
        object.addProperty("confidence", confidence);
        object.addProperty("timestamp", timestamp);

        return object;
    }

    @Override
    public String toString(){
        return toJson().toString();
    }

    /**
     * Two TraceModality objects are considered equal if the have the same modality and confidence
     * levels, regardless of their timestamp.
     * @param o The TraceModality to compare
     * @return True if they are equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        TraceModality m2 = (TraceModality) o;

        return m2.getConfidence() == this.getConfidence() && m2.getModality() == this.getModality();
    }
}