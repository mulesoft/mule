package org.mule.example.gpswalker;

import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;

import java.util.Random;


/**
 * Generates a walk around San Francisco
 */
public class StrollAroundSanFran
{

    private GpsCoord currentCoord;

    public StrollAroundSanFran()
    {
        currentCoord = new GpsCoord(37.789167f, -122.419281f);
    }

    //could use a better algorithm here or real test dats for better results
    public GpsCoord generateNextCoord()
    {
        double dist = Math.random() * 0.002;
        double angle = Math.random() * Math.PI;


        float lat = currentCoord.getLatitude() + (float) (dist * Math.sin(angle));
        float lng = currentCoord.getLongitude() + (float) (dist * Math.sin(angle));

        currentCoord = new GpsCoord(lat, lng);
        return currentCoord;
    }
}
