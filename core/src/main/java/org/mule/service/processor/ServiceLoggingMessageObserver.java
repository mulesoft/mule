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
import org.mule.processor.AbstractMessageObserver;

public class ServiceLoggingMessageObserver extends AbstractMessageObserver
{
    protected Service service;

    public ServiceLoggingMessageObserver(Service service)
    {
        this.service = service;
    }

    @Override
    public void observe(MuleEvent event)
    {
        if (event.isSynchronous())
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Service: " + service.getName() + " has received synchronous event on: "
                             + event.getEndpoint().getEndpointURI());
            }
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Service: " + service.getName() + " has received asynchronous event on: "
                             + event.getEndpoint().getEndpointURI());
            }
        }
    }
}
