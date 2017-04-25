/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.mule.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.DefaultMuleEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.model.InvocationResult;
import org.mule.runtime.core.api.model.resolvers.ExplicitMethodEntryPointResolver;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Fruit;

import org.junit.Before;
import org.junit.Test;

public class ExplicitMethodEntryPointResolverTestCase extends AbstractMuleContextTestCase {

  private FlowConstruct flowConstruct;

  @Before
  public void before() throws Exception {
    flowConstruct = getTestFlow(muleContext);
  }

  @Test
  public void testMethodSetPass() throws Exception {
    ExplicitMethodEntryPointResolver resolver = new ExplicitMethodEntryPointResolver();
    resolver.addMethod("someBusinessMethod");
    final Event event = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR_LOCATION))
        .message(of("blah"))
        .build();
    MuleEventContext eventContext = new DefaultMuleEventContext(flowConstruct, event);
    InvocationResult result =
        resolver.invoke(new MultiplePayloadsTestObject(), eventContext, Event.builder(eventContext.getEvent()));
    assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
  }

  @Test
  public void testMethodSetMatchFirst() throws Exception {
    ExplicitMethodEntryPointResolver resolver = new ExplicitMethodEntryPointResolver();
    resolver.addMethod("someBusinessMethod");
    resolver.addMethod("someSetter");
    final Event event = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR_LOCATION))
        .message(of("blah"))
        .build();
    MuleEventContext eventContext = new DefaultMuleEventContext(flowConstruct, event);
    InvocationResult result =
        resolver.invoke(new MultiplePayloadsTestObject(), eventContext, Event.builder(eventContext.getEvent()));
    assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
  }

  @Test
  public void testMethodNotFound() throws Exception {
    ExplicitMethodEntryPointResolver resolver = new ExplicitMethodEntryPointResolver();
    resolver.addMethod("noMethod");
    resolver.addMethod("noMethod2");
    final Event event =
        Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR_LOCATION)).message(of("blah")).build();
    MuleEventContext eventContext = new DefaultMuleEventContext(flowConstruct, event);
    InvocationResult result =
        resolver.invoke(new MultiplePayloadsTestObject(), eventContext, Event.builder(eventContext.getEvent()));
    assertEquals(result.getState(), InvocationResult.State.FAILED);
  }

  @Test
  public void testNoMethodSet() throws Exception {
    ExplicitMethodEntryPointResolver resolver = new ExplicitMethodEntryPointResolver();
    try {
      final Event event = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR_LOCATION))
          .message(of("blah"))
          .build();
      MuleEventContext eventContext = new DefaultMuleEventContext(flowConstruct, event);
      InvocationResult result =
          resolver.invoke(new MultiplePayloadsTestObject(), eventContext, Event.builder(eventContext.getEvent()));
      fail("method property is not set, this should cause an error");
    } catch (IllegalStateException e) {
      // Expected
    }
  }

  /**
   * If a method with correct name is available then it should be used is the parameter type is assignable from the payload type
   * and not just if there is an exact match. See MULE-3636.
   *
   * @throws Exception
   */
  @Test
  public void testMethodPropertyParameterAssignableFromPayload() throws Exception {
    ExplicitMethodEntryPointResolver resolver = new ExplicitMethodEntryPointResolver();
    resolver.addMethod("wash");
    final Event event = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR_LOCATION))
        .message(of(new Apple()))
        .build();
    MuleEventContext ctx = new DefaultMuleEventContext(flowConstruct, event);
    InvocationResult result = resolver.invoke(new TestFruitCleaner(), ctx, Event.builder(ctx.getEvent()));
    assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
  }

  /**
   * If a method with correct name is available then it should be used even if one or more parameter types in the payload are
   * null, as long as the parameter count matches.
   *
   * @throws Exception
   */
  @Test
  public void testMethodPropertyParameterNull() throws Exception {
    ExplicitMethodEntryPointResolver resolver = new ExplicitMethodEntryPointResolver();
    resolver.addMethod("someOtherBusinessMethod");
    final Event event = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR_LOCATION))
        .message(of(new Object[] {null, "blah"}))
        .build();
    MuleEventContext eventContext = new DefaultMuleEventContext(flowConstruct, event);
    InvocationResult result =
        resolver.invoke(new MultiplePayloadsTestObject(), eventContext, Event.builder(eventContext.getEvent()));
    assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
  }

  public static class TestFruitCleaner {

    public void wash(Fruit fruit) {
      // dummy
    }

    public void polish(Fruit fruit) {
      // dummy
    }
  }
}
