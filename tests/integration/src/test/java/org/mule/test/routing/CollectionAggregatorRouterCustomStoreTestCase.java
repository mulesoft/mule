/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.store.ObjectStoreException;
import org.mule.runtime.core.util.store.PartitionedInMemoryObjectStore;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class CollectionAggregatorRouterCustomStoreTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "collection-aggregator-router-custom-store.xml";
  }

  @Test
  public void eventGroupWithCustomStore() throws Exception {
    MuleClient client = muleContext.getClient();
    List<String> list = Arrays.asList("first", "second");

    flowRunner("splitter").withPayload(list).run();

    MuleMessage request = client.request("test://out", 10000).getRight().get();
    assertNotNull(request);
    assertEquals(list.size(), ((List) request.getPayload()).size());

    assertThat(CustomPartitionableObjectStore.askedForKey, not(nullValue()));
    assertThat(CustomPartitionableObjectStore.askedForPartition, not(nullValue()));
  }


  public static class CustomPartitionableObjectStore extends PartitionedInMemoryObjectStore<Serializable> {

    private static Serializable askedForKey;
    private static Serializable askedForPartition;

    @Override
    public Serializable retrieve(Serializable key, String partitionKey) throws ObjectStoreException {
      askedForKey = key;
      askedForPartition = partitionKey;
      return super.retrieve(key, partitionKey);
    }
  }

}
