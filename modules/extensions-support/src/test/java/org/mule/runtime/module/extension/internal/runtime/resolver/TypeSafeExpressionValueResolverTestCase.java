/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.toDataType;
import static org.mule.tck.junit4.rule.DataWeaveExpressionLanguage.dataWeaveRule;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;

import static java.lang.Thread.currentThread;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.api.JavaTypeLoader;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.el.ExpressionManagerSession;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingContext;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.DataWeaveExpressionLanguage;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.verification.VerificationMode;

public class TypeSafeExpressionValueResolverTestCase extends AbstractMuleTestCase {

  private static final String HELLO_WORLD = "Hello World!";
  private static final MetadataType STRING = new JavaTypeLoader(currentThread().getContextClassLoader()).load(String.class);

  @Rule
  public DataWeaveExpressionLanguage dw = dataWeaveRule();

  private ExtendedExpressionManager expressionManager;

  @Before
  public void setUp() throws Exception {
    expressionManager = spy(dw.getExpressionManager());
  }

  @Test
  public void expressionLanguageWithoutTransformation() throws Exception {
    ValueResolver<Object> resolver = getResolver("#['Hello ' ++ payload]", STRING);
    ValueResolvingContext ctx = ValueResolvingContext.builder(eventBuilder().message(of("World!")).build())
        .withExpressionManager(expressionManager)
        .build();
    assertResolved(resolver.resolve(ctx), HELLO_WORLD, times(1));
  }

  @Test
  public void expressionTemplateWithoutTransformation() throws Exception {
    ValueResolver<Object> resolver = getResolver("#['Hello $(payload)']", STRING);
    ValueResolvingContext ctx = ValueResolvingContext.builder(eventBuilder().message(of("World!")).build())
        .withExpressionManager(expressionManager)
        .build();
    assertResolved(resolver.resolve(ctx), HELLO_WORLD, times(1));
  }

  @Test
  public void constant() throws Exception {
    ValueResolver<Object> resolver = getResolver("Hello World!", STRING);

    ExpressionManager expressionManager = mock(ExpressionManager.class);
    ExpressionManagerSession session = mock(ExpressionManagerSession.class);
    when(expressionManager.openSession(any(), any(), any())).thenReturn(session);

    ValueResolvingContext ctx = ValueResolvingContext.builder(eventBuilder().message(of(HELLO_WORLD)).build())
        .withExpressionManager(expressionManager)
        .build();

    Object resolvedValue = resolver.resolve(ctx);
    assertResolved(resolvedValue, HELLO_WORLD, never());
    verify(session, never()).evaluate(anyString());
  }

  @Test
  public void expressionWithTransformation() throws Exception {
    ValueResolver<Object> resolver = getResolver("#[true]", STRING);
    ValueResolvingContext ctx = ValueResolvingContext.builder(eventBuilder().message(of(HELLO_WORLD)).build())
        .withExpressionManager(expressionManager)
        .build();
    assertResolved(resolver.resolve(ctx), "true", times(1));
  }

  @Test
  public void templateWithTransformation() throws Exception {
    ValueResolver<Object> resolver = getResolver("#['tru$('e')']", STRING);
    ValueResolvingContext ctx = ValueResolvingContext.builder(eventBuilder().message(of(HELLO_WORLD)).build())
        .withExpressionManager(expressionManager)
        .build();
    assertResolved(resolver.resolve(ctx), "true", times(1));
  }

  @Test
  public void nullExpression() throws Exception {
    var thrown = assertThrows(IllegalArgumentException.class, () -> getResolver(null, STRING));
    assertThat(thrown.getMessage(), containsString("Expression cannot be blank or null"));
  }

  @Test
  public void blankExpression() throws Exception {
    var thrown = assertThrows(IllegalArgumentException.class, () -> getResolver(EMPTY, STRING));
    assertThat(thrown.getMessage(), containsString("Expression cannot be blank or null"));
  }

  @Test
  public void nullExpectedType() throws Exception {
    var thrown = assertThrows(IllegalArgumentException.class, () -> getResolver("#[payload]", null));
    assertThat(thrown.getMessage(), containsString("expected type cannot be null"));
  }

  private void assertResolved(Object resolvedValue, Object expected, VerificationMode expressionManagerVerificationMode) {
    assertThat(resolvedValue, instanceOf(String.class));
    assertThat(resolvedValue, equalTo(expected));
    verify(expressionManager, expressionManagerVerificationMode).openSession(any(BindingContext.class));
  }

  private <T> ValueResolver<T> getResolver(String expression, MetadataType expectedType) throws Exception {
    TypeSafeExpressionValueResolver<T> valueResolver = new TypeSafeExpressionValueResolver(expression,
                                                                                           getType(expectedType).orElse(null),
                                                                                           toDataType(expectedType));
    valueResolver.setExtendedExpressionManager(expressionManager);
    valueResolver.setTransformationService(mock(TransformationService.class));
    valueResolver.initialise();
    return valueResolver;
  }
}
