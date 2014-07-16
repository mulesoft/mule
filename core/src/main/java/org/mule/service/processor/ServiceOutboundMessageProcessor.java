/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.processor;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.VoidMuleEvent;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.service.Service;
import org.mule.component.simple.PassThroughComponent;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.transport.NullPayload;

/**
 * Responsible for determining if the Service outbound phase should be used and
 * making a copy of the event to use.
 * <p>
 * If the service component is a {@link PassThroughComponent} a null from the
 * outbound phase will result in a {@link NullPayload} being returned, otherwise when
 * the outbound phase returns null this MessageProcessor will return the request
 * event.
 */
@Deprecated
public class ServiceOutboundMessageProcessor extends AbstractInterceptingMessageProcessor
{

    protected Service service;

    public ServiceOutboundMessageProcessor(Service service)
    {
        this.service = service;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        // Skip outbound phase is inbound is sync and payload is NullPayload
        boolean syncNullPayload = event.getExchangePattern().hasResponse()
                                  && (event.getMessage().getPayload() instanceof NullPayload);

        if (event.isStopFurtherProcessing())
        {
            logger.debug("MuleEvent stop further processing has been set, no outbound routing will be performed.");
            return event;
        }

        else if (event != null && !VoidMuleEvent.getInstance().equals(event) && !syncNullPayload)
        {
            if (!(service.getOutboundMessageProcessor() instanceof OutboundRouterCollection)
                || (service.getOutboundMessageProcessor() instanceof OutboundRouterCollection && ((OutboundRouterCollection) service.getOutboundMessageProcessor()).hasEndpoints()))
            {
                MuleEvent outboundEvent;
                if (event.getExchangePattern().hasResponse())
                {
                    // Copy of the inbound event for outbound phase
                    outboundEvent = new DefaultMuleEvent(new DefaultMuleMessage(event.getMessage()
                        .getPayload(), event.getMessage(), event.getMuleContext()), event);
                }
                else
                {
                    outboundEvent = event;
                }

                MuleEvent outboundResult = processNext(outboundEvent);

                if (outboundResult != null && !VoidMuleEvent.getInstance().equals(outboundResult))
                {
                    event = outboundResult;
                }
                else if (service.getComponent() instanceof PassThroughComponent)
                {
                    // If there was no component, then we really want to return
                    // the response from the outbound router as the actual
                    // payload - even if it's null.
                    event = new DefaultMuleEvent(new DefaultMuleMessage(NullPayload.getInstance(),
                        event.getMessage(), service.getMuleContext()), event);
                }
            }
            else
            {
                logger.debug("Outbound router on service '" + service.getName()
                             + "' doesn't have any targets configured.");
            }
            event = RequestContext.setEvent(new DefaultMuleEvent(event.getMessage(),event));
        }
        return event;
    }
}
