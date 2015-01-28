/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions;

public class Ricin
{

    private Long microgramsPerKilo;
    private Door destination;

    public Long getMicrogramsPerKilo()
    {
        return microgramsPerKilo;
    }

    public void setMicrogramsPerKilo(Long microgramsPerKilo)
    {
        this.microgramsPerKilo = microgramsPerKilo;
    }

    public Door getDestination()
    {
        return destination;
    }

    public void setDestination(Door destination)
    {
        this.destination = destination;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Ricin)
        {
            return microgramsPerKilo.equals(((Ricin) obj).microgramsPerKilo);
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return microgramsPerKilo.hashCode();
    }
}
