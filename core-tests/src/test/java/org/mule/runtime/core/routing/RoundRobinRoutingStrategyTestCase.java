/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.core.api.Event.getVariableValueOrNull;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.routing.RoutePathNotFoundException;
import org.mule.runtime.core.exception.MessagingException;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class RoundRobinRoutingStrategyTestCase extends AbstractDynamicRoundRobinTestCase {

  private RoundRobinRoutingStrategy roundRobinRoutingStrategy;

  @Before
  public void setUp() {
    roundRobinRoutingStrategy = new RoundRobinRoutingStrategy(muleContext, new IdentifiableDynamicRouteResolver() {

      @Override
      public String getRouteIdentifier(Event event) throws MessagingException {
        return getVariableValueOrNull(ID_PROPERTY_NAME, event);
      }

      @Override
      public List<Processor> resolveRoutes(Event event) throws MessagingException {
        return null;
      }
    });

  }

  @Test(expected = RoutePathNotFoundException.class)
  public void testNullMessageProcessors() throws MuleException {
    roundRobinRoutingStrategy.route(mock(Event.class), null);
  }

  @Test(expected = RoutePathNotFoundException.class)
  public void testEmptyMessageProcessors() throws MuleException {
    roundRobinRoutingStrategy.route(mock(Event.class), Collections.EMPTY_LIST);
  }

  @Test
  public void testRoute() throws Exception {
    List<Processor> messageProcessors = getMessageProcessorsList();
    Event eventToRouteId1 = getEventWithId(ID_1);
    Event eventToRouteId2 = getEventWithId(ID_2);
    assertEquals(LETTER_A, getPayloadAsString(roundRobinRoutingStrategy.route(eventToRouteId1, messageProcessors).getMessage()));
    assertEquals(LETTER_B, getPayloadAsString(roundRobinRoutingStrategy.route(eventToRouteId1, messageProcessors).getMessage()));
    assertEquals(LETTER_A, getPayloadAsString(roundRobinRoutingStrategy.route(eventToRouteId2, messageProcessors).getMessage()));
    assertEquals(LETTER_C, getPayloadAsString(roundRobinRoutingStrategy.route(eventToRouteId1, messageProcessors).getMessage()));
    assertEquals(LETTER_B, getPayloadAsString(roundRobinRoutingStrategy.route(eventToRouteId2, messageProcessors).getMessage()));
    assertEquals(LETTER_C, getPayloadAsString(roundRobinRoutingStrategy.route(eventToRouteId2, messageProcessors).getMessage()));
    assertEquals(LETTER_A, getPayloadAsString(roundRobinRoutingStrategy.route(eventToRouteId1, messageProcessors).getMessage()));
  }

  @Test
  public void testRouteWithFailingMessageProcessor() throws Exception {
    List<Processor> messageProcessors = getMessageProcessorsListWithFailingMessageProcessor();
    Event eventToRoute = getEventWithId(ID_1);
    assertEquals(LETTER_A, getPayloadAsString(roundRobinRoutingStrategy.route(eventToRoute, messageProcessors).getMessage()));
    try {
      roundRobinRoutingStrategy.route(eventToRoute, messageProcessors);
      fail("Exception was expected");
    } catch (RoutingFailedException fe) {
      assertEquals(EXCEPTION_MESSAGE, fe.getCause().getMessage());
    }
    assertEquals(LETTER_B, getPayloadAsString(roundRobinRoutingStrategy.route(eventToRoute, messageProcessors).getMessage()));
    assertEquals(LETTER_A, getPayloadAsString(roundRobinRoutingStrategy.route(eventToRoute, messageProcessors).getMessage()));
  }

  @Test
  public void testNullIdentifier() throws Exception {
    List<Processor> messageProcessors = getMessageProcessorsList();
    Event eventToRoute = getEventWithId(null);
    assertEquals(LETTER_A, getPayloadAsString(roundRobinRoutingStrategy.route(eventToRoute, messageProcessors).getMessage()));
    assertEquals(LETTER_B, getPayloadAsString(roundRobinRoutingStrategy.route(eventToRoute, messageProcessors).getMessage()));
    assertEquals(LETTER_C, getPayloadAsString(roundRobinRoutingStrategy.route(eventToRoute, messageProcessors).getMessage()));
    assertEquals(LETTER_A, getPayloadAsString(roundRobinRoutingStrategy.route(eventToRoute, messageProcessors).getMessage()));
  }

}
