/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.testmodels.fruit;

import org.mule.umo.lifecycle.InitialisationException;
import org.mule.util.object.ObjectFactory;

public class FloridaSunnyOrangeFactory implements ObjectFactory
{
    Integer segments = new Integer(10);
    Double radius = new Double(4.34);

    public void initialise() throws InitialisationException
    {
        // nothing to do
    }
    
    public void dispose()
    {
        // nothing to do
    }

    public Object create() throws Exception
    {
        return new Orange(segments, radius, "Florida Sunny");
    }

    public Double getRadius()
    {
        return radius;
    }

    public void setRadius(Double radius)
    {
        this.radius = radius;
    }

    public Integer getSegments()
    {
        return segments;
    }

    public void setSegments(Integer segments)
    {
        this.segments = segments;
    }
}
