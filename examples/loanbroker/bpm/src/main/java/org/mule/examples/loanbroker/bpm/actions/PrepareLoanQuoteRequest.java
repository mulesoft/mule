/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.loanbroker.bpm.actions;

import org.mule.examples.loanbroker.messages.CreditProfile;
import org.mule.examples.loanbroker.messages.CustomerQuoteRequest;
import org.mule.examples.loanbroker.messages.LoanBrokerQuoteRequest;
import org.mule.providers.bpm.jbpm.actions.LoggingActionHandler;

import org.jbpm.graph.exe.ExecutionContext;

/**
 * Prepares a loan request for the banks based on the original customer request and the customer's 
 * credit profile.
 */
public class PrepareLoanQuoteRequest extends LoggingActionHandler
{
    public void execute(ExecutionContext executionContext) throws Exception
    {
        super.execute(executionContext);
        LoanBrokerQuoteRequest loanRequest = new LoanBrokerQuoteRequest();
        loanRequest.setCustomerRequest((CustomerQuoteRequest) executionContext.getVariable("customerRequest"));
        loanRequest.setCreditProfile((CreditProfile) executionContext.getVariable("creditProfile"));
        executionContext.setVariable("loanRequest", loanRequest);
    }
}
