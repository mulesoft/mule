/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.mule.model;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import org.mule.runtime.core.DefaultMuleEventContext;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.model.InvocationResult;
import org.mule.runtime.core.model.resolvers.MethodHeaderPropertyEntryPointResolver;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Fruit;

import java.io.Serializable;
import java.util.Map;

import org.junit.Test;

public class MethodHeaderEntryPointResolverTestCase extends AbstractMuleContextTestCase {

  private MethodHeaderPropertyEntryPointResolver resolver;

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    resolver = new MethodHeaderPropertyEntryPointResolver();
  }

  @Test
  public void testMethodSetPass() throws Exception {
    MuleEventContext ctx = createMuleEventContext("blah", singletonMap("method", "someBusinessMethod"));

    InvocationResult result = resolver.invoke(new MultiplePayloadsTestObject(), ctx);
    assertInvocationWasSuccessful(result);
  }

  @Test
  public void testMethodSetWithNoArgsPass() throws Exception {
    MuleEventContext ctx = createMuleEventContext(null, singletonMap("method", "wash"));

    InvocationResult result = resolver.invoke(new Apple(), ctx);
    assertInvocationWasSuccessful(result);
    assertEquals("wash", result.getMethodCalled());
  }

  @Test
  public void testCustomMethodProperty() throws Exception {
    resolver.setMethodProperty("serviceMethod");

    MuleEventContext ctx = createMuleEventContext("blah", singletonMap("serviceMethod", "someBusinessMethod"));

    InvocationResult result = resolver.invoke(new MultiplePayloadsTestObject(), ctx);
    assertInvocationWasSuccessful(result);
  }

  @Test
  public void testCustomMethodPropertyFail() throws Exception {
    resolver.setMethodProperty("serviceMethod");

    MuleEventContext ctx = createMuleEventContext("blah", singletonMap("serviceMethod", "noMethod"));

    InvocationResult result = resolver.invoke(new MultiplePayloadsTestObject(), ctx);
    assertInvocationFailed(result);
  }

  @Test
  public void testMethodPropertyFail() throws Exception {
    resolver.setMethodProperty("serviceMethod");

    MuleEventContext ctx = createMuleEventContext("blah", singletonMap("myMethod", "someBusinessMethod"));

    InvocationResult result = resolver.invoke(new MultiplePayloadsTestObject(), ctx);
    assertInvocationFailed(result);
  }

  @Test
  public void testMethodPropertyMismatch() throws Exception {
    MuleEventContext ctx = createMuleEventContext("blah", singletonMap("method", "noMethod"));

    InvocationResult result = resolver.invoke(new MultiplePayloadsTestObject(), ctx);
    assertInvocationFailed(result);
  }

  /**
   * If a method with correct name is available then it should be used if the parameter type is assignable from the payload type
   * and not just if there is an exact match. See MULE-3636.
   */
  @Test
  public void testMethodPropertyParameterAssignableFromPayload() throws Exception {
    MuleEventContext ctx = createMuleEventContext(new Apple(), singletonMap("method", "wash"));

    InvocationResult result = resolver.invoke(new TestFruitCleaner(), ctx);
    assertInvocationWasSuccessful(result);
  }

  private void assertInvocationWasSuccessful(InvocationResult result) {
    assertEquals(InvocationResult.State.SUCCESSFUL, result.getState());
  }

  private void assertInvocationFailed(InvocationResult result) {
    assertEquals(InvocationResult.State.FAILED, result.getState());
  }

  private MuleEventContext createMuleEventContext(Object payload, Map<String, Serializable> inboundProperties) throws Exception {
    return new DefaultMuleEventContext(getTestEvent(MuleMessage.builder().payload(payload).inboundProperties(inboundProperties)
        .build()));
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
