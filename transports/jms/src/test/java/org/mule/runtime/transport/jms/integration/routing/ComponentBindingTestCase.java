/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.integration.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import org.junit.Ignore;
import org.junit.Test;

public class ComponentBindingTestCase extends FunctionalTestCase
{
    private static final int number = 0xC0DE;

    @Override
    protected String getConfigFile()
    {
        return "integration/routing/interface-binding-test-flow.xml";
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
    @Ignore("MULE-6926: flaky test")
    public void testJmsTopicBinding() throws Exception
    {
        internalTest("jms://topic:t");
    }

    private void internalTest(String prefix) throws Exception
    {
        MuleClient client = muleContext.getClient();
        String message = "Mule";
        client.dispatch(prefix + "invoker.in", message, null);
        MuleMessage reply = client.request(prefix + "invoker.out", RECEIVE_TIMEOUT);
        assertNotNull(reply);
        assertNull(reply.getExceptionPayload());
        assertEquals("Received: Hello " + message + " " + number, reply.getPayload());
    }
}
