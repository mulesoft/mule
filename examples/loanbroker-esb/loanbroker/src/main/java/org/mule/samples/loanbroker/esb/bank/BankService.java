/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.samples.loanbroker.esb.bank;

import org.mule.samples.loanbroker.esb.message.LoanQuoteRequest;

/**
 * <code>BankService</code> is a representation of a bank form which to obtain loan
 * quotes.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface BankService
{
    org.mule.samples.loanbroker.esb.message.LoanQuote getLoanQuote(LoanQuoteRequest request);
}
