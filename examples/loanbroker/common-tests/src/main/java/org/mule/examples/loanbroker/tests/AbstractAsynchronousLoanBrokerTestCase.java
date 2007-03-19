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
import org.mule.extras.client.MuleClient;

/**
 * Tests the Loan Broker application asynchronously.  Note that a simple thread delay is used to wait for the 
 * incoming responses to arrive.  This may or may not be sufficient depending on external factors (processor 
 * speed, logging detail, etc.).  To make the tests reliable, a more accurate mechanism must be employed 
 * (notifications, thread-safe counter, etc.)
 */
public abstract class AbstractAsynchronousLoanBrokerTestCase extends AbstractLoanBrokerTestCase
{
    // @Override
    protected int getNumberOfRequests()
    {
        // TODO Once we actually make the asynchronous tests work (see comment above), increase this number.
        return 2;
    }
    
    /**
     * Milliseconds to wait after sending each message in order for the thread to "catch up" with the test.
     */
    protected int getDelay()
    {
        return 1000;
    }
    
    public void testSingleLoanRequest() throws Exception
    {
        MuleClient client = new MuleClient();
        Customer c = new Customer("Ross Mason", 1234);
        CustomerQuoteRequest request = new CustomerQuoteRequest(c, 100000, 48);
        // Send asynchronous request
        client.dispatch("vm://customer.requests", request, null);
        // Wait for asynchronous response
        Thread.sleep(getDelay());
        
        /* TODO The code below would work if an asynchronous transport (such as JMS) were used for 
         * vm://customer.responses 
         * Theoretically, in-memory queues (queueStore = true in VM connector) should work also, but
         * I wasn't able to get it working. - TC
        
        // Wait for asynchronous response
        UMOMessage result = client.receive("vm://customer.responses", getDelay());
        assertNotNull("Result is null", result);
        assertFalse("Result is null", result.getPayload() instanceof NullPayload);
        assertTrue("Result should be LoanQuote but is " + result.getPayload().getClass().getName(), 
                    result.getPayload() instanceof LoanQuote);
        LoanQuote quote = (LoanQuote)result.getPayload();
        assertTrue(quote.getInterestRate() > 0);
        */
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
                client.dispatch("vm://customer.requests", loanRequest, null);
            }
            Thread.sleep(getDelay() * numRequests);
            // TODO Look up the LoanBroker component from the registry and check its statistics.
            //LoanBrokerService lb = MuleManager.getInstance().lookupComponent(LoanBrokerService.class);
            //assertEquals(numRequests, lb.getRequests());
            //assertEquals(numRequests*5, lb.getQuotes());
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
