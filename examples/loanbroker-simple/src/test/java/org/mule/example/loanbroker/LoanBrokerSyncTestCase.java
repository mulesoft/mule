/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.loanbroker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.example.loanbroker.message.CustomerQuoteRequest;
import org.mule.example.loanbroker.model.Customer;
import org.mule.example.loanbroker.model.LoanQuote;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transformer.simple.ByteArrayToObject;
import org.mule.transport.NullPayload;
import org.mule.transport.http.HttpConstants;
import org.mule.util.SerializationUtils;

import java.util.Map;

import org.apache.commons.collections.map.SingletonMap;
import org.junit.Test;

public class LoanBrokerSyncTestCase extends FunctionalTestCase
{
    
    @Override
    protected String getConfigResources()
    {
        return "mule-config.xml";
    }

    @Test
    public void testLoanBrokerMuleClient() throws Exception
    {
        muleContext.getRegistry().registerObject("streamToObjectTransformer", new ByteArrayToObject());
        MuleClient client = new MuleClient(muleContext);
        Customer c = new Customer("Ross Mason", 1234);
        CustomerQuoteRequest request = new CustomerQuoteRequest(c, 100000, 48);
        MuleMessage result = client.send("http://localhost:11080?responseTransformers=streamToObjectTransformer", SerializationUtils.serialize(request), null);
        assertNotNull("Result is null", result);
        assertFalse("Result is null", result.getPayload() instanceof NullPayload);
        assertTrue("Result should be LoanQuote but is " + result.getPayload().getClass().getName(),
                   result.getPayload(Object.class) instanceof LoanQuote);
        LoanQuote quote = (LoanQuote) result.getPayload();
        assertTrue(quote.getInterestRate() > 0);
    }

    @Test
    public void testLoanBrokerHttpUrlWithDefaults() throws Exception
    {
        muleContext.getRegistry().registerObject("streamToObjectTransformer", new ByteArrayToObject());
        MuleClient client = new MuleClient(muleContext);
        // there are some default built into the http url service (note a different port)
        @SuppressWarnings("unchecked")
        Map<String, String> props = new SingletonMap("http.method", HttpConstants.METHOD_GET);
        MuleMessage result = client.send("http://localhost:11081", null, props);
        assertNotNull("Result is null", result);
        assertFalse("Result is null", result.getPayload() instanceof NullPayload);
        assertNull(result.getExceptionPayload());

        assertTrue("Unexpected response string", result.getPayloadAsString().matches("Bank #\\d, rate: \\d\\.(\\d)*$"));
    }
    
}


