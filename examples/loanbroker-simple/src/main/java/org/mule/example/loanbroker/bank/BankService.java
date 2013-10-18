/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
