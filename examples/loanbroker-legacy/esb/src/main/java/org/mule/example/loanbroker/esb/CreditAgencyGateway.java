/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.loanbroker.esb;

import org.mule.example.loanbroker.credit.CreditAgencyService;
import org.mule.example.loanbroker.messages.CreditProfile;
import org.mule.example.loanbroker.messages.LoanBrokerQuoteRequest;

/**
 * This service is the gateway used to pass requests to the credit agency service.  For the sake of the example we've added
 * some complexity here.
 * 1) We use a _component binding_ to bind the {@link org.mule.example.loanbroker.credit.CreditAgencyService} to the remote
 * CreditAgencyService EJB instance.
 * 2) The argument passed into this interface binding is {@link org.mule.example.loanbroker.messages.Customer} but the EJB instance
 * needs only a String (name) and Integer (ssn). Also the EJB service returns an XML message, but we convert it to a {@link org.mule.example.loanbroker.messages.CreditProfile}
 * object. We demonstrate how to perform argument transalations by configuring transformers and response-transformers on an endpoint.
 */
public class CreditAgencyGateway
{
    //This interface is bound to an endpoint (known as an interface binding).  When the getCreditProfile() method is invoked, the call
    //will be made to a remote service.
    private CreditAgencyService creditAgencyService;

    public LoanBrokerQuoteRequest process(LoanBrokerQuoteRequest request)
    {
        CreditProfile cp = creditAgencyService.getCreditProfile(request.getCustomerRequest().getCustomer());
        request.setCreditProfile(cp);
        return request;
    }

    public CreditAgencyService getCreditAgencyService()
    {
        return creditAgencyService;
    }

    public void setCreditAgencyService(CreditAgencyService creditAgencyService)
    {
        this.creditAgencyService = creditAgencyService;
    }
}
