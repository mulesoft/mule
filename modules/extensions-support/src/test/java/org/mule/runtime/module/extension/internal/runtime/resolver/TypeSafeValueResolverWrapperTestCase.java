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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class TypeSafeValueResolverWrapperTestCase extends AbstractMuleContextTestCase {

  private TransformationService transformationService = mock(TransformationService.class);
  private ValueResolver<String> staticValueResolver = mock(ValueResolver.class);
  private ValueResolver<String> dynamicValueResolver = mock(ValueResolver.class);
  private TypeSafeValueResolverWrapper<Integer> dynamicResolver;
  private TypeSafeValueResolverWrapper<Integer> staticResolver;

  @Override
  protected void doSetUp() throws Exception {
    muleContext = spy(muleContext);
    ExtendedExpressionManager expressionManager = spy(muleContext.getExpressionManager());

    when(staticValueResolver.isDynamic()).thenReturn(false);
    when(staticValueResolver.resolve(any(ValueResolvingContext.class))).thenReturn("123");

    when(dynamicValueResolver.isDynamic()).thenReturn(true);
    when(dynamicValueResolver.resolve(any(ValueResolvingContext.class))).thenReturn("123");

    when(muleContext.getExpressionManager()).thenReturn(expressionManager);

    dynamicResolver = new TypeSafeValueResolverWrapper<>(dynamicValueResolver, Integer.class);
    dynamicResolver.setTransformationService(transformationService);
    dynamicResolver.initialise();
    staticResolver = new TypeSafeValueResolverWrapper<>(staticValueResolver, Integer.class);
    staticResolver.setTransformationService(transformationService);
    staticResolver.initialise();

    when(transformationService.transform(eq("123"), any(DataType.class), any(DataType.class))).thenReturn(123);
  }

  @Test
  public void staticValueIsTransformed() throws MuleException {
    Integer resolve = staticResolver.resolve(ValueResolvingContext.from(testEvent()));
    assertThat(resolve, is(123));
  }

  @Test
  public void staticValueIsTransformedOnlyOnce() throws MuleException {
    staticResolver.resolve(ValueResolvingContext.from(testEvent()));
    staticResolver.resolve(ValueResolvingContext.from(testEvent()));
    staticResolver.resolve(ValueResolvingContext.from(testEvent()));
    staticResolver.resolve(ValueResolvingContext.from(testEvent()));
    staticResolver.resolve(ValueResolvingContext.from(testEvent()));

    verify(staticValueResolver, times(1)).resolve(any(ValueResolvingContext.class));
  }

  @Test
  public void dynamicValueIsTransformed() throws MuleException {
    Integer resolve = dynamicResolver.resolve(ValueResolvingContext.from(testEvent()));
    assertThat(resolve, is(123));
  }

  @Test
  public void dynamicValueIsTransformedOnlyOnce() throws MuleException {
    dynamicResolver.resolve(ValueResolvingContext.from(testEvent()));
    dynamicResolver.resolve(ValueResolvingContext.from(testEvent()));
    dynamicResolver.resolve(ValueResolvingContext.from(testEvent()));
    dynamicResolver.resolve(ValueResolvingContext.from(testEvent()));
    dynamicResolver.resolve(ValueResolvingContext.from(testEvent()));

    verify(dynamicValueResolver, times(5)).resolve(any(ValueResolvingContext.class));
  }

  @Test
  public void transformNullValue() throws MuleException {
    when(staticValueResolver.resolve(any(ValueResolvingContext.class))).thenReturn(null);
    Integer value = staticResolver.resolve(ValueResolvingContext.from(testEvent()));

    assertThat(value, is(nullValue()));
  }
}
