/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.api.routing.RoutingException;
import org.mule.routing.CorrelationMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <code>AbstractMessageSplitter</code> is an outbound Message Splitter used to split
 * the contents of a received message into sub parts that can be processed by other
 * components. Each Part is fired as a separate event to each endpoint on the router. The
 * targets can have filters on them to receive only certain message parts.
 */
public abstract class AbstractMessageSplitter extends FilteringOutboundRouter
{
    @Override
    public MuleEvent route(MuleEvent event) throws RoutingException
    {
        MuleMessage message = event.getMessage();

        String correlationId = event.getFlowConstruct().getMessageInfoMapping().getCorrelationId(event);

        List<MuleEvent> results = new ArrayList<MuleEvent>();
        int correlationSequence = 1;
        SplitMessage splitMessage = getMessageParts(message, getRoutes());

        // Cache the properties here because for some message types getting the
        // properties can be expensive
        Map<String, Object> props = new HashMap<String, Object>();
        for (String propertyKey : message.getOutboundPropertyNames())
        {
            Object value = message.getOutboundProperty(propertyKey);
            if (value != null)
            {
                props.put(propertyKey, value);
            }
        }

        for (int i = 0; i < splitMessage.size(); i++)
        {
            SplitMessage.MessagePart part = splitMessage.getPart(i);

            MuleMessage sendMessage;
            if (part.getPart() instanceof MuleMessage)
            {
                sendMessage = (MuleMessage) part.getPart();
            }
            else
            {
                sendMessage = new DefaultMuleMessage(part.getPart(), props, muleContext);
            }
            sendMessage.propagateRootId(message);
            try
            {
                if (enableCorrelation != CorrelationMode.NEVER)
                {
                    boolean correlationSet = message.getCorrelationId() != null;
                    if (!correlationSet && (enableCorrelation == CorrelationMode.IF_NOT_SET))
                    {
                        sendMessage.setCorrelationId(correlationId);
                    }

                    // take correlation group size from the message properties, set by concrete 
                    // message splitter implementations
                    sendMessage.setCorrelationGroupSize(splitMessage.size());
                    sendMessage.setCorrelationSequence(correlationSequence++);
                }

                MuleEvent toRoute = createEventToRoute(event, message);

                if (part.getEndpoint().getExchangePattern().hasResponse())
                {
                    results.add(sendRequest(event, toRoute, part.getEndpoint(), true));
                }
                else
                {
                    sendRequest(event, toRoute, part.getEndpoint(), false);
                }
            }
            catch (MuleException e)
            {
                throw new CouldNotRouteOutboundMessageException(event, part.getEndpoint(), e);
            }
        }

        return resultsHandler.aggregateResults(results, event, muleContext);
    }


    /**
     * Implementing classes should create a {@link org.mule.routing.outbound.SplitMessage} instance and for
     * each part can associate an endpoint.
     * Note that No state should be stored on the router itself. The {@link SplitMessage} provides the parts and
     * endpoint mapping info in order for the correct dispatching to occur.
     * <p/>
     * If users do not want to associate a message part with an endpoint, but just dispatch parts over the targets in
     * a round-robin way, they should use the {@link org.mule.routing.outbound.AbstractRoundRobinMessageSplitter} instead.
     *
     * @param message   the current message being processed
     * @param endpoints A list of {@link org.mule.api.endpoint.OutboundEndpoint} that will be used to dispatch each of the parts
     * @return a {@link org.mule.routing.outbound.SplitMessage} instance that contains the message parts and the
     *         endpoint to associate with the message part.
     * @see org.mule.routing.outbound.SplitMessage
     * @see org.mule.routing.outbound.AbstractRoundRobinMessageSplitter
     */
    protected abstract SplitMessage getMessageParts(MuleMessage message, List <MessageProcessor> endpoints);
}
