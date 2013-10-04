/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.loanbroker.message;

import org.mule.example.loanbroker.model.CreditProfile;
import org.mule.example.loanbroker.model.LoanQuote;

import java.io.Serializable;

/**
 * <code>LoanQuoteRequest</code> represents a customer request for a loan through a
 * loan broker
 */
public class LoanBrokerQuoteRequest implements Serializable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 46866005259682607L;

    /** The customer request */
    private CustomerQuoteRequest customerRequest;

    /** credit profile for the customer */
    private CreditProfile creditProfile;

    /** A list of lenders for this request */
    //private Bank[] lenders;

    /** A loan quote from a bank */
    private LoanQuote loanQuote;

    public LoanBrokerQuoteRequest()
    {
        super();
    }

    public CustomerQuoteRequest getCustomerRequest()
    {
        return customerRequest;
    }

    public void setCustomerRequest(CustomerQuoteRequest customerRequest)
    {
        this.customerRequest = customerRequest;
    }

    public CreditProfile getCreditProfile()
    {
        return creditProfile;
    }

    public void setCreditProfile(CreditProfile creditProfile)
    {
        this.creditProfile = creditProfile;
    }

    public LoanQuote getLoanQuote()
    {
        return loanQuote;
    }

    public void setLoanQuote(LoanQuote loanQuote)
    {
        this.loanQuote = loanQuote;
    }
}
