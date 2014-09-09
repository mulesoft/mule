/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.loanbroker.bank;

import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.example.loanbroker.LocaleMessage;
import org.mule.example.loanbroker.messages.LoanBrokerQuoteRequest;
import org.mule.example.loanbroker.messages.LoanQuote;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>Bank</code> is a representation of a bank from which to obtain loan
 * quotes.
 */

public class Bank implements FlowConstructAware, Serializable, BankService
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 4108271137166107769L;

    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(Bank.class);

    private String bankName;
    private double primeRate;
    
    public Bank()
    {
        this.primeRate = Math.random() * 10;
    }

    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.bankName = flowConstruct.getName(); 
    }

    public LoanQuote getLoanQuote(LoanBrokerQuoteRequest request)
    {
        LoanQuote quote = new LoanQuote();
        quote.setBankName(getBankName());
        quote.setInterestRate(primeRate);
        logger.info(LocaleMessage.receivedRate(quote));

        return quote;
    }

    public String getBankName()
    {
        return bankName;
    }

    public void setBankName(String bankName)
    {
        this.bankName = bankName;
    }

    public double getPrimeRate()
    {
        return primeRate;
    }

    public void setPrimeRate(double primeRate)
    {
        this.primeRate = primeRate;
    }
}
