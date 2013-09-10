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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
@Deprecated
public class ServiceLoggingMessageProcessor implements MessageProcessor
{
    protected final transient Log logger = LogFactory.getLog(getClass());
    protected Service service;

    public ServiceLoggingMessageProcessor(Service service)
    {
        this.service = service;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (event.getExchangePattern().hasResponse())
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Service: " + service.getName() + " has received synchronous event on: "
                             + event.getMessageSourceURI());
            }
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Service: " + service.getName() + " has received asynchronous event on: "
                             + event.getMessageSourceURI());
            }
        }

        return event;
    }
}
