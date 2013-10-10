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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
