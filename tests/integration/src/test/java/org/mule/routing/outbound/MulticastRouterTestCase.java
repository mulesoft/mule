/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.routing.RoutingException;
import org.mule.message.ExceptionMessage;
import org.mule.tck.junit4.FunctionalTestCase;

import java.io.ByteArrayInputStream;

import org.junit.Test;

public class MulticastRouterTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/outbound/multicasting-router-config.xml";
    }

    @Test
    public void testAll() throws Exception
    {
        ByteArrayInputStream bis = new ByteArrayInputStream("Hello, world".getBytes("UTF-8"));
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://inbound1", bis, null);

        MuleMessage response = client.request("vm://output1", 2000);
        assertNull(response);

        MuleMessage error = client.request("vm://errors", 2000);
        assertRoutingExceptionReceived(error);
    }

    @Test
    public void testFirstSuccessful() throws Exception
    {
        ByteArrayInputStream bis = new ByteArrayInputStream("Hello, world".getBytes("UTF-8"));

        MuleClient client = muleContext.getClient();
        client.dispatch("vm://inbound2", bis, null);

        MuleMessage response = client.request("vm://output4", 2000);
        assertNull(response);

        MuleMessage error = client.request("vm://errors2", 2000);
        assertRoutingExceptionReceived(error);
    }

    /**
     * Asserts that a {@link RoutingException} has been received.
     *
     * @param message The received message.
     */
    private void assertRoutingExceptionReceived(MuleMessage message)
    {
        assertNotNull(message);
        Object payload = message.getPayload();
        assertNotNull(payload);
        assertTrue(payload instanceof ExceptionMessage);
        ExceptionMessage exceptionMessage = (ExceptionMessage) payload;
        assertTrue(exceptionMessage.getException() instanceof RoutingException);
    }
}
