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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;
import org.mule.transport.http.HttpConstants;

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
    public void testDefaultLoanBrokerRequest() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        @SuppressWarnings("unchecked")
        Map<String, String> props = new SingletonMap("http.method", HttpConstants.METHOD_GET);
        MuleMessage result = client.send("http://localhost:11081?name=Muley&amount=20000&term=48&ssn=1234", null, props);
        assertNotNull("Result is null", result);
        assertFalse("Result is null", result.getPayload() instanceof NullPayload);
        assertNull(result.getExceptionPayload());
        assertTrue("Unexpected response string", result.getPayloadAsString().matches("Bank #\\d, rate: \\d\\.(\\d)*$"));
    }

    @Test
    public void testIncompleteLoanBrokerRequest() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        @SuppressWarnings("unchecked")
        Map<String, String> props = new SingletonMap("http.method", HttpConstants.METHOD_GET);
        MuleMessage result = client.send("http://localhost:11081?amount=1234", null, props);
        assertEquals("Error: incomplete request", result.getPayloadAsString());
    }

    @Test
    public void testWrongLoanBrokerRequest() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        @SuppressWarnings("unchecked")
        Map<String, String> props = new SingletonMap("http.method", HttpConstants.METHOD_GET);
        MuleMessage result = client.send("http://localhost:11081?name=Muley&term=48&ssn=1234&amount=abcd", null, props);
        assertEquals("Error processing loan request", result.getPayloadAsString());
    }
}


