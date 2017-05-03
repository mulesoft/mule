/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.store.ObjectStore;
import org.mule.runtime.core.routing.IdempotentMessageFilter;
import org.mule.runtime.core.util.store.SimpleMemoryObjectStore;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.Serializable;
import java.util.List;

import org.junit.Test;

/**
 * Tests for all object stores that can be configured on an {@link IdempotentMessageFilter}.
 */
public class IdempotentMessageFilterNamespaceHandlerTestCase extends AbstractIntegrationTestCase {

  public IdempotentMessageFilterNamespaceHandlerTestCase() {
    // we just test the wiring of the objects, no need to start the MuleContext
    setStartContext(false);
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/config/idempotent-message-filter-config.xml";
  }

  @Test
  public void testCustomObjectStore() throws Exception {
    testPojoObjectStore("customObjectStore");
  }

  private void testPojoObjectStore(final String flowName) throws Exception {
    final IdempotentMessageFilter filter = idempotentMessageFilterFromFlow(flowName);

    final ObjectStore<?> store = filter.getObjectStore();
    assertThat(store, instanceOf(CustomObjectStore.class));

    final CustomObjectStore customStore = (CustomObjectStore) store;
    assertEquals("the-value:" + flowName, customStore.getCustomProperty());
  }

  private IdempotentMessageFilter idempotentMessageFilterFromFlow(final String flowName) throws Exception {
    final FlowConstruct flow = getFlowConstruct(flowName);
    assertTrue(flow instanceof Flow);

    final Flow simpleFlow = (Flow) flow;
    final List<Processor> processors = simpleFlow.getMessageProcessors();
    assertEquals(1, processors.size());

    final Processor firstMP = processors.get(0);
    assertEquals(IdempotentMessageFilter.class, firstMP.getClass());

    return (IdempotentMessageFilter) firstMP;
  }

  public static class CustomObjectStore extends SimpleMemoryObjectStore<Serializable> {

    private String customProperty;

    public String getCustomProperty() {
      return customProperty;
    }

    public void setCustomProperty(final String value) {
      customProperty = value;
    }
  }
}
