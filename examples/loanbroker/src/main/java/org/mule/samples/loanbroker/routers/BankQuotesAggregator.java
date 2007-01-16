/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.loanbroker.routers;

import org.mule.config.i18n.Message;
import org.mule.impl.MuleMessage;
import org.mule.routing.AggregationException;
import org.mule.routing.inbound.CorrelationAggregator;
import org.mule.routing.inbound.EventGroup;
import org.mule.samples.loanbroker.LoanQuote;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.transformer.TransformerException;

import java.util.Iterator;

/**
 * <code>BankQuotesAggregator</code> receives a number of quotes and selects the
 * lowest
 */
public class BankQuotesAggregator extends CorrelationAggregator
{
    /**
     * This method is invoked if the shouldAggregate method is called and returns
     * true. Once this method returns an aggregated message the event group is
     * removed from the router
     * 
     * @param events the event group for this request
     * @return an aggregated message
     * @throws AggregationException if the aggregation fails. in this scenario the
     *             whole event group is removed and passed to the exception handler
     *             for this componenet
     */
    protected UMOMessage aggregateEvents(EventGroup events) throws AggregationException
    {
        try
        {
            LoanQuote lowestQuote = null;
            LoanQuote quote = null;
            UMOEvent event = null;

            for (Iterator iterator = events.iterator(); iterator.hasNext();)
            {
                event = (UMOEvent)iterator.next();
                quote = (LoanQuote)event.getTransformedMessage();
                logger.info("Processing quote: " + quote);

                if (lowestQuote == null)
                {
                    lowestQuote = quote;
                }
                else
                {
                    if (quote.getInterestRate() < lowestQuote.getInterestRate())
                    {
                        lowestQuote = quote;
                    }
                }
            }

            return new MuleMessage(lowestQuote, event.getMessage());
        }
        catch (TransformerException e)
        {
            throw new AggregationException(Message.createStaticMessage("Failed to get lowest quote"), events,
                null, e);
        }
    }

    /**
     * Determines if the event group is ready to be aggregated; this is entirely up
     * to the application. It could be determined by volume, last modified time or
     * some oher criteria based on the last event received.
     * 
     * @param events event group to examine
     * @return true if the events are ready to be aggregated
     */
    protected boolean shouldAggregateEvents(EventGroup events)
    {
        return super.shouldAggregateEvents(events);
    }

}
