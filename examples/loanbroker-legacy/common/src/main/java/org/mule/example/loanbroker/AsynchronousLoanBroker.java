/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.loanbroker;

import org.mule.example.loanbroker.messages.CustomerQuoteRequest;
import org.mule.example.loanbroker.messages.LoanBrokerQuoteRequest;

/**
 * <code>LoanBroker</code> is the Service that starts the loan request process. The
 * broker also receives the final quote.
 */
public class AsynchronousLoanBroker extends DefaultLoanBroker
{
    @Override
    public Object getLoanQuote(CustomerQuoteRequest request) throws LoanBrokerException
    {
        super.getLoanQuote(request);
        LoanBrokerQuoteRequest bqr = new LoanBrokerQuoteRequest();
        bqr.setCustomerRequest(request);
        return bqr;
    }
}
