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

    public AxisLoanBrokerSynchronousFunctionalTestCase()
    {
        super();
        setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources()
    {
        return "loan-broker-axis-sync-test-config.xml";
    }

    protected int getNumberOfRequests()
    {
    // fire 100 requests as default
        return 100;
    }


    public void testSingleLoanRequest() throws Exception
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

        long start = System.currentTimeMillis();

        int numRequests = this.getNumberOfRequests();
        int i = 0;

        try
        {
            for (; i < numRequests; i++)
            {
                LoanRequest loanRequest = requests[i % 3];

                // must set the CreditProfile to null otherwise the first
                // JXPathFilter will be bypassed and CreditAgency component will be
                // bypassed as well!
                loanRequest.setCreditProfile(null);
                UMOMessage result = client.send("vm://LoanBrokerRequests", loanRequest, null);
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
