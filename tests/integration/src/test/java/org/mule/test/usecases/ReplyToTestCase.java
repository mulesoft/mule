/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.usecases;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.NullPayload;

/**
 * see MULE-2721
 */ 
public class ReplyToTestCase extends FunctionalTestCase
{
    static final long RECEIVE_DELAY = 3000;
    
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/usecases/replyto.xml";
    }

    public void testVm() throws Exception
    {
        MuleClient client = new MuleClient();
        
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

    public void testAxis() throws Exception
    {
        MuleClient client = new MuleClient();
        
        MuleMessage msg = new DefaultMuleMessage("testing", muleContext);
        msg.setReplyTo("ReplyTo");
        
        // Send asynchronous request
        client.dispatch("EchoAxisSend", msg, null);

        // Wait for asynchronous response
        MuleMessage result = client.request("ReplyTo", RECEIVE_DELAY);
        assertNotNull("Result is null", result);
        assertFalse("Result is null", result.getPayload() instanceof NullPayload);
        assertEquals("testing", result.getPayload());

        // Make sure there are no more responses
        result = client.request("ReplyTo", RECEIVE_DELAY);
        assertNull("Extra message received at replyTo destination: " + result, result);        
    }

    public void testCxf() throws Exception
    {
        MuleClient client = new MuleClient();
        
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
