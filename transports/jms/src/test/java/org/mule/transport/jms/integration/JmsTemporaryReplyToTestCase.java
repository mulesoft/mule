/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;

import org.junit.Test;

/**
 * TODO this test does not use the Test scenarios, I think it would need a new Method
 * sendAndReceive It might make sense to leave this test as is because it tests that
 * the client also works with ReplyTo correctly
 */
public class JmsTemporaryReplyToTestCase extends AbstractJmsFunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "integration/jms-temporary-replyTo.xml";
    }

    @Test
    public void testTemporaryReplyEnabledSync() throws MuleException
    {
        MuleClient muleClient = muleContext.getClient();
        MuleMessage response = muleClient.send("vm://in1Sync", TEST_MESSAGE, null);
        assertEquals(TEST_MESSAGE + " TestService1", response.getPayload());
    }

    @Test
    public void testTemporaryReplyDisabledSync() throws MuleException
    {
        MuleClient muleClient = muleContext.getClient();
        MuleMessage response = muleClient.send("vm://in2Sync", TEST_MESSAGE, null);
        // TODO This behaviour appears inconsistent.  NullPayload would be more appropriate.
        assertNull(response);
    }

    @Test
    public void testDisableTemporaryReplyOnTheConnector() throws MuleException
    {
        MuleClient muleClient = muleContext.getClient();
        MuleMessage response = muleClient.send("vm://in3", TEST_MESSAGE, null);

        assertEquals(TEST_MESSAGE, response.getPayload());
    }

    @Test
    public void testExplicitReplyToAsyncSet() throws MuleException
    {
        MuleClient muleClient = muleContext.getClient();
        MuleMessage response = muleClient.send("vm://in4", TEST_MESSAGE, null);
        // We get the original message back, not the result from the remote component
        assertEquals(TEST_MESSAGE + " TestService1", response.getPayload());
    }
}
