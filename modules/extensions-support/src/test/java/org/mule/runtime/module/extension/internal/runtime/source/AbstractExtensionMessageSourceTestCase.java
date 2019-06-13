/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXTENSION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STREAMING_MANAGER;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Unhandleable.FLOW_BACK_PRESSURE;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy.FAIL;
import static org.mule.tck.MuleTestUtils.spyInjector;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.getDefaultCursorStreamProviderFactory;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockClassLoaderModelProperty;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockExceptionEnricher;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockMetadataResolverFactory;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockSubTypes;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.setRequires;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.java.api.JavaTypeLoader;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate;
import org.mule.runtime.core.api.streaming.DefaultStreamingManager;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.streaming.bytes.CursorStreamProviderFactory;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.util.UUID;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.execution.ExceptionCallback;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheId;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheIdGenerator;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheIdGeneratorFactory;
import org.mule.runtime.core.internal.streaming.bytes.factory.NullCursorStreamProviderFactory;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;
import org.mule.runtime.core.privileged.execution.MessageProcessContext;
import org.mule.runtime.core.privileged.execution.MessageProcessingManager;
import org.mule.runtime.extension.api.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.metadata.NullMetadataResolver;
import org.mule.runtime.extension.api.model.ImmutableOutputModel;
import org.mule.runtime.extension.api.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.exception.ExceptionHandlerFactory;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.BackPressureAction;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MetadataResolverFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.SourceCallbackModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.tck.core.streaming.SimpleByteBufferManager;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.test.metadata.extension.resolver.TestNoConfigMetadataResolver;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;

public abstract class AbstractExtensionMessageSourceTestCase extends AbstractMuleContextTestCase {

  protected static final String CONFIG_NAME = "myConfig";
  protected static final String ERROR_MESSAGE = "ERROR";
  protected static final String SOURCE_NAME = "source";
  protected static final String METADATA_KEY = "metadataKey";
  protected final SimpleRetryPolicyTemplate retryPolicyTemplate = new SimpleRetryPolicyTemplate(0, 2);
  protected final JavaTypeLoader typeLoader = new JavaTypeLoader(this.getClass().getClassLoader());
  protected CursorStreamProviderFactory cursorStreamProviderFactory;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  protected ExtensionModel extensionModel;

  @Mock(answer = RETURNS_DEEP_STUBS)
  protected SourceModel sourceModel;

  @Mock
  protected SourceAdapterFactory sourceAdapterFactory;

  @Mock
  protected SourceCallbackFactory sourceCallbackFactory;

  @Mock
  protected MessageProcessContext messageProcessContext;

  @Mock
  protected TransactionConfig transactionConfig;

  @Mock
  protected Scheduler ioScheduler;

  @Mock
  protected Scheduler cpuLightScheduler;

  @Mock(answer = RETURNS_DEEP_STUBS)
  protected Processor messageProcessor;

  @Mock
  protected SourceCompletionHandlerFactory completionHandlerFactory;

  @Mock
  protected FlowConstruct flowConstruct;

  @Mock(extraInterfaces = Lifecycle.class)
  protected Source source;

  @Mock(answer = RETURNS_DEEP_STUBS)
  protected ExtensionManager extensionManager;

  @Mock
  protected MetadataCacheIdGeneratorFactory<ComponentAst> cacheIdGeneratorFactory;

  @Mock
  protected MetadataCacheIdGenerator<ComponentAst> cacheIdGenerator;

  @Mock
  protected MessageProcessingManager messageProcessingManager;

  @Mock
  protected ExceptionCallback exceptionCallback;

  @Mock
  protected ExceptionHandlerFactory enricherFactory;

  @Mock
  protected ConfigurationProvider configurationProvider;

  @Mock(answer = RETURNS_DEEP_STUBS)
  protected ConfigurationModel configurationModel;

  @Mock
  protected ConfigurationInstance configurationInstance;

  @Mock(answer = RETURNS_DEEP_STUBS)
  protected ResolverSet callbackParameters;

  @Mock(answer = RETURNS_DEEP_STUBS)
  protected Result result;

  @Mock
  protected MetadataResolverFactory metadataResolverFactory;


  protected boolean primaryNodeOnly = false;
  protected SourceAdapter sourceAdapter;
  protected SourceCallback sourceCallback;
  protected ExtensionMessageSource messageSource;
  protected StreamingManager streamingManager = spy(new DefaultStreamingManager());

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    ErrorTypeRepository errorTypeRepository = Mockito.mock(ErrorTypeRepository.class);
    when(errorTypeRepository.getErrorType(FLOW_BACK_PRESSURE)).thenReturn(of(mock(ErrorType.class)));

    Map<String, Object> registryObjects = new HashMap<>();
    registryObjects.put("errorTypeRepository", errorTypeRepository);
    return registryObjects;
  }

  @Before
  public void before() throws Exception {
    initMocks(this);
    spyInjector(muleContext);
    reset(muleContext.getSchedulerService());
    when(result.getMediaType()).thenReturn(of(ANY));
    when(result.getAttributes()).thenReturn(empty());

    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject(OBJECT_STREAMING_MANAGER, streamingManager);

    when(extensionModel.getXmlDslModel()).thenReturn(XmlDslModel.builder().setPrefix("test-extension").build());

    cursorStreamProviderFactory = getDefaultCursorStreamProviderFactory(streamingManager);

    sourceAdapter = createSourceAdapter();

    when(sourceAdapterFactory.createAdapter(any(), any(), any(), any(), any(), any())).thenReturn(sourceAdapter);
    when(sourceAdapterFactory.getSourceParameters()).thenReturn(new ResolverSet(muleContext));

    mockExceptionEnricher(sourceModel, null);
    when(sourceModel.requiresConnection()).thenReturn(true);
    when(sourceModel.getName()).thenReturn(SOURCE_NAME);
    when(sourceModel.getModelProperty(MetadataResolverFactoryModelProperty.class)).thenReturn(empty());
    when(sourceModel.getModelProperty(SourceCallbackModelProperty.class)).thenReturn(empty());
    when(sourceModel.getModelProperty(MediaTypeModelProperty.class)).thenReturn(empty());
    setRequires(sourceModel, true, true);
    when(sourceModel.getOutput().getType()).thenReturn(TYPE_LOADER.load(String.class));
    when(sourceModel.getNotificationModels()).thenReturn(emptySet());
    mockExceptionEnricher(extensionModel, null);
    mockClassLoaderModelProperty(extensionModel, getClass().getClassLoader());

    retryPolicyTemplate
        .setNotificationFirer(((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(NotificationDispatcher.class));
    initialiseIfNeeded(retryPolicyTemplate, muleContext);

    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject(OBJECT_EXTENSION_MANAGER, extensionManager);

    when(flowConstruct.getMuleContext()).thenReturn(muleContext);

    mockSubTypes(extensionModel);
    when(configurationModel.getSourceModel(SOURCE_NAME)).thenReturn(of(sourceModel));
    when(extensionManager.getConfigurationProvider(CONFIG_NAME)).thenReturn(of(configurationProvider));
    when(extensionManager.getExtensions()).thenReturn(Collections.singleton(extensionModel));
    when(configurationProvider.get(any())).thenReturn(configurationInstance);
    when(configurationProvider.getConfigurationModel()).thenReturn(configurationModel);
    when(configurationProvider.getName()).thenReturn(CONFIG_NAME);
    when(configurationInstance.getConnectionProvider()).thenReturn(empty());

    mockMetadataResolverFactory(sourceModel, metadataResolverFactory);
    when(metadataResolverFactory.getKeyResolver()).thenReturn(new TestNoConfigMetadataResolver());
    when(metadataResolverFactory.getInputResolver("content")).thenReturn(new TestNoConfigMetadataResolver());
    when(metadataResolverFactory.getInputResolver("type")).thenReturn(new NullMetadataResolver());
    when(metadataResolverFactory.getOutputResolver()).thenReturn(new TestNoConfigMetadataResolver());
    when(metadataResolverFactory.getOutputAttributesResolver()).thenReturn(new TestNoConfigMetadataResolver());

    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject("metadata.cache.id.model.generator.factory",
                                                                         cacheIdGeneratorFactory);
    when(cacheIdGeneratorFactory.create(any(), any())).thenReturn(cacheIdGenerator);
    when(cacheIdGenerator.getIdForComponentMetadata(any()))
        .then(invocation -> Optional.of(new MetadataCacheId(UUID.getUUID(), null)));
    when(cacheIdGenerator.getIdForGlobalMetadata(any()))
        .then(invocation -> Optional.of(new MetadataCacheId(UUID.getUUID(), null)));
    when(cacheIdGenerator.getIdForMetadataKeys(any()))
        .then(invocation -> Optional.of(new MetadataCacheId(UUID.getUUID(), null)));

    when(sourceModel.getOutput())
        .thenReturn(new ImmutableOutputModel("Output", BaseTypeBuilder.create(JAVA).stringType().build(), true, emptySet()));
    when(sourceModel.getOutputAttributes())
        .thenReturn(new ImmutableOutputModel("Output", BaseTypeBuilder.create(JAVA).stringType().build(), false, emptySet()));
    when(sourceModel.getModelProperty(MetadataKeyIdModelProperty.class))
        .thenReturn(of(new MetadataKeyIdModelProperty(typeLoader.load(String.class), METADATA_KEY)));
    when(sourceModel.getAllParameterModels()).thenReturn(emptyList());

    when(messageProcessContext.getTransactionConfig()).thenReturn(empty());

    messageSource = getNewExtensionMessageSourceInstance();

    sourceCallback = DefaultSourceCallback.builder()
        .setSourceModel(sourceModel)
        .setProcessingManager(messageProcessingManager)
        .setListener(messageProcessor)
        .setSource(messageSource)
        .setMuleContext(muleContext)
        .setProcessContextSupplier(() -> messageProcessContext)
        .setCompletionHandlerFactory(completionHandlerFactory)
        .setExceptionCallback(exceptionCallback)
        .setCursorStreamProviderFactory(cursorStreamProviderFactory)
        .build();

    when(sourceCallbackFactory.createSourceCallback(any())).thenReturn(sourceCallback);
  }

  @After
  public void after() throws MuleException {
    try {
      if (messageSource.getLifecycleState().isStarted()) {
        messageSource.stop();
      }
    } finally {
      if (messageSource.getLifecycleState().isStopped() || messageSource.getLifecycleState().isInitialised()) {
        messageSource.dispose();
      }
    }
  }

  protected SourceAdapter createSourceAdapter() {
    return new SourceAdapter(extensionModel,
                             sourceModel,
                             source,
                             of(configurationInstance),
                             new NullCursorStreamProviderFactory(new SimpleByteBufferManager(), streamingManager),
                             sourceCallbackFactory,
                             mock(Component.class),
                             mock(SourceConnectionManager.class),
                             null, callbackParameters, null,
                             mock(MessagingExceptionResolver.class),
                             of(BackPressureAction.FAIL));
  }

  protected ExtensionMessageSource getNewExtensionMessageSourceInstance() throws MuleException {

    ExtensionMessageSource messageSource =
        new ExtensionMessageSource(extensionModel, sourceModel, sourceAdapterFactory, configurationProvider, primaryNodeOnly,
                                   retryPolicyTemplate, cursorStreamProviderFactory, FAIL, extensionManager);
    messageSource.setListener(messageProcessor);
    messageSource.setAnnotations(getAppleFlowComponentLocationAnnotations());
    muleContext.getInjector().inject(messageSource);
    return messageSource;
  }


}
