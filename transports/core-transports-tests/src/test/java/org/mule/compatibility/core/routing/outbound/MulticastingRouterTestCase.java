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

import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.endpoint.outbound.EndpointMulticastingRouter;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.routing.CorrelationMode;
import org.mule.runtime.core.routing.filters.RegExFilter;
import org.mule.tck.MuleEventCheckAnswer;
import org.mule.tck.junit4.AbstractMuleContextEndpointTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    List<MessageProcessor> endpoints = new ArrayList<>();
    endpoints.add(mockendpoint1);
    endpoints.add(mockendpoint2);
    router.setRoutes(endpoints);

    assertTrue(router.isMatch(getTestEvent(TEST_MESSAGE)));

    when(mockendpoint1.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer());
    when(mockendpoint2.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer());
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
    List<MessageProcessor> endpoints = new ArrayList<>();
    endpoints.add(mockendpoint1);
    endpoints.add(mockendpoint2);
    router.setRoutes(endpoints);

    assertEquals(filter, router.getFilter());

    MuleMessage message = MuleMessage.builder().payload(TEST_MESSAGE).build();

    assertTrue(router.isMatch(getTestEvent(message)));

    MuleEvent event = new OutboundRoutingTestEvent(message, null, muleContext);

    when(mockendpoint1.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(event));
    when(mockendpoint2.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(event));

    MuleSession session = mock(MuleSession.class);
    MuleEvent result = router.route(new OutboundRoutingTestEvent(message, session, muleContext));
    assertNotNull(result);
    MuleMessage resultMessage = result.getMessage();
    assertNotNull(resultMessage);
    assertEquals(2, ((List<MuleMessage>) resultMessage.getPayload()).size());
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

    List<MessageProcessor> endpoints = new ArrayList<>();
    endpoints.add(mockendpoint1);
    endpoints.add(mockendpoint2);
    router.setRoutes(endpoints);

    MuleMessage message = MuleMessage.builder().payload(TEST_MESSAGE).build();

    assertTrue(router.isMatch(getTestEvent(message)));
    MuleEvent event = new OutboundRoutingTestEvent(message, null, muleContext);

    when(mockendpoint1.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(event));
    when(mockendpoint2.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer());

    MuleSession session = mock(MuleSession.class);
    MuleEvent result = router.route(new OutboundRoutingTestEvent(message, session, muleContext));
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

    List<MessageProcessor> endpoints = new ArrayList<>();
    endpoints.add(mockendpoint1);
    endpoints.add(mockendpoint2);
    router.setRoutes(endpoints);
    router.setEnableCorrelation(CorrelationMode.NEVER);

    MuleMessage message = MuleMessage.builder().payload(TEST_MESSAGE).correlationId("MyCustomCorrelationId").build();

    assertTrue(router.isMatch(getTestEvent(message)));

    Answer<MuleEvent> answer = invocation -> {
      Object[] arguments = invocation.getArguments();
      assertEquals(1, arguments.length);
      assertTrue(arguments[0] instanceof MuleEvent);

      MuleEvent event = (MuleEvent) arguments[0];
      Optional<String> correlationId = event.getMessage().getCorrelation().getId();
      assertThat(correlationId.isPresent(), is(true));
      assertThat(correlationId.get(), is("MyCustomCorrelationId"));

      return event;
    };
    when(mockendpoint1.process(any(MuleEvent.class))).thenAnswer(answer);
    when(mockendpoint2.process(any(MuleEvent.class))).thenAnswer(answer);

    MuleSession session = mock(MuleSession.class);
    router.route(new OutboundRoutingTestEvent(message, session, muleContext));
  }

  private String getPayload(MuleMessage message) throws Exception {
    Object payload = message.getPayload();
    if (payload instanceof List) {
      payload = ((List<?>) payload).get(0);
    }
    return payload.toString();
  }
}
