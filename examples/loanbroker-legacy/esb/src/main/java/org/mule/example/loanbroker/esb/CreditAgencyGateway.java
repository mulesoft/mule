/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
