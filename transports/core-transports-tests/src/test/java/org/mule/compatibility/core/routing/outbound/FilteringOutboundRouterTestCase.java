/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.routing.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.endpoint.outbound.EndpointMulticastingRouter;
import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.api.MessageContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.routing.filters.PayloadTypeFilter;
import org.mule.runtime.core.routing.outbound.FilteringOutboundRouter;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.tck.MuleEventCheckAnswer;
import org.mule.tck.junit4.AbstractMuleContextEndpointTestCase;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class FilteringOutboundRouterTestCase extends AbstractMuleContextEndpointTestCase {

  public FilteringOutboundRouterTestCase() {
    setStartContext(true);
  }

  @Test
  public void testFilteringOutboundRouterAsync() throws Exception {
    OutboundEndpoint endpoint1 =
        getTestOutboundEndpoint("Test1Provider", "test://Test1Provider?exchangePattern=request-response");
    assertNotNull(endpoint1);
    OutboundEndpoint mockEndpoint = RouterTestUtils.createMockEndpoint(endpoint1);

    FilteringOutboundRouter router = new EndpointMulticastingRouter();
    router.setMuleContext(muleContext);
    PayloadTypeFilter filter = new PayloadTypeFilter(String.class);
    router.setMuleContext(muleContext);
    router.setFilter(filter);
    List<MessageProcessor> endpoints = new ArrayList<>();
    endpoints.add(mockEndpoint);
    router.setRoutes(endpoints);

    // Default is now true
    assertTrue(router.isUseTemplates());
    assertEquals(filter, router.getFilter());

    MuleMessage message = MuleMessage.builder().payload("test event").build();

    assertTrue(router.isMatch(getTestEvent(message)));

    when(mockEndpoint.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer());
    MuleSession session = mock(MuleSession.class);
    Flow flow = getTestFlow();
    router.route(MuleEvent.builder(DefaultMessageContext.create(flow, TEST_CONNECTOR)).message(message).flow(flow)
        .session(session).build());

    // Test with transform
    message = MuleMessage.builder().payload(new Exception("test event")).build();

    assertTrue(!router.isMatch(getTestEvent(message)));

    router.setTransformers(Arrays.<Transformer>asList(new AbstractTransformer() {

      @Override
      public Object doTransform(Object src, Charset outputEncoding) throws TransformerException {
        return ((Exception) src).getMessage();
      }
    }));

    assertTrue(router.isMatch(getTestEvent(message)));
  }

  @Test
  public void testFilteringOutboundRouterSync() throws Exception {
    OutboundEndpoint endpoint1 =
        getTestOutboundEndpoint("Test1Provider", "test://Test1Provider?exchangePattern=request-response");
    assertNotNull(endpoint1);
    OutboundEndpoint mockEndpoint = RouterTestUtils.createMockEndpoint(endpoint1);

    FilteringOutboundRouter router = new EndpointMulticastingRouter();
    router.setMuleContext(muleContext);
    PayloadTypeFilter filter = new PayloadTypeFilter(String.class);
    router.setFilter(filter);
    List<OutboundEndpoint> endpoints = new ArrayList<>();
    endpoints.add(mockEndpoint);
    router.setRoutes(new ArrayList<MessageProcessor>(endpoints));

    // Default is now true
    assertTrue(router.isUseTemplates());
    assertEquals(filter, router.getFilter());

    MuleMessage message = MuleMessage.builder().payload("test event").build();
    Flow flow = getTestFlow();
    final MessageContext context = DefaultMessageContext.create(flow, TEST_CONNECTOR);
    MuleEvent event = MuleEvent.builder(context).message(message).flow(flow).build();
    when(mockEndpoint.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(event));

    MuleSession session = mock(MuleSession.class);
    MuleEvent result = router.route(MuleEvent.builder(context).message(message).flow(flow).build());
    assertNotNull(result);
    assertEquals(message, result.getMessage());
  }

  @Test
  public void testFilteringOutboundRouterWithTemplates() throws Exception {
    OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider", "test://foo?[barValue]");
    assertNotNull(endpoint1);

    FilteringOutboundRouter router = new EndpointMulticastingRouter();
    router.setMuleContext(muleContext);
    PayloadTypeFilter filter = new PayloadTypeFilter(String.class);
    router.setFilter(filter);
    List<OutboundEndpoint> endpoints = new ArrayList<>();
    endpoints.add(endpoint1);
    router.setRoutes(new ArrayList<MessageProcessor>(endpoints));

    assertTrue(router.isUseTemplates());
    assertEquals(filter, router.getFilter());

    Map<String, Serializable> m = new HashMap<>();
    m.put("barValue", "bar");
    MuleMessage message = MuleMessage.builder().payload("test event").outboundProperties(m).build();
    Flow flow = getTestFlow();
    final MessageContext context = DefaultMessageContext.create(flow, TEST_CONNECTOR);
    MuleEvent event = MuleEvent.builder(context).message(message).flow(flow).build();

    assertTrue(router.isMatch(getTestEvent(message)));
    OutboundEndpoint ep = (OutboundEndpoint) router.getRoute(0, event);
    // MULE-2690: assert that templated targets are not mutated
    assertNotSame(endpoint1, ep);
    // assert that the returned endpoint has a resolved URI
    assertEquals("test://foo?bar", ep.getEndpointURI().toString());
  }
}
