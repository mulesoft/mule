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

import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;

import java.util.Iterator;

/**
 * <code>OrFilter</code> accepts if any of the filters accept the message
 */

public class OrFilter extends AbstractFilterCollection
{
    public OrFilter()
    {
        super();
    }

    /**
     * @param left
     * @param right
     */
    public OrFilter(Filter left, Filter right)
    {
        super(left, right);
    }

    public boolean accept(MuleMessage message)
    {
        for (Iterator iterator = getFilters().iterator(); iterator.hasNext();)
        {
            Filter filter = (Filter) iterator.next();
            if(filter.accept(message))
            {
                return true;
            }
        }
        return false;
    }
}
