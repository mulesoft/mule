/*
 * $Header$
 * $Revision$
 * $Date$
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

import org.mule.extras.client.MuleClient;
import org.mule.providers.NullPayload;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class AxisLoanBrokerSynchronousFunctionalTestCase extends FunctionalTestCase {

    public static final int REQUESTS = 100;

    protected String getConfigResources() {
        return "loan-broker-axis-sync-config.xml";
    }

    public void testSingleLoanRequest() throws Exception {
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

    public void testLotsOfLoanRequests() throws Exception {
        MuleClient client = new MuleClient();
        Customer c = new Customer("Ross Mason", 1234);
        LoanRequest[] requests = new LoanRequest[2];
        requests[0] = new LoanRequest(c, 100000, 48);
        requests[1] = new LoanRequest(c, 1000, 12);
        UMOMessage result;
        int i = 0;
        long start = System.currentTimeMillis();
        try {
            for (; i < REQUESTS; i++) {
                result = client.send("vm://LoanBrokerRequests",  requests[i % 2], null);
                assertNotNull(result);
                assertFalse(result.getPayload() instanceof NullPayload);
                assertTrue(result.getPayload() instanceof LoanQuote);
                LoanQuote quote = (LoanQuote)result.getPayload();
                assertTrue(quote.getInterestRate() > 0);
            }
        } finally {
            System.out.println("Requests processed was: " + i);
            long el = System.currentTimeMillis() - start;
            System.out.println("Total running time was: " + el);
            float mps = 1000 / (el / REQUESTS);
            System.out.println("MPS: " + mps + " (no warm up)");
        }
    }
}
