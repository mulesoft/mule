/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.gpswalker;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * A bean that holds Geo Coordinates
 * Note that this bean is annotated with Jackson Json annotations.  This allows Mule to automically
 * serialise this object to and from JSON.
 */
public class GpsCoord
{
    @JsonProperty
    private float latitude;
    @JsonProperty
    private float longitude;

    public GpsCoord(float lat, float lng)
    {
        latitude = lat;
        longitude = lng;
    }

    public float getLatitude()
    {
        return latitude;
    }

    public float getLongitude()
    {
        return longitude;
    }

    public void setLatitude(float latitude)
    {
        this.latitude = latitude;
    }

    public void setLongitude(float longitude)
    {
        this.longitude = longitude;
    }
}
