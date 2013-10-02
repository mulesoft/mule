/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.service.Service;
import org.mule.management.stats.ServiceStatistics;
@Deprecated
public class ServiceOutboundStatisticsMessageProcessor implements MessageProcessor
{

    protected Service service;

    public ServiceOutboundStatisticsMessageProcessor(Service service)
    {
        this.service = service;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        ServiceStatistics stats = service.getStatistics();
        if (stats.isEnabled())
        {
            if (!(service.getOutboundMessageProcessor() instanceof OutboundRouterCollection)
                || (service.getOutboundMessageProcessor() instanceof OutboundRouterCollection && ((OutboundRouterCollection) service.getOutboundMessageProcessor()).hasEndpoints()))
            {
                if (event.getExchangePattern().hasResponse())
                {
                    stats.incSentEventSync();
                }
                else
                {
                    stats.incSentEventASync();
                }
            }
        }

        return event;
    }
}
