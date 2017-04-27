/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.mule.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.Event.getCurrentEvent;
import static org.mule.runtime.core.api.Event.setCurrentEvent;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.DefaultMuleEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.model.InvocationResult;
import org.mule.runtime.core.api.model.resolvers.ReflectionEntryPointResolver;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.FruitLover;
import org.mule.tck.testmodels.fruit.Kiwi;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.fruit.WaterMelon;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.mockito.cglib.proxy.Enhancer;
import org.mockito.cglib.proxy.MethodInterceptor;
import org.mockito.cglib.proxy.MethodProxy;

public class ReflectionEntryPointResolverTestCase extends AbstractMuleContextTestCase {

  private FlowConstruct flowConstruct;

  @Before
  public void before() throws Exception {
    flowConstruct = getTestFlow(muleContext);
  }

  @Test
  public void testExplicitMethodMatch() throws Exception {
    ReflectionEntryPointResolver resolver = new ReflectionEntryPointResolver();
    final Event event = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR_LOCATION))
        .message(of("blah"))
        .build();
    MuleEventContext eventContext = new DefaultMuleEventContext(flowConstruct, event);
    InvocationResult result = resolver.invoke(new WaterMelon(), eventContext, Event.builder(eventContext.getEvent()));
    assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
  }

  @Test
  public void testExplicitMethodMatchComplexObject() throws Exception {
    ReflectionEntryPointResolver resolver = new ReflectionEntryPointResolver();
    final Event event = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR_LOCATION))
        .message(of(new FruitLover("Mmmm")))
        .build();
    MuleEventContext eventContext = new DefaultMuleEventContext(flowConstruct, event);
    InvocationResult result = resolver.invoke(new FruitBowl(), eventContext, Event.builder(eventContext.getEvent()));
    assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
  }

  @Test
  public void testMethodMatchWithArguments() throws Exception {
    ReflectionEntryPointResolver resolver = new ReflectionEntryPointResolver();
    MuleEventContext eventContext =
        new DefaultMuleEventContext(flowConstruct,
                                    Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR_LOCATION))
                                        .message(of(new Object[] {new Apple(), new Banana()}))
                                        .build());
    InvocationResult result = resolver.invoke(new FruitBowl(), eventContext, Event.builder(eventContext.getEvent()));
    assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
    assertTrue(result.getResult() instanceof Fruit[]);
    // test that the correct methd was called
    assertTrue(((Fruit[]) result.getResult())[0] instanceof Apple);
    assertTrue(((Fruit[]) result.getResult())[1] instanceof Banana);
    assertEquals("addAppleAndBanana", result.getMethodCalled());
    eventContext =
        new DefaultMuleEventContext(flowConstruct,
                                    Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR_LOCATION))
                                        .message(of(new Object[] {new Banana(), new Apple()}))
                                        .build());
    result = resolver.invoke(new FruitBowl(), eventContext, Event.builder(eventContext.getEvent()));
    assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
    assertTrue(result.getResult() instanceof Fruit[]);
    assertTrue(((Fruit[]) result.getResult())[0] instanceof Banana);
    assertTrue(((Fruit[]) result.getResult())[1] instanceof Apple);
    assertEquals("addBananaAndApple", result.getMethodCalled());
  }

  @Test
  public void testExplicitMethodMatchSetArrayFail() throws Exception {
    ReflectionEntryPointResolver resolver = new ReflectionEntryPointResolver();
    final Event event = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR_LOCATION))
        .message(of(new Fruit[] {new Apple(), new Orange()}))
        .build();
    MuleEventContext eventContext = new DefaultMuleEventContext(flowConstruct, event);
    InvocationResult result = resolver.invoke(new FruitBowl(), eventContext, Event.builder(eventContext.getEvent()));
    assertEquals("Test should have failed because the arguments were not wrapped properly: ",
                 result.getState(), InvocationResult.State.FAILED);
  }

  @Test
  public void testExplicitMethodMatchSetArrayPass() throws Exception {
    ReflectionEntryPointResolver resolver = new ReflectionEntryPointResolver();
    final Event event = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR_LOCATION))
        .message(of(new Object[] {new Fruit[] {new Apple(), new Orange()}}))
        .build();
    MuleEventContext eventContext =
        new DefaultMuleEventContext(flowConstruct, event);
    InvocationResult result = resolver.invoke(new FruitBowl(), eventContext, Event.builder(eventContext.getEvent()));
    assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
  }

  /**
   * Tests entrypoint discovery when there is more than one discoverable method with MuleEventContext parameter.
   */
  @Test
  public void testFailEntryPointMultiplePayloadMatches() throws Exception {
    ReflectionEntryPointResolver resolver = new ReflectionEntryPointResolver();
    final Event testEvent = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR_LOCATION))
        .message(of("Hello")).build();
    setCurrentEvent(testEvent);
    MuleEventContext eventContext = new DefaultMuleEventContext(flowConstruct, getCurrentEvent());
    InvocationResult result =
        resolver.invoke(new MultiplePayloadsTestObject(), eventContext, Event.builder(getCurrentEvent()));
    assertEquals(result.getState(), InvocationResult.State.FAILED);
  }

  @Test
  public void testMatchOnNoArgs() throws Exception {
    ReflectionEntryPointResolver resolver = new ReflectionEntryPointResolver();
    final Event event =
        Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR_LOCATION)).message(of(null)).build();
    // This should fail because the Kiwi.bite() method has a void return type, and by default
    // void methods are ignorred
    MuleEventContext eventContext = new DefaultMuleEventContext(flowConstruct, event);
    InvocationResult result = resolver.invoke(new Kiwi(), eventContext, Event.builder(eventContext.getEvent()));
    assertEquals(result.getState(), InvocationResult.State.FAILED);

    resolver.setAcceptVoidMethods(true);
    result = resolver.invoke(new Kiwi(), eventContext, Event.builder(eventContext.getEvent()));
    assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
    assertEquals("bite", result.getMethodCalled());
  }

  @Test
  public void testAnnotatedMethodOnProxyWithMethodSet() throws Exception {
    ReflectionEntryPointResolver resolver = new ReflectionEntryPointResolver();

    Enhancer e = new Enhancer();
    e.setSuperclass(WaterMelon.class);
    e.setCallback(new DummyMethodCallback());
    Object proxy = e.create();
    final Event event =
        Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR_LOCATION)).message(of("Blah")).build();

    MuleEventContext context = new DefaultMuleEventContext(flowConstruct, event);
    InvocationResult result = resolver.invoke(proxy, context, Event.builder(context.getEvent()));
    assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
  }

  private class DummyMethodCallback implements MethodInterceptor {

    public DummyMethodCallback() {
      super();
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
      System.out.println("before: " + method.getName());
      Object r = proxy.invokeSuper(obj, args);
      System.out.println("after: " + method.getName());

      // Add handler code here
      return r;
    }
  }
}
