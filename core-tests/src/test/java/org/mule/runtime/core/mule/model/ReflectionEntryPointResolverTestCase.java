/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.mule.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.core.DefaultMuleEvent.getCurrentEvent;
import static org.mule.runtime.core.DefaultMuleEvent.setCurrentEvent;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;

import org.mule.runtime.core.DefaultMuleEventContext;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.model.InvocationResult;
import org.mule.runtime.core.model.resolvers.ReflectionEntryPointResolver;
import org.mule.tck.MuleTestUtils;
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

import org.junit.Test;
import org.mockito.cglib.proxy.Enhancer;
import org.mockito.cglib.proxy.MethodInterceptor;
import org.mockito.cglib.proxy.MethodProxy;

public class ReflectionEntryPointResolverTestCase extends AbstractMuleContextTestCase {

  @Test
  public void testExplicitMethodMatch() throws Exception {
    ReflectionEntryPointResolver resolver = new ReflectionEntryPointResolver();
    InvocationResult result =
        resolver.invoke(new WaterMelon(), MuleTestUtils.getTestEventContext("blah", REQUEST_RESPONSE, muleContext));
    assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
  }

  @Test
  public void testExplicitMethodMatchComplexObject() throws Exception {
    ReflectionEntryPointResolver resolver = new ReflectionEntryPointResolver();
    InvocationResult result = resolver
        .invoke(new FruitBowl(), MuleTestUtils.getTestEventContext(new FruitLover("Mmmm"), REQUEST_RESPONSE, muleContext));
    assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
  }

  @Test
  public void testMethodMatchWithArguments() throws Exception {
    ReflectionEntryPointResolver resolver = new ReflectionEntryPointResolver();
    InvocationResult result = resolver
        .invoke(new FruitBowl(),
                MuleTestUtils.getTestEventContext(new Object[] {new Apple(), new Banana()}, REQUEST_RESPONSE, muleContext));
    assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
    assertTrue(result.getResult() instanceof Fruit[]);
    // test that the correct methd was called
    assertTrue(((Fruit[]) result.getResult())[0] instanceof Apple);
    assertTrue(((Fruit[]) result.getResult())[1] instanceof Banana);
    assertEquals("addAppleAndBanana", result.getMethodCalled());

    result = resolver
        .invoke(new FruitBowl(),
                MuleTestUtils.getTestEventContext(new Object[] {new Banana(), new Apple()}, REQUEST_RESPONSE, muleContext));
    assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
    assertTrue(result.getResult() instanceof Fruit[]);
    assertTrue(((Fruit[]) result.getResult())[0] instanceof Banana);
    assertTrue(((Fruit[]) result.getResult())[1] instanceof Apple);
    assertEquals("addBananaAndApple", result.getMethodCalled());
  }

  @Test
  public void testExplicitMethodMatchSetArrayFail() throws Exception {
    ReflectionEntryPointResolver resolver = new ReflectionEntryPointResolver();
    InvocationResult result = resolver
        .invoke(new FruitBowl(),
                MuleTestUtils.getTestEventContext(new Fruit[] {new Apple(), new Orange()}, REQUEST_RESPONSE, muleContext));
    assertEquals("Test should have failed because the arguments were not wrapped properly: ",
                 result.getState(), InvocationResult.State.FAILED);
  }

  @Test
  public void testExplicitMethodMatchSetArrayPass() throws Exception {
    ReflectionEntryPointResolver resolver = new ReflectionEntryPointResolver();
    InvocationResult result =
        resolver.invoke(new FruitBowl(), MuleTestUtils.getTestEventContext(new Object[] {new Fruit[] {new Apple(), new Orange()}},
                                                                           REQUEST_RESPONSE, muleContext));
    assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
  }

  /**
   * Tests entrypoint discovery when there is more than one discoverable method with MuleEventContext parameter.
   */
  @Test
  public void testFailEntryPointMultiplePayloadMatches() throws Exception {
    ReflectionEntryPointResolver resolver = new ReflectionEntryPointResolver();
    setCurrentEvent(getTestEvent("Hello"));
    InvocationResult result =
        resolver.invoke(new MultiplePayloadsTestObject(), new DefaultMuleEventContext(getTestFlow(), getCurrentEvent()));
    assertEquals(result.getState(), InvocationResult.State.FAILED);
  }

  @Test
  public void testMatchOnNoArgs() throws Exception {
    ReflectionEntryPointResolver resolver = new ReflectionEntryPointResolver();
    // This should fail because the Kiwi.bite() method has a void return type, and by default
    // void methods are ignorred
    InvocationResult result = resolver.invoke(new Kiwi(), MuleTestUtils.getTestEventContext(null, REQUEST_RESPONSE, muleContext));
    assertEquals(result.getState(), InvocationResult.State.FAILED);

    resolver.setAcceptVoidMethods(true);
    result = resolver.invoke(new Kiwi(), MuleTestUtils.getTestEventContext(null, REQUEST_RESPONSE, muleContext));
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

    MuleEventContext context = MuleTestUtils.getTestEventContext("Blah", REQUEST_RESPONSE, muleContext);
    InvocationResult result = resolver.invoke(proxy, context);
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
