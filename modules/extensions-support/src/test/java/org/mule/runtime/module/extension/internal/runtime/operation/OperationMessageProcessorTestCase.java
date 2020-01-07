/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonMap;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.meta.model.operation.ExecutionType.BLOCKING;
import static org.mule.runtime.api.meta.model.operation.ExecutionType.CPU_INTENSIVE;
import static org.mule.runtime.api.meta.model.operation.ExecutionType.CPU_LITE;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.api.util.collection.SmallMap.of;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_LANGUAGE;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.util.SystemUtils.getDefaultEncoding;
import static org.mule.runtime.core.internal.event.EventQuickCopy.quickCopy;
import static org.mule.runtime.core.internal.interception.DefaultInterceptionEvent.INTERCEPTION_RESOLVED_CONTEXT;
import static org.mule.runtime.core.privileged.util.EventUtils.getRoot;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_VALUE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.runtime.operation.Result.builder;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENCODING_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.MIME_TYPE_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor.INVALID_TARGET_MESSAGE;
import static org.mule.tck.MuleTestUtils.stubComponentExecutor;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;
import static org.mule.tck.util.MuleContextUtils.registerIntoMockContext;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.CollectionDataType;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MapDataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType;
import org.mule.runtime.core.api.retry.policy.NoRetryPolicyTemplate;
import org.mule.runtime.core.internal.el.DefaultExpressionManager;
import org.mule.runtime.core.internal.el.mvel.MVELExpressionLanguage;
import org.mule.runtime.core.internal.policy.OperationExecutionFunction;
import org.mule.runtime.core.internal.policy.OperationParametersProcessor;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.model.ImmutableOutputModel;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.ValueResolvingException;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.tck.size.SmallTest;
import org.mule.weave.v2.el.WeaveDefaultExpressionLanguageFactoryService;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.reflect.TypeToken;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class OperationMessageProcessorTestCase extends AbstractOperationMessageProcessorTestCase {

  private static final String SOME_PARAM_NAME = "someParam";
  private static final String FLOW_NAME = "flowName";

  @Rule
  public ExpectedException expectedException = none();

  private final ReflectionCache reflectionCache = new ReflectionCache();

  @Mock
  private ExpressionManager expressionManager;

  @Override
  protected OperationMessageProcessor createOperationMessageProcessor() throws MuleException {
    OperationMessageProcessor operationMessageProcessor =
        new OperationMessageProcessor(extensionModel, operationModel, configurationProvider, target, targetValue, resolverSet,
                                      cursorStreamProviderFactory, new NoRetryPolicyTemplate(), extensionManager,
                                      mockPolicyManager, reflectionCache);
    operationMessageProcessor.setAnnotations(getFlowComponentLocationAnnotations(FLOW_NAME));
    operationMessageProcessor.setComponentLocator(componentLocator);

    return operationMessageProcessor;
  }

  @Test
  public void operationContextIsWellFormed() throws Exception {
    ArgumentCaptor<ExecutionContext> operationContextCaptor = ArgumentCaptor.forClass(ExecutionContext.class);
    messageProcessor.process(event);

    verify(operationExecutor).execute(operationContextCaptor.capture(), any());
    ExecutionContext<ComponentModel> executionContext = operationContextCaptor.getValue();

    assertThat(executionContext, is(instanceOf(ExecutionContextAdapter.class)));
    ExecutionContextAdapter<ComponentModel> executionContextAdapter = (ExecutionContextAdapter) executionContext;

    assertThat(executionContextAdapter.getEvent(), is(sameInstance(event)));
    assertThat(executionContextAdapter.getConfiguration().get().getValue(), is(sameInstance(configuration)));
  }

  @Test
  public void operationExecutorIsInvoked() throws Exception {
    messageProcessor.process(event);
    verify(operationExecutor).execute(any(ExecutionContext.class), any());
  }

  @Test
  public void operationReturnsOperationResultWhichKeepsNoValues() throws Exception {
    Object payload = new Object();
    MediaType mediaType = ANY.withCharset(getDefaultEncoding(context));
    Object attributes = mock(Object.class);

    stubResultComponentExecutor(payload, mediaType, attributes);

    Message message = messageProcessor.process(event).getMessage();
    assertThat(message, is(notNullValue()));

    assertThat(message.getPayload().getValue(), is(sameInstance(payload)));
    assertThat(message.getAttributes().getValue(), is(sameInstance(attributes)));
    assertThat(message.getPayload().getDataType().getMediaType(), is(mediaType));
  }

  @Test
  public void operationReturnsOperationResultOnTarget() throws Exception {
    target = TARGET_VAR;
    messageProcessor = setUpOperationMessageProcessor();

    Object payload = new Object();
    MediaType mediaType = ANY.withCharset(getDefaultEncoding(context));
    Object attributes = mock(Object.class);

    stubResultComponentExecutor(payload, mediaType, attributes);

    Message message = (Message) messageProcessor.process(event).getVariables().get(TARGET_VAR).getValue();
    assertThat(message, is(notNullValue()));

    assertThat(message.getPayload().getValue(), is(sameInstance(payload)));
    assertThat(message.getAttributes().getValue(), is(sameInstance(attributes)));
    assertThat(message.getPayload().getDataType().getMediaType(), equalTo(mediaType));
  }

  @Test
  public void operationReturnsOperationResultButKeepsAttributes() throws Exception {
    Object payload = new Object();
    MediaType mediaType = ANY.withCharset(getDefaultEncoding(context));

    stubResultComponentExecutor(payload, mediaType, null);

    event =
        CoreEvent.builder(event).message(Message.builder().value("").attributesValue(mock(Object.class)).build()).build();

    Message message = messageProcessor.process(event).getMessage();
    assertThat(message, is(notNullValue()));

    assertThat(message.getPayload().getValue(), is(sameInstance(payload)));
    assertThat(message.getAttributes().getValue(), is(nullValue()));
    assertThat(message.getPayload().getDataType().getMediaType(), equalTo(mediaType));
  }

  @Test
  public void operationReturnsOperationResultThatOnlySpecifiesPayload() throws Exception {
    Object payload = "hello world!";

    stubResultComponentExecutor(payload, null, null);

    event =
        CoreEvent.builder(event).message(Message.builder().value("").attributesValue(mock(Object.class)).build()).build();

    Message message = messageProcessor.process(event).getMessage();
    assertThat(message, is(notNullValue()));

    assertThat(message.getPayload().getValue(), is(sameInstance(payload)));
    assertThat(message.getAttributes().getValue(), is(nullValue()));
    assertThat(message.getPayload().getDataType().getType().equals(String.class), is(true));
  }

  @Test
  public void operationReturnsOperationResultWithPayloadAndAttributes() throws Exception {
    Object payload = "hello world!";
    Object attributes = mock(Object.class);

    stubResultComponentExecutor(payload, null, attributes);

    Message message = messageProcessor.process(event).getMessage();
    assertThat(message, is(notNullValue()));

    assertThat(message.getPayload().getValue(), is(sameInstance(payload)));
    assertThat(message.getAttributes().getValue(), is(sameInstance(attributes)));
    assertThat(message.getPayload().getDataType().getType().equals(String.class), is(true));
  }

  @Test
  public void operationReturnsPayloadValue() throws Exception {
    Object value = new Object();
    stubComponentExecutor(operationExecutor, value);

    Message message = messageProcessor.process(event).getMessage();
    assertThat(message, is(notNullValue()));
    assertThat(message.getPayload().getValue(), is(sameInstance(value)));
  }

  @Test
  public void operationReturnsMapWithCorrectDataType() throws Exception {
    Object payload = new HashMap<>();
    setUpOperationReturning(payload, new TypeToken<Map<String, String>>() {}.getType());

    Message message = messageProcessor.process(event).getMessage();
    assertThat(message, is(notNullValue()));

    assertThat(message.getPayload().getValue(), is(sameInstance(payload)));
    DataType dataType = message.getPayload().getDataType();
    assertThat(dataType, instanceOf(MapDataType.class));
    assertThat(((MapDataType) dataType).getKeyDataType(), like(String.class, ANY.withCharset(null)));
    assertThat(((MapDataType) dataType).getValueDataType(), like(String.class, ANY));
  }

  @Test
  public void operationReturnsResultMapWithCorrectDataType() throws Exception {
    Object payload = new HashMap<>();
    setUpOperationReturning(Result.builder().output(payload).build(), new TypeToken<Map<String, String>>() {}.getType());

    Message message = messageProcessor.process(event).getMessage();
    assertThat(message, is(notNullValue()));

    assertThat(message.getPayload().getValue(), is(sameInstance(payload)));
    DataType dataType = message.getPayload().getDataType();
    assertThat(dataType, instanceOf(MapDataType.class));
    assertThat(((MapDataType) dataType).getKeyDataType(), like(String.class, ANY.withCharset(null)));
    assertThat(((MapDataType) dataType).getValueDataType(), like(String.class, ANY));
  }

  @Test
  public void operationReturnsCollectionWithCorrectDataType() throws Exception {
    Object payload = new ArrayList<>();
    setUpOperationReturning(payload, new TypeToken<List<Integer>>() {}.getType());

    Message message = messageProcessor.process(event).getMessage();
    assertThat(message, is(notNullValue()));

    assertThat(message.getPayload().getValue(), is(sameInstance(payload)));
    DataType dataType = message.getPayload().getDataType();
    assertThat(dataType, instanceOf(CollectionDataType.class));
    assertThat(((CollectionDataType) dataType).getItemDataType(), like(Integer.class, ANY.withCharset(null)));
  }

  @Test
  public void operationReturnsResultCollectionWithCorrectDataType() throws Exception {
    Object payload = new ArrayList<>();
    setUpOperationReturning(Result.builder().output(payload).build(), new TypeToken<List<Integer>>() {}.getType());

    Message message = messageProcessor.process(event).getMessage();
    assertThat(message, is(notNullValue()));

    assertThat(message.getPayload().getValue(), is(sameInstance(payload)));
    DataType dataType = message.getPayload().getDataType();
    assertThat(dataType, instanceOf(CollectionDataType.class));
    assertThat(((CollectionDataType) dataType).getItemDataType(), like(Integer.class, ANY.withCharset(null)));
  }

  @Test
  public void operationReturnsPayloadValueWithTarget() throws Exception {
    target = TARGET_VAR;
    messageProcessor = setUpOperationMessageProcessor();

    Object value = new Object();
    stubComponentExecutor(operationExecutor, value);

    Message message = (Message) messageProcessor.process(event).getVariables().get(TARGET_VAR).getValue();
    assertThat(message, is(notNullValue()));
    assertThat(message.getPayload().getValue(), is(sameInstance(value)));
  }

  @Test
  public void operationWithExpressionInTargetParameter() throws Exception {
    String flowName = FLOW_NAME;
    target = "#[mel:someExpression]";
    messageProcessor = createOperationMessageProcessor();

    registerIntoMockContext(context, OBJECT_EXPRESSION_LANGUAGE, new MVELExpressionLanguage(context));
    registerIntoMockContext(context, DefaultExpressionLanguageFactoryService.class,
                            new WeaveDefaultExpressionLanguageFactoryService(null));
    doReturn(new DefaultExpressionManager()).when(context).getExpressionManager();
    FlowConstruct flowConstruct = mock(FlowConstruct.class);
    when(flowConstruct.getName()).thenReturn(flowName);

    messageProcessor.setMuleContext(context);
    context.getInjector().inject(messageProcessor);

    expectedException.expect(IllegalOperationException.class);
    expectedException.expectMessage(format(INVALID_TARGET_MESSAGE, flowName, operationModel.getName(), "an expression",
                                           TARGET_PARAMETER_NAME));

    messageProcessor.initialise();
  }

  @Test
  public void operationWithoutExpressionInTargetValueParameter() throws Exception {

    String flowName = "flowName";
    target = TARGET_VAR;
    targetValue = TARGET_VAR;
    messageProcessor = createOperationMessageProcessor();

    registerIntoMockContext(context, OBJECT_EXPRESSION_LANGUAGE, new MVELExpressionLanguage(context));
    registerIntoMockContext(context, DefaultExpressionLanguageFactoryService.class,
                            new WeaveDefaultExpressionLanguageFactoryService(null));
    doReturn(new DefaultExpressionManager()).when(context)
        .getExpressionManager();
    FlowConstruct flowConstruct = mock(FlowConstruct.class);
    when(flowConstruct.getName()).thenReturn(flowName);

    messageProcessor.setMuleContext(context);
    context.getInjector().inject(messageProcessor);

    expectedException.expect(IllegalOperationException.class);
    expectedException
        .expectMessage(format(INVALID_TARGET_MESSAGE, flowName, operationModel.getName(), "something that is not an expression",
                              TARGET_VALUE_PARAMETER_NAME));

    messageProcessor.initialise();
  }

  @Test
  public void operationIsVoid() throws Exception {
    when(operationModel.getOutput())
        .thenReturn(new ImmutableOutputModel("Message.Payload", toMetadataType(void.class), false, emptySet()));
    messageProcessor = setUpOperationMessageProcessor();

    stubComponentExecutor(operationExecutor, null);
    assertThat(messageProcessor.process(event), is(sameInstance(event)));
  }

  @Test
  public void executesWithDefaultConfig() throws Exception {
    configurationName = null;
    messageProcessor = setUpOperationMessageProcessor();

    Object defaultConfigInstance = new Object();
    when(configurationInstance.getValue()).thenReturn(defaultConfigInstance);
    when(extensionManager.getConfiguration(extensionModel, operationModel, event)).thenReturn(of(configurationInstance));

    ArgumentCaptor<ExecutionContext> operationContextCaptor = ArgumentCaptor.forClass(ExecutionContext.class);
    messageProcessor.process(event);

    verify(operationExecutor).execute(operationContextCaptor.capture(), any());

    ExecutionContext<OperationModel> executionContext = operationContextCaptor.getValue();

    assertThat(executionContext, is(instanceOf(ExecutionContextAdapter.class)));
    assertThat(executionContext.getConfiguration().get().getValue(), is(sameInstance(defaultConfigInstance)));
  }

  @Test
  public void executeWithPolicy() throws Exception {
    // In order to execute policies, operation processor must have a component location
    messageProcessor.setAnnotations(singletonMap(LOCATION_KEY, TEST_CONNECTOR_LOCATION));
    messageProcessor.process(event);

    verify(mockPolicyManager).createOperationPolicy(eq(messageProcessor), same(event), any(OperationParametersProcessor.class));
    verify(mockOperationPolicy).process(same(event), any(OperationExecutionFunction.class), any(), any(), any());
  }

  @Test
  public void skipPolicyWithNoComponentLocation() throws Exception {
    messageProcessor.setAnnotations(new HashMap<>());
    messageProcessor.process(event);

    assertThat(mockOperationPolicy, is(nullValue()));
    verify(mockPolicyManager, never()).createOperationPolicy(eq(messageProcessor), same(event),
                                                             any(OperationParametersProcessor.class));
  }

  @Test
  public void getMetadataKeyIdObjectValue() throws MuleException, ValueResolvingException {
    setUpValueResolvers();
    final Object metadataKeyValue = messageProcessor.getParameterValueResolver().getParameterValue(SOME_PARAM_NAME);
    assertThat(metadataKeyValue, is("person"));
  }

  @Test
  public void getProcessingType() {
    assertProcessingType(CPU_INTENSIVE, ProcessingType.CPU_INTENSIVE);
    assertProcessingType(CPU_LITE, ProcessingType.CPU_LITE);
    assertProcessingType(BLOCKING, ProcessingType.BLOCKING);
  }

  @Test
  public void precalculateExecutionContextForInterceptedProcessor() throws MuleException {
    final AtomicReference<PrecalculatedExecutionContextAdapter> context = new AtomicReference<>();

    messageProcessor.resolveParameters(CoreEvent.builder(event), (params, ctx) -> {
      assertThat(ctx, instanceOf(PrecalculatedExecutionContextAdapter.class));
      context.set(spy((PrecalculatedExecutionContextAdapter) ctx));
    });

    messageProcessor.process(quickCopy(event, of(INTERCEPTION_RESOLVED_CONTEXT, context.get(),
                                                 "core:interceptionComponent", messageProcessor)));

    verify(operationExecutor).execute(same(context.get()), any());
    verify(context.get(), atLeastOnce()).getConfiguration();
    messageProcessor.disposeResolvedParameters(context.get());
  }

  @Test
  public void newExecutionContextForNonInterceptedProcessor() throws MuleException {
    final AtomicReference<PrecalculatedExecutionContextAdapter> context = new AtomicReference<>();

    Map<String, String> newContextParameters = of(MIME_TYPE_PARAMETER_NAME, MediaType.ANY.toRfcString(),
                                                  ENCODING_PARAMETER_NAME, Charset.defaultCharset().name());
    doReturn(newContextParameters).when(parameters).asMap();
    doReturn(parameters).when(resolverSet).resolve(any(ValueResolvingContext.class));
    messageProcessor.resolveParameters(CoreEvent.builder(event), (params, ctx) -> {
      assertThat(ctx, instanceOf(PrecalculatedExecutionContextAdapter.class));
      context.set(spy((PrecalculatedExecutionContextAdapter) ctx));
    });

    messageProcessor.process(quickCopy(event, of(INTERCEPTION_RESOLVED_CONTEXT, context.get())));

    verify(operationExecutor, never()).execute(same(context.get()), any());
    verify(operationExecutor).execute(any(), any());
    verify(context.get(), never()).getConfiguration();
    messageProcessor.disposeResolvedParameters(context.get());
  }

  @Test
  public void cursorStreamProvidersAreManaged() throws Exception {
    CursorStreamProvider provider = mock(CursorStreamProvider.class);
    final InputStream inputStream = mock(InputStream.class);

    doReturn(provider).when(cursorStreamProviderFactory).of(getRoot(event.getContext()), inputStream);
    stubComponentExecutor(operationExecutor, inputStream);

    messageProcessor.process(event);
    verify(streamingManager).manage(same(provider), any(EventContext.class));
  }

  private void assertProcessingType(ExecutionType executionType, ProcessingType expectedProcessingType) {
    when(operationModel.getExecutionType()).thenReturn(executionType);
    assertThat(messageProcessor.getInnerProcessingType(), is(expectedProcessingType));
    assertThat(messageProcessor.getProcessingType(), is(ProcessingType.CPU_LITE));
  }

  private void setUpValueResolvers() throws MuleException {
    final Map<String, ValueResolver<?>> valueResolvers = new HashMap<>();
    when(resolverSet.getResolvers()).thenReturn(valueResolvers);
    final ValueResolver valueResolver = mock(ValueResolver.class);
    valueResolvers.put(SOME_PARAM_NAME, valueResolver);
    when(valueResolver.resolve(any(ValueResolvingContext.class))).thenReturn("person");
  }

  private void setUpOperationReturning(Object payload, Type type) throws MuleException {
    after();

    messageProcessor = createOperationMessageProcessor();
    MetadataType mapType = new DefaultExtensionsTypeLoaderFactory().createTypeLoader().load(type);
    when(operationModel.getOutput()).thenReturn(new ImmutableOutputModel("desc", mapType, false, emptySet()));
    initialiseIfNeeded(messageProcessor, muleContext);
    startIfNeeded(messageProcessor);
    stubComponentExecutor(operationExecutor, payload);
  }

  private void stubResultComponentExecutor(Object payload, MediaType mediaType, Object attributes) {
    stubComponentExecutor(operationExecutor, builder()
        .output(payload)
        .mediaType(mediaType)
        .attributes(attributes)
        .build());
  }
}
