/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.outbound;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.routing.RoutingException;
import org.mule.message.ExceptionMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.io.ByteArrayInputStream;

import org.junit.Test;

public class MulticastRouterTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/outbound/multicasting-router-config.xml";
    }

    @Test
    public void testAll() throws Exception
    {
        ByteArrayInputStream bis = new ByteArrayInputStream("Hello, world".getBytes("UTF-8"));
        MuleClient client = new MuleClient(muleContext);
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
        MuleClient client = new MuleClient(muleContext);
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
