/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.loanbroker;

import org.mule.extras.client.MuleClient;
import org.mule.providers.NullPayload;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class AxisLoanBrokerSynchronousFunctionalTestCase extends FunctionalTestCase
{
    // only shoot 10 requests for now
    public static final int REQUESTS = 10;

    protected String getConfigResources()
    {
        return "loan-broker-axis-sync-test-config.xml";
    }

    public void _testSingleLoanRequest() throws Exception
    {
        MuleClient client = new MuleClient();
        Customer c = new Customer("Ross Mason", 1234);
        LoanRequest request = new LoanRequest(c, 100000, 48);
        UMOMessage result = client.send("vm://LoanBrokerRequests", request, null);
        assertNotNull(result);
        assertFalse(result.getPayload() instanceof NullPayload);
        assertTrue(result.getPayload() instanceof LoanQuote);
        LoanQuote quote = (LoanQuote)result.getPayload();
        assertTrue(quote.getInterestRate() > 0);
    }

    public void testLotsOfLoanRequests() throws Exception
    {
        MuleClient client = new MuleClient();
        Customer c = new Customer("Ross Mason", 1234);
        LoanRequest[] requests = new LoanRequest[3];
        requests[0] = new LoanRequest(c, 100000, 48);
        requests[1] = new LoanRequest(c, 1000, 12);
        requests[2] = new LoanRequest(c, 10, 24);
        UMOMessage result;
        int i = 0;
        long start = System.currentTimeMillis();
        try
        {
            for (; i < REQUESTS; i++)
            {
                LoanRequest loanRequest = requests[i % 3];

                // must set the CreditProfile to null otherwise the first
                // JXPathFilter
                // will be bypassed and CreditAgency component will be bypassed as
                // well!!
                loanRequest.setCreditProfile(null);
                result = client.send("vm://LoanBrokerRequests", loanRequest, null);
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
            System.out.println("Requests processed was: " + i);
            long el = System.currentTimeMillis() - start;
            System.out.println("Total running time was: " + el);
            float mps = 1000 / (el / REQUESTS);
            System.out.println("MPS: " + mps + " (no warm up)");
        }
    }
}
