/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.serialization.SerializationException;
import org.mule.runtime.core.api.store.ObjectStoreException;
import org.mule.runtime.core.routing.EventGroup;
import org.mule.runtime.core.util.store.SimpleMemoryObjectStore;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;

public class CollectionAggregatorRouterSerializationTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "collection-aggregator-router-serialization.xml";
  }

  @Test
  public void eventGroupDeserialization() throws Exception {
    muleContext.getRegistry().registerObject(MuleProperties.OBJECT_STORE_DEFAULT_IN_MEMORY_NAME,
                                             new EventGroupSerializerObjectStore<Serializable>());
    List<String> list = Arrays.asList("first", "second");
    flowRunner("splitter").withPayload(list).asynchronously().run();

    MuleClient client = muleContext.getClient();
    MuleMessage request = client.request("test://out", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(request);
    assertThat(request.getPayload(), instanceOf(List.class));
    assertThat(((List<MuleMessage>) request.getPayload()), hasSize(list.size()));
  }

  private class EventGroupSerializerObjectStore<T extends Serializable> extends SimpleMemoryObjectStore<Serializable> {

    @Override
    protected void doStore(Serializable key, Serializable value) throws ObjectStoreException {
      if (value instanceof EventGroup) {
        value = SerializationUtils.serialize(value);
      }
      super.doStore(key, value);
    }

    @Override
    protected Serializable doRetrieve(Serializable key) {
      Object value = super.doRetrieve(key);
      if (value instanceof byte[]) {
        try {
          value = SerializationUtils.deserialize((byte[]) value);
        } catch (SerializationException e) {
          // return original value
        }
      }
      return (Serializable) value;
    }
  }
}
