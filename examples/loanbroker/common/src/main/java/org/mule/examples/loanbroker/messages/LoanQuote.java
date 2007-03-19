/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.loanbroker.messages;

import org.mule.config.i18n.Message;

import java.io.Serializable;

/**
 * <code>LoanQuote</code> is a loan quote from a bank
 */

public class LoanQuote implements Serializable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -8432932027217141564L;

    private String bankName;
    private double interestRate = 0;

    public LoanQuote()
    {
        super();
    }

    public String getBankName()
    {
        return bankName;
    }

    public void setBankName(String bankName)
    {
        this.bankName = bankName;
    }

    public double getInterestRate()
    {
        return interestRate;
    }

    public void setInterestRate(double interestRate)
    {
        this.interestRate = interestRate;
    }

    public String toString()
    {
        return new Message("loanbroker-example", 4, bankName, String.valueOf(interestRate)).getMessage();
    }
}
