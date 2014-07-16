/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.loanbroker.creditagency;

import org.mule.example.loanbroker.model.CreditProfile;
import org.mule.example.loanbroker.model.Customer;

import javax.jws.WebService;


/**
 * <code>CreditAgencyService</code> the service that provides a credit score for a
 * customer.
 */
@WebService
public interface CreditAgencyService
{
    CreditProfile getCreditProfile(Customer customer);
}
