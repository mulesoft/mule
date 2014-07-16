/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.VoidMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.routing.RoutingException;
import org.mule.api.service.Service;

/**
 * <code>ServiceCatchAllStrategy</code> is used to catch any events and forward the
 * events to the service as is.
 * @deprecated
 */
@Deprecated
public class ServiceCatchAllStrategy extends AbstractCatchAllStrategy
{
    @Override
    public synchronized MuleEvent doCatchMessage(MuleEvent event)
        throws RoutingException
    {
        if (!(event.getFlowConstruct() instanceof Service))
        {
            throw new UnsupportedOperationException(
                "CollectionResponseWithCallbackCorrelator is only supported with Service");
        }

        logger.debug("Catch all strategy handling event: " + event);
        try
        {
            if (event.getExchangePattern().hasResponse())
            {
                if (statistics.isEnabled())
                {
                    statistics.incrementRoutedMessage(event.getMessageSourceName());
                }
                return ((Service) event.getFlowConstruct()).sendEvent(event);
            }
            else
            {
                if (statistics.isEnabled())
                {
                    statistics.incrementRoutedMessage(event.getMessageSourceName());
                }
                ((Service) event.getFlowConstruct()).dispatchEvent(event);
                return VoidMuleEvent.getInstance();
            }
        }
        catch (MuleException e)
        {
            throw new RoutingException(event, this, e);
        }
    }
}
