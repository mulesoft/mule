/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.client;

import org.mule.api.MuleException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MuleClientTestCase extends AbstractMuleClientTestCase
{

    @Test
    public void testCreateMuleClient() throws MuleException
    {
        assertNotNull(muleContext);
        MuleClient muleClient = new MuleClient(muleContext);
        assertEquals(muleContext, muleClient.getMuleContext());
        assertTrue(muleContext.isInitialised());

        muleContext.start();

        assertTrue(muleContext.isStarted());
        muleClient.dispatch("test://test", "message", null);
        muleClient.send("test://test", "message", null);
        muleClient.dispose();
        assertTrue(muleClient.getMuleContext().isInitialised());
        assertTrue(muleClient.getMuleContext().isStarted());
    }

}
