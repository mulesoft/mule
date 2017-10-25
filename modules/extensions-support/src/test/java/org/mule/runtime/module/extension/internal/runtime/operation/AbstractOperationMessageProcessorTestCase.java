/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.nio.charset.Charset.defaultCharset;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.CONTENT;
import static org.mule.runtime.api.util.ExtensionModelTestUtils.visitableMock;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STREAMING_MANAGER;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext.from;
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
import static reactor.core.publisher.Mono.just;

import org.mule.metadata.api.model.StringType;
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
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.retry.policy.NoRetryPolicyTemplate;
import org.mule.runtime.core.api.streaming.DefaultStreamingManager;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.streaming.bytes.CursorStreamProviderFactory;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.core.internal.connection.ConnectionProviderWrapper;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.internal.policy.OperationExecutionFunction;
import org.mule.runtime.core.internal.policy.OperationPolicy;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.metadata.NullMetadataResolver;
import org.mule.runtime.extension.api.model.ImmutableOutputModel;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.exception.ExceptionHandlerFactory;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutorFactory;
import org.mule.runtime.extension.api.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.property.MetadataKeyPartModelProperty;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.loader.java.property.InterceptorsModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.QueryParameterModelProperty;
import org.mule.runtime.module.extension.internal.runtime.exception.NullExceptionHandler;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.test.metadata.extension.resolver.TestNoConfigMetadataResolver;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractOperationMessageProcessorTestCase extends AbstractMuleContextTestCase {

  protected static final String EXTENSION_NAMESPACE = "extension_namespace";
  protected static final String CONFIG_NAME = "config";
  protected static final String OPERATION_NAME = "operation";
  protected static final String TARGET_VAR = "myFlowVar";

  @Mock(answer = RETURNS_DEEP_STUBS)
  protected ExtensionModel extensionModel;

  @Mock(answer = RETURNS_DEEP_STUBS)
  protected ConfigurationModel configurationModel;

  @Mock(answer = RETURNS_DEEP_STUBS)
  protected OperationModel operationModel;

  @Mock
  protected ExtensionManager extensionManager;

  @Mock
  protected ConnectionManagerAdapter connectionManagerAdapter;
  @Mock
  protected ComponentExecutorFactory operationExecutorFactory;

  @Mock(extraInterfaces = {Lifecycle.class, MuleContextAware.class})
  protected ComponentExecutor operationExecutor;

  @Mock(answer = RETURNS_DEEP_STUBS)
  protected ResolverSet resolverSet;

  @Mock
  protected ResolverSetResult parameters;

  protected CoreEvent event;

  @Mock(answer = RETURNS_DEEP_STUBS)
  protected InternalMessage message;

  @Mock(answer = RETURNS_DEEP_STUBS)
  protected MuleContextWithRegistries context;

  @Mock
  protected ConfigurationInstance configurationInstance;

  @Mock
  protected Object configuration;

  @Mock
  protected ExceptionHandlerFactory exceptionHandlerFactory;

  @Mock
  protected MetadataResolverFactory metadataResolverFactory;

  @Mock
  protected ConnectionProviderWrapper connectionProviderWrapper;

  @Mock
  protected ParameterModel contentMock;

  @Mock
  protected ParameterModel keyParamMock;

  @Mock
  protected OutputModel outputMock;

  @Mock
  protected StringType stringType;

  @Mock
  protected ConfigurationProvider configurationProvider;

  protected ParameterGroupModel parameterGroupModel;

  @Mock(answer = RETURNS_DEEP_STUBS)
  protected PolicyManager mockPolicyManager;

  @Mock
  private ExecutionContextAdapter<OperationModel> executionContext;

  protected OperationMessageProcessor messageProcessor;
  protected CursorStreamProviderFactory cursorStreamProviderFactory;
  protected String configurationName = CONFIG_NAME;
  protected String target = EMPTY;
  protected String targetValue = "#[message]";

  protected OperationPolicy mockOperationPolicy;

  protected StreamingManager streamingManager = spy(new DefaultStreamingManager());

  @Before
  public void before() throws Exception {
    ((MuleContextWithRegistries) muleContext).getRegistry().registerObject(OBJECT_STREAMING_MANAGER, streamingManager);
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
    when(operationExecutorFactory.createExecutor(operationModel)).thenReturn(operationExecutor);

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
        .thenReturn(of(new MetadataKeyPartModelProperty(0)));
    when(keyParamMock.getRole()).thenReturn(BEHAVIOUR);
    when(keyParamMock.getDisplayModel()).thenReturn(empty());
    when(keyParamMock.getLayoutModel()).thenReturn(empty());
    when(keyParamMock.getModelProperty(QueryParameterModelProperty.class)).thenReturn(empty());

    when(contentMock.getName()).thenReturn("content");
    when(contentMock.hasDynamicType()).thenReturn(true);
    when(contentMock.getType()).thenReturn(stringType);
    when(contentMock.getRole()).thenReturn(CONTENT);
    when(contentMock.getDisplayModel()).thenReturn(empty());
    when(contentMock.getLayoutModel()).thenReturn(empty());
    when(contentMock.getModelProperty(MetadataKeyPartModelProperty.class)).thenReturn(empty());
    when(contentMock.getModelProperty(QueryParameterModelProperty.class)).thenReturn(empty());

    parameterGroupModel = mockParameters(operationModel, keyParamMock, contentMock);
    when(parameterGroupModel.getDisplayModel()).thenReturn(empty());
    when(parameterGroupModel.getLayoutModel()).thenReturn(empty());
    when(parameterGroupModel.getModelProperty(MetadataKeyIdModelProperty.class)).thenReturn(empty());

    when(outputMock.getType()).thenReturn(stringType);
    when(outputMock.hasDynamicType()).thenReturn(true);
    when(operationModel.getOutput()).thenReturn(outputMock);
    when(operationModel.getOutputAttributes()).thenReturn(outputMock);
    when(operationModel.getModelProperty(InterceptorsModelProperty.class)).thenReturn(empty());

    when(operationExecutorFactory.createExecutor(operationModel)).thenReturn(operationExecutor);
    when(operationExecutor.execute(any())).thenReturn(just(""));

    when(resolverSet.resolve(from(event))).thenReturn(parameters);

    when(configurationInstance.getName()).thenReturn(CONFIG_NAME);
    when(configurationInstance.getModel()).thenReturn(configurationModel);
    when(configurationInstance.getValue()).thenReturn(configuration);
    when(configurationInstance.getConnectionProvider()).thenReturn(of(connectionProviderWrapper));

    when(configurationProvider.get(event)).thenReturn(configurationInstance);
    when(configurationProvider.getConfigurationModel()).thenReturn(configurationModel);
    when(configurationProvider.getName()).thenReturn(configurationName);

    when(configurationModel.getOperationModels()).thenReturn(asList(operationModel));
    when(configurationModel.getOperationModel(OPERATION_NAME)).thenReturn(of(operationModel));

    when(connectionProviderWrapper.getReconnectionConfig()).thenReturn(of(ReconnectionConfig.getDefault()));
    when(connectionProviderWrapper.getRetryPolicyTemplate()).thenReturn(new NoRetryPolicyTemplate());

    mockSubTypes(extensionModel);
    mockClassLoaderModelProperty(extensionModel, getClass().getClassLoader());
    when(extensionManager.getConfiguration(anyString(), anyObject())).thenReturn(configurationInstance);
    when(extensionManager.getConfiguration(extensionModel, operationModel, event)).thenReturn(of(configurationInstance));
    when(configurationProvider.get(anyObject())).thenReturn(configurationInstance);
    when(extensionManager.getConfigurationProvider(extensionModel, operationModel)).thenReturn(of(configurationProvider));
    when(extensionManager.getConfigurationProvider(CONFIG_NAME)).thenReturn(of(configurationProvider));

    when(mockPolicyManager.createOperationPolicy(any(), any(), any(), any())).thenAnswer(invocationOnMock -> {
      if (mockOperationPolicy == null) {
        mockOperationPolicy = mock(OperationPolicy.class);
        when(mockOperationPolicy.process(any()))
            .thenAnswer(operationPolicyInvocationMock -> ((OperationExecutionFunction) invocationOnMock.getArguments()[3])
                .execute((Map<String, Object>) invocationOnMock.getArguments()[2],
                         (CoreEvent) invocationOnMock.getArguments()[1]));
      }
      return mockOperationPolicy;
    });

    when(executionContext.getRetryPolicyTemplate()).thenReturn(empty());
    when(connectionManagerAdapter.getConnection(anyString())).thenReturn(null);
    messageProcessor = setUpOperationMessageProcessor();
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
    OperationMessageProcessor messageProcessor = createOperationMessageProcessor();
    messageProcessor.setMuleContext(context);
    ((MuleContextWithRegistries) muleContext).getRegistry().registerObject(OBJECT_CONNECTION_MANAGER, connectionManagerAdapter);
    muleContext.getInjector().inject(messageProcessor);
    messageProcessor.initialise();
    return messageProcessor;
  }

  protected abstract OperationMessageProcessor createOperationMessageProcessor();

  @Test
  public void initialise() throws Exception {
    verify((MuleContextAware) operationExecutor, atLeastOnce()).setMuleContext(any(MuleContext.class));
    verify((Initialisable) operationExecutor).initialise();
  }

  @Test
  public void start() throws Exception {
    messageProcessor.start();
    verify((Startable) operationExecutor).start();
  }

  @Test
  public void stop() throws Exception {
    messageProcessor.stop();
    verify((Stoppable) operationExecutor).stop();
  }

  @Test
  public void dispose() throws Exception {
    messageProcessor.dispose();
    verify((Disposable) operationExecutor).dispose();
  }

}
