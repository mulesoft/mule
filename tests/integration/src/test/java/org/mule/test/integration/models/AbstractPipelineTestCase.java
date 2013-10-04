/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.models;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public abstract class AbstractPipelineTestCase extends FunctionalTestCase
{

    protected int getNumberOfMessages()
    {
        return 100;
    }

    @Test
    public void testPipelineSynchronous() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        List results = new ArrayList();
        for (int i = 0; i < getNumberOfMessages(); i++)
        {
            MuleMessage result = client.send("component1.endpoint", "test", null);
            assertNotNull(result);
            results.add(result);
        }

        assertEquals(results.size(), getNumberOfMessages());
        for (Iterator iterator = results.iterator(); iterator.hasNext();)
        {
            MuleMessage message = (MuleMessage)iterator.next();
            assertEquals("request received by service 3", message.getPayloadAsString());
        }
    }

    @Test
    public void testPipelineAsynchronous() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        List results = new ArrayList();
        for (int i = 0; i < getNumberOfMessages(); i++)
        {
            client.dispatch("component1.endpoint", "test", null);
        }

        for (int i = 0; i < getNumberOfMessages(); i++)
        {
            MuleMessage result = client.request("results.endpoint", 1000);
            assertNotNull(result);
            results.add(result);
        }
        assertEquals(results.size(), getNumberOfMessages());
        for (Iterator iterator = results.iterator(); iterator.hasNext();)
        {
            MuleMessage message = (MuleMessage)iterator.next();
            assertEquals("request received by service 3", message.getPayloadAsString());
        }
    }
}
