/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.testmodels.fruit;

import org.mule.api.MuleException;

import java.util.HashMap;

/**
 * A specialisation of Orange
 */
public class BloodOrange extends Orange
{
    public BloodOrange()
    {
    }

    public BloodOrange(HashMap props)
            throws MuleException
    {
        super(props);
    }

    public BloodOrange(Integer segments, Double radius, String brand)
    {
        super(segments, radius, brand);
    }
}
