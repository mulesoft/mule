/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.loanbroker.bank;

import org.mule.example.loanbroker.message.LoanBrokerQuoteRequest;
import org.mule.example.loanbroker.model.LoanQuote;

import java.io.Serializable;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>Bank</code> is a representation of a bank from which to obtain loan
 * quotes.
 */

public class Bank implements Serializable, BankService
{

    /**
     * Serial version
     */
    private static final long serialVersionUID = 4108271137166107769L;

    private static final int PLATINUM_PROFILE_INDEX = 0;
    private static final int GOLD_PROFILE_INDEX = 1;
    private static final int BASIC_PROFILE_INDEX = 2;

    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(Bank.class);

    private String bankName;
    private final double[] rates;

    public Bank()
    {
        rates = new double[] {Math.random() * 10, Math.random() * 10, Math.random() * 10};
        Arrays.sort(rates);
    }

    public LoanQuote getLoanQuote(LoanBrokerQuoteRequest request)
    {
        LoanQuote quote = new LoanQuote();
        quote.setBankName(getBankName());
        int creditScore = request.getCreditProfile().getCreditScore();
        quote.setInterestRate(getCreditScoreRate(creditScore));
        logger.info("Returning Rate is: " + quote);

        return quote;
    }

    private double getCreditScoreRate(int creditScore)
    {
        // 300 <= creditScore < 900, higher values means better customer credit profile
        int index;
        if (creditScore < 500)
        {
            index = BASIC_PROFILE_INDEX;
        }
        else if (creditScore < 700)
        {
            index = GOLD_PROFILE_INDEX;
        }
        else
        {
            index = PLATINUM_PROFILE_INDEX;
        }

        return rates[index];
    }

    public String getBankName()
    {
        return bankName;
    }

    public void setBankName(String bankName)
    {
        this.bankName = bankName;
    }
}
