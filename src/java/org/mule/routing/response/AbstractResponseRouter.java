/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.routing.response;

import org.mule.config.MuleConfiguration;
import org.mule.config.PropertyExtractor;
import org.mule.management.stats.RouterStatistics;
import org.mule.routing.CorrelationPropertiesExtractor;
import org.mule.umo.routing.UMOResponseRouter;

/**
 * <code>AbstractResponseRouter</code> is a base class for all Response
 * Routers
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public abstract class AbstractResponseRouter implements UMOResponseRouter
{
    private RouterStatistics routerStatistics;

    private int timeout = MuleConfiguration.DEFAULT_TIMEOUT;

    protected PropertyExtractor correlationExtractor = new CorrelationPropertiesExtractor();

    public RouterStatistics getRouterStatistics()
    {
        return routerStatistics;
    }

    public void setRouterStatistics(RouterStatistics routerStatistics)
    {
        this.routerStatistics = routerStatistics;
    }

    public PropertyExtractor getCorrelationExtractor()
    {
        return correlationExtractor;
    }

    public void setCorrelationExtractor(PropertyExtractor correlationExtractor)
    {
        this.correlationExtractor = correlationExtractor;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
