/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.samples.loanbroker;

import java.io.Serializable;

/**
 * <code>BankQuoteRequest</code> represents customer a request for a loan broker
 * through a loan broker
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class BankQuoteRequest implements Serializable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 8302037103569473302L;

    private LoanRequest loanRequest;
    private Bank[] lenders;

    public BankQuoteRequest()
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

    public LoanRequest getLoanRequest()
    {
        return loanRequest;
    }

    public void setLoanRequest(LoanRequest loanRequest)
    {
        this.loanRequest = loanRequest;
    }
}
