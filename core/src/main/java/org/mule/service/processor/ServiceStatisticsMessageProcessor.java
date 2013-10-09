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
import org.mule.api.service.Service;

public class ServiceStatisticsMessageProcessor implements MessageProcessor
{
    protected Service service;

    public ServiceStatisticsMessageProcessor(Service service)
    {
        this.service = service;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (service.getStatistics().isEnabled())
        {
            if (event.getExchangePattern().hasResponse())
            {
                service.getStatistics().incReceivedEventSync();
            }
            else
            {
                service.getStatistics().incReceivedEventASync();
            }
        }

        return event;
    }
}
