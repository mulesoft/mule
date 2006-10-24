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
 * <code>NotFilter</code> accepts if the filter does not accept
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class NotFilter implements UMOFilter
{
    private UMOFilter filter;

    public NotFilter()
    {
        super();
    }

    public NotFilter(UMOFilter filter)
    {
        this.filter = filter;
    }

    public UMOFilter getFilter()
    {
        return filter;
    }

    public void setFilter(UMOFilter filter)
    {
        this.filter = filter;
    }

    public boolean accept(UMOMessage message)
    {
        return !filter.accept(message);
    }
}
