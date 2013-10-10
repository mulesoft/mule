/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.loanbroker.processor;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.TransformerException;
import org.mule.example.loanbroker.model.LoanQuote;

import java.util.List;

public class LowestQuoteProcessor implements MessageProcessor
{

    public MuleEvent process(MuleEvent event) throws TransformerException
    {

        Object payload = event.getMessage().getPayload();
        LoanQuote lowestQuote = null;

        if (payload instanceof LoanQuote)
        {
            lowestQuote = (LoanQuote) payload;
        }
        else
        {
            @SuppressWarnings("unchecked")
            List<LoanQuote> loanQuotes = (List<LoanQuote>) payload;
            for (LoanQuote loanQuote : loanQuotes)
            {

                if (lowestQuote == null)
                {
                    lowestQuote = loanQuote;
                }
                else
                {
                    if (loanQuote.getInterestRate() < lowestQuote.getInterestRate())
                    {
                        lowestQuote = loanQuote;
                    }
                }
            }
        }
        return new DefaultMuleEvent(new DefaultMuleMessage(lowestQuote, event.getMuleContext()), event);
    }

}
