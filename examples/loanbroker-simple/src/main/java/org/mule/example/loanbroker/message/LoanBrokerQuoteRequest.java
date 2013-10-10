/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
