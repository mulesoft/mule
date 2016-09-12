/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.routing.IdempotentMessageFilter;
import org.mule.runtime.core.routing.IdempotentSecureHashMessageFilter;
import org.mule.runtime.core.routing.outbound.AbstractOutboundRouter;
import org.mule.runtime.core.util.store.InMemoryObjectStore;
import org.mule.runtime.core.util.store.TextFileObjectStore;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.List;

import org.junit.Test;

public class CoreNamespaceRoutersFlowTestCase extends AbstractIntegrationTestCase {

  @Override
  public String getConfigFile() {
    return "core-namespace-routers-flow.xml";
  }

  @Test
  public void testIdempotentSecureHashReceiverRouter() throws Exception {
    Processor router = lookupMessageProcessorFromFlow("IdempotentSecureHashReceiverRouter");
    assertTrue(router instanceof IdempotentSecureHashMessageFilter);

    IdempotentSecureHashMessageFilter filter = (IdempotentSecureHashMessageFilter) router;
    assertEquals("SHA-128", filter.getMessageDigestAlgorithm());
    assertNotNull(filter.getStore());
    assertTrue(filter.getStore() instanceof InMemoryObjectStore);

    InMemoryObjectStore<String> store = (InMemoryObjectStore<String>) filter.getStore();
    assertEquals(1001, store.getEntryTTL());
    assertEquals(1001, store.getExpirationInterval());
    assertEquals(1001, store.getMaxEntries());
    assertEquals("xyz", store.getName());
    assertNotNull(store.getScheduler());
  }

  @Test
  public void testIdempotentReceiverRouter() throws Exception {
    Processor router = lookupMessageProcessorFromFlow("IdempotentReceiverRouter");
    assertTrue(router instanceof IdempotentMessageFilter);

    IdempotentMessageFilter filter = (IdempotentMessageFilter) router;
    assertEquals("#[message:id]-#[message:correlationId]", filter.getIdExpression());
    assertNotNull(filter.getStore());
    assertTrue(filter.getStore() instanceof TextFileObjectStore);

    TextFileObjectStore store = (TextFileObjectStore) filter.getStore();
    assertEquals(-1, store.getEntryTTL());
    assertEquals(1000, store.getExpirationInterval());
    assertEquals(10000000, store.getMaxEntries());
    assertEquals("foo", store.getDirectory());
    assertNotNull(store.getName());
    assertNotNull(store.getScheduler());
  }

  @Test
  public void testCustomRouter() throws Exception {
    Processor router = lookupCustomRouterFromFlow("CustomRouter");
    assertTrue(router instanceof CustomRouter);
  }

  protected Processor lookupCustomRouterFromFlow(String flowName) throws Exception {
    Flow flow = lookupFlow(flowName);
    return flow.getMessageProcessors().get(0);
  }

  protected Processor lookupMessageProcessorFromFlow(String flowName) throws Exception {
    Flow flow = lookupFlow(flowName);
    List<Processor> routers = flow.getMessageProcessors();
    assertEquals(1, routers.size());
    return routers.get(0);
  }

  protected Flow lookupFlow(String flowName) {
    Flow flow = muleContext.getRegistry().lookupObject(flowName);
    assertNotNull(flow);
    return flow;
  }

  public static class CustomRouter extends AbstractOutboundRouter {

    @Override
    public boolean isMatch(Event message, Event.Builder builder) throws MuleException {
      return true;
    }

    @Override
    protected Event route(Event event) throws MuleException {
      return event;
    }
  }
}
