/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.loanbroker.messages;

import org.mule.example.loanbroker.LocaleMessage;

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
        return LocaleMessage.loanQuote(bankName, interestRate);
    }
}
