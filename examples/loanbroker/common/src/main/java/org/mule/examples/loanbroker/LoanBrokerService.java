/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.loanbroker;

import org.mule.examples.loanbroker.messages.CustomerQuoteRequest;
import org.mule.examples.loanbroker.messages.LoanBrokerQuoteRequest;
import org.mule.examples.loanbroker.messages.LoanQuote;

/**
 * <code>LoanBroker</code> is the Service that starts the loan request process. The
 * broker also receives the final quote.
 */
public interface LoanBrokerService
{
    /**
     * Triggered by an incoming customer request for a loan.
     * @return LoanBrokerQuoteRequest to send to the banks.
     */
	LoanBrokerQuoteRequest getLoanQuote(CustomerQuoteRequest request) throws LoanBrokerException;

    /**
     * Triggered by an incoming offer from a bank.
     * @return LoanQuote to return to the customer.
     */
    LoanQuote receiveQuote(LoanQuote quote);
}
