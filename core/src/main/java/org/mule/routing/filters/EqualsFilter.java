/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.filters;

import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;

/**
 * <code>EqualsFilter</code> is a filter for comparing two objects using the
 * equals() method.
 */
public class EqualsFilter implements UMOFilter, ObjectFilter
{
    private Object pattern;

    public EqualsFilter()
    {
        super();
    }

    public EqualsFilter(Object compareTo)
    {
        this.pattern = compareTo;
    }

    public boolean accept(UMOMessage message)
    {
        return accept(message.getPayload());
    }

    public boolean accept(Object object)
    {
        if (object == null && pattern == null)
        {
            return true;
        }

        if (object == null || pattern == null)
        {
            return false;
        }

        return pattern.equals(object);
    }

    public Object getPattern()
    {
        return pattern;
    }

    public void setPattern(Object pattern)
    {
        this.pattern = pattern;
    }

}
