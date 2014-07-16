/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
