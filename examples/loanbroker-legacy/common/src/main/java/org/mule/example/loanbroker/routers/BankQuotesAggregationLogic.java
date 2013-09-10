/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.loanbroker.routers;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.example.loanbroker.LocaleMessage;
import org.mule.example.loanbroker.messages.LoanQuote;
import org.mule.routing.EventGroup;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BankQuotesAggregationLogic
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(BankQuotesAggregationLogic.class);

    public static MuleEvent aggregateEvents(EventGroup events) throws Exception
    {
        LoanQuote lowestQuote = null;
        LoanQuote quote = null;
        MuleEvent event = null;

        for (Iterator<MuleEvent> iterator = events.iterator(false); iterator.hasNext();)
        {
            event = iterator.next();
            Object o = event.getMessage().getPayload();
            if(o instanceof LoanQuote)
            {
                quote = (LoanQuote)o;
            }
            else
            {
                throw new IllegalArgumentException("Object received by Aggregator is not of expected type. Wanted: "
                        + LoanQuote.class.getName() + " Got: " + o);
            }
            logger.info(LocaleMessage.processingQuote(quote));

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

        logger.info(LocaleMessage.lowestQuote(lowestQuote));
        return new DefaultMuleEvent(new DefaultMuleMessage(lowestQuote, event.getMessage(),
            event.getMuleContext()), event);
    }
}
