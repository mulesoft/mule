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
import org.mule.api.service.Service;
@Deprecated
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
