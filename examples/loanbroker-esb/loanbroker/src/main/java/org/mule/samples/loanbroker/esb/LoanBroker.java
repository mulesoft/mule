/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.samples.loanbroker.esb;

import org.mule.samples.loanbroker.esb.message.CustomerQuoteRequest;
import org.mule.samples.loanbroker.esb.message.LoanQuoteRequest;

/**
 * <code>LoanBroker</code> is the Service that starts the loan
 * request process.  The broker also receives the final quote.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class LoanBroker
{
    /**
     * Triggers the Loan Broker process.  The LoanQuoteRequest returned is sent on the bus.
     * Because the LoanBroker component has a ResponseRouter configured on it it will not return
     * to the callee until the responseRouter and processed.  In this case it waits until  all
     * loan requests to the banks have been returned and then returns the lowest LoanQuote to
     * the callee.
     *
     * @param request
     * @return
     * @throws Exception
     */
    public LoanQuoteRequest requestLoanQuote(CustomerQuoteRequest request) throws Exception
    {
        LoanQuoteRequest bqr = new LoanQuoteRequest();
        bqr.setCustomerRequest(request);
        return bqr;
  }
}
