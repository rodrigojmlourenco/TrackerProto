/*
 * Copyright (c) 2016 Rodrigo Lourenço, Miguel Costa, Paulo Ferreira, João Barreto @  INESC-ID.
 *
 * This file is part of TRACE.
 *
 * TRACE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TRACE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TRACE.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.trace.tracker;


import android.location.Location;

import org.trace.tracker.storage.data.TrackSummary;

public interface TrackingEngine {

    /**
     * Initiates location and activity tracking.
     */
    public void startTracking();

    /**
     * Stops location and activity tracking.
     */
    public void stopTracking();

    /**
     * Aborts location and activity tracking. The main difference from the stopTracking method
     * is that this one should only be used when the track is to be discarded.
     */
    public void abortTracking();

    /**
     * Checks if the tracking engine is currently tracking.
     * @return True if it is tracking, false otherwise.
     */
    public boolean isTracking();

    /**
     * Enables access to the current track.
     * @return The current TrackSummary
     * @see TrackSummary
     */
    public TrackSummary getCurrentTrack();

    /**
     * Enables access to the current or last know location.
     * @return The current or last known Location
     */
    public Location getCurrentLocation();

    /**
     * Resets the current track object.
     * @param summary The new TrackSummary
     * @see TrackSummary
     */
    public void setCurrentTrack(TrackSummary summary);

    /**
     * Resets the idle timer to zero. This idle timer is responsible for automatically stopping
     * tracking when the user has not significantly moved.
     */
    public void resetIdleTimer();

    /**
     * Stops the idle timer permanently or until it is started once again. This idle timer is
     * responsible for automatically stopping tracking when the user has not significantly moved.
     */
    public void stopIdleTimer();

    /**
     * Notifies the tracking engine to update its tracking configuration profile.
     */
    public void updateSettings();


}
