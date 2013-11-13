/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.loanbroker.bpm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.example.loanbroker.messages.Customer;
import org.mule.example.loanbroker.messages.CustomerQuoteRequest;
import org.mule.example.loanbroker.messages.LoanQuote;
import org.mule.example.loanbroker.tests.AbstractAsynchronousLoanBrokerTestCase;
import org.mule.transport.NullPayload;

public class JBpmFunctionalTestCase extends AbstractAsynchronousLoanBrokerTestCase
{
    public JBpmFunctionalTestCase()
    {
        super();
        setDisposeContextPerClass(true);
    }
    
    @Override
    protected String getConfigFile()
    {
        return "mule-config.xml";
    }

    @Override
    protected int getDelay()
    {
        return 20000;
    }

    @Override
    protected int getNumberOfRequests()
    {
        return 100;
    }

    @Override
    public void testLotsOfLoanRequests() throws Exception
    {
        final MuleClient client = muleContext.getClient();
        Customer c = new Customer("Ross Mason", 1234);
        CustomerQuoteRequest request = new CustomerQuoteRequest(c, 100000, 48);

        for (int i = 0; i < getNumberOfRequests(); i++)
        {
            client.dispatch("CustomerRequests", request, null);
        }
        
        MuleMessage result;
        for (int i = 0; i < getNumberOfRequests(); i++)
        {
            result = client.request("CustomerResponses", getDelay());
            assertNotNull("Result is null", result);
            assertFalse("Result is null", result.getPayload() instanceof NullPayload);
            assertTrue("Result should be LoanQuote but is " + result.getPayload().getClass().getName(),
                    result.getPayload() instanceof LoanQuote);
            LoanQuote quote = (LoanQuote) result.getPayload();
            assertTrue(quote.getInterestRate() > 0);
            assertNotNull(quote.getBankName());
        }
    }
}
