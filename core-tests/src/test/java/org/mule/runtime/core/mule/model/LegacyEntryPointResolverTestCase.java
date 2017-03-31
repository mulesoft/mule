/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.mule.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.Event.getCurrentEvent;
import static org.mule.runtime.core.api.Event.setCurrentEvent;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.DefaultMuleEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.model.EntryPointResolverSet;
import org.mule.runtime.core.api.model.resolvers.ArrayEntryPointResolver;
import org.mule.runtime.core.api.model.resolvers.EntryPointNotFoundException;
import org.mule.runtime.core.api.model.resolvers.LegacyEntryPointResolverSet;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.FruitCleaner;
import org.mule.tck.testmodels.fruit.FruitLover;
import org.mule.tck.testmodels.fruit.Kiwi;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.fruit.WaterMelon;

import java.util.Arrays;

import org.junit.Test;

public class LegacyEntryPointResolverTestCase extends AbstractMuleContextTestCase {

  /** Name of the method override property on the event. */
  private static final String METHOD_PROPERTY_NAME = MuleProperties.MULE_METHOD_PROPERTY;

  /** Name of the non-existent method. */
  private static final String INVALID_METHOD_NAME = "nosuchmethod";

  @Test
  public void testExplicitMethodMatch() throws Exception {
    try {
      LegacyEntryPointResolverSet resolver = new LegacyEntryPointResolverSet();
      FlowConstruct flowConstruct = getTestFlow(muleContext);
      final Event event = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
          .message(of("blah"))
          .build();
      MuleEventContext eventContext = new DefaultMuleEventContext(flowConstruct, event);
      resolver.invoke(new WaterMelon(), eventContext, Event.builder(eventContext.getEvent()));
    } catch (MuleException e) {
      fail("Test should have passed: " + e);
    }
  }

  @Test
  public void testExplicitMethodMatchComplexObject() throws Exception {
    try {
      LegacyEntryPointResolverSet resolver = new LegacyEntryPointResolverSet();
      FlowConstruct flowConstruct = getTestFlow(muleContext);
      final Event event = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
          .message(of(new FruitLover("Mmmm")))
          .build();
      MuleEventContext eventContext = new DefaultMuleEventContext(flowConstruct, event);
      resolver.invoke(new FruitBowl(), eventContext, Event.builder(eventContext.getEvent()));
    } catch (MuleException e) {
      fail("Test should have passed: " + e);
    }
  }

  @Test
  public void testExplicitMethodMatchSetArrayFail() throws Exception {
    try {
      LegacyEntryPointResolverSet resolver = new LegacyEntryPointResolverSet();
      FlowConstruct flowConstruct = getTestFlow(muleContext);
      final Event event = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
          .message(of(new Fruit[] {new Apple(), new Orange()}))
          .build();
      MuleEventContext eventContext = new DefaultMuleEventContext(flowConstruct, event);
      resolver.invoke(new FruitBowl(), eventContext, Event.builder(eventContext.getEvent()));
      fail("Test should have failed because the arguments were not wrapped properly: ");
    } catch (MuleException e) {
      // expected
    }
  }

  @Test
  public void testExplicitMethodMatchSetArrayPass() throws Exception {
    try {
      LegacyEntryPointResolverSet resolver = new LegacyEntryPointResolverSet();
      FlowConstruct flowConstruct = getTestFlow(muleContext);
      final Event event = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
          .message(of(new Object[] {new Fruit[] {new Apple(), new Orange()}}))
          .build();
      MuleEventContext eventContext =
          new DefaultMuleEventContext(flowConstruct, event);
      resolver.invoke(new FruitBowl(), eventContext, Event.builder(eventContext.getEvent()));
    } catch (MuleException e) {
      fail("Test should have passed: " + e);
    }
  }

  /*
   * this tests the same as above except it uses the {@link ArrayEntryPointResolver} and does not wrap the args with an array
   */
  @Test
  public void testExplicitMethodMatchSetArrayPassUsingExplicitResolver() throws Exception {
    try {
      LegacyEntryPointResolverSet resolver = new LegacyEntryPointResolverSet();
      resolver.addEntryPointResolver(new ArrayEntryPointResolver());
      FlowConstruct flowConstruct = getTestFlow(muleContext);
      final Event event = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
          .message(of(new Fruit[] {new Apple(), new Orange()}))
          .build();
      MuleEventContext eventContext = new DefaultMuleEventContext(flowConstruct, event);
      resolver.invoke(new FruitBowl(), eventContext, Event.builder(eventContext.getEvent()));
    } catch (MuleException e) {
      fail("Test should have passed: " + e);
    }
  }

  /**
   * Tests entrypoint discovery when there is more than one discoverable method with MuleEventContext parameter.
   */
  @Test
  public void testFailEntryPointMultiplePayloadMatches() throws Exception {
    EntryPointResolverSet resolverSet = new LegacyEntryPointResolverSet();

    try {
      FlowConstruct flowConstruct = MuleTestUtils.getTestFlow(muleContext);
      setCurrentEvent(Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
          .message(of("Hello"))
          .build());
      MuleEventContext eventContext = new DefaultMuleEventContext(getTestFlow(muleContext), getCurrentEvent());
      resolverSet.invoke(new MultiplePayloadsTestObject(), eventContext, Event.builder(getCurrentEvent()));
      fail("Should have failed to find entrypoint.");
    } catch (EntryPointNotFoundException itex) {
      // expected
    }
  }

  /**
   * If there was a method parameter specified to override the discovery mechanism and no such method exists, an exception should
   * be thrown, and no fallback to the default discovery should take place.
   */
  @Test
  public void testMethodOverrideDoesNotFallback() throws Exception {
    EntryPointResolverSet resolverSet = new LegacyEntryPointResolverSet();
    FlowConstruct flowConstruct = MuleTestUtils.getTestFlow(muleContext);
    setCurrentEvent(Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
        .message(of(new FruitLover("Yummy!")))
        .build());

    // those are usually set on the endpoint and copied over to the message
    final String methodName = "nosuchmethod";
    final String propertyName = MuleProperties.MULE_METHOD_PROPERTY;

    Event event = getCurrentEvent();
    event = Event.builder(event)
        .message(InternalMessage.builder(event.getMessage()).addOutboundProperty(propertyName, methodName).build()).build();

    MuleEventContext eventContext = new DefaultMuleEventContext(getTestFlow(muleContext), event);
    resolverSet.invoke(new FruitBowl(), eventContext, Event.builder(event));
    // fail("Should have failed to find an entrypoint.");
  }

  /**
   * If there was a method parameter specified to override the discovery mechanism and a Callable instance is serving the request,
   * call the Callable, ignore the method override parameter.
   */
  @Test
  public void testMethodOverrideIgnoredWithCallable() throws Exception {
    EntryPointResolverSet resolver = new LegacyEntryPointResolverSet();
    FlowConstruct flowConstruct = MuleTestUtils.getTestFlow(muleContext);

    setCurrentEvent(Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
        .message(of(new FruitLover("Yummy!")))
        .build());

    // those are usually set on the endpoint and copied over to the message
    Event event = getCurrentEvent();
    event = Event.builder(event)
        .message(InternalMessage.builder(event.getMessage()).addOutboundProperty(METHOD_PROPERTY_NAME, INVALID_METHOD_NAME)
            .build())
        .build();

    Apple apple = new Apple();
    apple.setAppleCleaner(new FruitCleaner() {

      @Override
      public void wash(Fruit fruit) {
        // dummy
      }

      @Override
      public void polish(Fruit fruit) {
        // dummy
      }
    });
    apple.setMuleContext(muleContext);
    MuleEventContext eventContext = new DefaultMuleEventContext(getTestFlow(muleContext), event);
    resolver.invoke(apple, eventContext, Event.builder(event));
  }

  /**
   * If there was a method parameter specified to override the discovery mechanism and a target instance has a method accepting
   * MuleEventContext, proceed to call this method, ignore the method override parameter.
   */
  @Test
  public void testMethodOverrideIgnoredWithEventContext() throws Exception {
    EntryPointResolverSet resolverSet = new LegacyEntryPointResolverSet();
    FlowConstruct flowConstruct = MuleTestUtils.getTestFlow(muleContext);

    setCurrentEvent(Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
        .message(of(new FruitLover("Yummy!")))
        .build());

    // those are usually set on the endpoint and copied over to the message
    final String methodName = "nosuchmethod";
    final String propertyName = MuleProperties.MULE_METHOD_PROPERTY;
    Event event = getCurrentEvent();
    event = Event.builder(event)
        .message(InternalMessage.builder(event.getMessage()).addOutboundProperty(propertyName, methodName).build()).build();

    try {
      MuleEventContext eventContext = new DefaultMuleEventContext(getTestFlow(muleContext), event);
      resolverSet.invoke(new Kiwi(), eventContext, Event.builder(event));
      fail("no such method on service");
    } catch (EntryPointNotFoundException e) {
      // expected
    }
  }

  /** Test for proper resolution of a method that takes an array as argument. */
  // TODO MULE-1088: currently fails, therefore disabled
  @Test
  public void testArrayArgumentResolution() throws Exception {
    EntryPointResolverSet resolverSet = new LegacyEntryPointResolverSet();

    Object payload = new Object[] {new Fruit[] {new Apple(), new Banana()}};
    FlowConstruct flowConstruct = MuleTestUtils.getTestFlow(muleContext);
    Event event = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR)).message(of(payload)).build();
    setCurrentEvent(event);

    FruitBowl bowl = new FruitBowl();
    assertFalse(bowl.hasApple());
    assertFalse(bowl.hasBanana());

    MuleEventContext eventContext = new DefaultMuleEventContext(getTestFlow(muleContext), event);
    resolverSet.invoke(bowl, eventContext, Event.builder(event));

    assertTrue(bowl.hasApple());
    assertTrue(bowl.hasBanana());
  }

  /** Test for proper resolution of a method that takes a List as argument. */
  @Test
  public void testListArgumentResolution() throws Exception {
    EntryPointResolverSet resolverSet = new LegacyEntryPointResolverSet();
    Object payload = Arrays.asList(new Fruit[] {new Apple(), new Banana()});
    FlowConstruct flowConstruct = MuleTestUtils.getTestFlow(muleContext);
    Event event = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
        .message(of(payload))
        .build();
    setCurrentEvent(event);

    FruitBowl bowl = new FruitBowl();
    assertFalse(bowl.hasApple());
    assertFalse(bowl.hasBanana());

    MuleEventContext eventContext = new DefaultMuleEventContext(getTestFlow(muleContext), event);
    resolverSet.invoke(bowl, eventContext, Event.builder(event));

    assertTrue(bowl.hasApple());
    assertTrue(bowl.hasBanana());
  }

  /** Test for proper resolution of an existing method specified as override */
  @Test
  public void testExplicitOverride() throws Exception {
    EntryPointResolverSet resolverSet = new LegacyEntryPointResolverSet();

    Object payload = Arrays.asList(new Fruit[] {new Apple(), new Banana()});
    FlowConstruct flowConstruct = MuleTestUtils.getTestFlow(muleContext);
    Event event = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
        .message(of(payload))
        .build();
    setCurrentEvent(event);

    final String methodName = "setFruit";
    final String propertyName = MuleProperties.MULE_METHOD_PROPERTY;
    event = Event.builder(event)
        .message(InternalMessage.builder(event.getMessage()).addOutboundProperty(propertyName, methodName).build()).build();

    FruitBowl bowl = new FruitBowl();
    assertFalse(bowl.hasApple());
    assertFalse(bowl.hasBanana());

    MuleEventContext eventContext = new DefaultMuleEventContext(getTestFlow(muleContext), event);
    resolverSet.invoke(bowl, eventContext, Event.builder(event));

    assertTrue(bowl.hasApple());
    assertTrue(bowl.hasBanana());
  }
}
