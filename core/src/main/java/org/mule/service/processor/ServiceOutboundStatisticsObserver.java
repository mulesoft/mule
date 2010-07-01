/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.service.processor;

import org.mule.api.MuleEvent;
import org.mule.api.service.Service;
import org.mule.management.stats.ServiceStatistics;
import org.mule.processor.AbstractMessageObserver;

public class ServiceOutboundStatisticsObserver extends AbstractMessageObserver
{

    protected Service service;

    public ServiceOutboundStatisticsObserver(Service service)
    {
        this.service = service;
    }

    @Override
    public void observe(MuleEvent event)
    {
        ServiceStatistics stats = service.getStatistics();
        if (service.getOutboundRouter().hasEndpoints() && stats.isEnabled())
        {
            if (event.isSynchronous())
            {
                stats.incSentEventSync();
            }
            else
            {
                stats.incSentEventASync();
            }
        }
    }
}
