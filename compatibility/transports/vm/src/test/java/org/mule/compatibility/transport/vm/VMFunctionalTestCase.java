/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.vm;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.store.ObjectStoreException;
import org.mule.runtime.core.api.store.QueueStore;
import org.mule.runtime.core.util.queue.DelegateQueueManager;
import org.mule.runtime.core.util.store.SimpleMemoryObjectStore;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.Serializable;

import org.junit.Rule;
import org.junit.Test;

public class VMFunctionalTestCase extends CompatibilityFunctionalTestCase {

  @Rule
  public SystemProperty useOldQueueMode = new SystemProperty(DelegateQueueManager.MULE_QUEUE_OLD_MODE_KEY, "true");

  public VMFunctionalTestCase() {
    setDisposeContextPerClass(true);
  }

  @Override
  protected String getConfigFile() {
    return "vm/vm-functional-test-flow.xml";
  }

  @Test
  public void testSingleMessage() throws Exception {
    MuleClient client = muleContext.getClient();
    client.dispatch("vm://in", "Marco", null);
    InternalMessage response = client.request("vm://out", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull("Response is null", response);
    assertEquals("Polo", response.getPayload().getValue());
  }

  @Test
  public void testRequest() throws Exception {
    MuleClient client = muleContext.getClient();
    client.dispatch("vm://in", "Marco", null);
    InternalMessage response = client.request("vm://out", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull("Response is null", response);
    assertEquals("Polo", response.getPayload().getValue());
  }

  @Test
  public void testMultipleMessages() throws Exception {
    MuleClient client = muleContext.getClient();
    client.dispatch("vm://in", "Marco", null);
    client.dispatch("vm://in", "Marco", null);
    client.dispatch("vm://in", "Marco", null);
    InternalMessage response;
    for (int i = 0; i < 3; ++i) {
      response = client.request("vm://out", RECEIVE_TIMEOUT).getRight().get();
      assertNotNull("Response is null", response);
      assertEquals("Polo", response.getPayload().getValue());
    }

    assertThat(client.request("vm://out", RECEIVE_TIMEOUT).getRight().isPresent(), is(false));
  }

  @Test
  public void testOneWayChain() throws Exception {
    MuleClient client = muleContext.getClient();
    client.dispatch("vm://in1", "Marco", null);
    InternalMessage response = client.request("vm://out1", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull("Response is null", response);
    assertEquals("Polo", response.getPayload().getValue());
    assertTrue(CustomObjectStore.count > 0); // ensure custom store was used
  }

  @Test
  public void testRequestResponseChain() throws Exception {
    MuleClient client = muleContext.getClient();
    InternalMessage response = client.send("vm://in2", "Marco", null).getRight();
    assertNotNull("Response is null", response);
    assertEquals("Polo", response.getPayload().getValue());
  }

  @Test
  public void testNoMessageDuplication() throws Exception {
    MuleClient client = muleContext.getClient();
    client.dispatch("vm://in", "Marco", null);
    InternalMessage response = client.request("vm://out", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull("Response is null", response);
    assertEquals("Polo", response.getPayload().getValue());
    assertThat(client.request("vm://out", RECEIVE_TIMEOUT).getRight().isPresent(), is(false));
  }

  public static class CustomObjectStore<T extends Serializable> extends SimpleMemoryObjectStore<T> implements QueueStore<T> {

    static int count;

    public CustomObjectStore() {
      super();
    }

    @Override
    protected void doStore(Serializable key, T value) throws ObjectStoreException {
      count++;
      super.doStore(key, value);
    }
  }
}
