/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.filters.logic;

import org.mule.api.routing.filter.Filter;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a filter collection. Used as the base clas for the Or and AND filters
 */

public abstract class AbstractFilterCollection implements Filter
{
    private List filters;

    public AbstractFilterCollection()
    {
        filters = new ArrayList();

    }

    /**
     * @param left
     * @param right
     */
    public AbstractFilterCollection(Filter left, Filter right)
    {
        this();
        filters.add(left);
        filters.add(right);
    }

    /**
     * @deprecated
     * @param leftFilter
     */
    public void setLeftFilter(Filter leftFilter)
    {
        filters.add(0, leftFilter);
    }

    /**
     * @deprecated
     * @param rightFilter
     */
    public void setRightFilter(Filter rightFilter)
    {
        filters.add(rightFilter);

    }

    /**
     * @deprecated
     * @return
     */
    public Filter getLeftFilter()
    {
        if(filters.size()==0) return null;
        return (Filter)filters.get(0);
    }

    /**
     * @deprecated
     * @return
     */
    public Filter getRightFilter()
    {
        if(filters.size() > 1)
        {
            return (Filter)filters.get(1);
        } else if(filters.size()==0) {
            return null;
        }
        else
        {
            return (Filter)filters.get(0);

        }
    }

    public List getFilters()
    {
        return filters;
    }

    public void setFilters(List filters)
    {
        this.filters = filters;
    }
}
