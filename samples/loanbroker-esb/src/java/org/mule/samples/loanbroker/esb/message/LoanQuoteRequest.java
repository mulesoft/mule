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
package org.mule.samples.loanbroker.esb.message;

import org.mule.samples.loanbroker.esb.Bank;

import java.io.Serializable;

/**
 * <code>LoanQuoteRequest</code> represents customer a request for a loan broker
 * thriugh a loan broker
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class LoanQuoteRequest implements Serializable
{
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
