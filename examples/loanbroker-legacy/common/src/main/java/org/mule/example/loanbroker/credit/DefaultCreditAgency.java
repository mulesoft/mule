/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.loanbroker.credit;

import org.mule.example.loanbroker.messages.CreditProfile;
import org.mule.example.loanbroker.messages.Customer;

/**
 * Provides the credit profile for a customer.
 */
public class DefaultCreditAgency implements CreditAgencyService
{
     public CreditProfile getCreditProfile(Customer customer)
     {
         CreditProfile cp = new CreditProfile();
         cp.setCreditHistory(getCreditHistoryLength(customer.getSsn()));
         cp.setCreditScore(getCreditScore(customer.getSsn()));
         return cp;
     }

     protected int getCreditScore(int ssn)
     {
         int credit_score;

         credit_score = (int)(Math.random() * 600 + 300);

         return credit_score;
     }

     protected int getCreditHistoryLength(int ssn)
     {
         int credit_history_length;

         credit_history_length = (int)(Math.random() * 19 + 1);

         return credit_history_length;
     }
}
