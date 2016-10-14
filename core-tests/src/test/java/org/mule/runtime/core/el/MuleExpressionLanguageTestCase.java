/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el;

import static java.util.Optional.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.el.v2.MuleExpressionLanguage.ATTRIBUTES;
import static org.mule.runtime.core.el.v2.MuleExpressionLanguage.ERROR;
import static org.mule.runtime.core.el.v2.MuleExpressionLanguage.VARIABLES;
import static org.mule.runtime.core.el.v2.MuleExpressionLanguage.PAYLOAD;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.el.v2.MuleExpressionLanguage;
import org.mule.runtime.core.metadata.DefaultTypedValue;
import org.mule.tck.junit4.AbstractMuleTestCase;

import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

public class MuleExpressionLanguageTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void nullExpressionExecutor() throws Exception {
    ExpressionLanguage expressionLanguage = new MuleExpressionLanguage();
    expectedException.expect(NullPointerException.class);
    expressionLanguage.evaluate("hey", BindingContext.builder().build());
  }

  @Test
  public void attributesBinding() throws Exception {
    ExpressionLanguage expressionLanguage = spy(MuleExpressionLanguage.class);
    MuleEvent event = getEventWithError(empty());
    Message message = mock(Message.class);
    when(event.getMessage()).thenReturn(message);
    Attributes at = mock(Attributes.class);
    when(message.getAttributes()).thenReturn(at);

    BindingContext context = getBindingContextFromEvaluation(expressionLanguage, event);
    assertThat(context.lookup(ATTRIBUTES).get().getValue(), is(sameInstance(at)));
  }

  @Test
  public void errorBinding() throws Exception {
    ExpressionLanguage expressionLanguage = spy(MuleExpressionLanguage.class);
    Error error = mock(Error.class);
    Optional opt = Optional.of(error);
    MuleEvent event = getEventWithError(opt);

    BindingContext context = getBindingContextFromEvaluation(expressionLanguage, event);
    assertThat(context.lookup(ERROR).get().getValue(), is(sameInstance(error)));
  }

  @Test
  public void payloadBinding() throws Exception {
    ExpressionLanguage expressionLanguage = spy(MuleExpressionLanguage.class);
    MuleEvent event = getEventWithError(empty());
    Message message = mock(Message.class, RETURNS_DEEP_STUBS);
    when(event.getMessage()).thenReturn(message);
    TypedValue payload = mock(TypedValue.class);
    when(message.getPayload()).thenReturn(payload);

    BindingContext context = getBindingContextFromEvaluation(expressionLanguage, event);
    assertThat(context.lookup(PAYLOAD).get(), is(sameInstance(payload)));
  }

  @Test
  public void variablesBindings() throws Exception {
    ExpressionLanguage expressionLanguage = spy(MuleExpressionLanguage.class);
    MuleEvent event = getEventWithError(empty());
    String var1 = "var1";
    String var2 = "var2";
    when(event.getVariableNames()).thenReturn(Sets.newHashSet(var1, var2));
    TypedValue varValue = mock(TypedValue.class);
    when(event.getVariable(var1)).thenReturn(varValue);
    when(event.getVariable(var2)).thenReturn(varValue);

    BindingContext context = getBindingContextFromEvaluation(expressionLanguage, event);
    Map<String, TypedValue> vars = (Map<String, TypedValue>) context.lookup(VARIABLES).get().getValue();
    assertThat(vars.get(var1), is(sameInstance(varValue)));
    assertThat(vars.get(var2), is(sameInstance(varValue)));
  }

  private MuleEvent getEventWithError(Optional<Error> error) {
    MuleEvent event = mock(MuleEvent.class, RETURNS_DEEP_STUBS);
    when(event.getError()).thenReturn(error);
    return event;
  }

  private BindingContext getBindingContextFromEvaluation(ExpressionLanguage expressionLanguage, MuleEvent event) {
    ArgumentCaptor<BindingContext> contextCaptor = ArgumentCaptor.forClass(BindingContext.class);
    doReturn(new DefaultTypedValue("ok", DataType.STRING)).when(expressionLanguage).evaluate(anyString(),
                                                                                             contextCaptor.capture());

    expressionLanguage.evaluate("exp", BindingContext.builder().build(), event);
    return contextCaptor.getValue();
  }

}
