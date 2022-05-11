/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.dataweave;

import static java.io.File.separator;
import static java.lang.String.format;
import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.component.AbstractComponent.ROOT_CONTAINER_NAME_KEY;
import static org.mule.runtime.api.el.BindingContextUtils.ATTRIBUTES;
import static org.mule.runtime.api.el.BindingContextUtils.AUTHENTICATION;
import static org.mule.runtime.api.el.BindingContextUtils.DATA_TYPE;
import static org.mule.runtime.api.el.BindingContextUtils.ERROR;
import static org.mule.runtime.api.el.BindingContextUtils.FLOW;
import static org.mule.runtime.api.el.BindingContextUtils.ITEM_SEQUENCE_INFO;
import static org.mule.runtime.api.el.BindingContextUtils.MESSAGE;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.el.BindingContextUtils.PARAMS;
import static org.mule.runtime.api.el.BindingContextUtils.PAYLOAD;
import static org.mule.runtime.api.el.BindingContextUtils.VARS;
import static org.mule.runtime.api.el.BindingContextUtils.getTargetBindingContext;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.DataType.BOOLEAN;
import static org.mule.runtime.api.metadata.DataType.JSON_STRING;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.api.metadata.DataType.fromType;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_EXPRESSIONS_COMPILATION_FAIL_DEPLOYMENT;
import static org.mule.runtime.core.internal.el.ExpressionLanguageUtils.compile;
import static org.mule.runtime.core.privileged.component.AnnotatedObjectInvocationHandler.addAnnotationsToClass;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.from;
import static org.mule.tck.junit4.matcher.DataTypeCompatibilityMatcher.assignableTo;
import static org.mule.tck.probe.PollingProber.DEFAULT_POLLING_INTERVAL;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.EXPRESSION_LANGUAGE;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.ExpressionLanguageStory.SUPPORT_DW;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.CompiledExpression;
import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.el.ExpressionCompilationException;
import org.mule.runtime.api.el.ExpressionExecutionException;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.ItemSequenceInfo;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.security.DefaultMuleAuthentication;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleManifest;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.security.DefaultMuleCredentials;
import org.mule.runtime.core.internal.el.ExpressionLanguageSessionAdaptor;
import org.mule.runtime.core.internal.el.IllegalCompiledExpression;
import org.mule.runtime.core.internal.message.BaseAttributes;
import org.mule.runtime.core.internal.message.ErrorBuilder;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.io.IOException;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

import org.hamcrest.BaseMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(EXPRESSION_LANGUAGE)
@Story(SUPPORT_DW)
public class DataWeaveExpressionLanguageAdaptorTestCase extends AbstractWeaveExpressionLanguageTestCase {

  private static final int GC_POLLING_TIMEOUT = 10000;

  @Rule
  public ExpectedException expectedEx = none();

  private final ExpressionLanguage genericExpressionLanguage = spy(ExpressionLanguage.class);
  private DefaultExpressionLanguageFactoryService genericExpressionLanguageService;
  private final BindingContext bindingContext = NULL_BINDING_CONTEXT;

  @Before
  public void before() {
    genericExpressionLanguageService = mock(DefaultExpressionLanguageFactoryService.class);
    when(genericExpressionLanguageService.create(any())).thenReturn(genericExpressionLanguage);
  }

  @Test
  public void stringExpression() throws Exception {
    TypedValue<?> result = expressionLanguage.evaluate("\"hey\"", testEvent(), BindingContext.builder().build());
    assertThat(result.getValue(), is("hey"));
    assertThat(result.getDataType(), is(assignableTo(STRING)));
  }

  @Test
  public void withPrefixExpression() throws Exception {
    TypedValue<?> result = expressionLanguage.evaluate("#[dw:\"hey\"]", testEvent(), BindingContext.builder().build());
    assertThat(result.getValue(), is("hey"));
    assertThat(result.getDataType(), is(assignableTo(STRING)));
  }

  @Test
  public void attributesBinding() {
    CoreEvent event = getEventWithError(empty());
    SomeAttributes attributes = new SomeAttributes();
    InternalMessage message = (InternalMessage) Message.builder().nullValue().attributesValue(attributes).build();
    when(event.getMessage()).thenReturn(message);

    TypedValue<?> result = expressionLanguage.evaluate(ATTRIBUTES, event, BindingContext.builder().build());
    assertThat(result.getValue(), is(equalTo(attributes)));
    assertThat(result.getDataType().getType(), is((equalTo(SomeAttributes.class))));
  }

  @Test
  public void splitByJson() throws Exception {
    CoreEvent jsonMessage =
        eventBuilder(muleContext).message(Message.builder().value("[1,2,3]").mediaType(APPLICATION_JSON).build()).build();
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
    CoreEvent jsonMessage =
        eventBuilder(muleContext).message(Message.builder().value("{\"student\": false}").mediaType(APPLICATION_JSON).build())
            .build();
    TypedValue<?> result = expressionLanguage.evaluate("payload.student", BOOLEAN, jsonMessage, BindingContext.builder().build());
    assertThat(result.getValue(), is(false));
  }

  @Test
  public void errorBinding() throws Exception {
    Error error = mock(Error.class);
    Optional<Error> opt = of(error);
    CoreEvent event = getEventWithError(opt);
    doReturn(testEvent().getMessage()).when(event).getMessage();

    TypedValue<?> result = expressionLanguage.evaluate(ERROR, event, BindingContext.builder().build());
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
    Optional<Error> opt = of(error);
    CoreEvent event = getEventWithError(opt);
    doReturn(testEvent().getMessage()).when(event).getMessage();

    String expression =
        "'$(error.description) $(error.detailedDescription) $(error.cause.message) $(error.errorType.identifier)'";
    TypedValue<?> result = expressionLanguage.evaluate(expression, event, BindingContext.builder().build());
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

    Optional<Error> opt = of(error);
    CoreEvent event = getEventWithError(opt);
    doReturn(testEvent().getMessage()).when(event).getMessage();

    String expression = "error.childErrors reduce ((child, acc = '') -> acc ++ child.cause.message)";
    TypedValue<?> result = expressionLanguage.evaluate(expression, event, BindingContext.builder().build());
    assertThat(result.getValue(), is(format("%s%s", childErrorMessage, otherChildErrorMessage)));
  }

  @Test
  public void messageErrorBinding() throws Exception {
    Error error = mock(Error.class);
    when(error.getErrorMessage()).thenReturn(Message.of(new Integer[] {1, 3, 6}));

    Optional<Error> opt = of(error);
    CoreEvent event = getEventWithError(opt);
    doReturn(testEvent().getMessage()).when(event).getMessage();

    String expression = "error.errorMessage.payload reduce ($$ + $)";
    TypedValue<?> result = expressionLanguage.evaluate(expression, event, BindingContext.builder().build());
    assertThat(result.getValue(), is(10));
  }

  @Test
  public void errorMessageContainsDataweaveExceptionCauseMessage() throws Exception {
    Error error = mock(Error.class);
    Optional<Error> opt = of(error);
    CoreEvent event = getEventWithError(opt);
    doReturn(testEvent().getMessage()).when(event).getMessage();
    String expressionThatThrowsException = "payload + 'foo'";

    expectedEx.expect(ExpressionRuntimeException.class);
    expectedEx.expectMessage(containsString("You called the function '+' with these arguments"));
    expectedEx.expectMessage(containsString("evaluating expression: \"" + expressionThatThrowsException));

    expressionLanguage.evaluate(expressionThatThrowsException, event, BindingContext.builder().build());
  }

  @Test
  public void errorIsOfExpectedTypeWhenParsingLogExpression() throws Exception {
    final String invalidExpression = "#[INVALID_EXPRESSION()]";

    Error error = mock(Error.class);
    Optional<Error> opt = of(error);
    CoreEvent event = getEventWithError(opt);
    doReturn(testEvent().getMessage()).when(event).getMessage();

    expectedEx.expect(ExpressionRuntimeException.class);

    expressionLanguage.evaluateLogExpression(invalidExpression, event, null, BindingContext.builder().build());
  }

  @Test
  public void payloadBinding() {
    CoreEvent event = getEventWithError(empty());
    InternalMessage message = mock(InternalMessage.class, RETURNS_DEEP_STUBS);
    when(event.getMessage()).thenReturn(message);
    TypedValue<Object> payload = new TypedValue<>("hey", STRING);
    TypedValue<Object> attributes = new TypedValue<>(null, OBJECT);
    when(message.getPayload()).thenReturn(payload);
    when(message.getAttributes()).thenReturn(attributes);

    TypedValue<?> result = expressionLanguage.evaluate(PAYLOAD, event, BindingContext.builder().build());
    assertThat(result.getValue(), is(equalTo(payload.getValue())));
    assertThat(result.getDataType(), is(equalTo(payload.getDataType())));
  }

  @Test
  public void dataTypeBinding() {
    CoreEvent event = getEventWithError(empty());
    InternalMessage message = mock(InternalMessage.class, RETURNS_DEEP_STUBS);
    when(event.getMessage()).thenReturn(message);
    TypedValue<Object> payload = new TypedValue<>("hey", STRING);
    TypedValue<Object> attributes = new TypedValue<>(null, OBJECT);
    when(message.getPayload()).thenReturn(payload);
    when(message.getAttributes()).thenReturn(attributes);

    TypedValue<?> result = expressionLanguage.evaluate(DATA_TYPE, event, BindingContext.builder().build());
    assertThat(result.getValue(), is(equalTo(STRING)));
    assertThat(result.getDataType(), is(assignableTo(fromType(DataType.class))));
  }

  @Test
  public void variablesBindings() {
    CoreEvent event = getEventWithError(empty());
    String var1 = "var1";
    String var2 = "var2";
    when(event.getVariables().keySet()).thenReturn(Sets.newHashSet(var1, var2));
    TypedValue<?> varValue = new TypedValue<>(null, OBJECT);
    when(event.getVariables()).thenReturn(ImmutableMap.<String, TypedValue<?>>builder()
        .put(var1, varValue).put(var2, varValue).build());

    TypedValue<?> result = expressionLanguage.evaluate(VARS, event, BindingContext.builder().build());
    assertThat(result.getValue(), is(instanceOf(Map.class)));
    assertThat((Map<String, TypedValue<?>>) result.getValue(), hasEntry(var1, varValue));
    assertThat((Map<String, TypedValue<?>>) result.getValue(), hasEntry(var2, varValue));
  }

  @Test
  public void parametersBindings() {
    CoreEvent event = getEventWithError(empty());
    String param1 = "params1";
    String param2 = "params2";
    when(event.getParameters().keySet()).thenReturn(Sets.newHashSet(param1, param2));
    TypedValue<?> paramValue = new TypedValue<>(null, OBJECT);
    when(event.getParameters()).thenReturn(ImmutableMap.<String, TypedValue<?>>builder()
        .put(param1, paramValue).put(param2, paramValue).build());

    TypedValue<?> result = expressionLanguage.evaluate(PARAMS, event, BindingContext.builder().build());
    assertThat(result.getValue(), is(instanceOf(Map.class)));
    assertThat((Map<String, TypedValue<?>>) result.getValue(), hasEntry(param1, paramValue));
    assertThat((Map<String, TypedValue<?>>) result.getValue(), hasEntry(param2, paramValue));
  }

  @Test
  public void variablesCannotOverrideEventBindings() throws MuleException {
    CoreEvent event = spy(testEvent());
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
    assertThat(expressionLanguage.evaluate("flow.name", event, from(flowName), BindingContext.builder().build())
        .getValue(),
               is(flowName));
  }

  @Test
  public void messageBinding() throws Exception {
    CoreEvent event = testEvent();
    TypedValue<?> result = expressionLanguage.evaluate(MESSAGE, event, BindingContext.builder().build());
    assertThat(result.getValue(), is(instanceOf(Message.class)));
    assertEquals(((Message) result.getValue()).getPayload(), event.getMessage().getPayload());
  }

  @Test
  public void nullItemSequenceInfoBinding() throws Exception {
    CoreEvent event = testEvent();
    TypedValue<?> result = expressionLanguage.evaluate(ITEM_SEQUENCE_INFO, event, BindingContext.builder().build());
    assertThat(result.getValue(), is(nullValue()));
  }

  @Test
  public void noSequenceSizeItemSequenceInfoBinding() throws Exception {
    CoreEvent event = spy(testEvent());
    when(event.getItemSequenceInfo()).thenReturn(of(ItemSequenceInfo.of(43)));
    TypedValue<?> result = expressionLanguage.evaluate(ITEM_SEQUENCE_INFO + ".position", event, BindingContext.builder().build());
    assertThat(result.getValue(), is(43));
    result = expressionLanguage.evaluate(ITEM_SEQUENCE_INFO + ".sequenceSize", event, BindingContext.builder().build());
    assertThat(result.getValue(), is(nullValue()));
  }

  @Test
  public void itemSequenceInfoBinding() throws Exception {
    CoreEvent event = spy(testEvent());
    when(event.getItemSequenceInfo()).thenReturn(of(ItemSequenceInfo.of(43, 100)));
    TypedValue<?> result = expressionLanguage.evaluate(ITEM_SEQUENCE_INFO + ".position", event, BindingContext.builder().build());
    assertThat(result.getValue(), is(43));
    result = expressionLanguage.evaluate(ITEM_SEQUENCE_INFO + ".sequenceSize", event, BindingContext.builder().build());
    assertThat(result.getValue(), is(100));
  }

  @Test
  public void authenticationBinding() throws Exception {
    CoreEvent event = spy(testEvent());
    Authentication authentication =
        new DefaultMuleAuthentication(new DefaultMuleCredentials("username", "password".toCharArray()));
    when(event.getAuthentication()).thenReturn(of(authentication));
    TypedValue<?> result = expressionLanguage.evaluate(AUTHENTICATION, event, BindingContext.builder().build());
    assertThat(result.getValue(), is(instanceOf(Authentication.class)));
    assertThat(result.getValue(), is(authentication));
    assertThat(result.getDataType().getType(), is(equalTo(Authentication.class)));
  }

  @Test
  public void authenticationBindingWhenNullSecurityContext() throws Exception {
    CoreEvent event = spy(testEvent());
    TypedValue<?> result = expressionLanguage.evaluate(AUTHENTICATION, event, BindingContext.builder().build());
    assertThat(result.getValue(), is(nullValue()));
  }

  @Test
  public void accessRegistryBean() throws MuleException {
    CoreEvent event = testEvent();
    when(registry.lookupByName("myBean")).thenReturn(of(new MyBean("DataWeave")));
    TypedValue<?> evaluate = expressionLanguage.evaluate("app.registry.myBean.name", event, BindingContext.builder().build());
    assertThat(evaluate.getValue(), is("DataWeave"));
  }

  @Test
  public void accessRegistryAnnotatedBean() throws MuleException {
    CoreEvent event = testEvent();
    when(registry.lookupByName("myBean")).thenReturn(of(new MyAnnotatedBean("DataWeave")));
    TypedValue<?> evaluate = expressionLanguage.evaluate("app.registry.myBean", fromType(MyBean.class), event,
                                                         from("flow"), BindingContext.builder().build(), false);
    assertThat(evaluate.getValue(), is(instanceOf(MyAnnotatedBean.class)));
  }

  @Test
  public void accessRegistryCglibAnnotatedBean() throws Exception {
    CoreEvent event = testEvent();

    MyBean annotatedMyBean = (MyBean) addAnnotationsToClass(MyBean.class).newInstance();
    annotatedMyBean.setName("DataWeave");
    when(registry.lookupByName("myBean")).thenReturn(of(annotatedMyBean));
    TypedValue<?> evaluate = expressionLanguage.evaluate("app.registry.myBean", fromType(MyBean.class), event,
                                                         from("flow"), BindingContext.builder().build(), false);
    assertThat(evaluate.getValue(), is(instanceOf(MyBean.class)));
  }

  @Test
  public void accessServerFileSeparator() throws MuleException {
    CoreEvent event = testEvent();
    when(registry.lookupByName("myBean")).thenReturn(of(new MyBean("DataWeave")));
    TypedValue<?> evaluate = expressionLanguage.evaluate("server.fileSeparator", event, BindingContext.builder().build());
    assertThat(evaluate.getValue(), is(separator));
  }

  @Test
  public void accessMuleVersion() throws MuleException {
    CoreEvent event = testEvent();
    when(registry.lookupByName("myBean")).thenReturn(of(new MyBean("DataWeave")));
    TypedValue<?> evaluate = expressionLanguage.evaluate("mule.version", event, BindingContext.builder().build());
    assertThat(evaluate.getValue(), is(MuleManifest.getProductVersion()));
  }

  @Test
  public void flowNameBinding() {
    CoreEvent event = getEventWithError(empty());
    String flowName = "myFlowName";

    TypedValue<?> result =
        expressionLanguage.evaluate("flow.name", event, from(flowName), BindingContext.builder().build());
    assertThat(result.getDataType(), is(assignableTo(STRING)));
    assertThat(result.getValue(), is(flowName));
  }

  @Test
  public void payloadExpressionShouldNotBeEvaluate() throws MuleException {
    new DataWeaveExpressionLanguageAdaptor(mock(MuleContext.class, RETURNS_DEEP_STUBS), registry,
                                           genericExpressionLanguageService, getFeatureFlaggingService())
                                               .evaluate("#[payload]", testEvent(), bindingContext);
    verify(genericExpressionLanguage, never()).evaluate(eq("payload"), any(BindingContext.class));
  }

  @Test
  @Description("When calling evaluate with just a BindingContext (no Event), it is passed to DW. No new context is built based on the contexts of the passed one.")
  public void evaluateNoEventDoesntInstantiateExtraBindingContexts() {
    new DataWeaveExpressionLanguageAdaptor(mock(MuleContext.class, RETURNS_DEEP_STUBS), registry,
                                           genericExpressionLanguageService, getFeatureFlaggingService())
                                               .evaluate("#['Hello World']", null, bindingContext);
    verify(genericExpressionLanguage).evaluate(anyString(), eq(bindingContext));
  }

  @Test
  @Description("When calling evaluate with just a BindingContext (no Event or DataType), it is passed to DW. No new context is built based on the contexts of the passed one.")
  public void evaluateWithDataTypeNoEventDoesntInstantiateExtraBindingContexts() {
    new DataWeaveExpressionLanguageAdaptor(mock(MuleContext.class, RETURNS_DEEP_STUBS), registry,
                                           genericExpressionLanguageService, getFeatureFlaggingService())
                                               .evaluate("#['Hello World']", OBJECT, null, bindingContext);
    verify(genericExpressionLanguage).evaluate(anyString(), eq(OBJECT), eq(bindingContext));
  }

  @Test
  @Description("When calling evaluate with just a BindingContext (no Event, with DataType), it is passed to DW. No new context is built based on the contexts of the passed one.")
  public void evaluateNoLocationNoEventWithDataTypeDoesntInstantiateExtraBindingContexts() {
    new DataWeaveExpressionLanguageAdaptor(mock(MuleContext.class, RETURNS_DEEP_STUBS), registry,
                                           genericExpressionLanguageService, getFeatureFlaggingService())
                                               .evaluate("#['Hello World']", OBJECT, null, null, bindingContext, false);
    verify(genericExpressionLanguage).evaluate(anyString(), eq(OBJECT), eq(bindingContext));
  }

  @Test
  @Description("When calling evaluate with just a BindingContext (no Event, no Location, no DataType), it is passed to DW. No new context is built based on the contexts of the passed one.")
  public void evaluateNoLocationNoEventDoesntInstantiateExtraBindingContexts() {
    new DataWeaveExpressionLanguageAdaptor(mock(MuleContext.class, RETURNS_DEEP_STUBS), registry,
                                           genericExpressionLanguageService, getFeatureFlaggingService())
                                               .evaluate("#['Hello World']", null, null, null, bindingContext);
    verify(genericExpressionLanguage).evaluate(anyString(), eq(bindingContext));
  }

  @Test
  @Description("When calling evaluate with just a BindingContext (no Event, no Location), it is passed to DW. No new context is built based on the contexts of the passed one.")
  public void evaluateLogExpressionNoLocationNoEventDoesntInstantiateExtraBindingContexts() {
    new DataWeaveExpressionLanguageAdaptor(mock(MuleContext.class, RETURNS_DEEP_STUBS), registry,
                                           genericExpressionLanguageService, getFeatureFlaggingService())
                                               .evaluateLogExpression("#['Hello World']", null, null, bindingContext);
    verify(genericExpressionLanguage).evaluateLogExpression(anyString(), eq(bindingContext));
  }

  @Test
  public void entrySetFunction() throws Exception {
    final String key = "foo";
    final String value = "bar";
    CoreEvent event = eventBuilder(muleContext).message(Message.builder().value(singletonMap(key, value)).build()).build();
    TypedValue<?> result =
        expressionLanguage.evaluate("dw::core::Objects::entrySet(payload)", event, BindingContext.builder().build());
    assertThat(result.getValue(), instanceOf(List.class));
    assertThat(((List<?>) result.getValue()).get(0), instanceOf(Map.class));
    Map<?, ?> entry = (Map<?, ?>) ((List<?>) result.getValue()).get(0);
    assertThat(entry.get("key"), equalTo(key));
    assertThat(entry.get("value"), equalTo(value));
  }

  @Test
  public void unbalancedBrackets() throws MuleException {
    CoreEvent event = eventBuilder(muleContext).message(Message.of(TEST_PAYLOAD)).build();

    expectedEx.expect(ExpressionExecutionException.class);
    expectedEx.expectMessage(containsString("Unbalanced brackets in expression"));
    expressionLanguage.evaluate("#[unbalanced", event, BindingContext.builder().build());
  }

  @Test
  public void bindingValueNotReferencedByDWParserCache() {
    Object bindingValue = new Object();
    PhantomReference<CoreEvent> bindingValueRef = new PhantomReference(bindingValue, new ReferenceQueue<>());

    expressionLanguage.evaluate("#[value]", null, BindingContext.builder()
        .addBinding("value", new TypedValue<>(bindingValue, OBJECT))
        .build());

    bindingValue = null;

    new PollingProber(GC_POLLING_TIMEOUT, DEFAULT_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      System.gc();
      assertThat(bindingValueRef.isEnqueued(), is(true));
      return true;
    }, "A hard reference is being mantained to the bindingValue."));
  }

  @Test
  public void handlesOptional() throws Exception {
    TypedValue<?> result =
        expressionLanguage.evaluate("'hello' as String {class: 'java.util.Optional'}", testEvent(),
                                    BindingContext.builder().build());
    assertThat(result.getValue(), is(instanceOf(Optional.class)));
    assertThat(((Optional<?>) result.getValue()).get(), is("hello"));
  }

  @Test
  public void handlesEmptyOptional() throws Exception {
    TypedValue<?> result =
        expressionLanguage.evaluate("null as Null {class: 'java.util.Optional'}", testEvent(), BindingContext.builder().build());
    assertThat(result.getValue(), is(instanceOf(Optional.class)));
    assertThat(result.getValue(), is(empty()));
  }

  @Test
  public void handlesExpectedOptional() throws Exception {
    TypedValue<?> result =
        expressionLanguage.evaluate("java!java::util::Optional::of('hello')", DataType.fromType(Optional.class), testEvent(),
                                    BindingContext.builder().build());
    assertThat(result.getValue(), is(instanceOf(Optional.class)));
    assertThat(((Optional<?>) result.getValue()).get(), is("hello"));
  }

  @Test
  public void handlesEmptyExpectedOptional() throws Exception {
    TypedValue<?> result =
        expressionLanguage.evaluate("java!java::util::Optional::empty()", DataType.fromType(Optional.class), testEvent(),
                                    BindingContext.builder().build());
    assertThat(result.getValue(), is(instanceOf(Optional.class)));
    assertThat(result.getValue(), is(empty()));
  }

  @Test
  public void sessionWithEventAndLocation() throws MuleException {
    ExpressionLanguageSessionAdaptor session =
        expressionLanguage.openSession(TEST_CONNECTOR_LOCATION, testEvent(), NULL_BINDING_CONTEXT);

    assertThat(session.evaluate("payload").getValue(), is("test"));
    assertThat(session.evaluate("flow.name").getValue(), is("test"));
  }

  @Test
  public void sessionWithBindingContext() throws MuleException {
    ExpressionLanguageSessionAdaptor session = expressionLanguage.openSession(null, null, testEvent().asBindingContext());

    assertThat(session.evaluate("payload").getValue(), is("test"));
    expectedEx.expect(ExpressionRuntimeException.class);
    assertThat(session.evaluate("flow.name").getValue(), is(nullValue()));
  }

  @Test
  public void sessionWithBindingContextAndLocation() throws MuleException {
    ExpressionLanguageSessionAdaptor session =
        expressionLanguage.openSession(TEST_CONNECTOR_LOCATION, null, testEvent().asBindingContext());

    assertThat(session.evaluate("payload").getValue(), is("test"));
    assertThat(session.evaluate("flow.name").getValue(), is("test"));
  }

  @Test
  @Issue("MULE-18490")
  public void compiledWithBindingContextAndLocation() {
    final ExpressionLanguage el = mock(ExpressionLanguage.class);

    compile("flow.name", el);

    verify(el).compile(eq("flow.name"), argThat(new BaseMatcher<BindingContext>() {

      @Override
      public boolean matches(Object item) {
        return item instanceof BindingContext && ((BindingContext) item).lookup("flow").isPresent();
      }

      @Override
      public void describeTo(org.hamcrest.Description description) {
        description.appendText("A bindingContext containing a 'flow' binding");
      }
    }));
  }

  @Test
  public void sessionEvaluatesPayloadExpressionWithExpectedDataType() throws MuleException {
    TypedValue<String> stringJsonPayload = new TypedValue<>("\"string\"", JSON_STRING);
    CoreEvent eventWithJsonPayload = CoreEvent.builder(testEvent()).message(Message.of(stringJsonPayload)).build();
    BindingContext bindingContext = eventWithJsonPayload.asBindingContext();
    ExpressionLanguageSessionAdaptor session = expressionLanguage.openSession(TEST_CONNECTOR_LOCATION, null, bindingContext);
    assertThat(session.evaluate("payload", STRING).getValue(), is("string"));
  }

  @Test
  public void compilationExceptionDoesntPropagate() {
    CompiledExpression compiled = expressionLanguage.compile("#[ble]", getTargetBindingContext(Message.of("")));
    assertThat(compiled, is(instanceOf(IllegalCompiledExpression.class)));
  }

  @Test
  public void compilationExceptionPropagates() {
    setProperty(MULE_EXPRESSIONS_COMPILATION_FAIL_DEPLOYMENT, "true");
    expectedEx.expect(ExpressionCompilationException.class);
    try {
      expressionLanguage.compile("#[ble]", getTargetBindingContext(Message.of("")));
    } finally {
      clearProperty(MULE_EXPRESSIONS_COMPILATION_FAIL_DEPLOYMENT);
    }
  }

  @Test
  public void evaluateInvalidCompiledExpression() throws MuleException {
    ExpressionCompilationException e = new ExpressionCompilationException(createStaticMessage("oopsy"));
    expectedEx.expect(ExpressionRuntimeException.class);
    expectedEx.expectCause(is(sameInstance(e)));

    CompiledExpression compiled = new IllegalCompiledExpression("#[ble]", e);
    try (ExpressionLanguageSessionAdaptor session =
        expressionLanguage.openSession(TEST_CONNECTOR_LOCATION, testEvent(), NULL_BINDING_CONTEXT)) {
      session.evaluate(compiled);
    }
  }

  private CoreEvent getEventWithError(Optional<Error> error) {
    CoreEvent event = mock(CoreEvent.class, RETURNS_DEEP_STUBS);
    doReturn(error).when(event).getError();
    when(event.getMessage().getPayload()).thenReturn(new TypedValue<>(null, OBJECT));
    when(event.getMessage().getAttributes()).thenReturn(new TypedValue<>(null, OBJECT));
    when(event.getAuthentication()).thenReturn(empty());
    when(event.getItemSequenceInfo()).thenReturn(empty());
    return event;
  }

  private static class SomeAttributes extends BaseAttributes {

  }

  public static class MyBean {

    private String name;

    public MyBean() {}

    public MyBean(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  private static class MyAnnotatedBean extends MyBean implements Component {

    public MyAnnotatedBean(String name) {
      super(name);
    }

    private final Map<QName, Object> annotations = new ConcurrentHashMap<>();

    @Override
    public Object getAnnotation(QName qName) {
      return annotations.get(qName);
    }

    @Override
    public Map<QName, Object> getAnnotations() {
      return unmodifiableMap(annotations);
    }

    @Override
    public synchronized void setAnnotations(Map<QName, Object> newAnnotations) {
      annotations.clear();
      annotations.putAll(newAnnotations);
    }

    @Override
    public ComponentLocation getLocation() {
      return (ComponentLocation) getAnnotation(LOCATION_KEY);
    }

    private String getRootContainerName() {
      String rootContainerName = (String) getAnnotation(ROOT_CONTAINER_NAME_KEY);
      if (rootContainerName == null) {
        rootContainerName = getLocation().getRootContainerName();
      }
      return rootContainerName;
    }

    @Override
    public Location getRootContainerLocation() {
      return Location.builder().globalName(getRootContainerName()).build();
    }
  }

}
