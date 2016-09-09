/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.mule.model;

import static org.junit.Assert.assertEquals;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.tck.MuleTestUtils.getTestEventContext;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.model.InvocationResult;
import org.mule.runtime.core.model.resolvers.CallableEntryPointResolver;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.WaterMelon;

import org.junit.Test;

public class CallableEntryPointDiscoveryTestCase extends AbstractMuleContextTestCase {

  @Test
  public void testBadMatch() throws Exception {
    CallableEntryPointResolver resolver = new CallableEntryPointResolver();
    MuleEventContext eventContext = getTestEventContext(new StringBuilder("foo"), REQUEST_RESPONSE, muleContext);
    InvocationResult result = resolver.invoke(new WaterMelon(), eventContext, Event.builder(eventContext.getEvent()));
    assertEquals("Service doesn't implement Callable", result.getState(), InvocationResult.State.NOT_SUPPORTED);
  }

  @Test
  public void testGoodMatch() throws Exception {
    CallableEntryPointResolver resolver = new CallableEntryPointResolver();
    final Apple apple = new Apple();
    apple.setMuleContext(muleContext);
    MuleEventContext eventContext = getTestEventContext(new StringBuilder("blah"), REQUEST_RESPONSE, muleContext);
    InvocationResult result = resolver.invoke(apple, eventContext, Event.builder(eventContext.getEvent()));
    assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
  }
}
