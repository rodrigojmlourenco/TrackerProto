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

import org.trace.tracker.exceptions.MissingLocationPermissionsException;
import org.trace.tracker.settings.ConfigurationProfile;
import org.trace.tracker.storage.data.Track;
import org.trace.tracker.storage.data.TrackSummary;

import java.util.List;

public interface Tracker {

    /**
     * Initiates the location and activity tracking endeavors, therefore, initiating a new track,
     * which is identifiable by an unique identifier.
     *
     * @return
     */
    String startTracking() throws MissingLocationPermissionsException;

    /**
     * Initiates the location and activity tracking endeavors, therefore, initiating a new track,
     * which is identifiable by an unique identifier.
     *
     * @param modality
     * @param isAutomatic
     * @param isSilent
     *
     * @return
     */
    String startTracking(int modality, boolean isAutomatic, boolean isSilent) throws MissingLocationPermissionsException;

    /**
     * Initiates the location and activity tracking endeavors, therefore, initiating a new track,
     * which is identifiable by an unique identifier.
     *
     * @param isAutomatic
     * @param isSilent
     * @return
     */
    String startTracking(boolean isAutomatic, boolean isSilent) throws MissingLocationPermissionsException;

    /**
     * Stops the location and activity tracking modules. Additionally, it returns the local
     * identifier of the traced track.
     *
     * @return The track's local unique identifier, or null if the track is considered to short.
     */
    Track stopTracking();

    /**
     * Stops the location and activity tracking modules. Additionally, it returns the local
     * identifier of the traced track.
     * @param isManual
     * @return
     */
    Track stopTracking(boolean isManual);

    /**
     * Request the most recent location.
     *
     * @return Most recent location
     */
    Location getLastLocation();

    /**
     * Fetches the currently enforced tracking profile.
     *
     * @return The currently enforced tracking profile.
     *
     * @see ConfigurationProfile
     */
    ConfigurationProfile getCurrentTrackingProfile();

    /**
     * Updates the tracking profile settings.
     * <br>
     * These define the sampling rates used, how outliers are identified, among other information,
     * and are described in detail further ahead.
     *
     * @param profile The updated tracking profile
     */
    void updateTrackingProfile(ConfigurationProfile profile);

    /**
     * Fetches the list of all stored tracks. The tracks are represented by a TrackSummary that
     * contains only top-level information, such as the track identifier, elapsed time, traveled
     * distance.
     *
     * @return List of the tracks as summaries.
     *
     * @see TrackSummary
     */
    List<TrackSummary> getAllTracedTracks();

    /**
     * Fetches the track identified by the specified track identifier. The Track contains not only
     * top-level information, but also all the traced locations and corresponding transportation
     * modalities.
     *
     * @param trackId The track's local unique identifier.
     *
     * @return The track identified by the track identifier.
     */
    Track getTracedTrack(String trackId);

    /**
     * Permanently removes the specified track, identified by the provided track identifier, from
     * the mobile device. Already uploaded tracks will remain stored in the servers.
     * @param trackId
     */
    void deleteTracedTrack(String trackId);



}
