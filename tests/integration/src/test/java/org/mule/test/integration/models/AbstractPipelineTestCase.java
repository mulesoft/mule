/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
