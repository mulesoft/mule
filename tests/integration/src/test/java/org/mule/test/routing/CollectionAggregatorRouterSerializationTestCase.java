/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessageCollection;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.module.client.MuleClient;
import org.mule.routing.EventGroup;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.store.DefaultObjectStoreFactoryBean;
import org.mule.util.store.MuleDefaultObjectStoreFactory;
import org.mule.util.store.SimpleMemoryObjectStore;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.SerializationUtils;
import org.hibernate.type.SerializationException;
import org.junit.Test;

public class CollectionAggregatorRouterSerializationTestCase extends FunctionalTestCase
{
    static
    {
        DefaultObjectStoreFactoryBean.setDelegate(new MuleDefaultObjectStoreFactory(){
            @Override
            public ObjectStore<Serializable> createDefaultInMemoryObjectStore()
            {
                return new EventGroupSerializerObjectStore();
            }
        });
    }

    @Override
    protected String getConfigResources()
    {
        return "collection-aggregator-router-serialization.xml";
    }

    @Test
    public void eventGroupDeserialization() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        List<String> list = Arrays.asList("first", "second");
        client.dispatch("vm://splitter", list, null);
        MuleMessageCollection request = (MuleMessageCollection) client.request("vm://out?connector=queue", 10000);
        assertNotNull(request);
        assertEquals(list.size(), request.size());
    }

    private static class EventGroupSerializerObjectStore<T extends Serializable> extends SimpleMemoryObjectStore<Serializable>
    {
        @Override
        protected void doStore(Serializable key, Serializable value) throws ObjectStoreException
        {
            if (value instanceof EventGroup)
            {
                value = SerializationUtils.serialize(value);
            }
            super.doStore(key, value);
        }

        @Override
        protected Serializable doRetrieve(Serializable key)
        {
            Object value = super.doRetrieve(key);
            if (value instanceof byte[])
            {
                try
                {
                    value = SerializationUtils.deserialize((byte[]) value);
                }
                catch (SerializationException e)
                {
                    // return original value
                }
            }
            return (Serializable) value;
        }
    }
}
