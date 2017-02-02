/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.routing.outbound;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.tck.MuleTestUtils.getTestFlow;

import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.endpoint.outbound.EndpointMulticastingRouter;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.routing.filters.RegExFilter;
import org.mule.tck.MuleEventCheckAnswer;
import org.mule.tck.junit4.AbstractMuleContextEndpointTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.stubbing.Answer;

public class MulticastingRouterTestCase extends AbstractMuleContextEndpointTestCase {

  public MulticastingRouterTestCase() {
    setStartContext(true);
  }

  @Test
  public void testMulticastingRouterAsync() throws Exception {
    RegExFilter filter = new RegExFilter("(.*) Message");
    filter.setMuleContext(muleContext);

    OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider", "test://test1", null, filter, null);
    assertNotNull(endpoint1);
    OutboundEndpoint mockendpoint1 = RouterTestUtils.createMockEndpoint(endpoint1);

    OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Provider", "test://test2", null, filter, null);
    assertNotNull(endpoint2);
    OutboundEndpoint mockendpoint2 = RouterTestUtils.createMockEndpoint(endpoint2);

    EndpointMulticastingRouter router = createObject(EndpointMulticastingRouter.class);

    List<Processor> endpoints = new ArrayList<>();
    endpoints.add(mockendpoint1);
    endpoints.add(mockendpoint2);
    router.setRoutes(endpoints);

    assertTrue(router.isMatch(testEvent(), mock(Event.Builder.class)));

    when(mockendpoint1.process(any(Event.class))).thenAnswer(new MuleEventCheckAnswer());
    when(mockendpoint2.process(any(Event.class))).thenAnswer(new MuleEventCheckAnswer());
  }

  @Test
  public void testMulticastingRouterSync() throws Exception {
    OutboundEndpoint endpoint1 =
        getTestOutboundEndpoint("Test1Provider", "test://Test1Provider?exchangePattern=request-response");
    assertNotNull(endpoint1);
    OutboundEndpoint mockendpoint1 = RouterTestUtils.createMockEndpoint(endpoint1);

    OutboundEndpoint endpoint2 =
        getTestOutboundEndpoint("Test2Provider", "test://Test2Provider?exchangePattern=request-response");
    assertNotNull(endpoint2);
    OutboundEndpoint mockendpoint2 = RouterTestUtils.createMockEndpoint(endpoint2);

    EndpointMulticastingRouter router = createObject(EndpointMulticastingRouter.class);
    RegExFilter filter = new RegExFilter("(.*) Message");
    filter.setMuleContext(muleContext);
    router.setFilter(filter);
    List<Processor> endpoints = new ArrayList<>();
    endpoints.add(mockendpoint1);
    endpoints.add(mockendpoint2);
    router.setRoutes(endpoints);

    assertEquals(filter, router.getFilter());

    InternalMessage message = InternalMessage.builder().payload(TEST_MESSAGE).build();

    Flow flow = getTestFlow(muleContext);
    final EventContext context = DefaultEventContext.create(flow, TEST_CONNECTOR);
    assertTrue(router.isMatch(Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR)).message(message).build(),
                              mock(Event.Builder.class)));

    Event event = Event.builder(context).message(message).flow(flow).build();

    when(mockendpoint1.process(any(Event.class))).thenAnswer(new MuleEventCheckAnswer(event));
    when(mockendpoint2.process(any(Event.class))).thenAnswer(new MuleEventCheckAnswer(event));

    MuleSession session = mock(MuleSession.class);
    Event result = router.route(Event.builder(context).message(message).flow(flow).session(session).build());
    assertNotNull(result);
    InternalMessage resultMessage = result.getMessage();
    assertNotNull(resultMessage);
    assertEquals(2, ((List<InternalMessage>) resultMessage.getPayload().getValue()).size());
  }

  @Test
  public void testMulticastingRouterMixedSyncAsync() throws Exception {
    OutboundEndpoint endpoint1 =
        getTestOutboundEndpoint("Test1Provider", "test://Test1Provider?exchangePattern=request-response");
    assertNotNull(endpoint1);
    OutboundEndpoint mockendpoint1 = RouterTestUtils.createMockEndpoint(endpoint1);

    OutboundEndpoint endpoint2 =
        getTestOutboundEndpoint("Test2Provider", "test://Test2Provider?exchangePattern=request-response");
    assertNotNull(endpoint2);
    OutboundEndpoint mockendpoint2 = RouterTestUtils.createMockEndpoint(endpoint2);

    EndpointMulticastingRouter router = createObject(EndpointMulticastingRouter.class);

    List<Processor> endpoints = new ArrayList<>();
    endpoints.add(mockendpoint1);
    endpoints.add(mockendpoint2);
    router.setRoutes(endpoints);

    InternalMessage message = InternalMessage.builder().payload(TEST_MESSAGE).build();

    Flow flow = getTestFlow(muleContext);
    final EventContext context = DefaultEventContext.create(flow, TEST_CONNECTOR);
    assertTrue(router.isMatch(Event.builder(context).message(message).build(), mock(Event.Builder.class)));
    Event event = Event.builder(context).message(message).flow(flow).build();

    when(mockendpoint1.process(any(Event.class))).thenAnswer(new MuleEventCheckAnswer(event));
    when(mockendpoint2.process(any(Event.class))).thenAnswer(new MuleEventCheckAnswer());

    MuleSession session = mock(MuleSession.class);

    Event result = router.route(Event.builder(context).message(message).flow(flow).session(session).build());
    assertNotNull(result);
    assertEquals(getPayload(message), getPayload(result.getMessage()));
  }

  @Test
  public void testMulticastingRouterCorrelationIdPropagation() throws Exception {
    RegExFilter filter = new RegExFilter("(.*) Message");
    filter.setMuleContext(muleContext);

    OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider", "test://test1", null, filter, null);
    assertNotNull(endpoint1);
    OutboundEndpoint mockendpoint1 = RouterTestUtils.createMockEndpoint(endpoint1);

    OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Provider", "test://test2", null, filter, null);
    assertNotNull(endpoint2);
    OutboundEndpoint mockendpoint2 = RouterTestUtils.createMockEndpoint(endpoint2);

    EndpointMulticastingRouter router = createObject(EndpointMulticastingRouter.class);

    List<Processor> endpoints = new ArrayList<>();
    endpoints.add(mockendpoint1);
    endpoints.add(mockendpoint2);
    router.setRoutes(endpoints);

    InternalMessage message = InternalMessage.builder().payload(TEST_MESSAGE).build();
    Flow flow = getTestFlow(muleContext);
    final EventContext context = create(flow, TEST_CONNECTOR, "MyCustomCorrelationId");
    final Event testEvent = Event.builder(context).message(message).flow(flow).build();

    assertTrue(router.isMatch(testEvent, mock(Event.Builder.class)));

    Answer<Event> answer = invocation -> {
      Object[] arguments = invocation.getArguments();
      assertEquals(1, arguments.length);
      assertTrue(arguments[0] instanceof Event);

      Event event = (Event) arguments[0];
      String correlationId = event.getCorrelationId();
      assertThat(correlationId, is("MyCustomCorrelationId"));

      return event;
    };
    when(mockendpoint1.process(any(Event.class))).thenAnswer(answer);
    when(mockendpoint2.process(any(Event.class))).thenAnswer(answer);

    MuleSession session = mock(MuleSession.class);
    router.route(Event.builder(context).message(message).flow(flow).session(session).build());
  }

  private String getPayload(InternalMessage message) throws Exception {
    Object payload = message.getPayload().getValue();
    if (payload instanceof List) {
      payload = ((List<?>) payload).get(0);
    }
    return payload.toString();
  }
}
