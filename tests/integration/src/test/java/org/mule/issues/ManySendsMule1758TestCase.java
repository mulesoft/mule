/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ManySendsMule1758TestCase extends FunctionalTestCase
{

    private static int NUM_MESSAGES = 3000;

    @Override
    protected String getConfigResources()
    {
        return "org/mule/issues/many-sends-mule-1758-test.xml";
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
