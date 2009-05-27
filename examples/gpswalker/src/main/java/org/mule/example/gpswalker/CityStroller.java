/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.gpswalker;

/**
 * Generates a random walk around a city
 */
public class CityStroller
{

    public static final GpsCoord SAN_FRANCISCO = new GpsCoord(37.789167f, -122.419281f);
    public static final GpsCoord LONDON = new GpsCoord(37.788423f, -122.39823f);
    public static final GpsCoord VALLETTA = new GpsCoord(35.898504f, 14.514313f);


    private GpsCoord currentCoord = SAN_FRANCISCO;
    private boolean firstTime = true;

    //could use a better algorithm here or real test dats for better results
    public GpsCoord generateNextCoord()
    {
        if(firstTime)
        {
            firstTime = false;
            return currentCoord;
        }

        double dist = Math.random() * 0.002;
        double angle = Math.random() * Math.PI;


        float lat = currentCoord.getLatitude() + (float) (dist * Math.sin(angle));
        float lng = currentCoord.getLongitude() + (float) (dist * Math.sin(angle));

        currentCoord = new GpsCoord(lat, lng);
        return currentCoord;
    }

    public GpsCoord getCurrentCoord()
    {
        return currentCoord;
    }

    public void setCurrentCoord(GpsCoord currentCoord)
    {
        this.currentCoord = currentCoord;
        firstTime = true;
    }
}
