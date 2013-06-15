/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.routing.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.SerializationUtils;
import org.mule.util.store.DefaultObjectStoreFactoryBean;
import org.mule.util.store.MuleDefaultObjectStoreFactory;
import org.mule.util.store.SimpleMemoryObjectStore;

import java.io.Serializable;

import org.junit.Rule;
import org.junit.Test;

public class SerializationOnResposeAggregatorTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/usecases/routing/response/serialization-on-response-router-config.xml";
    }

    @Override
    protected MuleContext createMuleContext() throws Exception
    {
        // Replaces the original object store with one that serializes the data
        DefaultObjectStoreFactoryBean.setDelegate(new TestObjectStoreFactory());
        return super.createMuleContext();
    }

    @Test
    public void testSyncResponse() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("http://localhost:" + dynamicPort.getNumber() , "request", null);
        assertNotNull(message);
        assertEquals("request processed", new String(message.getPayloadAsBytes()));
    }

    private static class TestObjectStoreFactory extends MuleDefaultObjectStoreFactory
    {
        @Override
        public ObjectStore<Serializable> createDefaultInMemoryObjectStore()
        {
            return new TestObjectStore<Serializable>();
        }
    }

    private static class TestObjectStore<T extends Serializable> extends SimpleMemoryObjectStore<Serializable>
    {
        @Override
        protected void doStore(Serializable key, Serializable value) throws ObjectStoreException
        {
            byte[] serialized = SerializationUtils.serialize(value);
            super.doStore(key, serialized);
        }

        @Override
        protected Serializable doRetrieve(Serializable key)
        {
            Serializable serialized = super.doRetrieve(key);
            return (Serializable) SerializationUtils.deserialize((byte[]) serialized);
        }
    }
}
