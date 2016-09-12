/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.mule.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.tck.MuleTestUtils.getTestEventContext;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.model.InvocationResult;
import org.mule.runtime.core.model.resolvers.AbstractArgumentEntryPointResolver;
import org.mule.runtime.core.model.resolvers.NoArgumentsEntryPointResolver;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.InvalidSatsuma;

import org.junit.Test;

public class NoArgsEntryPointResolverTestCase extends AbstractMuleContextTestCase {

  @Test
  public void testExplicitMethodMatch() throws Exception {
    AbstractArgumentEntryPointResolver resolver = new NoArgumentsEntryPointResolver();
    resolver.addMethod("bite");
    MuleEventContext eventContext = getTestEventContext("blah", REQUEST_RESPONSE, muleContext);
    InvocationResult result = resolver.invoke(new InvalidSatsuma(), eventContext, Event.builder(eventContext.getEvent()));
    assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
  }

  @Test
  public void testExplicitMethodMatch2() throws Exception {
    AbstractArgumentEntryPointResolver resolver = new NoArgumentsEntryPointResolver();
    resolver.addMethod("wash");
    MuleEventContext eventContext = getTestEventContext("blah", REQUEST_RESPONSE, muleContext);
    InvocationResult result = resolver.invoke(new Apple(), eventContext, Event.builder(eventContext.getEvent()));
    assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
  }

  @Test
  public void testDynamicMethodMatchFail() throws Exception {
    AbstractArgumentEntryPointResolver resolver = new NoArgumentsEntryPointResolver();
    MuleEventContext eventContext = getTestEventContext("blah", REQUEST_RESPONSE, muleContext);
    InvocationResult result = resolver.invoke(new Apple(), eventContext, Event.builder(eventContext.getEvent()));
    assertEquals("Apple service has a number of matching method, so should have failed", result.getState(),
                 InvocationResult.State.FAILED);
  }

  @Test
  public void testDynamicMethodMatchPass() throws Exception {
    AbstractArgumentEntryPointResolver resolver = new NoArgumentsEntryPointResolver();
    MuleEventContext eventContext = getTestEventContext("blah", REQUEST_RESPONSE, muleContext);
    InvocationResult result = resolver.invoke(new InvalidSatsuma(), eventContext, Event.builder(eventContext.getEvent()));
    assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
  }

  @Test
  public void testDynamicMethodMatchFailOnWildcardMatch() throws Exception {
    AbstractArgumentEntryPointResolver resolver = new NoArgumentsEntryPointResolver();
    assertTrue(resolver.removeIgnoredMethod("is*"));
    MuleEventContext eventContext = getTestEventContext("blah", REQUEST_RESPONSE, muleContext);
    InvocationResult result = resolver.invoke(new InvalidSatsuma(), eventContext, Event.builder(eventContext.getEvent()));
    assertEquals("Satsuma service has a number of matching method, so should have failed", result.getState(),
                 InvocationResult.State.FAILED);
  }

  /** Having a null payload should make no difference */
  @Test
  public void testExplicitMethodMatchAndNullPayload() throws Exception {
    AbstractArgumentEntryPointResolver resolver = new NoArgumentsEntryPointResolver();
    resolver.addMethod("wash");
    MuleEventContext eventContext = getTestEventContext(null, REQUEST_RESPONSE, muleContext);
    InvocationResult result = resolver.invoke(new Apple(), eventContext, Event.builder(eventContext.getEvent()));
    assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
  }
}
