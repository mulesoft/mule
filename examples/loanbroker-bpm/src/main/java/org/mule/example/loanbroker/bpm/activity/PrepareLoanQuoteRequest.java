/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.loanbroker.bpm.activity;

import org.mule.example.loanbroker.messages.CreditProfile;
import org.mule.example.loanbroker.messages.CustomerQuoteRequest;
import org.mule.example.loanbroker.messages.LoanBrokerQuoteRequest;

/**
 * Prepares a loan request for the banks based on the original customer request and the customer's 
 * credit profile.
 */
public class PrepareLoanQuoteRequest
{
    public static LoanBrokerQuoteRequest prepareRequest(CustomerQuoteRequest customerRequest, CreditProfile creditProfile)
    {  
        LoanBrokerQuoteRequest loanRequest = new LoanBrokerQuoteRequest();
        loanRequest.setCustomerRequest(customerRequest);
        loanRequest.setCreditProfile(creditProfile);
        return loanRequest;
    }
}
