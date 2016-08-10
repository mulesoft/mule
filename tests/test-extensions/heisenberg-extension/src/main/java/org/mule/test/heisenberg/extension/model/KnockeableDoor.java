/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension.model;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;

@Alias("door")
public class KnockeableDoor
{

    public static String knock(String value)
    {
        return "Knocked on " + value;
    }

    @Parameter
    private String victim;

    @Parameter
    private String address;

    @Parameter
    private KnockeableDoor previous;

    public KnockeableDoor()
    {
    }

    public KnockeableDoor(String victim)
    {
        this.victim = victim;
    }

    public String knock()
    {
        return knock(victim);
    }

    public String getVictim()
    {
        return victim;
    }

    public String getAddress()
    {
        return address;
    }

    public KnockeableDoor getPrevious()
    {
        return previous;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof KnockeableDoor)
        {
            return victim.equals(((KnockeableDoor) obj).victim);
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return victim.hashCode();
    }
}
