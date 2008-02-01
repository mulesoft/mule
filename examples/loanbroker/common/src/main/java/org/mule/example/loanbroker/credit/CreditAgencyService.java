/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.loanbroker.credit;

import org.mule.example.loanbroker.messages.CreditProfile;
import org.mule.example.loanbroker.messages.Customer;


/**
 * <code>CreditAgencyService</code> the service that provides a credit score for a
 * customer.
 */
public interface CreditAgencyService
{
    CreditProfile getCreditProfile(Customer customer);
}
