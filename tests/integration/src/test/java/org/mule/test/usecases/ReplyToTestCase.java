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

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.providers.NullPayload;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

/**
 * @see MULE-2721
 */ 
public class ReplyToTestCase extends FunctionalTestCase
{
    static final long RECEIVE_DELAY = 3000;
    
    // @Override
    protected String getConfigResources()
    {
        return "org/mule/test/usecases/replyto.xml";
    }

    public void testVm() throws Exception
    {
        MuleClient client = new MuleClient();
        
        UMOMessage msg = new MuleMessage("testing");
        msg.setReplyTo("ReplyTo");
        
        // Send asynchronous request
        client.dispatch("EchoVm", msg, null);

        // Wait for asynchronous response
        UMOMessage result = client.receive("ReplyTo", RECEIVE_DELAY);
        assertNotNull("Result is null", result);
        assertFalse("Result is null", result.getPayload() instanceof NullPayload);

        // Make sure there are no more responses
        result = client.receive("ReplyTo", RECEIVE_DELAY);
        assertNull("Extra message received at replyTo destination: " + result, result);        
    }

    public void testAxis() throws Exception
    {
        MuleClient client = new MuleClient();
        
        UMOMessage msg = new MuleMessage("testing");
        msg.setReplyTo("ReplyTo");
        
        // Send asynchronous request
        client.dispatch("EchoAxisSend", msg, null);

        // Wait for asynchronous response
        UMOMessage result = client.receive("ReplyTo", RECEIVE_DELAY);
        assertNotNull("Result is null", result);
        assertFalse("Result is null", result.getPayload() instanceof NullPayload);

        // Make sure there are no more responses
        result = client.receive("ReplyTo", RECEIVE_DELAY);
        assertNull("Extra message received at replyTo destination: " + result, result);        
    }

    public void testXFire() throws Exception
    {
        MuleClient client = new MuleClient();
        
        UMOMessage msg = new MuleMessage("testing");
        msg.setReplyTo("ReplyTo");
        
        // Send asynchronous request
        client.dispatch("EchoXFireSend", msg, null);

        // Wait for asynchronous response
        UMOMessage result = client.receive("ReplyTo", RECEIVE_DELAY);
        assertNotNull("Result is null", result);
        assertFalse("Result is null", result.getPayload() instanceof NullPayload);

        // Make sure there are no more responses
        result = client.receive("ReplyTo", RECEIVE_DELAY);
        assertNull("Extra message received at replyTo destination: " + result, result);        
    }
}
