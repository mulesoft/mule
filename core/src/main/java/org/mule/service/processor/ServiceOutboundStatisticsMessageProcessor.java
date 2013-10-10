/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.service.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.service.Service;
import org.mule.management.stats.ServiceStatistics;

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
