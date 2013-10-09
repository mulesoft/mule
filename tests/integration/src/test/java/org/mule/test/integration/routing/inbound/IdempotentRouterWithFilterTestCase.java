/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.routing.inbound;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class IdempotentRouterWithFilterTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/routing/inbound/idempotent-router-with-filter-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/routing/inbound/idempotent-router-with-filter-flow.xml"}
        });
    }

    public IdempotentRouterWithFilterTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    @SuppressWarnings("null")
    public void testWithValidData()
    {
        /*
         * This test will pass a message containing a String to the Mule server and
         * verifies that it gets received.
         */
        MuleClient myClient;
        DefaultMuleMessage myMessage = new DefaultMuleMessage("Mule is the best!", muleContext);
        MuleMessage response = null;

        try
        {
            myClient = new MuleClient(muleContext);
            myClient.dispatch("vm://FromTestCase", myMessage);
            response = myClient.request("vm://ToTestCase", 5000);
        }
        catch (MuleException e)
        {
            fail(e.getDetailedMessage());
        }

        assertNotNull(response);
        assertNotNull(response.getPayload());
        assertEquals("Mule is the best!", response.getPayload());
    }

    @Test
    public void testWithInvalidData()
    {
        /*
         * This test will pass a message containing an Object to the Mule server and
         * verifies that it does not get received.
         */
        MuleClient myClient;
        DefaultMuleMessage myMessage = new DefaultMuleMessage(new Object(), muleContext);
        MuleMessage response = null;

        try
        {
            myClient = new MuleClient(muleContext);
            myClient.dispatch("vm://FromTestCase", myMessage);
            response = myClient.request("vm://ToTestCase", 5000);
        }
        catch (MuleException e)
        {
            fail(e.getDetailedMessage());
        }

        assertNull(response);
    }
}
