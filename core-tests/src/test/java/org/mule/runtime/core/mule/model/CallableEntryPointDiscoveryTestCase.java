/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.mule.model;

import static org.junit.Assert.assertEquals;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.DefaultMuleEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.model.InvocationResult;
import org.mule.runtime.core.api.model.resolvers.CallableEntryPointResolver;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.WaterMelon;

import org.junit.Before;
import org.junit.Test;

public class CallableEntryPointDiscoveryTestCase extends AbstractMuleContextTestCase {

  private FlowConstruct flowConstruct;

  @Before
  public void before() throws Exception {
    flowConstruct = getTestFlow(muleContext);
  }

  @Test
  public void testBadMatch() throws Exception {
    CallableEntryPointResolver resolver = new CallableEntryPointResolver();
    final Event event =
        Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR_LOCATION))
            .message(of(new StringBuilder("foo"))).build();
    MuleEventContext eventContext = new DefaultMuleEventContext(flowConstruct, event);
    InvocationResult result = resolver.invoke(new WaterMelon(), eventContext, Event.builder(eventContext.getEvent()));
    assertEquals("Service doesn't implement Callable", result.getState(), InvocationResult.State.NOT_SUPPORTED);
  }

  @Test
  public void testGoodMatch() throws Exception {
    CallableEntryPointResolver resolver = new CallableEntryPointResolver();
    final Apple apple = new Apple();
    apple.setMuleContext(muleContext);
    final Event event = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR_LOCATION))
        .message(of(new StringBuilder("blah")))
        .build();
    MuleEventContext eventContext = new DefaultMuleEventContext(flowConstruct, event);
    InvocationResult result = resolver.invoke(apple, eventContext, Event.builder(eventContext.getEvent()));
    assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
  }
}
