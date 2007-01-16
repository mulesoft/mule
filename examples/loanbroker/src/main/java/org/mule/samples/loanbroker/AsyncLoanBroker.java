/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.loanbroker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.samples.loanbroker.service.LoanBroker;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

/**
 * <code>LoanBroker</code> is the Service that starts the loan request process. The
 * broker also receives the final quote.
 */
public class AsyncLoanBroker implements LoanBroker
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(AsyncLoanBroker.class);

    private static final AtomicInteger quotes = new AtomicInteger(0);
    private static final AtomicInteger requests = new AtomicInteger(0);

    public AsyncLoanBroker()
    {
        super();
    }

    public BankQuoteRequest getLoanQuote(LoanRequest request)
    {
        logger.info("\nClient " + request.getCustomer().getName() + " with ssn= "
                        + request.getCustomer().getSsn() + " requests a loan of amount= "
                        + request.getLoanAmount() + " for " + request.getLoanDuration() + " months");
        BankQuoteRequest bqr = new BankQuoteRequest();
        bqr.setLoanRequest(request);
        return bqr;
    }

    public Object receiveQuote(LoanQuote quote)
    {
        System.out.println("Quote " + incQuotes() + " received: " + quote);
        return null;
    }

    public int incQuotes()
    {
        return quotes.incrementAndGet();
    }

    public int incRequests()
    {
        return requests.incrementAndGet();
    }

}
