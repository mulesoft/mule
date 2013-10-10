/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.usecases.routing.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.module.client.MuleClient;
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
        MuleClient client = new MuleClient(muleContext);
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

    private static class TestObjectStore<T extends Serializable> extends SimpleMemoryObjectStore
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
