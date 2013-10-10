/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.loanbroker.bank;

import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.service.Service;
import org.mule.api.service.ServiceAware;
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

public class Bank implements ServiceAware, FlowConstructAware, Serializable, BankService
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

    public void setService(Service service)
    {
        this.bankName = service.getName(); 
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
