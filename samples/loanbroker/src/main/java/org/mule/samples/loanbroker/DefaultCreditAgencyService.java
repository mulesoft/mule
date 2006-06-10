/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.samples.loanbroker;

import org.mule.samples.loanbroker.service.CreditAgencyService;

/**
 * <code>DefaultCreditAgencyService</code> the service that provides a credit
 * score for a customer
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class DefaultCreditAgencyService implements CreditAgencyService
{
    public int getCreditScore(int ssn)
  {
    int credit_score;

    credit_score = (int)(Math.random()*600+300);

    return credit_score;
  }

  public int getCreditHistoryLength(int ssn)
  {
    int credit_history_length;

    credit_history_length = (int)(Math.random()*19+1);

    return credit_history_length;
  }

//    public CreditProfile getCreditProfile(Customer customer)
//    {
//        CreditProfile cp = new CreditProfile();
//        cp.setCreditHistoryLength(getCreditHistoryLength(customer.getSsn()));
//        cp.setCreditScore(getCreditScore(customer.getSsn()));
//
//        return cp;
//    }

    public BankQuoteRequest getCreditProfile(BankQuoteRequest request)
    {
        CreditProfile cp = new CreditProfile();
        Customer customer = request.getLoanRequest().getCustomer();
        cp.setCreditHistoryLength(getCreditHistoryLength(customer.getSsn()));
        cp.setCreditScore(getCreditScore(customer.getSsn()));
        request.getLoanRequest().setCreditProfile(cp);
        return request;
    }
}
