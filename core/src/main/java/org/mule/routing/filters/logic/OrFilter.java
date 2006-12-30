/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.filters.logic;

import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;

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
     * @deprecated
     * @param left
     * @param right
     */
    public OrFilter(UMOFilter left, UMOFilter right)
    {
        super(left, right);
    }

    public boolean accept(UMOMessage message)
    {
        for (Iterator iterator = getFilters().iterator(); iterator.hasNext();)
        {
            UMOFilter umoFilter = (UMOFilter) iterator.next();
            if(umoFilter.accept(message))
            {
                return true;
            }
        }
        return false;
    }
}
