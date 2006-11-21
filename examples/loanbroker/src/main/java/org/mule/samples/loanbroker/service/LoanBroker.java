/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.loanbroker.service;

import org.mule.samples.loanbroker.BankQuoteRequest;
import org.mule.samples.loanbroker.LoanQuote;
import org.mule.samples.loanbroker.LoanRequest;

/**
 * <code>LoanBroker</code> is the Servie that starts the loan request process. The
 * broker also receives the final quote.
 */
public interface LoanBroker
{
    BankQuoteRequest getLoanQuote(LoanRequest request);

    Object receiveQuote(LoanQuote quote);
}
