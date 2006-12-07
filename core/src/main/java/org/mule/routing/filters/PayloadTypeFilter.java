/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.filters;

import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;

/**
 * <code>PayloadTypeFilter</code> filters based on the type of the object received.
 */

public class PayloadTypeFilter implements UMOFilter
{
    private Class expectedType;

    public PayloadTypeFilter()
    {
        super();
    }

    public PayloadTypeFilter(Class expectedType)
    {
        this.expectedType = expectedType;
    }

    public boolean accept(UMOMessage message)
    {
        return (expectedType != null ? expectedType.isAssignableFrom(message.getPayload().getClass()) : false);
    }

    public Class getExpectedType()
    {
        return expectedType;
    }

    public void setExpectedType(Class expectedType)
    {
        this.expectedType = expectedType;
    }

}
