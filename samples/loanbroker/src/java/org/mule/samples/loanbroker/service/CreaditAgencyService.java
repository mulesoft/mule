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
import org.mule.samples.loanbroker.Customer;

/**
 * <code>CreaditAgencyService</code> the service that provides a credit
 * score for a customer
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface CreaditAgencyService
{
    CreditProfile getCreditProfile(Customer customer);

    void getCreditProfile(BankQuoteRequest request);
}
