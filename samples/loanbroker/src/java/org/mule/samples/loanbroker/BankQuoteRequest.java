/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.samples.loanbroker;

import java.io.Serializable;

/**
 * <code>BankQuoteRequest</code> represents customer a request for a loan broker
 * thriugh a loan broker
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class BankQuoteRequest implements Serializable
{
    private LoanRequest loanRequest;
    private Bank[] lenders;

    public BankQuoteRequest()
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

    public LoanRequest getLoanRequest()
    {
        return loanRequest;
    }

    public void setLoanRequest(LoanRequest loanRequest)
    {
        this.loanRequest = loanRequest;
    }
}
