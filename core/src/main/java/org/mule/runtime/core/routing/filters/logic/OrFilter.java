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
