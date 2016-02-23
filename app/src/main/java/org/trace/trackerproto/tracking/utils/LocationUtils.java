package org.trace.trackerproto.tracking.utils;

import android.location.Location;

import org.alternativevision.gpx.GPXParser;
import org.alternativevision.gpx.beans.GPX;
import org.alternativevision.gpx.beans.Track;
import org.alternativevision.gpx.beans.Waypoint;
import org.trace.trackerproto.tracking.exceptions.UnableToParseTraceException;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * @version 0
 * @author Rodrigo Lourenço
 *
 * Created by Rodrigo Lourenço on 18/02/2016.
 */
public class LocationUtils {

    /**
     * Generates a GPS Exchange Format string. This can then be stored as a file, as to easy
     * the creation of graphical representations of the recorded track.
     *
     * @param track Collection of the tracked locations.
     *
     * @return The traced track as a GPS Exchange Format string.
     */
    public static String generateGPXTrack(List<Location> track) throws UnableToParseTraceException {

        GPX gpx = new GPX();
        Track gpxTrack = new Track();
        GPXParser parser = new GPXParser();
        ByteArrayOutputStream s = new ByteArrayOutputStream();

        ArrayList<Waypoint> builtTrack = new ArrayList<>();

        for(Location l : track){
            Waypoint w = new Waypoint();
            w.setLatitude(l.getLatitude());
            w.setLongitude(l.getLongitude());
            w.setElevation(l.getAltitude());
            w.setTime(new Date(l.getTime()));
            w.setComment("Accuracy: " + l.getAccuracy());

            builtTrack.add(w);
        }

        gpxTrack.setTrackPoints(builtTrack);
        gpx.addTrack(gpxTrack);

        try {
            parser.writeGPX(gpx, s);
            return new String(s.toByteArray());
        } catch (ParserConfigurationException | TransformerException e) {
            throw new UnableToParseTraceException(e.getMessage());
        }


    }
}
