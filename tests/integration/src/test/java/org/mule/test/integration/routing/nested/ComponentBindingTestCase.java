/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.routing.nested;

import org.junit.Test;
import org.junit.runners.Parameterized;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
    public void testJmsTopicBinding() throws Exception
    {
        internalTest("jms://topic:t");
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
}
