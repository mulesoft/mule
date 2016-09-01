/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.mule.model;

import static org.junit.Assert.assertEquals;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;

import org.mule.runtime.core.api.model.InvocationResult;
import org.mule.runtime.core.model.resolvers.AbstractArgumentEntryPointResolver;
import org.mule.runtime.core.model.resolvers.ArrayEntryPointResolver;
import org.mule.tck.MuleTestUtils;
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
    InvocationResult ctx = resolver
        .invoke(new FruitBowl(),
                MuleTestUtils.getTestEventContext(new Fruit[] {new Apple(), new Orange()}, REQUEST_RESPONSE, muleContext));
    assertEquals(ctx.getState(), InvocationResult.State.SUCCESSFUL);

  }

  @Test
  public void testArrayMatchGenericFail() throws Exception {
    AbstractArgumentEntryPointResolver resolver = new ArrayEntryPointResolver();
    InvocationResult ctx = resolver
        .invoke(new FruitBowl(),
                MuleTestUtils.getTestEventContext(new Object[] {new Apple(), new Orange()}, REQUEST_RESPONSE, muleContext));
    assertEquals(ctx.getState(), InvocationResult.State.FAILED);
  }


  @Test
  public void testArrayMatchFail() throws Exception {
    AbstractArgumentEntryPointResolver resolver = new ArrayEntryPointResolver();
    InvocationResult ctx =
        resolver.invoke(new Apple(), MuleTestUtils.getTestEventContext(new Object[] {"blah"}, REQUEST_RESPONSE, muleContext));
    assertEquals(ctx.getState(), InvocationResult.State.FAILED);
  }
}
