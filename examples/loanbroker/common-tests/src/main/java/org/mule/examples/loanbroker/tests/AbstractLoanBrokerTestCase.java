/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.loanbroker.tests;

import org.mule.examples.loanbroker.messages.Customer;
import org.mule.examples.loanbroker.messages.CustomerQuoteRequest;
import org.mule.examples.loanbroker.messages.LoanQuote;
import org.mule.extras.client.MuleClient;
import org.mule.providers.NullPayload;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public abstract class AbstractLoanBrokerTestCase extends FunctionalTestCase
{
    public AbstractLoanBrokerTestCase()
    {
        super();
        setDisposeManagerPerSuite(true);
    }

    protected int getNumberOfRequests()
    {
        return 10;
    }

    public void testSingleLoanRequest() throws Exception
    {
        MuleClient client = new MuleClient();
        Customer c = new Customer("Ross Mason", 1234);
        CustomerQuoteRequest request = new CustomerQuoteRequest(c, 100000, 48);
        UMOMessage result = client.send("CustomerRequests", request, null);
        assertNotNull("Result is null", result);
        assertFalse("Result is null", result.getPayload() instanceof NullPayload);
        assertTrue("Result should be LoanQuote but is " + result.getPayload().getClass().getName(), 
                    result.getPayload() instanceof LoanQuote);
        LoanQuote quote = (LoanQuote)result.getPayload();
        assertTrue(quote.getInterestRate() > 0);
    }

    public void testLotsOfLoanRequests() throws Exception
    {
        MuleClient client = new MuleClient();
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

                UMOMessage result = client.send("CustomerRequests", loanRequest, null);
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