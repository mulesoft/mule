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
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
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
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_LANGUAGE;
import static org.mule.runtime.core.api.util.SystemUtils.getDefaultEncoding;
import static org.mule.runtime.core.internal.interception.DefaultInterceptionEvent.INTERCEPTION_RESOLVED_CONTEXT;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_VALUE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.runtime.operation.Result.builder;
import static org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor.INVALID_TARGET_MESSAGE;
import static org.mule.tck.junit4.matcher.MetadataKeyMatcher.metadataKeyWithId;
import static org.mule.tck.util.MuleContextUtils.registerIntoMockContext;
import static org.mule.test.metadata.extension.resolver.TestNoConfigMetadataResolver.KeyIds.BOOLEAN;
import static org.mule.test.metadata.extension.resolver.TestNoConfigMetadataResolver.KeyIds.STRING;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType;
import org.mule.runtime.core.api.retry.policy.NoRetryPolicyTemplate;
import org.mule.runtime.core.internal.el.DefaultExpressionManager;
import org.mule.runtime.core.internal.el.mvel.MVELExpressionLanguage;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.internal.policy.OperationExecutionFunction;
import org.mule.runtime.extension.api.model.ImmutableOutputModel;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.ValueResolvingException;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.tck.size.SmallTest;
import org.mule.weave.v2.el.WeaveDefaultExpressionLanguageFactoryService;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class OperationMessageProcessorTestCase extends AbstractOperationMessageProcessorTestCase {

  private static final String SOME_PARAM_NAME = "someParam";
  private static final String FLOW_NAME = "flowName";

  @Rule
  public ExpectedException expectedException = none();

  @Override
  protected OperationMessageProcessor createOperationMessageProcessor() {
    OperationMessageProcessor operationMessageProcessor =
        new OperationMessageProcessor(extensionModel, operationModel, configurationProvider, target, targetValue, resolverSet,
                                      cursorStreamProviderFactory, new NoRetryPolicyTemplate(), extensionManager,
                                      mockPolicyManager);
    operationMessageProcessor.setAnnotations(getFlowComponentLocationAnnotations(FLOW_NAME));
    return operationMessageProcessor;
  }

  @Test
  public void operationContextIsWellFormed() throws Exception {
    ArgumentCaptor<ExecutionContext> operationContextCaptor = ArgumentCaptor.forClass(ExecutionContext.class);
    messageProcessor.process(event);

    verify(operationExecutor).execute(operationContextCaptor.capture());
    ExecutionContext<ComponentModel> executionContext = operationContextCaptor.getValue();

    assertThat(executionContext, is(instanceOf(ExecutionContextAdapter.class)));
    ExecutionContextAdapter<ComponentModel> executionContextAdapter = (ExecutionContextAdapter) executionContext;

    assertThat(executionContextAdapter.getEvent(), is(sameInstance(event)));
    assertThat(executionContextAdapter.getConfiguration().get().getValue(), is(sameInstance(configuration)));
  }

  @Test
  public void operationExecutorIsInvoked() throws Exception {
    messageProcessor.process(event);
    verify(operationExecutor).execute(any(ExecutionContext.class));
  }

  @Test
  public void operationReturnsOperationResultWhichKeepsNoValues() throws Exception {
    Object payload = new Object();
    MediaType mediaType = ANY.withCharset(getDefaultEncoding(context));
    Object attributes = mock(Object.class);

    when(operationExecutor.execute(any(ExecutionContext.class)))
        .thenReturn(just(builder().output(payload).mediaType(mediaType).attributes(attributes).build()));

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

    when(operationExecutor.execute(any(ExecutionContext.class)))
        .thenReturn(just(builder().output(payload).mediaType(mediaType).attributes(attributes).build()));

    InternalMessage message = (InternalMessage) messageProcessor.process(event).getVariables().get(TARGET_VAR).getValue();
    assertThat(message, is(notNullValue()));

    assertThat(message.getPayload().getValue(), is(sameInstance(payload)));
    assertThat(message.getAttributes().getValue(), is(sameInstance(attributes)));
    assertThat(message.getPayload().getDataType().getMediaType(), equalTo(mediaType));
  }

  @Test
  public void operationReturnsOperationResultButKeepsAttributes() throws Exception {
    Object payload = new Object();
    MediaType mediaType = ANY.withCharset(getDefaultEncoding(context));

    when(operationExecutor.execute(any(ExecutionContext.class)))
        .thenReturn(just(builder().output(payload).mediaType(mediaType).build()));

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

    when(operationExecutor.execute(any(ExecutionContext.class))).thenReturn(just(builder().output(payload).build()));
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

    when(operationExecutor.execute(any(ExecutionContext.class)))
        .thenReturn(just(builder().output(payload).attributes(attributes).build()));

    Message message = messageProcessor.process(event).getMessage();
    assertThat(message, is(notNullValue()));

    assertThat(message.getPayload().getValue(), is(sameInstance(payload)));
    assertThat(message.getAttributes().getValue(), is(sameInstance(attributes)));
    assertThat(message.getPayload().getDataType().getType().equals(String.class), is(true));
  }

  @Test
  public void operationReturnsPayloadValue() throws Exception {
    Object value = new Object();
    when(operationExecutor.execute(any(ExecutionContext.class))).thenReturn(just(value));

    Message message = messageProcessor.process(event).getMessage();
    assertThat(message, is(notNullValue()));
    assertThat(message.getPayload().getValue(), is(sameInstance(value)));
  }

  @Test
  public void operationReturnsPayloadValueWithTarget() throws Exception {
    target = TARGET_VAR;
    messageProcessor = setUpOperationMessageProcessor();

    Object value = new Object();
    when(operationExecutor.execute(any(ExecutionContext.class))).thenReturn(just(value));

    InternalMessage message = (InternalMessage) messageProcessor.process(event).getVariables().get(TARGET_VAR).getValue();
    assertThat(message, is(notNullValue()));
    assertThat(message.getPayload().getValue(), is(sameInstance(value)));
  }

  @Test
  public void operationWithExpressionInTargetParameter() throws Exception {

    String flowName = FLOW_NAME;
    expectedException.expect(IllegalOperationException.class);
    expectedException.expectMessage(format(INVALID_TARGET_MESSAGE, flowName, operationModel.getName(), "an expression",
                                           TARGET_PARAMETER_NAME));

    target = "#[mel:someExpression]";
    messageProcessor = createOperationMessageProcessor();

    registerIntoMockContext(context, OBJECT_EXPRESSION_LANGUAGE, new MVELExpressionLanguage(context));
    registerIntoMockContext(context, DefaultExpressionLanguageFactoryService.class,
                            new WeaveDefaultExpressionLanguageFactoryService());
    doReturn(new DefaultExpressionManager()).when(context).getExpressionManager();
    FlowConstruct flowConstruct = mock(FlowConstruct.class);
    when(flowConstruct.getName()).thenReturn(flowName);

    messageProcessor.setMuleContext(context);
    messageProcessor.initialise();
  }

  @Test
  public void operationWithoutExpressionInTargetValueParameter() throws Exception {

    String flowName = "flowName";
    expectedException.expect(IllegalOperationException.class);
    expectedException
        .expectMessage(format(INVALID_TARGET_MESSAGE, flowName, operationModel.getName(), "something that is not an expression",
                              TARGET_VALUE_PARAMETER_NAME));

    target = TARGET_VAR;
    targetValue = TARGET_VAR;
    messageProcessor = createOperationMessageProcessor();

    when(context.getRegistry().lookupObject(OBJECT_EXPRESSION_LANGUAGE)).thenReturn(new MVELExpressionLanguage(context));
    when(context.getRegistry().lookupObject(DefaultExpressionLanguageFactoryService.class))
        .thenReturn(new WeaveDefaultExpressionLanguageFactoryService());
    doReturn(new DefaultExpressionManager()).when(context)
        .getExpressionManager();
    FlowConstruct flowConstruct = mock(FlowConstruct.class);
    when(flowConstruct.getName()).thenReturn(flowName);

    messageProcessor.setMuleContext(context);
    messageProcessor.initialise();
  }

  @Test
  public void operationIsVoid() throws Exception {
    when(operationModel.getOutput())
        .thenReturn(new ImmutableOutputModel("Message.Payload", toMetadataType(void.class), false, emptySet()));
    messageProcessor = setUpOperationMessageProcessor();

    when(operationExecutor.execute(any(ExecutionContext.class))).thenReturn(empty());
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
    verify(operationExecutor).execute(operationContextCaptor.capture());

    ExecutionContext<OperationModel> executionContext = operationContextCaptor.getValue();

    assertThat(executionContext, is(instanceOf(ExecutionContextAdapter.class)));
    assertThat(executionContext.getConfiguration().get().getValue(), is(sameInstance(defaultConfigInstance)));
  }

  @Test
  public void executeWithPolicy() throws Exception {
    // In order to execute policies, operation processor must have a component location
    messageProcessor.setAnnotations(singletonMap(LOCATION_KEY, TEST_CONNECTOR_LOCATION));
    messageProcessor.process(event);

    verify(mockPolicyManager).createOperationPolicy(eq(messageProcessor), same(event), any(Map.class),
                                                    any(OperationExecutionFunction.class));
    verify(mockOperationPolicy).process(same(event));
  }

  @Test
  public void skipPolicyWithNoComponentLocation() throws Exception {
    messageProcessor.setAnnotations(new HashMap<>());
    messageProcessor.process(event);

    assertThat(mockOperationPolicy, is(nullValue()));
    verify(mockPolicyManager, never()).createOperationPolicy(eq(messageProcessor), same(event), any(Map.class),
                                                             any(OperationExecutionFunction.class));
  }

  @Test
  public void getMetadataKeyIdObjectValue() throws MuleException, ValueResolvingException {
    setUpValueResolvers();
    final Object metadataKeyValue = messageProcessor.getParameterValueResolver().getParameterValue(SOME_PARAM_NAME);
    assertThat(metadataKeyValue, is("person"));
  }

  @Test
  public void getMetadataKeys() throws Exception {
    MetadataResult<MetadataKeysContainer> metadataKeysResult = messageProcessor.getMetadataKeys();

    verify(metadataResolverFactory).getKeyResolver();

    assertThat(metadataKeysResult.isSuccess(), is(true));
    final Set<MetadataKey> metadataKeys = getKeysFromContainer(metadataKeysResult.get());
    assertThat(metadataKeys.size(), is(2));

    assertThat(metadataKeys, hasItem(metadataKeyWithId(BOOLEAN.name())));
    assertThat(metadataKeys, hasItem(metadataKeyWithId(STRING.name())));
  }

  @Test
  public void getProcessingType() {
    assertProcessingType(CPU_INTENSIVE, ProcessingType.CPU_INTENSIVE);
    assertProcessingType(CPU_LITE, ProcessingType.CPU_LITE);
    assertProcessingType(BLOCKING, ProcessingType.BLOCKING);
  }

  @Test
  public void precalculateExecutionContext() throws MuleException {
    final AtomicReference<PrecalculatedExecutionContextAdapter> context = new AtomicReference<>();

    messageProcessor.resolveParameters(CoreEvent.builder(event), (params, ctx) -> {
      assertThat(ctx, instanceOf(PrecalculatedExecutionContextAdapter.class));
      context.set(spy((PrecalculatedExecutionContextAdapter) ctx));
    });

    messageProcessor.process(InternalEvent.builder(event)
        .internalParameters(singletonMap(INTERCEPTION_RESOLVED_CONTEXT, context.get()))
        .build());

    verify(operationExecutor).execute(any(ExecutionContext.class));
    messageProcessor.disposeResolvedParameters(context.get());
  }

  @Test
  public void cursorStreamProvidersAreManaged() throws Exception {
    CursorStreamProvider provider = mock(CursorStreamProvider.class);
    final InputStream inputStream = mock(InputStream.class);

    doReturn(provider).when(cursorStreamProviderFactory).of(event, inputStream);
    doReturn(provider).when(streamingManager).manage(provider, event);
    when(operationExecutor.execute(any())).thenReturn(just(inputStream));

    messageProcessor.process(event);
    verify(streamingManager).manage(same(provider), any());
  }

  private void assertProcessingType(ExecutionType executionType, ProcessingType expectedProcessingType) {
    when(operationModel.getExecutionType()).thenReturn(executionType);
    assertThat(messageProcessor.getProcessingType(), is(expectedProcessingType));
  }


  private Set<MetadataKey> getKeysFromContainer(MetadataKeysContainer metadataKeysContainer) {
    return metadataKeysContainer.getKeys(metadataKeysContainer.getCategories().iterator().next()).get();
  }

  private void setUpValueResolvers() throws MuleException {
    final Map<String, ValueResolver<?>> valueResolvers = mock(Map.class);
    when(resolverSet.getResolvers()).thenReturn(valueResolvers);
    final ValueResolver valueResolver = mock(ValueResolver.class);
    when(valueResolvers.get(eq(SOME_PARAM_NAME))).thenReturn(valueResolver);
    when(valueResolvers.containsKey(eq(SOME_PARAM_NAME))).thenReturn(true);
    when(valueResolver.resolve(any(ValueResolvingContext.class))).thenReturn("person");
  }
}
