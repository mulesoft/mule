/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.api.meta.model.operation.ExecutionType.BLOCKING;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.CONTENT;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STREAMING_MANAGER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.retry.ReconnectionConfig.defaultReconnectionConfig;
import static org.mule.runtime.metadata.internal.cache.MetadataCacheManager.METADATA_CACHE_MANAGER_KEY;
import static org.mule.tck.MuleTestUtils.stubComponentExecutor;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.getDefaultCursorStreamProviderFactory;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockClassLoaderModelProperty;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockExceptionEnricher;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockExecutorFactory;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockMetadataResolverFactory;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockParameters;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockSubTypes;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.setRequires;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.visitableMock;

import static java.nio.charset.Charset.defaultCharset;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Answers.RETURNS_MOCKS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.OutputModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.retry.policy.NoRetryPolicyTemplate;
import org.mule.runtime.core.api.streaming.DefaultStreamingManager;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.streaming.bytes.CursorStreamProviderFactory;
import org.mule.runtime.core.api.util.UUID;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.core.internal.connection.ConnectionProviderWrapper;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.internal.policy.OperationExecutionFunction;
import org.mule.runtime.core.internal.policy.OperationParametersProcessor;
import org.mule.runtime.core.internal.policy.OperationPolicy;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.metadata.NullMetadataResolver;
import org.mule.runtime.extension.api.model.ImmutableOutputModel;
import org.mule.runtime.extension.api.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.property.MetadataKeyPartModelProperty;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.exception.SdkExceptionHandlerFactory;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutorFactory;
import org.mule.runtime.metadata.api.cache.MetadataCacheId;
import org.mule.runtime.metadata.api.cache.MetadataCacheIdGenerator;
import org.mule.runtime.metadata.api.cache.MetadataCacheIdGeneratorFactory;
import org.mule.runtime.metadata.internal.cache.MetadataCacheManager;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.loader.java.property.FieldOperationParameterModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.QueryParameterModelProperty;
import org.mule.runtime.module.extension.internal.runtime.exception.NullExceptionHandler;
import org.mule.runtime.module.extension.internal.runtime.execution.OperationArgumentResolverFactory;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.test.metadata.extension.resolver.TestNoConfigMetadataResolver;

import java.util.Collections;
import java.util.List;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractOperationMessageProcessorTestCase extends AbstractMuleContextTestCase {

  protected static final String EXTENSION_NAMESPACE = "extension_namespace";
  protected static final String CONFIG_NAME = "config";
  protected static final String OPERATION_NAME = "operation";
  protected static final String TARGET_VAR = "myFlowVar";
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOperationMessageProcessorTestCase.class);
  @Rule
  public MockitoRule rule = MockitoJUnit.rule().silent();

  @Mock(answer = RETURNS_MOCKS, lenient = true)
  protected ExtensionModel extensionModel;

  @Mock(answer = RETURNS_DEEP_STUBS, lenient = true)
  protected ConfigurationModel configurationModel;

  @Mock(answer = RETURNS_DEEP_STUBS, lenient = true)
  protected OperationModel operationModel;

  @Mock(lenient = true)
  protected ExtensionManager extensionManager;

  @Mock(lenient = true)
  protected ConnectionManagerAdapter connectionManagerAdapter;

  @Mock(lenient = true)
  protected CompletableComponentExecutorFactory operationExecutorFactory;

  @Mock(lenient = true)
  protected CompletableComponentExecutorOperationArgumentResolverFactory operationExecutor;

  @Mock(lenient = true)
  protected ExecutorCallback executorCallback;

  @Mock(answer = RETURNS_DEEP_STUBS, lenient = true)
  protected ResolverSet resolverSet;

  @Mock(lenient = true)
  protected ResolverSetResult parameters;

  protected CoreEvent event;

  @Mock(answer = RETURNS_DEEP_STUBS, lenient = true)
  protected InternalMessage message;

  @Mock(answer = RETURNS_DEEP_STUBS, lenient = true)
  protected MuleContextWithRegistry context;

  @Mock(lenient = true)
  protected ConfigurationInstance configurationInstance;

  @Mock(lenient = true)
  protected Object configuration;

  @Mock(lenient = true)
  protected SdkExceptionHandlerFactory exceptionHandlerFactory;

  @Mock(lenient = true)
  protected MetadataResolverFactory metadataResolverFactory;

  @Mock(lenient = true)
  protected ConnectionProviderWrapper connectionProviderWrapper;

  @Mock(lenient = true)
  protected ParameterModel contentMock;

  @Mock(lenient = true)
  protected ParameterModel keyParamMock;

  @Mock(lenient = true)
  protected OutputModel outputMock;

  @Mock(lenient = true)
  protected StringType stringType;

  @Mock(lenient = true)
  protected ConfigurationProvider configurationProvider;

  protected ParameterGroupModel parameterGroupModel;

  @Mock(answer = RETURNS_DEEP_STUBS, lenient = true)
  protected PolicyManager mockPolicyManager;
  protected OperationMessageProcessor messageProcessor;
  protected CursorStreamProviderFactory cursorStreamProviderFactory;
  protected String configurationName = CONFIG_NAME;
  protected String target = EMPTY;
  protected String targetValue = "#[message]";
  protected OperationPolicy mockOperationPolicy;
  protected StreamingManager streamingManager = spy(new DefaultStreamingManager());
  @Mock(lenient = true)
  private ExecutionContextAdapter<OperationModel> executionContext;
  @Mock(lenient = true)
  private MetadataCacheIdGeneratorFactory<ComponentAst> cacheIdGeneratorFactory;
  @Mock(lenient = true)
  private MetadataCacheIdGenerator<ComponentAst> cacheIdGenerator;
  @Mock(lenient = true)
  private MetadataCacheManager metadataCacheManager;

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    super.addBuilders(builders);
    builders.add(new AbstractConfigurationBuilder() {

      @Override
      protected void doConfigure(MuleContext muleContext) throws Exception {
        muleContext.getCustomizationService().overrideDefaultServiceImpl(MuleProperties.OBJECT_POLICY_MANAGER,
                                                                         mockPolicyManager);
      }
    });
  }

  @Before
  public void before() throws Exception {
    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject(OBJECT_STREAMING_MANAGER, streamingManager);
    cursorStreamProviderFactory = spy(getDefaultCursorStreamProviderFactory(streamingManager));
    event = configureEvent();
    when(context.getInjector().inject(any())).thenAnswer(invocationOnMock -> {
      final Object subject = invocationOnMock.getArguments()[0];
      muleContext.getInjector().inject(subject);
      return subject;
    });

    when(extensionModel.getName()).thenReturn(EXTENSION_NAMESPACE);
    when(extensionModel.getConfigurationModels()).thenReturn(asList(configurationModel));
    when(operationModel.getName()).thenReturn(getClass().getName());
    when(operationModel.isBlocking()).thenReturn(true);
    when(operationModel.getExecutionType()).thenReturn(BLOCKING);
    when(extensionModel.getXmlDslModel()).thenReturn(XmlDslModel.builder().setPrefix("test-extension").build());
    when(operationModel.getOutput())
        .thenReturn(new ImmutableOutputModel("Message.Payload", toMetadataType(String.class), false, emptySet()));
    mockExecutorFactory(operationModel, operationExecutorFactory);
    visitableMock(operationModel);
    when(operationModel.getModelProperty(MetadataKeyIdModelProperty.class)).thenReturn(
                                                                                       of(new MetadataKeyIdModelProperty(ExtensionsTypeLoaderFactory
                                                                                           .getDefault().createTypeLoader()
                                                                                           .load(String.class), "someParam")));
    setRequires(operationModel, true, true);
    when(operationExecutorFactory.createExecutor(same(operationModel), anyMap())).thenReturn(operationExecutor);
    when(operationExecutor.createArgumentResolver(any())).thenReturn(ctx -> emptyMap());
    stubComponentExecutor(operationExecutor, "");

    when(operationModel.getName()).thenReturn(OPERATION_NAME);
    when(operationModel.getDisplayModel()).thenReturn(empty());
    when(operationModel.getModelProperty(MediaTypeModelProperty.class)).thenReturn(empty());

    mockExceptionEnricher(operationModel, exceptionHandlerFactory);
    when(exceptionHandlerFactory.createHandler()).thenReturn(new NullExceptionHandler());

    mockMetadataResolverFactory(operationModel, metadataResolverFactory);
    when(metadataResolverFactory.getKeyResolver()).thenReturn(new TestNoConfigMetadataResolver());
    when(metadataResolverFactory.getInputResolver("content")).thenReturn(new TestNoConfigMetadataResolver());
    when(metadataResolverFactory.getInputResolver("type")).thenReturn(new NullMetadataResolver());
    when(metadataResolverFactory.getOutputResolver()).thenReturn(new TestNoConfigMetadataResolver());
    when(metadataResolverFactory.getOutputAttributesResolver()).thenReturn(new TestNoConfigMetadataResolver());
    when(metadataResolverFactory.getQueryEntityResolver()).thenReturn(new TestNoConfigMetadataResolver());

    when(keyParamMock.getName()).thenReturn("type");
    when(keyParamMock.getType()).thenReturn(stringType);
    when(keyParamMock.getModelProperty(MetadataKeyPartModelProperty.class))
        .thenReturn(of(new MetadataKeyPartModelProperty(1)));
    when(keyParamMock.getRole()).thenReturn(BEHAVIOUR);
    when(keyParamMock.getDisplayModel()).thenReturn(empty());
    when(keyParamMock.getLayoutModel()).thenReturn(empty());
    when(keyParamMock.getModelProperty(QueryParameterModelProperty.class)).thenReturn(empty());
    when(keyParamMock.getModelProperty(FieldOperationParameterModelProperty.class)).thenReturn(empty());

    when(contentMock.getName()).thenReturn("content");
    when(contentMock.hasDynamicType()).thenReturn(true);
    when(contentMock.getType()).thenReturn(stringType);
    when(contentMock.getRole()).thenReturn(CONTENT);
    when(contentMock.getDisplayModel()).thenReturn(empty());
    when(contentMock.getLayoutModel()).thenReturn(empty());
    when(contentMock.getModelProperty(MetadataKeyPartModelProperty.class)).thenReturn(empty());
    when(contentMock.getModelProperty(QueryParameterModelProperty.class)).thenReturn(empty());
    when(contentMock.getModelProperty(FieldOperationParameterModelProperty.class)).thenReturn(empty());

    parameterGroupModel = mockParameters(operationModel, keyParamMock, contentMock);
    when(parameterGroupModel.getDisplayModel()).thenReturn(empty());
    when(parameterGroupModel.getLayoutModel()).thenReturn(empty());
    when(parameterGroupModel.getModelProperty(MetadataKeyIdModelProperty.class)).thenReturn(empty());

    when(outputMock.getType()).thenReturn(stringType);
    when(outputMock.hasDynamicType()).thenReturn(true);
    when(operationModel.getOutput()).thenReturn(outputMock);
    when(operationModel.getOutputAttributes()).thenReturn(outputMock);

    when(extensionManager.getExtensions()).thenReturn(Collections.singleton(extensionModel));

    when(cacheIdGeneratorFactory.create(any(), any())).thenReturn(cacheIdGenerator);

    Answer<Object> cacheIdAnswer = invocation -> of(new MetadataCacheId(UUID.getUUID(), null));

    when(cacheIdGenerator.getIdForComponentMetadata(any())).then(cacheIdAnswer);
    when(cacheIdGenerator.getIdForGlobalMetadata(any())).then(cacheIdAnswer);
    when(cacheIdGenerator.getIdForMetadataKeys(any())).then(cacheIdAnswer);

    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject("metadata.cache.id.model.generator.factory",
                                                                         cacheIdGeneratorFactory);

    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject(METADATA_CACHE_MANAGER_KEY,
                                                                         metadataCacheManager);

    when(resolverSet.getResolvers()).thenReturn(Collections.emptyMap());
    when(resolverSet.resolve(argThat(new BaseMatcher<ValueResolvingContext>() {

      @Override
      public boolean matches(Object item) {
        if (!(item instanceof ValueResolvingContext)) {
          return false;
        }

        ValueResolvingContext vrCtx = (ValueResolvingContext) item;
        return vrCtx.getEvent() == event;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Not the expected Event");
      }
    }))).thenReturn(parameters);


    when(configurationInstance.getName()).thenReturn(CONFIG_NAME);
    when(configurationInstance.getModel()).thenReturn(configurationModel);
    when(configurationInstance.getValue()).thenReturn(configuration);
    when(configurationInstance.getConnectionProvider()).thenReturn(of(connectionProviderWrapper));

    when(configurationProvider.get(event)).thenReturn(configurationInstance);
    when(configurationProvider.getConfigurationModel()).thenReturn(configurationModel);
    when(configurationProvider.getName()).thenReturn(configurationName);

    when(configurationModel.getOperationModels()).thenReturn(asList(operationModel));
    when(configurationModel.getOperationModel(OPERATION_NAME)).thenReturn(of(operationModel));

    when(connectionProviderWrapper.getReconnectionConfig()).thenReturn(of(defaultReconnectionConfig()));
    when(connectionProviderWrapper.getRetryPolicyTemplate()).thenReturn(new NoRetryPolicyTemplate());

    mockSubTypes(extensionModel);
    mockClassLoaderModelProperty(extensionModel, getClass().getClassLoader());
    when(extensionManager.getConfiguration(anyString(), any())).thenReturn(configurationInstance);
    when(extensionManager.getConfiguration(extensionModel, operationModel, event)).thenReturn(of(configurationInstance));
    when(configurationProvider.get(any())).thenReturn(configurationInstance);
    when(extensionManager.getConfigurationProvider(extensionModel, operationModel)).thenReturn(of(configurationProvider));
    when(extensionManager.getConfigurationProvider(CONFIG_NAME)).thenReturn(of(configurationProvider));

    when(stringType.getAnnotation(any())).thenReturn(empty());

    when(mockPolicyManager.createOperationPolicy(any(), any(), any())).thenAnswer(invocationOnMock -> {
      if (mockOperationPolicy == null) {
        mockOperationPolicy = mock(OperationPolicy.class);
        doAnswer(invocation -> {
          ((OperationExecutionFunction) invocation.getArgument(1))
              .execute(((OperationParametersProcessor) invocationOnMock.getArgument(2)).getOperationParameters(),
                       invocationOnMock.getArgument(1),
                       invocation.getArgument(4));
          return null;
        }).when(mockOperationPolicy).process(any(), any(), any(), any(), any());
      }
      return mockOperationPolicy;
    });

    when(executionContext.getRetryPolicyTemplate()).thenReturn(empty());
    when(connectionManagerAdapter.getConnection(anyString())).thenReturn(null);
    when(connectionManagerAdapter.getReconnectionConfigFor(any())).thenReturn(defaultReconnectionConfig());
    messageProcessor = setUpOperationMessageProcessor();
  }

  @After
  public void after() throws MuleException {
    stopIfNeeded(messageProcessor);
    disposeIfNeeded(messageProcessor, LOGGER);
  }

  protected CoreEvent configureEvent() throws Exception {
    when(message.getPayload())
        .thenReturn(new TypedValue<>(TEST_PAYLOAD,
                                     DataType.builder().mediaType(MediaType.create("*", "*", defaultCharset())).build()));
    when(message.getAttributes())
        .thenReturn(new TypedValue<>(null, DataType.builder().fromObject(null).build()));
    return eventBuilder(muleContext).message(message).build();
  }

  protected OperationMessageProcessor setUpOperationMessageProcessor() throws Exception {
    after();
    OperationMessageProcessor messageProcessor = createOperationMessageProcessor();
    messageProcessor.setMuleContext(context);
    messageProcessor.setMuleConfiguration(context.getConfiguration());
    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject(OBJECT_CONNECTION_MANAGER, connectionManagerAdapter);

    initialiseIfNeeded(messageProcessor, muleContext);

    // Since the initialization of the message processor reassigns its inner resolver set, we need to set the spy back.
    messageProcessor.resolverSet = resolverSet;

    startIfNeeded(messageProcessor);

    return messageProcessor;
  }

  protected abstract OperationMessageProcessor createOperationMessageProcessor() throws MuleException;

  @Test
  public void initialise() throws Exception {
    verify((MuleContextAware) operationExecutor, atLeastOnce()).setMuleContext(any(MuleContext.class));
    verify((Initialisable) operationExecutor).initialise();
  }

  @Test
  public void start() throws Exception {
    verify((Startable) operationExecutor).start();
  }

  @Test
  public void stopAndDispose() throws Exception {
    messageProcessor.stop();
    messageProcessor.dispose();

    verify((Stoppable) operationExecutor).stop();
    verify((Disposable) operationExecutor).dispose();
  }

  public interface CompletableComponentExecutorOperationArgumentResolverFactory
      extends CompletableComponentExecutor, Lifecycle, MuleContextAware, OperationArgumentResolverFactory {

  }

}
