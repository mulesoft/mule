/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.loanbroker.creditagency;

import org.mule.example.loanbroker.model.CreditProfile;
import org.mule.example.loanbroker.model.Customer;

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
