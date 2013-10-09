/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.integration;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.transport.NullPayload;

import org.junit.Test;

/**
 * TODO this test does not use the Test scenarios, I think it would need a new Method
 * sendAndReceive It might make sense to leave this test as is because it tests that
 * the client also works with ReplyTo correctly
 */
public class JmsTemporaryReplyToTestCase extends AbstractJmsFunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "integration/jms-temporary-replyTo.xml";
    }
    
    @Test
    public void testTemporaryReplyEnabledSync() throws MuleException
    {
        MuleClient muleClient = new MuleClient(muleContext);
        MuleMessage response = muleClient.send("vm://in1Sync", TEST_MESSAGE, null);
        assertEquals(TEST_MESSAGE + " TestService1", response.getPayload());
    }

    @Test
    public void testTemporaryReplyDisabledSync() throws MuleException
    {
        MuleClient muleClient = new MuleClient(muleContext);
        MuleMessage response = muleClient.send("vm://in2Sync", TEST_MESSAGE, null);
        assertEquals(TEST_MESSAGE, response.getPayload());
    }    
    
    @Test
    public void testDisableTemporaryReplyOnTheConnector() throws MuleException
    {
        MuleClient muleClient = new MuleClient(muleContext);
        MuleMessage response = muleClient.send("vm://in3", TEST_MESSAGE, null);
        
        assertEquals(NullPayload.getInstance(), response.getPayload());
    }

    @Test
    public void testExplicitReplyToAsyncSet() throws MuleException
    {
        MuleClient muleClient = new MuleClient(muleContext);
        MuleMessage response = muleClient.send("vm://in4", TEST_MESSAGE, null);
        // We get the original message back, not the result from the remote component
        assertEquals(TEST_MESSAGE + " TestService1", response.getPayload());
    }
}
