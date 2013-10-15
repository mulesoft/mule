/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.vm;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class VMFunctionalTestCase extends FunctionalTestCase
{

    public VMFunctionalTestCase()
    {
        setDisposeContextPerClass(true);
    }

    @Override
    protected String getConfigResources()
    {
        return "vm/vm-functional-test.xml";
    }

    @Test
    public void testSingleMessage() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://in", "Marco", null);
        MuleMessage response = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull("Response is null", response);
        assertEquals("Polo", response.getPayload());
    }

    @Test
    public void testRequest() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://in", "Marco", null);
        MuleMessage response = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull("Response is null", response);
        assertEquals("Polo", response.getPayload());
    }

    @Test
    public void testMultipleMessages() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://in", "Marco", null);
        client.dispatch("vm://in", "Marco", null);
        client.dispatch("vm://in", "Marco", null);
        MuleMessage response;
        for (int i = 0; i < 3; ++i)
        {
            response = client.request("vm://out", RECEIVE_TIMEOUT);
            assertNotNull("Response is null", response);
            assertEquals("Polo", response.getPayload());
        }

        MuleMessage secondMessage = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNull(secondMessage);
    }

    @Test
    public void testOneWayChain() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://in1", "Marco", null);
        MuleMessage response = client.request("vm://out1", RECEIVE_TIMEOUT);
        assertNotNull("Response is null", response);
        assertEquals("Polo", response.getPayload());
    }

    @Test
    public void testRequestResponseChain() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("vm://in2", "Marco", null);
        assertNotNull("Response is null", response);
        assertEquals("Polo", response.getPayload());
    }
    
    @Test
    public void testNoMessageDuplication() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://in", "Marco", null);
        MuleMessage response = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull("Response is null", response);
        assertEquals("Polo", response.getPayload());
        MuleMessage secondMessage = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNull(secondMessage);
    }
}
