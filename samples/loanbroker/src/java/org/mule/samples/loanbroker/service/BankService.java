/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.samples.loanbroker.service;

import org.mule.samples.loanbroker.BankQuoteRequest;
import org.mule.samples.loanbroker.CreditProfile;
import org.mule.samples.loanbroker.LoanQuote;
import org.mule.samples.loanbroker.LoanRequest;

/**
 * <code>BankService</code> is a representation of a bank form which to obtain loan
 * quotes.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface BankService
{
    LoanQuote getLoanQuote(LoanRequest request, CreditProfile creditProfile);

    LoanQuote getLoanQuote(BankQuoteRequest request);
}
