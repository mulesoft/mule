/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.loanbroker;

import org.mule.config.i18n.Message;
import org.mule.examples.loanbroker.messages.CustomerQuoteRequest;
import org.mule.examples.loanbroker.messages.LoanBrokerQuoteRequest;
import org.mule.examples.loanbroker.messages.LoanQuote;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>LoanBroker</code> is the Service that starts the loan request process. The
 * broker also receives the final quote.
 */
public class DefaultLoanBroker implements LoanBrokerService
{
    /**
     * logger used by this class
     */
    protected final Log logger = LogFactory.getLog(getClass());
    
    private final AtomicInteger quotes = new AtomicInteger(0);
    private final AtomicInteger requests = new AtomicInteger(0);

    public LoanBrokerQuoteRequest getLoanQuote(CustomerQuoteRequest request) throws LoanBrokerException
    {
        int requests = incRequests();
        if (logger.isInfoEnabled())
        {
            String[] params = new String[] { String.valueOf(requests), 
                request.getCustomer().getName(), 
                String.valueOf(request.getCustomer().getSsn()), 
                String.valueOf(request.getLoanAmount()),
                String.valueOf(request.getLoanDuration()) };

            logger.info("\n***** " + new Message("loanbroker-example", 1, params).getMessage());
        }        
        LoanBrokerQuoteRequest bqr = new LoanBrokerQuoteRequest();
        bqr.setCustomerRequest(request);
        return bqr;
    }

    public LoanQuote receiveQuote(LoanQuote quote)
    {
        int quotes = incQuotes();
        if (logger.isInfoEnabled())
        {
            String[] params = new String[] { String.valueOf(quotes), 
                quote.toString() };
            logger.info("\n***** " + new Message("loanbroker-example", 2, params).getMessage());
        }
        return quote;
    }

    protected int incQuotes()
    {
        return quotes.incrementAndGet();
    }

    protected int incRequests()
    {
        return requests.incrementAndGet();
    }
}
