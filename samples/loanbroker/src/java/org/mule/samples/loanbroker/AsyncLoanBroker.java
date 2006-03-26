/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.samples.loanbroker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.samples.loanbroker.service.LoanBroker;

/**
 * <code>LoanBroker</code> is the Service that starts the loan
 * request process.  The broker also receives the final quote.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class AsyncLoanBroker implements LoanBroker
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(AsyncLoanBroker.class);

    private static volatile int quotes = 0;
    private static volatile int requests = 0;
    private static long start = 0;
    public BankQuoteRequest getLoanQuote(LoanRequest request) {

        logger.info("\nClient " + request.getCustomer().getName() + " with ssn= " + request.getCustomer().getSsn() + " requests a loan of amount= " + request.getLoanAmount() + " for " + request.getLoanDuration() + " months");
        BankQuoteRequest bqr = new BankQuoteRequest();
        bqr.setLoanRequest(request);
        //System.out.println("ORequest: " + incRequests());

        return bqr ;
  }

    public Object receiveQuote(LoanQuote quote) {
        logger.info("\nLoan Broker Quote received: " + quote);
        System.out.println("Quote " + incQuotes() + " received: " + quote);

        //System.out.println("OQuote: " + incQuotes());
        return null;
    }

    public synchronized int incQuotes()
    {
//        if(quotes % 100 == 0) {
//            System.out.println("%% Received " + quotes + " quotes in: " + (System.currentTimeMillis() - start));
//        }
        return ++quotes;
    }

    public synchronized int incRequests()
    {
        if(requests==0) start = System.currentTimeMillis();
//        if(requests % 100 == 0) {
//            System.out.println("## Sent " + requests + " messages in: " + (System.currentTimeMillis() - start));
//        }
        return ++requests;
    }
}
