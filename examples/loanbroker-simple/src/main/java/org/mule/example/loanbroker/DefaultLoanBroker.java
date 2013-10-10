/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.loanbroker;

import org.mule.example.loanbroker.bank.Bank;
import org.mule.example.loanbroker.creditagency.CreditAgencyService;
import org.mule.example.loanbroker.message.CustomerQuoteRequest;
import org.mule.example.loanbroker.message.LoanBrokerQuoteRequest;
import org.mule.example.loanbroker.model.CreditProfile;

/**
 * <code>SyncLoanBroker</code> is a synchronous Loan Broker that makes the calls to
 * various components through the event context synchronously.
 */
public class DefaultLoanBroker extends AbstractLoanBroker
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

        setLenderList(bqr);

        return bqr;
    }

    /**
     * Sets the list of lenders on the LoanBrokerQuoteRequest and returns it.
     */
    public void setLenderList(LoanBrokerQuoteRequest request)
    {
        Bank[] lenders = getLenders(request.getCreditProfile(), request.getCustomerRequest()
                .getLoanAmount());
        request.setLenders(lenders);
    }

    public Bank[] getLenders(CreditProfile creditProfile, Double loanAmount)
    {
        // TODO Add creditProfile info. to the logic below.
        // TODO Look up the existing banks from the config/registry instead of
        // creating them programatically here.
        Bank[] lenders;
        if ((loanAmount >= 20000))
        {
            lenders = new Bank[2];
            lenders[0] = new Bank("Bank1");
            lenders[1] = new Bank("Bank2");
        }
        else if (((loanAmount >= 10000) && (loanAmount <= 19999)))
        {
            lenders = new Bank[2];
            lenders[0] = new Bank("Bank3");
            lenders[1] = new Bank("Bank4");
        }
        else
        {
            lenders = new Bank[1];
            lenders[0] = new Bank("Bank5");
        }

        return lenders;
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
