/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension;

import org.mule.extension.annotations.Parameter;

public class Ricin
{
    @Parameter
    private Long microgramsPerKilo;

    @Parameter
    private KnockeableDoor destination;

    public Long getMicrogramsPerKilo()
    {
        return microgramsPerKilo;
    }

    public KnockeableDoor getDestination()
    {
        return destination;
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
