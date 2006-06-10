/*
 * $Id$
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
import org.mule.config.MuleProperties;
import org.mule.config.PropertyExtractor;
import org.mule.management.stats.RouterStatistics;
import org.mule.routing.CorrelationPropertiesExtractor;
import org.mule.umo.UMOMessage;
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

    /**
     * Extracts a 'Correlation Id' from a reply message.  The correlation Id does not have to be the Message
     * Correlation Id. It can be extracted from the message payload if desired.
     * @param message a received reply message
     * @return the correlation Id for this message
     */
    protected Object getReplyAggregateIdentifier(UMOMessage message) {
        return correlationExtractor.getProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY, message);
    }

    /**
     * Extracts a Group identifier from the current event.  When an event is received with a group identifier
     * not registered with this router, a new group is created.  The id returned here can be a correlationId or
     * some custom aggregation Id.
     *
     * This implementation uses the Unique Message Id of the UMOMessage being returned a
     * @param message A response messages received on the response router endpoint
     * @return an aggregation Id for this event
     */
    protected Object getCallResponseAggregateIdentifier(UMOMessage message) {
        return correlationExtractor.getProperty(MuleProperties.MULE_MESSAGE_ID_PROPERTY, message);
    }
}
