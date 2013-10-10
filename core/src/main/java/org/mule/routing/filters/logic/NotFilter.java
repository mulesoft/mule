/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
