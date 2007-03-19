/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.loanbroker.routers;

import org.mule.config.i18n.Message;
import org.mule.examples.loanbroker.messages.LoanQuote;
import org.mule.impl.MuleMessage;
import org.mule.routing.inbound.EventGroup;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BankQuotesAggregationLogic
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(BankQuotesAggregationLogic.class);
    
    public static UMOMessage aggregateEvents(EventGroup events) throws Exception
    {
        LoanQuote lowestQuote = null;
        LoanQuote quote = null;
        UMOEvent event = null;

        for (Iterator iterator = events.iterator(); iterator.hasNext();)
        {
            event = (UMOEvent)iterator.next();
            quote = (LoanQuote)event.getTransformedMessage();
            logger.info(new Message("loanbroker-example", 5, quote.toString()).getMessage());

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

        logger.info(new Message("loanbroker-example", 6, lowestQuote.toString()).getMessage());
        return new MuleMessage(lowestQuote, event.getMessage());
    }
}


