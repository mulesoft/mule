/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
