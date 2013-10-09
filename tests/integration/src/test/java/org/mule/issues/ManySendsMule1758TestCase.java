/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.issues;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ManySendsMule1758TestCase extends AbstractServiceAndFlowTestCase
{
    private static int NUM_MESSAGES = 3000;

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/issues/many-sends-mule-1758-test-service.xml"},
            {ConfigVariant.FLOW, "org/mule/issues/many-sends-mule-1758-test-flow.xml"}
        });
    }

    public ManySendsMule1758TestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testSingleSend() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("vm://s-in", "Marco", null);
        assertNotNull("Response is null", response);
        assertEquals("Polo", response.getPayload());
    }

    @Test
    public void testManySends() throws Exception
    {
        long then = System.currentTimeMillis();
        MuleClient client = new MuleClient(muleContext);
        for (int i = 0; i < NUM_MESSAGES; ++i)
        {
            logger.debug("Message " + i);
            MuleMessage response = client.send("vm://s-in", "Marco", null);
            assertNotNull("Response is null", response);
            assertEquals("Polo", response.getPayload());
        }
        long now = System.currentTimeMillis();
        logger.info("Total time " + ((now - then) / 1000.0) + "s; per message " + ((now - then) / (1.0 * NUM_MESSAGES)) + "ms");
    }
}
