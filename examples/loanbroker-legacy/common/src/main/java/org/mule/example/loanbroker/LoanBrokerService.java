/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.loanbroker;

import org.mule.example.loanbroker.messages.CustomerQuoteRequest;
import org.mule.example.loanbroker.messages.LoanQuote;

/**
 * <code>LoanBroker</code> is the Service that starts the loan request process. The
 * broker also receives the final quote.
 */
public interface LoanBrokerService
{
    /**
     * Triggered by an incoming customer request for a loan.
     * @return Outgoing payload will depend on the implementation
     */
    Object getLoanQuote(CustomerQuoteRequest request) throws LoanBrokerException;

    /**
     * Triggered by an incoming offer from a bank.
     * @return Outgoing payload will depend on the implementation
     */
    Object receiveQuote(LoanQuote quote);
}
