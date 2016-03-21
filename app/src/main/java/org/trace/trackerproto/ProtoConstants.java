package org.trace.trackerproto;

import android.Manifest;

public final class ProtoConstants {

    public interface extras {
        String TRACK_KEY_EXTRA = "org.trace.intent.extra.TRACK_KEY_EXTRA";
    }

    public interface home {
        String MARKER_INDEX_KEY = "MARKER_INDEX_KEY";
        String SERVICE_BOUND_KEY ="BOUND_TRACKER_SERVICE";
        String TRACKING_STATE_KEY = "TRACKING_TRACKER_SERVICE";
        String LAST_LOCATION_KEY = "LAST_LOCATION";
    }
}
