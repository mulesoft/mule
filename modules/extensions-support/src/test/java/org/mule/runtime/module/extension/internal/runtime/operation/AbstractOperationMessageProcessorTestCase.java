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
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.metadata.PartAwareMetadataKeyBuilder.newKey;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.TYPE_BUILDER;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.mockClassLoaderModelProperty;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import static org.mule.test.metadata.extension.resolver.TestNoConfigMetadataResolver.KeyIds.BOOLEAN;
import static org.mule.test.metadata.extension.resolver.TestNoConfigMetadataResolver.KeyIds.STRING;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.OutputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.core.internal.connection.ConnectionProviderWrapper;
import org.mule.runtime.core.internal.connection.DefaultConnectionManager;
import org.mule.runtime.extension.api.introspection.ImmutableOutputModel;
import org.mule.runtime.extension.api.introspection.OutputModel;
import org.mule.runtime.extension.api.introspection.RuntimeExtensionModel;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.introspection.exception.ExceptionEnricherFactory;
import org.mule.runtime.extension.api.introspection.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.introspection.operation.RuntimeOperationModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.introspection.property.MetadataContentModelProperty;
import org.mule.runtime.extension.api.introspection.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.introspection.property.MetadataKeyPartModelProperty;
import org.mule.runtime.extension.api.introspection.property.SubTypesModelProperty;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutor;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutorFactory;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.runtime.module.extension.internal.model.property.ConnectivityModelProperty;
import org.mule.runtime.module.extension.internal.runtime.exception.NullExceptionEnricher;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.matcher.MetadataKeyMatcher;
import org.mule.test.metadata.extension.resolver.TestNoConfigMetadataResolver;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

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

  @Mock
  protected RuntimeExtensionModel extensionModel;

  @Mock(answer = RETURNS_DEEP_STUBS)
  protected RuntimeConfigurationModel configurationModel;

  @Mock
  protected RuntimeOperationModel operationModel;

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

  @Mock(answer = RETURNS_DEEP_STUBS)
  protected MuleEvent event;

  @Mock
  protected MuleMessage message;

  @Mock(answer = RETURNS_DEEP_STUBS)
  protected MuleContext context;

  @Mock
  protected ConfigurationInstance<Object> configurationInstance;

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
  protected ConfigurationProvider<Object> configurationProvider;

  protected OperationMessageProcessor messageProcessor;

  protected String configurationName = CONFIG_NAME;
  protected String target = EMPTY;

  protected DefaultConnectionManager connectionManager;

  @Before
  public void before() throws Exception {
    configureMockEvent(event);

    when(operationModel.getName()).thenReturn(getClass().getName());
    when(operationModel.getOutput())
        .thenReturn(new ImmutableOutputModel("MuleMessage.Payload", toMetadataType(String.class), false, emptySet()));
    when(operationModel.getExecutor()).thenReturn(operationExecutorFactory);
    when(operationModel.getModelProperty(MetadataKeyIdModelProperty.class)).thenReturn(Optional
        .of(new MetadataKeyIdModelProperty(ExtensionsTypeLoaderFactory.getDefault().createTypeLoader().load(String.class))));
    when(operationModel.getModelProperty(ConnectivityModelProperty.class)).thenReturn(empty());
    when(operationExecutorFactory.createExecutor(operationModel)).thenReturn(operationExecutor);

    when(operationModel.getName()).thenReturn(OPERATION_NAME);
    when(operationModel.getExceptionEnricherFactory()).thenReturn(Optional.of(exceptionEnricherFactory));

    when(exceptionEnricherFactory.createEnricher()).thenReturn(new NullExceptionEnricher());

    when(operationModel.getMetadataResolverFactory()).thenReturn(metadataResolverFactory);
    when(metadataResolverFactory.getKeyResolver()).thenReturn(new TestNoConfigMetadataResolver());
    when(metadataResolverFactory.getContentResolver()).thenReturn(new TestNoConfigMetadataResolver());
    when(metadataResolverFactory.getOutputResolver()).thenReturn(new TestNoConfigMetadataResolver());
    when(metadataResolverFactory.getOutputAttributesResolver()).thenReturn(new TestNoConfigMetadataResolver());

    when(keyParamMock.getName()).thenReturn("type");
    when(keyParamMock.getType()).thenReturn(stringType);
    when(keyParamMock.getModelProperty(MetadataKeyPartModelProperty.class))
        .thenReturn(Optional.of(new MetadataKeyPartModelProperty(0)));
    when(keyParamMock.getModelProperty(MetadataContentModelProperty.class)).thenReturn(empty());

    when(contentMock.getName()).thenReturn("content");
    when(contentMock.getType()).thenReturn(stringType);
    when(contentMock.getModelProperty(MetadataContentModelProperty.class))
        .thenReturn(Optional.of(new MetadataContentModelProperty()));
    when(contentMock.getModelProperty(MetadataKeyPartModelProperty.class)).thenReturn(empty());

    when(operationModel.getParameterModels()).thenReturn(Arrays.asList(keyParamMock, contentMock));

    when(outputMock.getType()).thenReturn(stringType);
    when(outputMock.hasDynamicType()).thenReturn(true);
    when(operationModel.getOutput()).thenReturn(outputMock);
    when(operationModel.getOutputAttributes()).thenReturn(outputMock);

    when(operationExecutorFactory.createExecutor(operationModel)).thenReturn(operationExecutor);

    when(resolverSet.resolve(event)).thenReturn(parameters);

    when(configurationInstance.getName()).thenReturn(CONFIG_NAME);
    when(configurationInstance.getModel()).thenReturn(configurationModel);
    when(configurationInstance.getValue()).thenReturn(configuration);
    when(configurationInstance.getConnectionProvider()).thenReturn(Optional.of(connectionProviderWrapper));

    when(configurationProvider.get(event)).thenReturn(configurationInstance);
    when(configurationProvider.getModel()).thenReturn(configurationModel);

    when(configurationModel.getOperationModel(OPERATION_NAME)).thenReturn(Optional.of(operationModel));

    connectionManager = new DefaultConnectionManager(context);
    connectionManager.initialise();
    when(connectionProviderWrapper.getRetryPolicyTemplate()).thenReturn(connectionManager.getDefaultRetryPolicyTemplate());

    when(extensionModel.getModelProperty(SubTypesModelProperty.class)).thenReturn(empty());
    mockClassLoaderModelProperty(extensionModel, getClass().getClassLoader());
    when(extensionManager.getConfiguration(anyString(), anyObject())).thenReturn(configurationInstance);
    when(extensionManager.getConfiguration(extensionModel, event)).thenReturn(configurationInstance);
    when(configurationProvider.get(anyObject())).thenReturn(configurationInstance);
    when(extensionManager.getConfigurationProvider(extensionModel)).thenReturn(Optional.of(configurationProvider));
    when(extensionManager.getConfigurationProvider(CONFIG_NAME)).thenReturn(Optional.of(configurationProvider));

    messageProcessor = setUpOperationMessageProcessor();
  }

  protected MuleEvent configureMockEvent(MuleEvent mockEvent) {
    when(mockEvent.getMessage().getDataType().getMediaType()).thenReturn(MediaType.create("*", "*", defaultCharset()));
    when(mockEvent.getMessage()).thenReturn(message);
    when(message.getPayload()).thenReturn(TEST_PAYLOAD);
    return mockEvent;
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
  public void getOperationStaticMetadata() throws Exception {
    MetadataResult<ComponentMetadataDescriptor> metadata = messageProcessor.getMetadata();

    verify(metadataResolverFactory, never()).getContentResolver();
    verify(metadataResolverFactory, never()).getOutputResolver();

    assertThat(metadata.isSuccess(), is(true));

    assertThat(metadata.get().getOutputMetadata().get().getPayloadMetadata().get().getType(), is(outputMock.getType()));

    assertThat(metadata.get().getContentMetadata().get().get().getType(), is(stringType));

    assertThat(metadata.get().getParametersMetadata().size(), is(1));
    assertThat(metadata.get().getParametersMetadata().get(0).get().getType(), is(stringType));
  }

  @Test
  public void getOperationDynamicMetadata() throws Exception {
    MetadataResult<ComponentMetadataDescriptor> metadata = messageProcessor.getMetadata(newKey("person", "Person").build());

    assertThat(metadata.isSuccess(), is(true));

    MetadataResult<OutputMetadataDescriptor> outputMetadataDescriptor = metadata.get().getOutputMetadata();

    MetadataResult<TypeMetadataDescriptor> payloadMetadata = outputMetadataDescriptor.get().getPayloadMetadata();
    assertThat(payloadMetadata.get().getType(), is(TYPE_BUILDER.booleanType().build()));

    MetadataResult<TypeMetadataDescriptor> attributesMetadata = outputMetadataDescriptor.get().getAttributesMetadata();
    assertThat(attributesMetadata.get().getType(), is(TYPE_BUILDER.booleanType().build()));

    assertThat(metadata.get().getContentMetadata().get().get().getType(), is(TYPE_BUILDER.stringType().build()));
    assertThat(metadata.get().getParametersMetadata().size(), is(1));
    assertThat(metadata.get().getParametersMetadata().get(0).get().getType(), is(stringType));
  }

  @Test
  public void getMetadataKeys() throws Exception {
    MetadataResult<Set<MetadataKey>> metadataKeysResult = messageProcessor.getMetadataKeys();

    verify(operationModel).getMetadataResolverFactory();
    verify(metadataResolverFactory).getKeyResolver();

    assertThat(metadataKeysResult.isSuccess(), is(true));
    final Set<MetadataKey> metadataKeys = metadataKeysResult.get();
    assertThat(metadataKeys.size(), is(2));

    assertThat(metadataKeys, hasItem(MetadataKeyMatcher.metadataKeyWithId(BOOLEAN.name())));
    assertThat(metadataKeys, hasItem(MetadataKeyMatcher.metadataKeyWithId(STRING.name())));
  }

  @Test
  public void initialise() throws Exception {
    verify((MuleContextAware) operationExecutor).setMuleContext(context);
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
