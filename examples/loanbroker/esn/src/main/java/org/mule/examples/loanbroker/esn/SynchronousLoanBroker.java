/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.loanbroker.esn;

import org.mule.examples.loanbroker.DefaultLoanBroker;
import org.mule.examples.loanbroker.LoanBrokerException;
import org.mule.examples.loanbroker.credit.CreditAgencyService;
import org.mule.examples.loanbroker.messages.CreditProfile;
import org.mule.examples.loanbroker.messages.CustomerQuoteRequest;
import org.mule.examples.loanbroker.messages.LoanBrokerQuoteRequest;

/**
 * <code>SyncLoanBroker</code> is a synchronous Loan Broker that makes the calls to
 * various components through the event context synchronously.
 */
public class SynchronousLoanBroker extends DefaultLoanBroker
{
    //A proxy for this object gets injected via the <nested-router> element configured on this service.
    //The proxy will then call out to an endpoint and return the result.
    //The transformers configured on the endpoint control how data is marshalled into and out of the call.
    private CreditAgencyService creditAgency;

    public Object getLoanQuote(CustomerQuoteRequest request) throws LoanBrokerException
    {
        super.getLoanQuote(request);
        LoanBrokerQuoteRequest bqr = new LoanBrokerQuoteRequest();
        bqr.setCustomerRequest(request);

        //This calls out to the CreditAgency service (see above)
        CreditProfile cp = creditAgency.getCreditProfile(request.getCustomer());
        bqr.setCreditProfile(cp);

        return bqr;
    }

    public CreditAgencyService getCreditAgency()
    {
        return creditAgency;
    }

    public void setCreditAgency(CreditAgencyService creditAgency)
    {
        this.creditAgency = creditAgency;
    }
}
