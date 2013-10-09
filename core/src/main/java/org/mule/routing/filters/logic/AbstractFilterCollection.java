/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
