/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.config.MuleProperties;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.api.routing.RoutingException;

import java.util.Iterator;
import java.util.List;

/**
 * <code>AbstractMessageSplitter</code> is an outbound Message Splitter used to split
 * the contents of a received message into sub parts that can be processed by other
 * components. Each Part is fired as a separate event to each endpoint on the router. The
 * endpoints can have filters on them to receive only certain message parts.
 */
public abstract class AbstractMessageSplitter extends FilteringOutboundRouter
{
    public MuleMessage route(MuleMessage message, MuleSession session) throws RoutingException
    {
        String correlationId = messageInfoMapping.getCorrelationId(message);

        List results = new java.util.ArrayList();
        int correlationSequence = 1;
        SplitMessage splitMessage = getMessageParts(message, getEndpoints());

        // Cache the properties here because for some message types getting the
        // properties can be expensive
        java.util.Map props = new java.util.HashMap();
        for (Iterator iterator = message.getPropertyNames().iterator(); iterator.hasNext();)
        {
            String propertyKey = (String) iterator.next();
            props.put(propertyKey, message.getProperty(propertyKey));
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
                sendMessage = new org.mule.DefaultMuleMessage(part.getPart(), props);
            }

            try
            {
                if (enableCorrelation != ENABLE_CORRELATION_NEVER)
                {
                    boolean correlationSet = message.getCorrelationId() != null;
                    if (!correlationSet && (enableCorrelation == ENABLE_CORRELATION_IF_NOT_SET))
                    {
                        sendMessage.setCorrelationId(correlationId);
                    }

                    // take correlation group size from the message
                    // properties, set by concrete message splitter
                    // implementations
                    //final int groupSize = sendMessage.getCorrelationGroupSize();
                    //message.setCorrelationGroupSize(groupSize);
                    sendMessage.setCorrelationGroupSize(splitMessage.size());
                    sendMessage.setCorrelationSequence(correlationSequence++);
                }

                //Use sync config from endpoint
                boolean synchronous = part.getEndpoint().isSynchronous();
                sendMessage.setBooleanProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY,
                        part.getEndpoint().isRemoteSync());

                if (synchronous)
                {
                    results.add(send(session, sendMessage, part.getEndpoint()));
                }
                else
                {
                    dispatch(session, sendMessage, part.getEndpoint());
                }
            }
            catch (MuleException e)
            {
                throw new CouldNotRouteOutboundMessageException(sendMessage, part.getEndpoint(), e);
            }
        }

        return resultsHandler.aggregateResults(results, message);
    }


    /**
     * Implementing classes should create a {@link org.mule.routing.outbound.SplitMessage} instance and for
     * each part can associate an endpoint.
     * Note that No state should be stored on the router itself. The {@link SplitMessage} provides the parts and
     * endpoint mapping info in order for the correct dispatching to occur.
     * <p/>
     * If users do not want to associate a message part with an endpoint, but just dispatch parts over the endpoints in
     * a round-robin way, they should use the {@link org.mule.routing.outbound.AbstractRoundRobinMessageSplitter} instead.
     *
     * @param message   the current message being processed
     * @param endpoints A list of {@link org.mule.api.endpoint.OutboundEndpoint} that will be used to dispatch each of the parts
     * @return a {@link org.mule.routing.outbound.SplitMessage} instance that contains the message parts and the
     *         endpoint to associate with the message part.
     * @see org.mule.routing.outbound.SplitMessage
     * @see org.mule.routing.outbound.AbstractRoundRobinMessageSplitter
     */
    protected abstract SplitMessage getMessageParts(MuleMessage message, List /* <OutboundEndpoint> */ endpoints);
}
