/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.Test;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

@SmallTest
public class TypeSafeValueResolverWrapperTestCase extends AbstractMuleContextTestCase {

  private ValueResolver<String> staticValueResolver = mock(ValueResolver.class);
  private ValueResolver<String> dynamicValueResolver = mock(ValueResolver.class);
  private TypeSafeValueResolverWrapper<Integer> dynamicResolver;
  private TypeSafeValueResolverWrapper<Integer> staticResolver;

  @Override
  protected void doSetUp() throws Exception {
    muleContext = spy(muleContext);
    ExtendedExpressionManager expressionManager = spy(muleContext.getExpressionManager());

    when(staticValueResolver.isDynamic()).thenReturn(false);
    when(staticValueResolver.resolve(any(Event.class))).thenReturn("123");

    when(dynamicValueResolver.isDynamic()).thenReturn(true);
    when(dynamicValueResolver.resolve(any(Event.class))).thenReturn("123");

    when(muleContext.getExpressionManager()).thenReturn(expressionManager);

    dynamicResolver = new TypeSafeValueResolverWrapper<>(dynamicValueResolver, Integer.class, muleContext);
    staticResolver = new TypeSafeValueResolverWrapper<>(staticValueResolver, Integer.class, muleContext);
  }

  @Test
  public void staticValueIsTransformed() throws MuleException {
    Integer resolve = staticResolver.resolve(testEvent());
    assertThat(resolve, is(123));
  }

  @Test
  public void staticValueIsTransformedOnlyOnce() throws MuleException {
    staticResolver.resolve(testEvent());
    staticResolver.resolve(testEvent());
    staticResolver.resolve(testEvent());
    staticResolver.resolve(testEvent());
    staticResolver.resolve(testEvent());

    verify(staticValueResolver, times(1)).resolve(any(Event.class));
  }

  @Test
  public void dynamicValueIsTransformed() throws MuleException {
    Integer resolve = dynamicResolver.resolve(testEvent());
    assertThat(resolve, is(123));
  }

  @Test
  public void dynamicValueIsTransformedOnlyOnce() throws MuleException {
    dynamicResolver.resolve(testEvent());
    dynamicResolver.resolve(testEvent());
    dynamicResolver.resolve(testEvent());
    dynamicResolver.resolve(testEvent());
    dynamicResolver.resolve(testEvent());

    verify(dynamicValueResolver, times(5)).resolve(any(Event.class));
  }

  @Test
  public void transformNullValue() throws MuleException {
    when(staticValueResolver.resolve(any(Event.class))).thenReturn(null);
    Integer value = staticResolver.resolve(testEvent());

    assertThat(value, is(nullValue()));
  }
}
