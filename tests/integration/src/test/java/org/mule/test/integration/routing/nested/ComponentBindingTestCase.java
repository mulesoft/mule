/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.nested;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;

import java.util.Date;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ComponentBindingTestCase extends FunctionalTestCase
{
    private static final int number = 0xC0DE;

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/nested/interface-binding-test.xml";
    }

    private void internalTest(String prefix) throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        String message = "Mule";
        client.dispatch(prefix + "invoker.in", message, null);
        MuleMessage reply = client.request(prefix + "invoker.out", RECEIVE_TIMEOUT);
        assertNotNull(reply);
        assertNull(reply.getExceptionPayload());
        assertEquals("Received: Hello " + message + " " + number, reply.getPayload());
    }

    private void internalNullTest(String prefix) throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Date message = new Date();
        client.dispatch(prefix + "invoker.in", message, null);
        MuleMessage reply = client.request(prefix + "invoker.out", RECEIVE_TIMEOUT);
        assertNotNull(reply);
        assertNull(reply.getExceptionPayload());
        assertEquals(NullPayload.getInstance(), reply.getPayload());
    }

    @Test
    public void testVmBinding() throws Exception
    {
        internalTest("vm://");
    }

    @Test
    public void testJmsQueueBinding() throws Exception
    {
        internalTest("jms://");
    }

    @Test
    public void testJmsTopicBinding() throws Exception
    {
        internalTest("jms://topic:t");
    }

    @Test
    public void testVmBindingReturnNull() throws Exception
    {
        internalNullTest("vm://");
    }

    @Test
    public void testJmsQueueBindingReturnNull() throws Exception
    {
        internalNullTest("jms://");
    }

    @Test
    public void testJmsTopicBindingReturnNull() throws Exception
    {
        internalNullTest("jms://topic:t");
    }
}
