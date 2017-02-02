/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.routing.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.routing.outbound.AbstractOutboundRouter;
import org.mule.tck.junit4.AbstractMuleContextEndpointTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class OutboundRouterTestCase extends AbstractMuleContextEndpointTestCase {

  @Test
  public void testAddGoodEndpoint() throws Exception {
    AbstractOutboundRouter router = new DummyOutboundRouter();
    OutboundEndpoint endpoint = getTestOutboundEndpoint("test");
    router.addRoute(endpoint);
    assertNotNull(router.getRoutes());
    assertTrue(router.getRoutes().contains(endpoint));
  }

  @Test
  public void testSetGoodEndpoints() throws Exception {
    List<Processor> list = new ArrayList<Processor>();
    list.add(getTestOutboundEndpoint("test"));
    list.add(getTestOutboundEndpoint("test"));
    AbstractOutboundRouter router = new DummyOutboundRouter();
    assertNotNull(router.getRoutes());
    assertEquals(0, router.getRoutes().size());
    router.addRoute(getTestOutboundEndpoint("test"));
    assertEquals(1, router.getRoutes().size());
    router.setRoutes(list);
    assertNotNull(router.getRoutes());
    assertEquals(2, router.getRoutes().size());
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void testSetBadEndpoints() throws Exception {
    List list = new ArrayList();
    list.add(getTestInboundEndpoint("test"));
    list.add(getTestOutboundEndpoint("test"));
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
  public void testSetBad2Endpoints() throws Exception {
    List list = new ArrayList();
    list.add(getTestOutboundEndpoint("test"));
    list.add(getTestInboundEndpoint("test"));
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
    public boolean isMatch(Event event, Event.Builder builder) throws MuleException {
      return false;
    }

    @Override
    protected Event route(Event event) throws MuleException {
      return null;
    }
  }
}
