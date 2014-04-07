/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.api.MuleMessageCollection;
import org.mule.api.client.MuleClient;
import org.mule.api.config.MuleProperties;
import org.mule.api.store.ObjectStoreException;
import org.mule.routing.EventGroup;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.store.SimpleMemoryObjectStore;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.SerializationUtils;
import org.hibernate.type.SerializationException;
import org.junit.Test;

public class CollectionAggregatorRouterSerializationTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "collection-aggregator-router-serialization.xml";
    }

    @Test
    public void eventGroupDeserialization() throws Exception
    {
        muleContext.getRegistry().registerObject(MuleProperties.OBJECT_STORE_DEFAULT_IN_MEMORY_NAME,
                                                 new EventGroupSerializerObjectStore<Serializable>());
        MuleClient client = muleContext.getClient();
        List<String> list = Arrays.asList("first", "second");
        client.dispatch("vm://splitter", list, null);
        MuleMessageCollection request = (MuleMessageCollection) client.request("vm://out?connector=queue", 10000);
        assertNotNull(request);
        assertEquals(list.size(), request.size());
    }

    private class EventGroupSerializerObjectStore<T extends Serializable> extends SimpleMemoryObjectStore<Serializable>
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
