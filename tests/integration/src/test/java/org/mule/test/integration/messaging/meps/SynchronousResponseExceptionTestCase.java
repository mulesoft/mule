/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.messaging.meps;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @see MULE-4512
 */
public class SynchronousResponseExceptionTestCase extends FunctionalTestCase
{
    
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/messaging/meps/synchronous-response-exception.xml";
    }

    @Test
    public void testComponentException() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply = client.send("vm://in1", "request", null);
        assertTrue("Response should be null but is " + reply.getPayload(), reply.getPayload() instanceof NullPayload);
    }

    @Test
    public void testOutboundRoutingException() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply = client.send("vm://in2", "request", null);
        assertTrue("Response should be null but is " + reply.getPayload(), reply.getPayload() instanceof NullPayload);
    }

    @Test
    public void testInboundTransformerException() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply = client.send("vm://in3", "request", null);
        assertTrue("Response should be null but is " + reply.getPayload(), reply.getPayload() instanceof NullPayload);
    }

    @Test
    public void testOutboundTransformerException() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply = client.send("vm://in4", "request", null);
        assertTrue("Response should be null but is " + reply.getPayload(), reply.getPayload() instanceof NullPayload);
    }

    @Test
    public void testResponseTransformerException() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply = client.send("vm://in5", "request", null);
        assertTrue("Response should be null but is " + reply.getPayload(), reply.getPayload() instanceof NullPayload);
    }
}


