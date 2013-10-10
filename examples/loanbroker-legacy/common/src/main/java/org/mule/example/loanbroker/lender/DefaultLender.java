/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.loanbroker.lender;

import org.mule.example.loanbroker.bank.Bank;
import org.mule.example.loanbroker.messages.CreditProfile;
import org.mule.example.loanbroker.messages.LoanBrokerQuoteRequest;

/**
 * <code>DefaultLenderService</code> is responsible for contacting the relivant
 * banks depending on the amount of the loan
 */
public class DefaultLender implements LenderService
{
    /**
     * Sets the list of lenders on the LoanBrokerQuoteRequest and returns it.
     */
    public void setLenderList(LoanBrokerQuoteRequest request)
    {
        Bank[] lenders = getLenders(request.getCreditProfile(), new Double(request.getCustomerRequest()
            .getLoanAmount()));
        request.setLenders(lenders);
    }

    /**
     * {@inheritDoc}
     */
    public Bank[] getLenders(CreditProfile creditProfile, Double loanAmount)
    {
        // TODO Add creditProfile info. to the logic below.
        // TODO Look up the existing banks from the config/registry instead of
        // creating them programatically here.
        Bank[] lenders;
        if ((loanAmount.doubleValue() >= 20000))
        {
            lenders = new Bank[2];
            lenders[0] = new Bank("Bank1");
            lenders[1] = new Bank("Bank2");
        }
        else if (((loanAmount.doubleValue() >= 10000) && (loanAmount.doubleValue() <= 19999)))
        {
            lenders = new Bank[2];
            lenders[0] = new Bank("Bank3");
            lenders[1] = new Bank("Bank4");
        }
        else
        {
            lenders = new Bank[1];
            lenders[0] = new Bank("Bank5");
        }

        return lenders;
    }
}
