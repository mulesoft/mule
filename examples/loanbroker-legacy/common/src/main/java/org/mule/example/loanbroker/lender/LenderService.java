/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.loanbroker.lender;

import org.mule.example.loanbroker.bank.Bank;
import org.mule.example.loanbroker.messages.CreditProfile;

public interface LenderService
{
    /** 
     * Returns a list of banks willing to offer a loan based on a customer's credit profile and the amount of 
     * the loan.
     */
    public abstract Bank[] getLenders(CreditProfile creditProfile, Double loanAmount);
}
