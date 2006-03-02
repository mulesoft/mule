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

    protected String getConfigResources() {
        return "loan-broker-sync-config.xml";
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
}
