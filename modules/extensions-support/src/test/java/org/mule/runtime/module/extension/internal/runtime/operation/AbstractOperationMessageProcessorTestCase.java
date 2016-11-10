/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.nio.charset.Charset.defaultCharset;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.metadata.PartAwareMetadataKeyBuilder.newKey;

import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockClassLoaderModelProperty;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockExceptionEnricher;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockExecutorFactory;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockMetadataResolverFactory;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockSubTypes;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.setRequires;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.OutputModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.core.internal.connection.ConnectionProviderWrapper;
import org.mule.runtime.core.internal.connection.DefaultConnectionManager;
import org.mule.runtime.extension.api.model.ImmutableOutputModel;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.runtime.exception.ExceptionEnricherFactory;
import org.mule.runtime.extension.api.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.metadata.NullMetadataResolver;
import org.mule.runtime.extension.api.model.property.ContentParameterModelProperty;
import org.mule.runtime.extension.api.model.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.model.property.MetadataKeyPartModelProperty;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutor;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutorFactory;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.runtime.module.extension.internal.model.property.InterceptorsModelProperty;
import org.mule.runtime.module.extension.internal.model.property.QueryParameterModelProperty;
import org.mule.runtime.module.extension.internal.runtime.exception.NullExceptionEnricher;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.test.metadata.extension.resolver.TestNoConfigMetadataResolver;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractOperationMessageProcessorTestCase extends AbstractMuleContextTestCase {

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
  protected ExtensionManagerAdapter extensionManager;

  @Mock
  protected ConnectionManagerAdapter connectionManagerAdapter;
  @Mock
  protected OperationExecutorFactory operationExecutorFactory;

  @Mock(extraInterfaces = {Lifecycle.class, MuleContextAware.class})
  protected OperationExecutor operationExecutor;

  @Mock(answer = RETURNS_DEEP_STUBS)
  protected ResolverSet resolverSet;

  @Mock
  protected ResolverSetResult parameters;

  protected Event event;

  @Mock(answer = RETURNS_DEEP_STUBS)
  protected InternalMessage message;

  @Mock(answer = RETURNS_DEEP_STUBS)
  protected MuleContext context;

  @Mock
  protected ConfigurationInstance configurationInstance;

  @Mock
  protected Object configuration;

  @Mock
  protected ExceptionEnricherFactory exceptionEnricherFactory;

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

  protected OperationMessageProcessor messageProcessor;

  protected String configurationName = CONFIG_NAME;
  protected String target = EMPTY;

  protected DefaultConnectionManager connectionManager;

  @Before
  public void before() throws Exception {
    event = configureEvent();
    when(context.getInjector().inject(any())).thenAnswer(invocationOnMock -> {
      final Object subject = invocationOnMock.getArguments()[0];
      muleContext.getInjector().inject(subject);
      return subject;
    });

    when(operationModel.getName()).thenReturn(getClass().getName());
    when(operationModel.getOutput())
        .thenReturn(new ImmutableOutputModel("Message.Payload", toMetadataType(String.class), false, emptySet()));
    mockExecutorFactory(operationModel, operationExecutorFactory);
    when(operationModel.getModelProperty(MetadataKeyIdModelProperty.class)).thenReturn(
                                                                                       of(new MetadataKeyIdModelProperty(ExtensionsTypeLoaderFactory
                                                                                           .getDefault().createTypeLoader()
                                                                                           .load(String.class), "someParam")));
    setRequires(operationModel, true, true);
    when(operationExecutorFactory.createExecutor(operationModel)).thenReturn(operationExecutor);

    when(operationModel.getName()).thenReturn(OPERATION_NAME);

    mockExceptionEnricher(operationModel, exceptionEnricherFactory);
    when(exceptionEnricherFactory.createEnricher()).thenReturn(new NullExceptionEnricher());

    mockMetadataResolverFactory(operationModel, metadataResolverFactory);
    when(metadataResolverFactory.getKeyResolver()).thenReturn(new TestNoConfigMetadataResolver());
    when(metadataResolverFactory.getInputResolver("content")).thenReturn(new TestNoConfigMetadataResolver());
    when(metadataResolverFactory.getInputResolver("type")).thenReturn(new NullMetadataResolver());
    when(metadataResolverFactory.getOutputResolver()).thenReturn(new TestNoConfigMetadataResolver());
    when(metadataResolverFactory.getOutputAttributesResolver()).thenReturn(new TestNoConfigMetadataResolver());

    when(keyParamMock.getName()).thenReturn("type");
    when(keyParamMock.getType()).thenReturn(stringType);
    when(keyParamMock.getModelProperty(MetadataKeyPartModelProperty.class))
        .thenReturn(of(new MetadataKeyPartModelProperty(0)));
    when(keyParamMock.getModelProperty(ContentParameterModelProperty.class)).thenReturn(empty());
    when(keyParamMock.getModelProperty(QueryParameterModelProperty.class)).thenReturn(empty());

    when(contentMock.getName()).thenReturn("content");
    when(contentMock.hasDynamicType()).thenReturn(true);
    when(contentMock.getType()).thenReturn(stringType);
    when(contentMock.getModelProperty(ContentParameterModelProperty.class))
        .thenReturn(of(new ContentParameterModelProperty()));
    when(contentMock.getModelProperty(MetadataKeyPartModelProperty.class)).thenReturn(empty());
    when(contentMock.getModelProperty(QueryParameterModelProperty.class)).thenReturn(empty());

    when(operationModel.getParameterModels()).thenReturn(Arrays.asList(keyParamMock, contentMock));

    when(outputMock.getType()).thenReturn(stringType);
    when(outputMock.hasDynamicType()).thenReturn(true);
    when(operationModel.getOutput()).thenReturn(outputMock);
    when(operationModel.getOutputAttributes()).thenReturn(outputMock);
    when(operationModel.getModelProperty(InterceptorsModelProperty.class)).thenReturn(empty());

    when(operationExecutorFactory.createExecutor(operationModel)).thenReturn(operationExecutor);

    when(resolverSet.resolve(event)).thenReturn(parameters);

    when(configurationInstance.getName()).thenReturn(CONFIG_NAME);
    when(configurationInstance.getModel()).thenReturn(configurationModel);
    when(configurationInstance.getValue()).thenReturn(configuration);
    when(configurationInstance.getConnectionProvider()).thenReturn(of(connectionProviderWrapper));

    when(configurationProvider.get(event)).thenReturn(configurationInstance);
    when(configurationProvider.getConfigurationModel()).thenReturn(configurationModel);
    when(configurationProvider.getName()).thenReturn(configurationName);

    when(configurationModel.getOperationModel(OPERATION_NAME)).thenReturn(of(operationModel));

    connectionManager = new DefaultConnectionManager(context);
    connectionManager.initialise();
    when(connectionProviderWrapper.getRetryPolicyTemplate()).thenReturn(connectionManager.getDefaultRetryPolicyTemplate());

    mockSubTypes(extensionModel);
    mockClassLoaderModelProperty(extensionModel, getClass().getClassLoader());
    when(extensionManager.getConfiguration(anyString(), anyObject())).thenReturn(configurationInstance);
    when(extensionManager.getConfiguration(extensionModel, event)).thenReturn(configurationInstance);
    when(configurationProvider.get(anyObject())).thenReturn(configurationInstance);
    when(extensionManager.getConfigurationProvider(extensionModel)).thenReturn(of(configurationProvider));
    when(extensionManager.getConfigurationProvider(CONFIG_NAME)).thenReturn(of(configurationProvider));

    messageProcessor = setUpOperationMessageProcessor();
  }

  protected Event configureEvent() throws Exception {
    when(message.getPayload().getDataType().getMediaType()).thenReturn(MediaType.create("*", "*", defaultCharset()));
    when(message.getPayload().getValue()).thenReturn(TEST_PAYLOAD);
    return eventBuilder().message(message).build();
  }

  protected OperationMessageProcessor setUpOperationMessageProcessor() throws Exception {
    OperationMessageProcessor messageProcessor = createOperationMessageProcessor();
    messageProcessor.setMuleContext(context);
    messageProcessor.initialise();
    muleContext.getInjector().inject(messageProcessor);
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
