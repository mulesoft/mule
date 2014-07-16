/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
