/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.loanbroker.esn;

import org.mule.example.loanbroker.DefaultLoanBroker;
import org.mule.example.loanbroker.LoanBrokerException;
import org.mule.example.loanbroker.credit.CreditAgencyService;
import org.mule.example.loanbroker.messages.CreditProfile;
import org.mule.example.loanbroker.messages.CustomerQuoteRequest;
import org.mule.example.loanbroker.messages.LoanBrokerQuoteRequest;

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

    @Override
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
