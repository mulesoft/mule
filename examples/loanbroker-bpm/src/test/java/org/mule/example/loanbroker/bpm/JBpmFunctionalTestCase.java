/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.loanbroker.bpm;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.example.loanbroker.messages.Customer;
import org.mule.example.loanbroker.messages.CustomerQuoteRequest;
import org.mule.example.loanbroker.messages.LoanQuote;
import org.mule.example.loanbroker.tests.AbstractAsynchronousLoanBrokerTestCase;
import org.mule.transport.NullPayload;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JBpmFunctionalTestCase extends AbstractAsynchronousLoanBrokerTestCase
{

    public JBpmFunctionalTestCase()
    {
        super();
        setDisposeContextPerClass(true);
    }
    
    @Override
    protected String getConfigResources()
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
