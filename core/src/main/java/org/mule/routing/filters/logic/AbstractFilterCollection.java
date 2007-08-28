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

import org.mule.umo.UMOFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a filter collection. Used as the base clas for the Or and AND filters
 */

public abstract class AbstractFilterCollection implements UMOFilter
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
    public AbstractFilterCollection(UMOFilter left, UMOFilter right)
    {
        this();
        filters.add(left);
        filters.add(right);
    }

    /**
     * @deprecated
     * @param leftFilter
     */
    public void setLeftFilter(UMOFilter leftFilter)
    {
        filters.add(0, leftFilter);
    }

    /**
     * @deprecated
     * @param rightFilter
     */
    public void setRightFilter(UMOFilter rightFilter)
    {
        filters.add(rightFilter);

    }

    /**
     * @deprecated
     * @return
     */
    public UMOFilter getLeftFilter()
    {
        if(filters.size()==0) return null;
        return (UMOFilter)filters.get(0);
    }

    /**
     * @deprecated
     * @return
     */
    public UMOFilter getRightFilter()
    {
        if(filters.size() > 1)
        {
            return (UMOFilter)filters.get(1);
        } else if(filters.size()==0) {
            return null;
        }
        else
        {
            return (UMOFilter)filters.get(0);

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
