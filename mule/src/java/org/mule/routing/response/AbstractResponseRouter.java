/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.routing.response;

import org.mule.management.stats.RouterStatistics;
import org.mule.umo.routing.UMOResponseRouter;

/**
 * <code>AbstractResponseRouter</code> is a base class for all Response Routers
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public abstract class AbstractResponseRouter implements UMOResponseRouter
{
    private RouterStatistics routerStatistics;

    public RouterStatistics getRouterStatistics()
    {
        return routerStatistics;
    }

    public void setRouterStatistics(RouterStatistics routerStatistics)
    {
        this.routerStatistics = routerStatistics;
    }
}
