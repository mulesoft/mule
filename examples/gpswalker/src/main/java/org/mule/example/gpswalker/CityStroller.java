/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.gpswalker;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Generates a random walk around a city
 */
public class CityStroller
{

    public static final GpsCoord SAN_FRANCISCO = new GpsCoord(37.789167f, -122.419281f);
    public static final GpsCoord LONDON = new GpsCoord(37.788423f, -122.39823f);
    public static final GpsCoord VALLETTA = new GpsCoord(35.898504f, 14.514313f);


    private volatile GpsCoord currentCoord = SAN_FRANCISCO;
    private AtomicBoolean firstTime = new AtomicBoolean(true);

    //could use a better algorithm here or real test data for better results
    public GpsCoord generateNextCoord()
    {
        if (firstTime.get())
        {
            firstTime.set(false);
        }
        else
        {
            double dist = Math.random() * 0.002;
            double angle = Math.random() * Math.PI;


            float lat = currentCoord.getLatitude() + (float) (dist * Math.sin(angle));
            float lng = currentCoord.getLongitude() + (float) (dist * Math.cos(angle));

            currentCoord = new GpsCoord(lat, lng);
        }
        return currentCoord;
    }

    public GpsCoord getCurrentCoord()
    {
        return currentCoord;
    }

    public void setCurrentCoord(GpsCoord currentCoord)
    {
        this.currentCoord = currentCoord;
        firstTime.set(false);
    }
}
