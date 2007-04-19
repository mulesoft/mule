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

import org.mule.examples.loanbroker.messages.CustomerQuoteRequest;
import org.mule.examples.loanbroker.messages.LoanBrokerQuoteRequest;

/**
 * <code>LoanBroker</code> is the Service that starts the loan request process. The
 * broker also receives the final quote.
 */
public class AsynchronousLoanBroker extends DefaultLoanBroker
{
    public Object getLoanQuote(CustomerQuoteRequest request) throws LoanBrokerException
    {
        super.getLoanQuote(request);
        LoanBrokerQuoteRequest bqr = new LoanBrokerQuoteRequest();
        bqr.setCustomerRequest(request);
        return bqr;
    }
}
