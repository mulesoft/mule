/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
