/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.mule.model;

import static org.junit.Assert.assertEquals;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;

import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.DefaultMuleEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.model.InvocationResult;
import org.mule.runtime.core.model.resolvers.AbstractArgumentEntryPointResolver;
import org.mule.runtime.core.model.resolvers.ArrayEntryPointResolver;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.Orange;

import org.junit.Test;

public class ArrayEntryPointResolverTestCase extends AbstractMuleContextTestCase {

  @Test
  public void testArrayMatch() throws Exception {
    AbstractArgumentEntryPointResolver resolver = new ArrayEntryPointResolver();
    FlowConstruct flowConstruct = getTestFlow();
    final Event event = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
        .message(InternalMessage.of(new Fruit[] {new Apple(), new Orange()}))
        .exchangePattern(REQUEST_RESPONSE)
        .build();
    MuleEventContext eventContext = new DefaultMuleEventContext(flowConstruct, event);
    InvocationResult ctx = resolver.invoke(new FruitBowl(), eventContext, Event.builder(eventContext.getEvent()));
    assertEquals(ctx.getState(), InvocationResult.State.SUCCESSFUL);
  }

  @Test
  public void testArrayMatchGenericFail() throws Exception {
    AbstractArgumentEntryPointResolver resolver = new ArrayEntryPointResolver();
    FlowConstruct flowConstruct = getTestFlow();
    final Event event = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
        .message(InternalMessage.of(new Object[] {new Apple(), new Orange()}))
        .exchangePattern(REQUEST_RESPONSE)
        .build();
    MuleEventContext eventContext = new DefaultMuleEventContext(flowConstruct, event);
    InvocationResult ctx = resolver.invoke(new FruitBowl(), eventContext, Event.builder(eventContext.getEvent()));
    assertEquals(ctx.getState(), InvocationResult.State.FAILED);
  }

  @Test
  public void testArrayMatchFail() throws Exception {
    AbstractArgumentEntryPointResolver resolver = new ArrayEntryPointResolver();
    FlowConstruct flowConstruct = getTestFlow();
    final Event event = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
        .message(InternalMessage.of(new Object[] {"blah"}))
        .exchangePattern(REQUEST_RESPONSE)
        .build();
    MuleEventContext eventContext = new DefaultMuleEventContext(flowConstruct, event);
    InvocationResult ctx = resolver.invoke(new Apple(), eventContext, Event.builder(eventContext.getEvent()));
    assertEquals(ctx.getState(), InvocationResult.State.FAILED);
  }
}
