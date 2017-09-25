/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.routing.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class OutboundRouterTestCase extends AbstractMuleContextTestCase {

  @Test
  public void testAddGoodProcessor() throws Exception {
    AbstractOutboundRouter router = new DummyOutboundRouter();
    Processor processor = getTestMessageProcessor();
    router.addRoute(processor);
    assertNotNull(router.getRoutes());
    assertTrue(router.getRoutes().contains(processor));
  }

  private Processor getTestMessageProcessor() {
    return mock(Processor.class);
  }

  @Test
  public void testSetGoodProcessors() throws Exception {
    List<Processor> list = new ArrayList<Processor>();
    list.add(getTestMessageProcessor());
    list.add(getTestMessageProcessor());
    AbstractOutboundRouter router = new DummyOutboundRouter();
    assertNotNull(router.getRoutes());
    assertEquals(0, router.getRoutes().size());
    router.addRoute(getTestMessageProcessor());
    assertEquals(1, router.getRoutes().size());
    router.setRoutes(list);
    assertNotNull(router.getRoutes());
    assertEquals(2, router.getRoutes().size());
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void testSetBadProcessors() throws Exception {
    List list = new ArrayList();
    list.add(mock(MessageSource.class));
    list.add(getTestMessageProcessor());
    AbstractOutboundRouter router = new DummyOutboundRouter();

    try {
      router.setRoutes(list);
      fail("Invalid endpoint: Expecting an exception");
    } catch (Exception e) {
      assertEquals(ClassCastException.class, e.getClass());
    }
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void testSetBad2Processors() throws Exception {
    List list = new ArrayList();
    list.add(getTestMessageProcessor());
    list.add(mock(MessageSource.class));
    AbstractOutboundRouter router = new DummyOutboundRouter();

    try {
      router.setRoutes(list);
      fail("Invalid endpoint: Expecting an exception");
    } catch (Exception e) {
      assertEquals(ClassCastException.class, e.getClass());
    }
  }

  private static class DummyOutboundRouter extends AbstractOutboundRouter {

    @Override
    public boolean isMatch(CoreEvent event, CoreEvent.Builder builder) throws MuleException {
      return false;
    }

    @Override
    protected CoreEvent route(CoreEvent event) throws MuleException {
      return null;
    }
  }
}
