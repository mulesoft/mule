/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.nested;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.runners.Parameterized;

public class ComponentBindingTestCase extends AbstractServiceAndFlowTestCase
{
    private static final int number = 0xC0DE;

    public ComponentBindingTestCase(AbstractServiceAndFlowTestCase.ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {AbstractServiceAndFlowTestCase.ConfigVariant.SERVICE, "org/mule/test/integration/routing/nested/interface-binding-test.xml"},
            {AbstractServiceAndFlowTestCase.ConfigVariant.FLOW, "org/mule/test/integration/routing/nested/interface-binding-test-flow.xml"}
        });
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
