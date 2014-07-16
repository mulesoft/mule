/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.filters.logic;

import static org.mule.util.ClassUtils.equal;
import static org.mule.util.ClassUtils.hash;

import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;

/**
 * <code>NotFilter</code> accepts if the filter does not accept.
 */

public class NotFilter implements Filter
{
    private Filter filter;

    public NotFilter()
    {
        super();
    }

    public NotFilter(Filter filter)
    {
        this.filter = filter;
    }

    public Filter getFilter()
    {
        return filter;
    }

    public void setFilter(Filter filter)
    {
        this.filter = filter;
    }

    public boolean accept(MuleMessage message)
    {
        return (filter != null ? !filter.accept(message) : false);
    }

    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        final NotFilter other = (NotFilter) obj;
        return equal(filter, other.filter);
    }

    public int hashCode()
    {
        return hash(new Object[]{this.getClass(), filter});
    }
}
