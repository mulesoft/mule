/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.loanbroker.messages;

import java.io.Serializable;

/**
 * <code>CreditProfile</code> is a dummy finance profile for a customer
 */
public class CreditProfile implements Serializable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -5924690191061177417L;

    private int creditScore;
    private int creditHistory;

    public CreditProfile()
    {
        super();
    }

    public int getCreditScore()
    {
        return creditScore;
    }

    public void setCreditScore(int creditScore)
    {
        this.creditScore = creditScore;
    }

    public int getCreditHistory()
    {
        return creditHistory;
    }

    public void setCreditHistory(int creditHistory)
    {
        this.creditHistory = creditHistory;
    }

}
