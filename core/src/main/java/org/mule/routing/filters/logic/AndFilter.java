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
 * <code>AndFilter</code> accepts only if all the filters
 * accept.
 */
public class AndFilter extends AbstractFilterCollection
{

    public AndFilter()
    {
        super();
    }

    public AndFilter(Filter... filters)
    {
        super(filters);
    }

    public AndFilter(List<Filter> filters)
    {
        super(filters);
    }

    public boolean accept(MuleMessage message)
    {
        if (getFilters().size() == 0)
        {
            return false;
        }
        for (Filter filter : getFilters())
        {
            if (!filter.accept(message))
            {
                return false;
            }
        }

        return true;
    }
}
