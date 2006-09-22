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

/**
 * <code>OrFilter</code> accepts if the leftFilter or rightFilter filter
 * accept
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class OrFilter implements UMOFilter
{
    private UMOFilter leftFilter;
    private UMOFilter rightFilter;

    public OrFilter(UMOFilter leftFilter, UMOFilter rightFilder)
    {
        this.leftFilter = leftFilter;
        this.rightFilter = rightFilder;
    }

    public OrFilter()
    {
        super();
    }

    public void setLeftFilter(UMOFilter leftFilter)
    {
        this.leftFilter = leftFilter;
    }

    public void setRightFilter(UMOFilter rightFilter)
    {
        this.rightFilter = rightFilter;
    }

    public UMOFilter getLeftFilter()
    {
        return leftFilter;
    }

    public UMOFilter getRightFilter()
    {
        return rightFilter;
    }

    public boolean accept(UMOMessage message)
    {
        return leftFilter.accept(message) || rightFilter.accept(message);
    }
}
