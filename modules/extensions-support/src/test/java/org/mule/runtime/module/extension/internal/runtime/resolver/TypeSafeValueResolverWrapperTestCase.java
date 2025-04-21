/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.tck.junit4.rule.DataWeaveExpressionLanguage.dataWeaveRule;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingContext;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.DataWeaveExpressionLanguage;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@SmallTest
public class TypeSafeValueResolverWrapperTestCase extends AbstractMuleTestCase {

  private final TransformationService transformationService = mock(TransformationService.class);
  private final ValueResolver<String> staticValueResolver = mock(ValueResolver.class);
  private final ValueResolver<String> dynamicValueResolver = mock(ValueResolver.class);
  private final ExpressionManager expressionManager = mock(ExpressionManager.class);
  private TypeSafeValueResolverWrapper<Integer> dynamicResolver;
  private TypeSafeValueResolverWrapper<Integer> staticResolver;

  @Rule
  public DataWeaveExpressionLanguage dw = dataWeaveRule();

  @Before
  public void setUp() throws Exception {
    ExtendedExpressionManager expressionManager = spy(dw.getExpressionManager());

    when(staticValueResolver.isDynamic()).thenReturn(false);
    when(staticValueResolver.resolve(any(ValueResolvingContext.class))).thenReturn("123");

    when(dynamicValueResolver.isDynamic()).thenReturn(true);
    when(dynamicValueResolver.resolve(any(ValueResolvingContext.class))).thenReturn("123");

    dynamicResolver = new TypeSafeValueResolverWrapper<>(dynamicValueResolver, Integer.class);
    dynamicResolver.setTransformationService(transformationService);
    initialiseIfNeeded(dynamicResolver);
    staticResolver = new TypeSafeValueResolverWrapper<>(staticValueResolver, Integer.class);
    staticResolver.setTransformationService(transformationService);
    initialiseIfNeeded(staticResolver);

    when(transformationService.transform(eq("123"), any(DataType.class), any(DataType.class))).thenReturn(123);
  }

  @Test
  public void staticValueIsTransformed() throws MuleException {
    Integer resolve =
        staticResolver.resolve(ValueResolvingContext.builder(testEvent()).withExpressionManager(expressionManager).build());
    assertThat(resolve, is(123));
  }

  @Test
  public void staticValueIsTransformedOnlyOnce() throws MuleException {
    staticResolver.resolve(ValueResolvingContext.builder(testEvent()).withExpressionManager(expressionManager).build());
    staticResolver.resolve(ValueResolvingContext.builder(testEvent()).withExpressionManager(expressionManager).build());
    staticResolver.resolve(ValueResolvingContext.builder(testEvent()).withExpressionManager(expressionManager).build());
    staticResolver.resolve(ValueResolvingContext.builder(testEvent()).withExpressionManager(expressionManager).build());
    staticResolver.resolve(ValueResolvingContext.builder(testEvent()).withExpressionManager(expressionManager).build());

    verify(staticValueResolver, times(1)).resolve(any(ValueResolvingContext.class));
  }

  @Test
  public void dynamicValueIsTransformed() throws MuleException {
    ValueResolvingContext ctx = ValueResolvingContext.builder(testEvent()).withExpressionManager(expressionManager).build();
    Integer resolve = dynamicResolver.resolve(ctx);
    assertThat(resolve, is(123));
  }

  @Test
  public void dynamicValueIsTransformedOnlyOnce() throws MuleException {
    dynamicResolver.resolve(ValueResolvingContext.builder(testEvent()).withExpressionManager(expressionManager).build());
    dynamicResolver.resolve(ValueResolvingContext.builder(testEvent()).withExpressionManager(expressionManager).build());
    dynamicResolver.resolve(ValueResolvingContext.builder(testEvent()).withExpressionManager(expressionManager).build());
    dynamicResolver.resolve(ValueResolvingContext.builder(testEvent()).withExpressionManager(expressionManager).build());
    dynamicResolver.resolve(ValueResolvingContext.builder(testEvent()).withExpressionManager(expressionManager).build());

    verify(dynamicValueResolver, times(5)).resolve(any(ValueResolvingContext.class));
  }

  @Test
  public void transformNullValue() throws MuleException {
    when(staticValueResolver.resolve(any(ValueResolvingContext.class))).thenReturn(null);
    Integer value =
        staticResolver.resolve(ValueResolvingContext.builder(testEvent()).withExpressionManager(expressionManager).build());

    assertThat(value, is(nullValue()));
  }
}
