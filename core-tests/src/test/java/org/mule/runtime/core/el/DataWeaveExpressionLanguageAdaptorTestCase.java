/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static org.apache.commons.lang3.SystemUtils.FILE_SEPARATOR;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.metadata.DataType.BOOLEAN;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.api.metadata.DataType.fromType;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON;
import static org.mule.runtime.core.el.DataWeaveExpressionLanguageAdaptor.FLOW;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static org.mule.runtime.internal.el.BindingContextUtils.ATTRIBUTES;
import static org.mule.runtime.internal.el.BindingContextUtils.AUTHENTICATION;
import static org.mule.runtime.internal.el.BindingContextUtils.DATA_TYPE;
import static org.mule.runtime.internal.el.BindingContextUtils.ERROR;
import static org.mule.runtime.internal.el.BindingContextUtils.PARAMETERS;
import static org.mule.runtime.internal.el.BindingContextUtils.PAYLOAD;
import static org.mule.runtime.internal.el.BindingContextUtils.PROPERTIES;
import static org.mule.runtime.internal.el.BindingContextUtils.VARS;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.EXPRESSION_LANGUAGE;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.ExpressionLanguageStory.SUPPORT_DW;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleManifest;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.message.BaseAttributes;
import org.mule.runtime.core.api.message.ErrorBuilder;
import org.mule.runtime.core.api.security.DefaultMuleAuthentication;
import org.mule.runtime.core.api.security.DefaultMuleCredentials;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.internal.security.DefaultSecurityContext;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(EXPRESSION_LANGUAGE)
@Story(SUPPORT_DW)
public class DataWeaveExpressionLanguageAdaptorTestCase extends AbstractWeaveExpressionLanguageTestCase {

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();

  @Test
  public void stringExpression() throws Exception {
    TypedValue result = expressionLanguage.evaluate("\"hey\"", testEvent(), BindingContext.builder().build());
    assertThat(result.getValue(), is("hey"));
    assertThat(result.getDataType(), is(equalTo(STRING)));
  }

  @Test
  public void withPrefixExpression() throws Exception {
    TypedValue result = expressionLanguage.evaluate("#[dw:\"hey\"]", testEvent(), BindingContext.builder().build());
    assertThat(result.getValue(), is("hey"));
    assertThat(result.getDataType(), is(equalTo(STRING)));
  }

  @Test
  public void attributesBinding() throws Exception {
    Event event = getEventWithError(empty());
    SomeAttributes attributes = new SomeAttributes();
    InternalMessage message = (InternalMessage) Message.builder().nullValue().attributesValue(attributes).build();
    when(event.getMessage()).thenReturn(message);

    TypedValue result = expressionLanguage.evaluate(ATTRIBUTES, event, BindingContext.builder().build());
    assertThat(result.getValue(), is(equalTo(attributes)));
    assertThat(result.getDataType().getType(), is((equalTo(SomeAttributes.class))));
  }

  @Test
  public void splitByJson() throws Exception {
    Event jsonMessage = eventBuilder().message(Message.builder().value("[1,2,3]").mediaType(APPLICATION_JSON).build()).build();
    Iterator<TypedValue<?>> payload = expressionLanguage.split("payload", jsonMessage, BindingContext.builder().build());
    assertThat(payload.hasNext(), is(true));
    assertThat(payload.next().getValue().toString(), is("1"));
    assertThat(payload.hasNext(), is(true));
    assertThat(payload.next().getValue().toString(), is("2"));
    assertThat(payload.hasNext(), is(true));
    assertThat(payload.next().getValue().toString(), is("3"));
    assertThat(payload.hasNext(), is(false));
  }

  @Test
  public void expectedOutputShouldBeUsed() throws Exception {
    Event jsonMessage =
        eventBuilder().message(Message.builder().value("{\"student\": false}").mediaType(APPLICATION_JSON).build()).build();
    TypedValue result = expressionLanguage.evaluate("payload.student", BOOLEAN, jsonMessage, BindingContext.builder().build());
    assertThat(result.getValue(), is(false));
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
  public void fullErrorBinding() throws Exception {
    String description = "An error occurred";
    String detailedDescription = "A division by zero has collapsed our systems.";
    String exceptionMessage = "dividend cannot be zero";
    String errorId = "WEAVE_TEST";

    ErrorType errorType = mock(ErrorType.class);
    when(errorType.getIdentifier()).thenReturn(errorId);

    Error error = ErrorBuilder.builder()
        .description(description)
        .detailedDescription(detailedDescription)
        .exception(new IllegalArgumentException(exceptionMessage))
        .errorType(errorType)
        .build();
    Optional opt = Optional.of(error);
    Event event = getEventWithError(opt);
    doReturn(testEvent().getMessage()).when(event).getMessage();

    String expression =
        "'$(error.description) $(error.detailedDescription) $(error.cause.message) $(error.errorType.identifier)'";
    TypedValue result = expressionLanguage.evaluate(expression, event, BindingContext.builder().build());
    assertThat(result.getValue(), is(format("%s %s %s %s", description, detailedDescription, exceptionMessage, errorId)));
  }

  @Test
  public void childErrorsErrorBinding() throws Exception {
    String childErrorMessage = "error";
    String otherChildErrorMessage = "oops";

    ErrorType errorType = mock(ErrorType.class);

    Error error = mock(Error.class);
    when(error.getChildErrors()).thenReturn(asList(
                                                   ErrorBuilder.builder(new IOException(childErrorMessage)).errorType(errorType)
                                                       .build(),
                                                   ErrorBuilder.builder(new DefaultMuleException(otherChildErrorMessage))
                                                       .errorType(errorType).build()));

    Optional opt = Optional.of(error);
    Event event = getEventWithError(opt);
    doReturn(testEvent().getMessage()).when(event).getMessage();

    String expression = "error.childErrors reduce ((child, acc = '') -> acc ++ child.cause.message)";
    TypedValue result = expressionLanguage.evaluate(expression, event, BindingContext.builder().build());
    assertThat(result.getValue(), is(format("%s%s", childErrorMessage, otherChildErrorMessage)));
  }

  @Test
  public void messageErrorBinding() throws Exception {
    Error error = mock(Error.class);
    when(error.getErrorMessage()).thenReturn(Message.of(new Integer[] {1, 3, 6}));

    Optional opt = Optional.of(error);
    Event event = getEventWithError(opt);
    doReturn(testEvent().getMessage()).when(event).getMessage();

    String expression = "error.errorMessage.payload reduce ($$ + $)";
    TypedValue result = expressionLanguage.evaluate(expression, event, BindingContext.builder().build());
    assertThat(result.getValue(), is(10));
  }

  @Test
  public void errorMessageContainsDataweaveExceptionCauseMessage() throws Exception {
    Error error = mock(Error.class);
    Optional opt = Optional.of(error);
    Event event = getEventWithError(opt);
    doReturn(testEvent().getMessage()).when(event).getMessage();
    String expressionThatThrowsException = "payload + 'foo'";

    expectedEx.expect(ExpressionRuntimeException.class);
    expectedEx.expectMessage(containsString("You called the function '+' with these arguments"));
    expectedEx.expectMessage(containsString("evaluating expression: \"" + expressionThatThrowsException));

    expressionLanguage.evaluate(expressionThatThrowsException, event, BindingContext.builder().build());
  }

  @Test
  public void payloadBinding() throws Exception {
    Event event = getEventWithError(empty());
    InternalMessage message = mock(InternalMessage.class, RETURNS_DEEP_STUBS);
    when(event.getMessage()).thenReturn(message);
    TypedValue payload = new TypedValue<>("hey", STRING);
    TypedValue attributes = new TypedValue<>(null, OBJECT);
    when(message.getPayload()).thenReturn(payload);
    when(message.getAttributes()).thenReturn(attributes);

    TypedValue result = expressionLanguage.evaluate(PAYLOAD, event, BindingContext.builder().build());
    assertThat(result.getValue(), is(equalTo(payload.getValue())));
    assertThat(result.getDataType(), is(equalTo(payload.getDataType())));
  }

  @Test
  public void dataTypeBinding() throws Exception {
    Event event = getEventWithError(empty());
    InternalMessage message = mock(InternalMessage.class, RETURNS_DEEP_STUBS);
    when(event.getMessage()).thenReturn(message);
    TypedValue payload = new TypedValue<>("hey", STRING);
    TypedValue attributes = new TypedValue<>(null, OBJECT);
    when(message.getPayload()).thenReturn(payload);
    when(message.getAttributes()).thenReturn(attributes);

    TypedValue result = expressionLanguage.evaluate(DATA_TYPE, event, BindingContext.builder().build());
    assertThat(result.getValue(), is(equalTo(STRING)));
    assertThat(fromType(DataType.class).isCompatibleWith(result.getDataType()), is(true));
  }

  @Test
  public void variablesBindings() throws Exception {
    Event event = getEventWithError(empty());
    String var1 = "var1";
    String var2 = "var2";
    when(event.getVariables().keySet()).thenReturn(Sets.newHashSet(var1, var2));
    TypedValue varValue = new TypedValue<>(null, OBJECT);
    when(event.getVariables()).thenReturn(ImmutableMap.<String, TypedValue<?>>builder()
        .put(var1, varValue).put(var2, varValue).build());

    TypedValue result = expressionLanguage.evaluate(VARS, event, BindingContext.builder().build());
    assertThat(result.getValue(), is(instanceOf(Map.class)));
    assertThat((Map<String, TypedValue>) result.getValue(), hasEntry(var1, varValue));
    assertThat((Map<String, TypedValue>) result.getValue(), hasEntry(var2, varValue));
  }

  @Test
  public void variablesCannotOverrideEventBindings() throws MuleException {
    Event event = spy(testEvent());
    TypedValue<String> varValue = new TypedValue<>("", STRING);
    ImmutableMap<String, TypedValue<?>> variablesMap = ImmutableMap.<String, TypedValue<?>>builder()
        .put(PAYLOAD, varValue).put(ATTRIBUTES, varValue).put(ERROR, varValue).put(VARS, varValue).put(FLOW, varValue)
        .build();
    when(event.getVariables()).thenReturn(variablesMap);
    String flowName = "myFlowName";

    assertThat(expressionLanguage.evaluate(PAYLOAD, event, BindingContext.builder().build()).getValue(), is(TEST_PAYLOAD));
    assertThat(expressionLanguage.evaluate(ATTRIBUTES, event, BindingContext.builder().build()).getValue(), is(nullValue()));
    assertThat(expressionLanguage.evaluate(ERROR, event, BindingContext.builder().build()).getValue(), is(nullValue()));
    assertThat(expressionLanguage.evaluate(VARS, event, BindingContext.builder().build()).getValue(),
               is(instanceOf(Map.class)));
    assertThat(expressionLanguage.evaluate("flow.name", event, fromSingleComponent(flowName), BindingContext.builder().build())
        .getValue(),
               is(flowName));
  }

  @Test
  public void propertiesBindings() throws Exception {
    Event event = getEventWithError(empty());
    String var1 = "var1";
    String var2 = "var2";
    TypedValue varValue = new TypedValue<>(null, OBJECT);
    final HashMap<String, TypedValue<?>> properties = new HashMap<>();
    properties.put(var1, varValue);
    properties.put(var2, varValue);
    when(event.getProperties()).thenReturn(properties);
    TypedValue result = expressionLanguage.evaluate(PROPERTIES, event, BindingContext.builder().build());
    assertThat(result.getValue(), is(instanceOf(Map.class)));
    assertThat((Map<String, TypedValue>) result.getValue(), hasEntry(var1, varValue));
    assertThat((Map<String, TypedValue>) result.getValue(), hasEntry(var2, varValue));
  }

  @Test
  public void parametersBindings() throws Exception {
    Event event = getEventWithError(empty());
    String var1 = "var1";
    String var2 = "var2";
    TypedValue varValue = new TypedValue<>(null, OBJECT);
    final HashMap<String, TypedValue<?>> parameters = new HashMap<>();
    parameters.put(var1, varValue);
    parameters.put(var2, varValue);
    when(event.getParameters()).thenReturn(parameters);
    TypedValue result = expressionLanguage.evaluate(PARAMETERS, event, BindingContext.builder().build());
    assertThat(result.getValue(), is(instanceOf(Map.class)));
    assertThat((Map<String, TypedValue>) result.getValue(), hasEntry(var1, varValue));
    assertThat((Map<String, TypedValue>) result.getValue(), hasEntry(var2, varValue));
  }

  @Test
  public void authenticationBinding() throws Exception {
    Event event = spy(testEvent());
    Authentication authentication =
        new DefaultMuleAuthentication(new DefaultMuleCredentials("username", "password".toCharArray()));
    SecurityContext securityContext = new DefaultSecurityContext(authentication);
    when(event.getSecurityContext()).thenReturn(securityContext);
    TypedValue result = expressionLanguage.evaluate(AUTHENTICATION, event, BindingContext.builder().build());
    assertThat(result.getValue(), is(instanceOf(Authentication.class)));
    assertThat(result.getValue(), is(authentication));
    assertThat(result.getDataType().getType(), is(equalTo(Authentication.class)));
  }

  @Test
  public void authenticationBindingWhenNullSecurityContext() throws Exception {
    Event event = spy(testEvent());
    TypedValue result = expressionLanguage.evaluate(AUTHENTICATION, event, BindingContext.builder().build());
    assertThat(result.getValue(), is(nullValue()));
  }

  @Test
  public void accessRegistryBean() throws MuleException {
    Event event = testEvent();
    muleContext.getRegistry().registerObject("myBean", new MyBean("DataWeave"));
    TypedValue evaluate = expressionLanguage.evaluate("app.registry.myBean.name", event, BindingContext.builder().build());
    assertThat(evaluate.getValue(), is("DataWeave"));
  }

  @Test
  public void accessServerFileSeparator() throws MuleException {
    Event event = testEvent();
    muleContext.getRegistry().registerObject("myBean", new MyBean("DataWeave"));
    TypedValue evaluate = expressionLanguage.evaluate("server.fileSeparator", event, BindingContext.builder().build());
    assertThat(evaluate.getValue(), is(FILE_SEPARATOR));
  }

  @Test
  public void accessMuleVersion() throws MuleException {
    Event event = testEvent();
    muleContext.getRegistry().registerObject("myBean", new MyBean("DataWeave"));
    TypedValue evaluate = expressionLanguage.evaluate("mule.version", event, BindingContext.builder().build());
    assertThat(evaluate.getValue(), is(MuleManifest.getProductVersion()));
  }

  @Test
  public void flowNameBinding() {
    Event event = getEventWithError(empty());
    String flowName = "myFlowName";

    TypedValue result =
        expressionLanguage.evaluate("flow.name", event, fromSingleComponent(flowName), BindingContext.builder().build());
    assertThat(result.getDataType(), is(STRING));
    assertThat(result.getValue(), is(flowName));
  }

  @Test
  public void payloadExpressionShouldNotBeEvaluate() throws MuleException {
    BindingContext bindingContext = BindingContext.builder().build();
    MuleContext muleContext = mock(MuleContext.class);
    DefaultExpressionLanguageFactoryService languageFactory = mock(DefaultExpressionLanguageFactoryService.class);
    ExpressionLanguage expressionLanguage = spy(ExpressionLanguage.class);
    when(languageFactory.create()).thenReturn(expressionLanguage);
    Event event = testEvent();
    new DataWeaveExpressionLanguageAdaptor(muleContext, languageFactory).evaluate("#[payload]", event, bindingContext);
    verify(expressionLanguage, never()).evaluate(eq("payload"), any(BindingContext.class));
  }

  private Event getEventWithError(Optional<Error> error) {
    Event event = mock(Event.class, RETURNS_DEEP_STUBS);
    doReturn(error).when(event).getError();
    when(event.getMessage().getPayload()).thenReturn(new TypedValue<>(null, OBJECT));
    when(event.getMessage().getAttributes()).thenReturn(new TypedValue<>(null, OBJECT));
    return event;
  }

  private class SomeAttributes extends BaseAttributes {

  }

  private class MyBean {

    private String name;

    public MyBean(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }
}
