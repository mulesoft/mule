/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.loanbroker;

import org.mule.examples.loanbroker.messages.CustomerQuoteRequest;
import org.mule.examples.loanbroker.messages.LoanQuote;

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
