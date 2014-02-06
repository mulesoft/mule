/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.loanbroker.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.example.loanbroker.messages.Customer;
import org.mule.example.loanbroker.messages.CustomerQuoteRequest;
import org.mule.example.loanbroker.messages.LoanQuote;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;

import org.junit.Ignore;
import org.junit.Test;

public abstract class AbstractLoanBrokerTestCase extends FunctionalTestCase
{
    protected int getNumberOfRequests()
    {
        return 10;
    }

    @Test
    @Ignore("MULE-6926: flaky test")
    public void testSingleLoanRequest() throws Exception
    {
        MuleClient client = muleContext.getClient();
        Customer c = new Customer("Ross Mason", 1234);
        CustomerQuoteRequest request = new CustomerQuoteRequest(c, 100000, 48);
        MuleMessage result = client.send("CustomerRequests", request, null);
        assertNotNull("Result is null", result);
        assertFalse("Result is null", result.getPayload() instanceof NullPayload);
        assertTrue("Result should be LoanQuote but is " + result.getPayload().getClass().getName(),
                    result.getPayload() instanceof LoanQuote);
        LoanQuote quote = (LoanQuote)result.getPayload();
        assertTrue(quote.getInterestRate() > 0);
    }

    @Test
    @Ignore("MULE-6926: flaky test")
    public void testLotsOfLoanRequests() throws Exception
    {
        MuleClient client = muleContext.getClient();
        Customer c = new Customer("Ross Mason", 1234);
        CustomerQuoteRequest[] requests = new CustomerQuoteRequest[3];
        requests[0] = new CustomerQuoteRequest(c, 100000, 48);
        requests[1] = new CustomerQuoteRequest(c, 1000, 12);
        requests[2] = new CustomerQuoteRequest(c, 10, 24);

        long start = System.currentTimeMillis();

        int numRequests = getNumberOfRequests();
        int i = 0;
        try
        {
            for (; i < numRequests; i++)
            {
                CustomerQuoteRequest loanRequest = requests[i % 3];

                MuleMessage result = client.send("CustomerRequests", loanRequest, null);
                assertNotNull(result);
                assertFalse("received a NullPayload", result.getPayload() instanceof NullPayload);
                assertTrue("did not receive a LoanQuote but: " + result.getPayload(),
                    result.getPayload() instanceof LoanQuote);
                LoanQuote quote = (LoanQuote)result.getPayload();
                assertTrue(quote.getInterestRate() > 0);
            }
        }
        finally
        {
            long el = System.currentTimeMillis() - start;
            System.out.println("Total running time was: " + el + "ms");
            System.out.println("Requests processed was: " + i);
            int mps = (int)(numRequests/((double)el/(double)1000));
            System.out.println("Msg/sec: " + mps + " (no warm up)");
        }
    }
}
