/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.routing.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class ChainingRouterNullsHandlingTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE,
            "org/mule/test/integration/routing/outbound/chaining-router-null-handling-service.xml"}});
    }

    public ChainingRouterNullsHandlingTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testNoComponentFails() throws Exception
    {
        MuleClient muleClient = new MuleClient(muleContext);
        MuleMessage message = new DefaultMuleMessage("thePayload", muleContext);
        MuleMessage result = muleClient.send("vm://incomingPass", message);
        assertNull("Shouldn't have any exceptions", result.getExceptionPayload());
        assertEquals("thePayload Received component1 Received component2Pass", result.getPayloadAsString());
    }

    @Test
    public void testLastComponentFails() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("thePayload", muleContext);

        MuleMessage result = muleContext.getClient().send("vm://incomingLastFail", message);
        assertNotNull(result);
        assertNotNull(result.getExceptionPayload());
        assertEquals(Component2Exception.class, result.getExceptionPayload().getRootException().getClass());
    }


    @Test
    public void testFirstComponentFails() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("thePayload", muleContext);
        MuleMessage result = muleContext.getClient().send("vm://incomingFirstFail", message);
        assertNotNull(result);
        assertNotNull(result.getExceptionPayload());
        assertEquals(Component1Exception.class, result.getExceptionPayload().getRootException().getClass());
    }
}
