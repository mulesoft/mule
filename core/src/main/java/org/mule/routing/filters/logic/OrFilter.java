/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.filters.logic;

import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;

import java.util.List;

/**
 * <code>OrFilter</code> accepts if any of the filters accept the message
 */

public class OrFilter extends AbstractFilterCollection
{
    public OrFilter()
    {
        super();
    }

    public OrFilter(Filter... filters)
    {
        super(filters);
    }

    public OrFilter(List<Filter> filters)
    {
        super(filters);
    }

    public boolean accept(MuleMessage message)
    {
        for (Filter filter : getFilters())
        {
            if(filter.accept(message))
            {
                return true;
            }
        }
        return false;
    }
}
