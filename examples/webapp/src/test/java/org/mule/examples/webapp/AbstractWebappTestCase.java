/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.webapp;

import org.mule.examples.loanbroker.messages.Customer;
import org.mule.examples.loanbroker.messages.CustomerQuoteRequest;
import org.mule.examples.loanbroker.messages.LoanQuote;
import org.mule.extras.client.MuleClient;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOMessage;
import org.mule.util.StringUtils;

public abstract class AbstractWebappTestCase extends AbstractMuleTestCase
{
    public void testSanity() throws Exception
    {
        new MuleClient();
    }
    
    public void testEchoExample() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage response = client.send("vm://echo", "Is anybody in there?", null);
        assertEquals("Is anybody in there?", response.getPayload());
    }
    
    public void testHelloExample() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage response = client.send("vm://greeter", "Julius Caesar", null);
        assertEquals("Hello Julius Caesar, how are you?", response.getPayloadAsString());
    }
    
    public void testStockQuoteExample() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage response = client.send("vm://stockquote", "HRB", null);
    
        if(response != null) 
        { 
            if (response.getExceptionPayload() == null) 
            {            
                assertTrue("Stock quote should contain \"BLOCK\": " + response.getPayload(), 
                            StringUtils.contains(response.getPayloadAsString(), "BLOCK"));
            }
            else
            {
                fail("Exception occurred: " + response.getExceptionPayload());
            }
        }
        else
        {
            fail("No response message.");
        }
     }

    public void testLoanBrokerExample() throws Exception
    {
        MuleClient client = new MuleClient();
        CustomerQuoteRequest loanRequest = new CustomerQuoteRequest(new Customer("I.M. Broke", 1234), 50000, 60);
        UMOMessage response = client.send("CustomerRequests", loanRequest, null);
        assertNotNull("Result is null", response);
        assertTrue("Result should be LoanQuote but is " + response.getPayload().getClass().getName(), 
                    response.getPayload() instanceof LoanQuote);
        LoanQuote quote = (LoanQuote) response.getPayload();
        assertTrue("Interest rate is missing.", quote.getInterestRate() > 0);
    }
}
