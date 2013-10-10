/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
