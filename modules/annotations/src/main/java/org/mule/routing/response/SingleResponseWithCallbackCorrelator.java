/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.response;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.routing.RoutingException;
import org.mule.api.transport.PropertyScope;
import org.mule.config.annotations.i18n.AnnotationsMessages;
import org.mule.routing.EventCorrelatorCallback;
import org.mule.routing.inbound.EventGroup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A Correlator that correlates one message at a time, esscentially a RPC callback. This callback allows the
 * user to set a callback method on the service component that recieves the message. Any changes made in the
 * callback method will be sent back to the client that initiated the request.
 */
public class SingleResponseWithCallbackCorrelator implements EventCorrelatorCallback
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(SingleResponseWithCallbackCorrelator.class);

    protected String callback;

    public SingleResponseWithCallbackCorrelator(String callbackMethod)
    {
        this.callback = callbackMethod;
    }

    /**
     * The <code>SingleResponseRouter</code> will return true if the event group
     * size is 1. If the group size is greater than 1, a warning will be logged.
     *
     * @param events event group to consider
     * @return true if the event group size is 1 or greater
     * @see {@link org.mule.routing.EventCorrelatorCallback#shouldAggregateEvents(EventGroup)}
     */
    public boolean shouldAggregateEvents(EventGroup events)
    {
        if (events.expectedSize() > 1)
        {
            logger.warn("CorrelationGroup's expected size is not 1."
                    + " The SingleResponseAggregator will only handle single replyTo events;"
                    + " if there will be multiple events for a single request, "
                    + " use the 'ResponseCorrelationAggregator'");
        }

        return (events.size() != 0);
    }

    /**
     * The <code>SingleResponseRouter</code> will always return the first event of
     * an event group.
     *
     * @param events the event group for this request
     * @return an aggregated message
     * @throws org.mule.api.routing.RoutingException
     *          if the aggregation fails. In
     *          this scenario the whole event group is removed and passed to the
     *          exception handler for this component.
     * @see {@link org.mule.routing.response.AbstractResponseAggregator#aggregateEvents(EventGroup)}
     */
    public MuleMessage aggregateEvents(EventGroup events) throws RoutingException
    {
        MuleEvent event = (MuleEvent) events.iterator().next();
        //Setting a callback is optional
        if(callback!=null)
        {
            event.getMessage().setProperty(MuleProperties.MULE_METHOD_PROPERTY, callback, PropertyScope.INVOCATION);
            try
            {
                return event.getService().getComponent().invoke(event);
            }
            catch (MuleException e)
            {
                throw new RoutingException(AnnotationsMessages.failedToInvokeReplyMethod(callback), event.getMessage(),
                            event.getEndpoint());
            }
        }
        else
        {
            return event.getMessage();
        }
    }

    public EventGroup createEventGroup(MuleEvent event, Object id)
    {
        return new EventGroup(id);
    }
}
