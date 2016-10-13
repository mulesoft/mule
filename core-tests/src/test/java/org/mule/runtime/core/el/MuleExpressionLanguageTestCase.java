/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
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
  public void eventBindings() throws Exception {
    ExpressionLanguage expressionLanguage = spy(MuleExpressionLanguage.class);
    MuleEvent event = mock(MuleEvent.class);
    String var1 = "flowVar1";
    String var2 = "flowVar2";
    when(event.getVariableNames()).thenReturn(Sets.newHashSet(var1, var2));
    TypedValue varValue = mock(TypedValue.class);
    when(event.getVariable(var1)).thenReturn(varValue);
    when(event.getVariable(var2)).thenReturn(varValue);
    Error error = mock(Error.class);
    Optional opt = Optional.of(error);
    when(event.getError()).thenReturn(opt);
    Message message = mock(Message.class);
    when(event.getMessage()).thenReturn(message);
    Attributes at = mock(Attributes.class);
    when(message.getAttributes()).thenReturn(at);
    TypedValue payload = mock(TypedValue.class);
    when(message.getPayload()).thenReturn(payload);
    ArgumentCaptor<BindingContext> contextCaptor = ArgumentCaptor.forClass(BindingContext.class);
    doReturn(new DefaultTypedValue("ok", DataType.STRING)).when(expressionLanguage).evaluate(anyString(),
                                                                                             contextCaptor.capture());

    expressionLanguage.evaluate("exp", BindingContext.builder().build(), event);
    BindingContext context = contextCaptor.getValue();
    assertThat(context.lookup(ATTRIBUTES).get().getValue(), is(sameInstance(at)));
    assertThat(context.lookup(PAYLOAD).get(), is(sameInstance(payload)));
    assertThat(context.lookup(ERROR).get().getValue(), is(sameInstance(error)));
    Map<String, TypedValue> vars = (Map<String, TypedValue>) context.lookup(VARIABLES).get().getValue();
    assertThat(vars.get(var1), is(sameInstance(varValue)));
    assertThat(vars.get(var2), is(sameInstance(varValue)));
  }

}
