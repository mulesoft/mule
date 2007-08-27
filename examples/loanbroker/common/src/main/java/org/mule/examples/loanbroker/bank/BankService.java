/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.loanbroker.bank;

import org.mule.examples.loanbroker.messages.LoanBrokerQuoteRequest;
import org.mule.examples.loanbroker.messages.LoanQuote;

/**
 * <code>BankService</code> is a representation of a bank form which to obtain loan
 * quotes.
 */
public interface BankService
{
    LoanQuote getLoanQuote(LoanBrokerQuoteRequest request);
}
