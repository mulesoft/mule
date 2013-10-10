/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.loanbroker;

import org.mule.example.loanbroker.message.CustomerQuoteRequest;
import org.mule.example.loanbroker.message.LoanBrokerQuoteRequest;
import org.mule.example.loanbroker.model.CreditProfile;
import org.mule.example.loanbroker.model.Customer;
import org.mule.example.loanbroker.model.LoanQuote;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>LoanBroker</code> is the Service that starts the loan request process. The
 * broker also receives the final quote.
 */
public class AbstractLoanBroker implements LoanBrokerService
{
    /**
     * logger used by this class
     */
    protected final Log logger = LogFactory.getLog(getClass());
    
    private final AtomicInteger quotes = new AtomicInteger(0);
    private final AtomicInteger requests = new AtomicInteger(0);
    private final AtomicInteger profiles = new AtomicInteger(0);

    public Object getLoanQuote(CustomerQuoteRequest request) throws LoanBrokerException
    {
        int requests = incRequests();
        if (logger.isInfoEnabled())
        {
            String[] params = new String[] { String.valueOf(requests), 
                request.getCustomer().getName(), 
                String.valueOf(request.getCustomer().getSsn()), 
                String.valueOf(request.getLoanAmount()),
                String.valueOf(request.getLoanDuration()) };

            logger.info("\n***** " + LocaleMessage.receivedRequest(params));
        }
        return request;
    }

    public LoanBrokerQuoteRequest receiveLoanBrokerQuoteRequest(LoanBrokerQuoteRequest request)
    {
        // Just pass through
        return request;
    }

    public Customer receiveCustomer(Customer customer)
    {
        // Just pass through
        return customer;
    }

    public Object receiveCreditProfile(CreditProfile profile)
    {
        int profiles = incProfiles();
        if (logger.isInfoEnabled())
        {
            String[] params = new String[] { String.valueOf(profiles), 
                String.valueOf(profile.getCreditScore()),
                String.valueOf(profile.getCreditHistory()) };

            logger.info("\n***** " + LocaleMessage.receivedProfile(params));
        }
        return profile;
    }

    public Object receiveQuote(LoanQuote quote)
    {
        int quotes = incQuotes();
        if (logger.isInfoEnabled())
        {
            String[] params = new String[] { String.valueOf(quotes), 
                quote.toString() };
            logger.info("\n***** " + LocaleMessage.receivedQuote(params));
        }
        return quote;
    }

    protected int incQuotes()
    {
        return quotes.incrementAndGet();
    }

    protected int incRequests()
    {
        return requests.incrementAndGet();
    }

    protected int incProfiles()
    {
        return profiles.incrementAndGet();
    }
}
