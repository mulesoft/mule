/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.filters;

import static org.mule.util.ClassUtils.equal;
import static org.mule.util.ClassUtils.hash;

import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;
import org.mule.util.ClassUtils;

/**
 * <code>PayloadTypeFilter</code> filters based on the type of the object received.
 */

public class PayloadTypeFilter implements Filter
{
    private Class expectedType;

    public PayloadTypeFilter()
    {
        super();
    }

    public PayloadTypeFilter(String expectedType) throws ClassNotFoundException
    {
        this(ClassUtils.loadClass(expectedType, PayloadTypeFilter.class));
    }

    public PayloadTypeFilter(Class expectedType)
    {
        this.expectedType = expectedType;
    }

    public boolean accept(MuleMessage message)
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
    
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        final PayloadTypeFilter other = (PayloadTypeFilter) obj;
        return equal(expectedType, other.expectedType);
    }

    public int hashCode()
    {
        return hash(new Object[]{this.getClass(), expectedType});
    }
}
