/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * see MULE-2721
 */ 
public class ReplyToTestCase extends FunctionalTestCase
{
    
    private static final long RECEIVE_DELAY = 3000;

    public ReplyToTestCase()
    {
        setDisposeContextPerClass(true);
    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/usecases/replyto.xml";
    }

    @Test
    public void testVm() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        
        MuleMessage msg = new DefaultMuleMessage("testing", muleContext);
        msg.setReplyTo("ReplyTo");
        
        // Send asynchronous request
        client.dispatch("EchoVm", msg, null);

        // Wait for asynchronous response
        MuleMessage result = client.request("ReplyTo", RECEIVE_DELAY);
        assertNotNull("Result is null", result);
        assertFalse("Result is null", result.getPayload() instanceof NullPayload);
        assertEquals("testing", result.getPayload());

        // Make sure there are no more responses
        result = client.request("ReplyTo", RECEIVE_DELAY);
        assertNull("Extra message received at replyTo destination: " + result, result);        
    }

    @Test
    public void testCxf() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        
        MuleMessage msg = new DefaultMuleMessage("testing", muleContext);
        msg.setReplyTo("ReplyTo");
        
        // Send asynchronous request
        client.dispatch("EchoCxfSend", msg, null);

        // Wait for asynchronous response
        MuleMessage result = client.request("ReplyTo", RECEIVE_DELAY);
        assertNotNull("Result is null", result);
        assertFalse("Result is null", result.getPayload() instanceof NullPayload);
        assertEquals("testing", result.getPayload());

        // Make sure there are no more responses
        result = client.request("ReplyTo", RECEIVE_DELAY);
        assertNull("Extra message received at replyTo destination: " + result, result);        
    }
}
