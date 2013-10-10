/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.service;

import org.mule.VoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.RoutingException;
import org.mule.api.service.Service;
import org.mule.routing.MessageFilter;

/**
 * <code>ForwardingConsumer</code> is used to forward an incoming event over another transport without
 * invoking a service. This can be used to implement a bridge accross different transports.
 * @deprecated
 */
@Deprecated
public class ForwardingConsumer extends MessageFilter
{
    @Override
    public MuleEvent processNext(MuleEvent event) throws MessagingException
    {
        if (!(event.getFlowConstruct() instanceof Service))
        {
            throw new UnsupportedOperationException("ForwardingConsumer is only supported with Service");
        }

        MessageProcessor processor = ((Service) event.getFlowConstruct()).getOutboundMessageProcessor();

        // Set the stopFurtherProcessing flag to true to inform the
        // DefaultInboundRouterCollection not to route these events to the service
        event.setStopFurtherProcessing(true);

        if (processor == null)
        {
            logger.debug("Descriptor has no outbound router configured to forward to, continuing with normal processing");
            return event;
        }
        else
        {
            try
            {
                MuleEvent resultEvent = processor.process(event);
                if (resultEvent != null && !VoidMuleEvent.getInstance().equals(resultEvent))
                {
                    return resultEvent;
                }
                else
                {
                    return null;
                }
            }
            catch (MuleException e)
            {
                throw new RoutingException(event, this, e);
            }
        }
    }
}
