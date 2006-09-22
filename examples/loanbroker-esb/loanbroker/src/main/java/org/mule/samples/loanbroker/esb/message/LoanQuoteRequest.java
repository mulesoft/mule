/*
 * $Id:LoanQuoteRequest.java 2944 2006-09-05 10:38:45 +0000 (Tue, 05 Sep 2006) tcarlson $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.samples.loanbroker.esb.message;

import org.mule.samples.loanbroker.esb.bank.Bank;

import java.io.Serializable;

/**
 * <code>LoanQuoteRequest</code> represents a customer request for a loan
 * through a loan broker
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision:2944 $
 */
public class LoanQuoteRequest implements Serializable
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
    private Bank[] lenders;

    /** A loan quote from a bank */
    private LoanQuote loanQuote;

    public LoanQuoteRequest()
    {
        super();
    }

    public Bank[] getLenders()
    {
        return lenders;
    }

    public void setLenders(Bank[] lenders)
    {
        this.lenders = lenders;
    }

    public CustomerQuoteRequest getCustomerRequest()
    {
        return customerRequest;
    }

    public void setCustomerRequest(CustomerQuoteRequest customerRequest)
    {
        this.customerRequest = customerRequest;
    }

    public CreditProfile getCreditProfile() {
        return creditProfile;
    }

    public void setCreditProfile(CreditProfile creditProfile) {
        this.creditProfile = creditProfile;
    }

    public LoanQuote getLoanQuote() {
        return loanQuote;
    }

    public void setLoanQuote(LoanQuote loanQuote) {
        this.loanQuote = loanQuote;
    }
}
