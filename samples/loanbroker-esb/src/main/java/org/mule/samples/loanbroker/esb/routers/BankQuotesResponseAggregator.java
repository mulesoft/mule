/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.samples.loanbroker.esb.routers;

import org.mule.config.i18n.Message;
import org.mule.impl.MuleMessage;
import org.mule.routing.inbound.EventGroup;
import org.mule.routing.response.ResponseCorrelationAggregator;
import org.mule.samples.loanbroker.esb.message.LoanQuote;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.routing.RoutingException;
import org.mule.umo.transformer.TransformerException;

import java.util.Iterator;

/**
 * <code>BankQuotesAggregator</code> receives a number of quotes and selectes the
 * lowest
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class BankQuotesResponseAggregator extends ResponseCorrelationAggregator
{
    /**
     * This method is invoked if the shouldAggregate method is called and returns
     * true. Once this method returns an aggregated message the event group is
     * removed from the router
     * 
     * @param events
     *            the event group for this request
     * @return an aggregated message
     * @throws org.mule.umo.routing.RoutingException
     *             if the aggregation fails. in this scenario the whole event group
     *             is removed and passed to the exception handler for this componenet
     */
    protected UMOMessage aggregateEvents(EventGroup events) throws RoutingException
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
            logger.info("Lowest quote is: " + lowestQuote);
            return new MuleMessage(lowestQuote, event.getMessage());
        }
        catch (TransformerException e)
        {
            throw new RoutingException(Message.createStaticMessage("Failed to get lowest quote"),
                    new MuleMessage(events), null, e);
        }
    }
}
