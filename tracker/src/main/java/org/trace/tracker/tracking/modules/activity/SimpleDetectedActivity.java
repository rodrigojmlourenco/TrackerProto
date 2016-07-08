package org.trace.tracker.tracking.modules.activity;

/**
 * Created by Rodrigo Lourenço on 18/02/2016.
 */
public class SimpleDetectedActivity {

    public final static int MIN_CONFIDENCE = 75;//%

    private String mActivity;
    private int mConfidence;

    public SimpleDetectedActivity(){}

    public SimpleDetectedActivity(String activity, int confidence) {
        this.mActivity = activity;
        this.mConfidence = confidence;
    }

    public String getActivity() {
        return mActivity;
    }

    public void setActivity(String activity) {
        this.mActivity = activity;
    }

    public int getConfidence() {
        return mConfidence;
    }

    public void setConfidence(int confidence) {
        this.mConfidence = confidence;
    }

    public boolean isAcceptable(){

        return this.mConfidence >= MIN_CONFIDENCE;

    }

    @Override
    public String toString() {
        return "{ activity: "+getActivity()+", confidence: "+getConfidence()+", isAcceptable: "+String.valueOf(isAcceptable())+"}";
    }
}
