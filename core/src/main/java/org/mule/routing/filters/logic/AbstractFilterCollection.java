/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.filters.logic;

import org.mule.api.routing.filter.Filter;
import org.mule.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a filter collection. Used as the base clas for the Or and AND filters
 */

public abstract class AbstractFilterCollection implements Filter
{
    private List<Filter> filters;

    public AbstractFilterCollection()
    {
        filters = new ArrayList<Filter>();
    }

    public AbstractFilterCollection(List<Filter> filters)
    {
        this();
        this.filters = filters;
    }

    public AbstractFilterCollection(Filter... filters)
    {
        this();
        for (int i = 0; i < filters.length; i++)
        {
            this.filters.add(filters[i]);
        }
    }

    public List<Filter> getFilters()
    {
        return filters;
    }

    public void setFilters(List<Filter> filters)
    {
        this.filters = filters;
    }
    
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        final AbstractFilterCollection other = (AbstractFilterCollection) obj;
        return ClassUtils.equal(filters, other.filters);
    }

    public int hashCode()
    {
        return ClassUtils.hash(new Object[]{this.getClass(), filters});
    }

}
