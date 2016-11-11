/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el;

import static java.util.Optional.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.core.el.v2.MuleExpressionLanguage.ATTRIBUTES;
import static org.mule.runtime.core.el.v2.MuleExpressionLanguage.ERROR;
import static org.mule.runtime.core.el.v2.MuleExpressionLanguage.PAYLOAD;
import static org.mule.runtime.core.el.v2.MuleExpressionLanguage.VARIABLES;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.el.v2.MuleExpressionLanguage;
import org.mule.runtime.core.message.BaseAttributes;
import org.mule.runtime.core.metadata.DefaultTypedValue;
import org.mule.tck.junit4.AbstractMuleTestCase;

import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Expression Language")
@Stories("Support DW")
public class MuleExpressionLanguageTestCase extends AbstractMuleTestCase {

  private MuleExpressionLanguage expressionLanguage = new MuleExpressionLanguage();

  @Test
  public void stringExpression() throws Exception {
    TypedValue result = expressionLanguage.evaluate("\"hey\"", testEvent(), BindingContext.builder().build());
    assertThat(result.getValue(), is("hey"));
    assertThat(result.getDataType(), is(equalTo(STRING)));
  }

  @Test
  public void attributesBinding() throws Exception {
    Event event = getEventWithError(empty());
    Attributes attributes = new SomeAttributes();
    InternalMessage message = (InternalMessage) Message.builder().nullPayload().attributes(attributes).build();
    when(event.getMessage()).thenReturn(message);

    TypedValue result = expressionLanguage.evaluate(ATTRIBUTES, event, BindingContext.builder().build());
    assertThat(result.getValue(), is(equalTo(attributes)));
    assertThat(result.getDataType().getType(), is((equalTo(SomeAttributes.class))));
  }

  @Test
  public void errorBinding() throws Exception {
    Error error = mock(Error.class);
    Optional opt = Optional.of(error);
    Event event = getEventWithError(opt);
    doReturn(testEvent().getMessage()).when(event).getMessage();

    TypedValue result = expressionLanguage.evaluate(ERROR, event, BindingContext.builder().build());
    assertThat(result.getValue(), is(sameInstance(error)));
  }

  @Test
  public void payloadBinding() throws Exception {
    Event event = getEventWithError(empty());
    InternalMessage message = mock(InternalMessage.class, RETURNS_DEEP_STUBS);
    when(event.getMessage()).thenReturn(message);
    TypedValue payload = new DefaultTypedValue("hey", STRING);
    when(message.getPayload()).thenReturn(payload);

    TypedValue result = expressionLanguage.evaluate(PAYLOAD, event, BindingContext.builder().build());
    assertThat(result.getValue(), is(equalTo(payload.getValue())));
    assertThat(result.getDataType(), is(equalTo(payload.getDataType())));
  }

  @Test
  public void variablesBindings() throws Exception {
    Event event = getEventWithError(empty());
    String var1 = "var1";
    String var2 = "var2";
    when(event.getVariableNames()).thenReturn(Sets.newHashSet(var1, var2));
    TypedValue varValue = mock(TypedValue.class);
    when(event.getVariable(var1)).thenReturn(varValue);
    when(event.getVariable(var2)).thenReturn(varValue);

    TypedValue result = expressionLanguage.evaluate(VARIABLES, event, BindingContext.builder().build());
    assertThat(result.getValue(), is(instanceOf(Map.class)));
    assertThat((Map<String, TypedValue>) result.getValue(), hasEntry(var1, varValue));
    assertThat((Map<String, TypedValue>) result.getValue(), hasEntry(var2, varValue));
  }

  @Test
  public void flowNameBinding() {
    Event event = getEventWithError(empty());
    FlowConstruct mockFlowConstruct = mock(FlowConstruct.class);
    String flowName = "myFlowName";
    when(mockFlowConstruct.getName()).thenReturn(flowName);

    TypedValue result = expressionLanguage.evaluate("flow.name", event, mockFlowConstruct, BindingContext.builder().build());
    assertThat(result.getDataType(), is(STRING));
    assertThat(result.getValue(), is(flowName));
  }

  private Event getEventWithError(Optional<Error> error) {
    Event event = mock(Event.class, RETURNS_DEEP_STUBS);
    doReturn(error).when(event).getError();
    return event;
  }

  private class SomeAttributes extends BaseAttributes {

  }
}
