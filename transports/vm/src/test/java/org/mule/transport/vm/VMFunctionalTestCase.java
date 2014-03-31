/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.store.QueueStore;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.util.queue.DelegateQueueManager;
import org.mule.util.store.SimpleMemoryObjectStore;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class VMFunctionalTestCase extends AbstractServiceAndFlowTestCase
{

    @Rule
    public SystemProperty useOldQueueMode = new SystemProperty(DelegateQueueManager.MULE_QUEUE_OLD_MODE_KEY, "true");

    public VMFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        setDisposeContextPerClass(true);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "vm/vm-functional-test-service.xml"},
            {ConfigVariant.FLOW, "vm/vm-functional-test-flow.xml"}
        });
    }

    @Test
    public void testSingleMessage() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://in", "Marco", null);
        MuleMessage response = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull("Response is null", response);
        assertEquals("Polo", response.getPayload());
    }

    @Test
    public void testRequest() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://in", "Marco", null);
        MuleMessage response = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull("Response is null", response);
        assertEquals("Polo", response.getPayload());
    }

    @Test
    public void testMultipleMessages() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://in", "Marco", null);
        client.dispatch("vm://in", "Marco", null);
        client.dispatch("vm://in", "Marco", null);
        MuleMessage response;
        for (int i = 0; i < 3; ++i)
        {
            response = client.request("vm://out", RECEIVE_TIMEOUT);
            assertNotNull("Response is null", response);
            assertEquals("Polo", response.getPayload());
        }

        MuleMessage secondMessage = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNull(secondMessage);
    }

    @Test
    public void testOneWayChain() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://in1", "Marco", null);
        MuleMessage response = client.request("vm://out1", RECEIVE_TIMEOUT);
        assertNotNull("Response is null", response);
        assertEquals("Polo", response.getPayload());
        assertTrue(CustomObjectStore.count > 0); // ensure custom store was used
    }

    @Test
    public void testRequestResponseChain() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://in2", "Marco", null);
        assertNotNull("Response is null", response);
        assertEquals("Polo", response.getPayload());
    }

    @Test
    public void testNoMessageDuplication() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://in", "Marco", null);
        MuleMessage response = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull("Response is null", response);
        assertEquals("Polo", response.getPayload());
        MuleMessage secondMessage = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNull(secondMessage);
    }

    public static class CustomObjectStore<T extends Serializable> extends SimpleMemoryObjectStore<T> implements QueueStore<T>
    {
        static int count;

        public CustomObjectStore()
        {
            super();
        }

        @Override
        protected void doStore(Serializable key, T value) throws ObjectStoreException
        {
            count++;
            super.doStore(key, value);
        }
    }
}
