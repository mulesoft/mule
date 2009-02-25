/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.webapp;

import org.mule.api.MuleMessage;
import org.mule.example.loanbroker.messages.Customer;
import org.mule.example.loanbroker.messages.CustomerQuoteRequest;
import org.mule.example.loanbroker.messages.LoanQuote;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.FunctionalTestCase;
import org.mule.util.StringUtils;

public abstract class AbstractWebappTestCase extends FunctionalTestCase
{
    
    public void testSanity() throws Exception
    {
        new MuleClient();
    }
    
    public void testEchoExample() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage response = client.send("vm://echo", "Is anybody in there?", null);
        assertEquals("Is anybody in there?", response.getPayload());
    }
    
    public void testHelloExample() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage response = client.send("vm://greeter", "Julius Caesar", null);
        // ATTENTION: thie message is localized, a full comparison cannot be done here
        assertTrue(response.getPayloadAsString().indexOf("Julius Caesar") > -1);
    }
    /* EE-332 : fails when external web service is not responsive/times out
    public void testStockQuoteExample() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage response = client.send("vm://stockquote", "CSCO", null);
    
        if (null == response)
        { 
            fail("No response message.");
        }
        else
        {
            if (null == response.getExceptionPayload())
            {
                String text = response.getPayloadAsString();
                assertNotNull("Null response", text);
                assertTrue("Stock quote should contain \"CISCO\": " + text, StringUtils.containsIgnoreCase(text, "CISCO"));
                assertTrue("Stock quote should start with \"StockQuote[\":" + text, text.startsWith("StockQuote["));
                logger.debug("**********");
                logger.debug(response.getPayload());
                logger.debug(response.getPayloadAsString());
                logger.debug("**********");
            }
            else
            {
                fail("Exception occurred: " + response.getExceptionPayload());
            }
        }
    }
    */
    public void testLoanBrokerExample() throws Exception
    {
        MuleClient client = new MuleClient();
        CustomerQuoteRequest loanRequest = new CustomerQuoteRequest(new Customer("I.M. Broke", 1234), 50000, 60);
        MuleMessage response = client.send("CustomerRequests", loanRequest, null);
        assertNotNull("Result is null", response);
        assertTrue("Result should be LoanQuote but is " + response.getPayload().getClass().getName(), 
                    response.getPayload() instanceof LoanQuote);
        LoanQuote quote = (LoanQuote) response.getPayload();
        assertTrue("Interest rate is missing.", quote.getInterestRate() > 0);
    }
}
