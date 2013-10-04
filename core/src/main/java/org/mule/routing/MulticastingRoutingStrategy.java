/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.VoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.api.routing.RoutePathNotFoundException;
import org.mule.api.routing.RouterResultsHandler;
import org.mule.config.i18n.CoreMessages;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * Routing strategy that will route a message through a set of {@link MessageProcessor}
 * and return an aggregation of the results.
 *
 */
public class MulticastingRoutingStrategy extends AbstractRoutingStrategy
{
    protected transient Log logger = LogFactory.getLog(getClass());
    private final RouterResultsHandler resultsHandler;

    /**
     * @param muleContext
     * @param resultAggregator aggregator used to create a response event
     */
    public MulticastingRoutingStrategy(MuleContext muleContext, RouterResultsHandler resultAggregator)
    {
        super(muleContext);
        this.resultsHandler = resultAggregator;
    }

    @Override
    public MuleEvent route(MuleEvent event, List<MessageProcessor> messageProcessors) throws MessagingException
    {
        MuleMessage message = event.getMessage();

        if (messageProcessors == null || messageProcessors.size() == 0)
        {
            throw new RoutePathNotFoundException(CoreMessages.noEndpointsForRouter(), event, null);
        }

        List<MuleEvent> results = new ArrayList<MuleEvent>(messageProcessors.size());

        validateMessageIsNotConsumable(event, message);

        try
        {
            for (int i = 0; i < messageProcessors.size(); i++)
            {
                MessageProcessor mp = messageProcessors.get(i);
                MuleMessage clonedMessage = cloneMessage(message,getMuleContext());
                AbstractRoutingStrategy.propagateMagicProperties(clonedMessage,clonedMessage);
                MuleEvent result = sendRequest(event, clonedMessage, mp, true);
                if (result != null && !VoidMuleEvent.getInstance().equals(result))
                {
                    results.add(result);
                }
            }
        }
        catch (MuleException e)
        {
            throw new CouldNotRouteOutboundMessageException(event, messageProcessors.get(0), e);
        }
        return resultsHandler.aggregateResults(results, event, getMuleContext());
    }



}
