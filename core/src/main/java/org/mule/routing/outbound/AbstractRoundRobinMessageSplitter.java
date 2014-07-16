/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.CoreMessages;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <code>FilteringListMessageSplitter</code> accepts a List as a message payload
 * then routes list elements as messages over an endpoint where the endpoint's filter
 * accepts the payload.
 */
public class AbstractRoundRobinMessageSplitter extends AbstractMessageSplitter
{
    private boolean deterministic = true;
    
    /* Users should only disable this if they have filters configured on the endpoint
     * that will control which endpoint will receive the message
     */
    private boolean disableRoundRobin = false;

    private static final AtomicInteger globalCounter = new AtomicInteger(0);
    private boolean failIfNoMatch = true;

    @Override
    public void initialise() throws InitialisationException
    {
        if (isDisableRoundRobin())
        {
            setDeterministic(true);
        }
        super.initialise();
    }

    /**
     * Method used just to split the message into parts.  Each part should be an entry in the list.
     * The list can contain either {@link org.mule.api.MuleMessage} objects or just payloads (Mule will
     * automatically convert the payloads into messages).
     * <p/>
     * This method can be overridden by custom implementations of splitter router where the distribution of
     * the message parts will be done using either round robin or endpoint filtering.
     *
     * @param message the source message to split into parts
     * @return a list of payload objects or {@link org.mule.api.MuleMessage} objects. Usually, it is sufficient
     *         just to return payload objects
     */
    protected List splitMessage(MuleMessage message)
    {
        if (message.getPayload() instanceof List)
        {
            return new LinkedList((List) message.getPayload());
        }
        else
        {
            throw new IllegalArgumentException(CoreMessages.objectNotOfCorrectType(
                    message.getPayload().getClass(), List.class).getMessage());
        }
    }

    /**
     * Retrieves a specific message part for the given endpoint. the message will then be
     * routed via the provider. <p/> <strong>NOTE:</strong>Implementations must provide
     * proper synchronization for shared state (payload, properties, etc.)
     *
     * @param message   the current message being processed
     * @param endpoints A list of targets that will be used to dispatch each of the parts
     * @return a {@link java.util.List} of message parts.  Each part will become the payload of the outgoing
     *         message.  Note that the parts will be dispatched to
     */
    @Override
    protected SplitMessage getMessageParts(MuleMessage message, List<MessageProcessor> endpoints)
    {
        SplitMessage splitMessage = new SplitMessage();

        List payloads = splitMessage(message);
        // Cache the properties here because for some message types getting the
        // properties can be expensive
        Map props = new HashMap();
        for (String propertyKey : message.getOutboundPropertyNames())
        {
            Object value = message.getOutboundProperty(propertyKey);
            if (value != null)
            {
                props.put(propertyKey, value);
            }
        }

        Counter counter = new Counter();

        for (Iterator iterator = payloads.iterator(); iterator.hasNext();)
        {
            Object payload = iterator.next();
            MuleMessage part = new DefaultMuleMessage(payload, props, muleContext);
            boolean matchFound = false;

            // If there is no filter assume that the endpoint can accept the
            // message. Endpoints will be processed in order to only the last
            // (if any) of the the targets may not have a filter
            //Try each endpoint in the list. If there is no match for any of them we drop out and throw an exception
            for (int j = 0; j < endpoints.size(); j++)
            {
                MessageProcessor target =  endpoints.get(counter.next());
                OutboundEndpoint endpoint = target instanceof OutboundEndpoint ? (OutboundEndpoint) target : null;
                if (endpoint == null || endpoint.getFilter() == null || endpoint.getFilter().accept(part))
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Endpoint filter matched. Routing message over: "
                                + endpoint.getEndpointURI().toString());
                    }
                    iterator.remove();
                    splitMessage.addPart(part, endpoint);
                    matchFound = true;
                    break;
                }
            }
            if (!matchFound)
            {
                if (isFailIfNoMatch())
                {
                    throw new IllegalStateException(CoreMessages.splitMessageNoEndpointMatch(endpoints, payload).getMessage());
                }
                else
                {
                    logger.info("No splitter match for message part. 'failIfNoMatch=false' ingoring message part.");
                }
            }

            //start from 0 again
            if (isDisableRoundRobin())
            {
                counter = new Counter();
            }
//            if (enableCorrelation != ENABLE_CORRELATION_NEVER)
//            {
//                // always set correlation group size, even if correlation id
//                // has already been set (usually you don't have group size yet
//                // by this point.
//                final int groupSize = payload.size();
//                message.setCorrelationGroupSize(groupSize);
//                if (logger.isDebugEnabled())
//                {
//                    logger.debug("java.util.List payload detected, setting correlation group size to "
//                                    + groupSize);
//                }
//            }
        }
        return splitMessage;

    }

    /**
     * If this option is true (the default)
     * then the first message part is routed to the first endpoint, the
     * second part to the second endpoint, etc, with the nth part going to
     * the (n modulo number of targets) endpoint.
     * If false then the messages will be distributed equally amongst all
     * targets.
     * <p/>
     * The behaviour changes if the targets have filters since the message part will get routed
     * based on the next endpoint that follows the above rule AND passes the endpoint filter.
     *
     * @return true if deterministic has been set to true
     */
    public boolean isDeterministic()
    {
        return deterministic;
    }

    /**
     * If this option is true (the default)
     * then the first message part is routed to the first endpoint, the
     * second part to the second endpoint, etc, with the nth part going to
     * the (n modulo number of targets) endpoint.
     * If false then the messages will be distributed equally amongst all
     * targets.
     * <p/>
     * The behaviour changes if the targets have filters since the message part will get routed
     * based on the next endpoint that follows the above rule AND passes the endpoint filter.
     *
     * @param deterministic the value to set
     */
    public void setDeterministic(boolean deterministic)
    {
        this.deterministic = deterministic;
    }


    /**
     * The default behaviour for splitter routers is to round-robin across
     * targets. When using filters on targets it is sometimes desirable to use only the filters to
     * control which endpoint the split message part goes too. For example, if you have 3 targets where
     * two have a filter but the last does not, you'll need to disable round robin since the 3rd endpoint
     * may end up routing a message that one of the other targets should have routed.
     * Generally it is good practice to either configure all targets with filters or none, in this case
     * there is not need to set this property.
     *
     * @return true if disabled
     */
    public boolean isDisableRoundRobin()
    {
        return disableRoundRobin;
    }

    /**
     * The default behaviour for splitter routers is to round-robin across
     * targets. When using filters on targets it is sometimes desirable to use only the filters to
     * control which endpoint the split message part goes too. For example, if you have 3 targets where
     * two have a filter but the last does not, you'll need to disable round robin since the 3rd endpoint
     * may end up routing a message that one of the other targets should have routed.
     * Generally it is good practice to either configure all targets with filters or none, in this case
     * there is not need to set this property.
     *
     * @param disableRoundRobin true if disabled
     */
    public void setDisableRoundRobin(boolean disableRoundRobin)
    {
        this.disableRoundRobin = disableRoundRobin;
    }

    /**
     * If none of the targets match a split message part i.e. each endpoint has a
     * filter for a certain message part. This flag controls whether the part is ignorred or an
     * exceptin is thrown.
     *
     * @return true if an exception should be thrown when no match is found
     */
    public boolean isFailIfNoMatch()
    {
        return failIfNoMatch;
    }

    /**
     * If none of the targets match a split message part i.e. each endpoint has a
     * filter for a certain message part. This flag controls whether the part is ignorred or an
     * exceptin is thrown.
     *
     * @param failIfNoMatch true if an exception should be thrown when no match is found
     */
    public void setFailIfNoMatch(boolean failIfNoMatch)
    {
        this.failIfNoMatch = failIfNoMatch;
    }

    private class Counter
    {

        private AtomicInteger counter;

        public Counter()
        {
            if (isDeterministic())
            {
                counter = new AtomicInteger(0);
            }
            else
            {
                counter = globalCounter;
            }
        }

        public int next()
        {
            return counter.getAndIncrement() % getRoutes().size();
        }

    }
}
