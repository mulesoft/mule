/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.service.processor;

import org.mule.api.MuleEvent;
import org.mule.api.service.Service;
import org.mule.processor.AbstractMessageObserver;

public class ServiceStatisticsMessageObserver extends AbstractMessageObserver
{
    protected Service service;

    public ServiceStatisticsMessageObserver(Service service)
    {
        this.service = service;
    }

    @Override
    public void observe(MuleEvent event)
    {
        if (service.getStatistics().isEnabled())
        {
            if (event.getEndpoint().getExchangePattern().hasResponse())
            {
                service.getStatistics().incReceivedEventSync();
            }
            else
            {
                service.getStatistics().incReceivedEventASync();
            }
        }
    }
}
