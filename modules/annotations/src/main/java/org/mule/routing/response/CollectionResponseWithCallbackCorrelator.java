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

import org.mule.DefaultMuleEvent;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.config.annotations.i18n.AnnotationsMessages;
import org.mule.routing.AggregationException;
import org.mule.routing.CollectionCorrelatorCallback;
import org.mule.routing.inbound.EventGroup;

/**
 * A Correlator that correlates messages based on Mule correlation settings. This callback allows the
 * user to set a callback method on the service component that recieves the message. Any changes made in the
 * callback method will be sent back to the client that initiated the request.
 */
public class CollectionResponseWithCallbackCorrelator extends CollectionCorrelatorCallback
{

    protected String callback;

    public CollectionResponseWithCallbackCorrelator(String callbackMethod, MuleContext muleContext)
    {
        super(muleContext);
        this.callback = callbackMethod;
    }

    /**
     * The <code>SingleResponseRouter</code> will always return the first event of
     * an event group.
     *
     * @param events the event group for this request
     * @return an aggregated message
     * @throws org.mule.routing.AggregationException
     *          if the aggregation fails. In
     *          this scenario the whole event group is removed and passed to the
     *          exception handler for this componenet.
     * @see {@link AbstractResponseAggregator#aggregateEvents(org.mule.routing.inbound.EventGroup)}
     */
    public MuleMessage aggregateEvents(EventGroup events) throws AggregationException
    {
        MuleEvent receivedEvent = (MuleEvent) events.iterator().next();
        MuleMessage result = super.aggregateEvents(events);
        MuleEvent event = new DefaultMuleEvent(result, receivedEvent.getEndpoint(), receivedEvent.getService(), receivedEvent);

        try
        {
            return event.getService().getComponent().invoke(event);
        }
        catch (MuleException e)
        {
            throw new AggregationException(AnnotationsMessages.failedToInvokeReplyMethod(callback), events, event.getEndpoint());
        }
    }
}