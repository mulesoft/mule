/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.loanbroker.bank;

import org.mule.example.loanbroker.message.LoanBrokerQuoteRequest;
import org.mule.example.loanbroker.model.LoanQuote;

import javax.jws.WebService;

/**
 * <code>BankService</code> is a representation of a bank form which to obtain loan
 * quotes.
 */
@WebService
public interface BankService
{
    LoanQuote getLoanQuote(LoanBrokerQuoteRequest request);
}
