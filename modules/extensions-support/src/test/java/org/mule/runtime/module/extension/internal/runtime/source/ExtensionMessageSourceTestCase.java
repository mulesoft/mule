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
import static org.apache.commons.lang3.exception.ExceptionUtils.getThrowables;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.ThrowableCauseMatcher.hasCause;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXTENSION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STREAMING_MANAGER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.tck.MuleTestUtils.spyInjector;
import static org.mule.test.heisenberg.extension.exception.HeisenbergConnectionExceptionEnricher.ENRICHED_MESSAGE;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.getDefaultCursorStreamProviderFactory;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockClassLoaderModelProperty;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockExceptionEnricher;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockMetadataResolverFactory;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockSubTypes;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.setRequires;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.java.api.JavaTypeLoader;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;
import org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate;
import org.mule.runtime.core.api.streaming.DefaultStreamingManager;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.streaming.bytes.CursorStreamProviderFactory;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.util.ExceptionUtils;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.execution.ExceptionCallback;
import org.mule.runtime.core.internal.streaming.bytes.factory.NullCursorStreamProviderFactory;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;
import org.mule.runtime.core.privileged.execution.MessageProcessContext;
import org.mule.runtime.core.privileged.execution.MessageProcessingManager;
import org.mule.runtime.extension.api.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.metadata.NullMetadataResolver;
import org.mule.runtime.extension.api.model.ImmutableOutputModel;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.exception.ExceptionHandler;
import org.mule.runtime.extension.api.runtime.exception.ExceptionHandlerFactory;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.property.MetadataKeyIdModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MetadataResolverFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.SourceCallbackModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.tck.core.streaming.SimpleByteBufferManager;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.test.heisenberg.extension.exception.HeisenbergConnectionExceptionEnricher;
import org.mule.test.metadata.extension.resolver.TestNoConfigMetadataResolver;

import java.io.IOException;

import javax.resource.spi.work.Work;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ExtensionMessageSourceTestCase extends AbstractMuleContextTestCase {

  private static final String CONFIG_NAME = "myConfig";
  private static final String ERROR_MESSAGE = "ERROR";
  private static final String SOURCE_NAME = "source";
  private static final String METADATA_KEY = "metadataKey";
  private final SimpleRetryPolicyTemplate retryPolicyTemplate = new SimpleRetryPolicyTemplate(0, 2);
  private final JavaTypeLoader typeLoader = new JavaTypeLoader(this.getClass().getClassLoader());
  private CursorStreamProviderFactory cursorStreamProviderFactory;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private ExtensionModel extensionModel;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private SourceModel sourceModel;

  @Mock
  private SourceAdapterFactory sourceAdapterFactory;

  @Mock
  private SourceCallbackFactory sourceCallbackFactory;

  @Mock
  private MessageProcessContext messageProcessContext;

  @Mock
  private TransactionConfig transactionConfig;

  @Mock
  private Scheduler ioScheduler;

  @Mock
  private Scheduler cpuLightScheduler;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private Processor messageProcessor;

  @Mock
  private SourceCompletionHandlerFactory completionHandlerFactory;

  @Mock
  private FlowConstruct flowConstruct;

  @Mock
  private Source source;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionManager extensionManager;

  @Mock
  private MessageProcessingManager messageProcessingManager;

  @Mock
  private ExceptionCallback exceptionCallback;

  @Mock
  private ExceptionHandlerFactory enricherFactory;

  @Mock
  private ConfigurationProvider configurationProvider;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ConfigurationModel configurationModel;

  @Mock
  private ConfigurationInstance configurationInstance;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ResolverSet callbackParameters;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private Result result;

  @Mock
  protected MetadataResolverFactory metadataResolverFactory;

  private SourceAdapter sourceAdapter;
  private SourceCallback sourceCallback;
  private ExtensionMessageSource messageSource;
  private StreamingManager streamingManager = spy(new DefaultStreamingManager());

  @Before
  public void before() throws Exception {
    spyInjector(muleContext);
    reset(muleContext.getSchedulerService());
    when(result.getMediaType()).thenReturn(of(ANY));
    when(result.getAttributes()).thenReturn(empty());

    ((MuleContextWithRegistries) muleContext).getRegistry().registerObject(OBJECT_STREAMING_MANAGER, streamingManager);

    when(extensionModel.getXmlDslModel()).thenReturn(XmlDslModel.builder().setPrefix("test-extension").build());

    cursorStreamProviderFactory = getDefaultCursorStreamProviderFactory(streamingManager);

    sourceAdapter = createSourceAdapter();

    when(sourceAdapterFactory.createAdapter(any(), any(), any(), any(), any())).thenReturn(sourceAdapter);

    mockExceptionEnricher(sourceModel, null);
    when(sourceModel.requiresConnection()).thenReturn(true);
    when(sourceModel.getName()).thenReturn(SOURCE_NAME);
    when(sourceModel.getModelProperty(MetadataResolverFactoryModelProperty.class)).thenReturn(empty());
    when(sourceModel.getModelProperty(SourceCallbackModelProperty.class)).thenReturn(empty());
    when(sourceModel.getModelProperty(MediaTypeModelProperty.class)).thenReturn(empty());
    setRequires(sourceModel, true, true);
    when(sourceModel.getOutput().getType()).thenReturn(TYPE_LOADER.load(String.class));
    mockExceptionEnricher(extensionModel, null);
    mockClassLoaderModelProperty(extensionModel, getClass().getClassLoader());

    retryPolicyTemplate
        .setNotificationFirer(((MuleContextWithRegistries) muleContext).getRegistry().lookupObject(NotificationDispatcher.class));
    initialiseIfNeeded(retryPolicyTemplate, muleContext);

    ((MuleContextWithRegistries) muleContext).getRegistry().registerObject(OBJECT_EXTENSION_MANAGER, extensionManager);

    when(flowConstruct.getMuleContext()).thenReturn(muleContext);

    mockSubTypes(extensionModel);
    when(configurationModel.getSourceModel(SOURCE_NAME)).thenReturn(of(sourceModel));
    when(extensionManager.getConfigurationProvider(CONFIG_NAME)).thenReturn(of(configurationProvider));
    when(configurationProvider.get(any())).thenReturn(configurationInstance);
    when(configurationProvider.getConfigurationModel()).thenReturn(configurationModel);
    when(configurationProvider.getName()).thenReturn(CONFIG_NAME);

    mockMetadataResolverFactory(sourceModel, metadataResolverFactory);
    when(metadataResolverFactory.getKeyResolver()).thenReturn(new TestNoConfigMetadataResolver());
    when(metadataResolverFactory.getInputResolver("content")).thenReturn(new TestNoConfigMetadataResolver());
    when(metadataResolverFactory.getInputResolver("type")).thenReturn(new NullMetadataResolver());
    when(metadataResolverFactory.getOutputResolver()).thenReturn(new TestNoConfigMetadataResolver());
    when(metadataResolverFactory.getOutputAttributesResolver()).thenReturn(new TestNoConfigMetadataResolver());

    when(sourceModel.getOutput())
        .thenReturn(new ImmutableOutputModel("Output", BaseTypeBuilder.create(JAVA).stringType().build(), true, emptySet()));
    when(sourceModel.getOutputAttributes())
        .thenReturn(new ImmutableOutputModel("Output", BaseTypeBuilder.create(JAVA).stringType().build(), false, emptySet()));
    when(sourceModel.getModelProperty(MetadataKeyIdModelProperty.class))
        .thenReturn(of(new MetadataKeyIdModelProperty(typeLoader.load(String.class), METADATA_KEY)));
    when(sourceModel.getAllParameterModels()).thenReturn(emptyList());

    when(messageProcessContext.getTransactionConfig()).thenReturn(empty());

    messageSource = getNewExtensionMessageSourceInstance();

    sourceCallback = spy(DefaultSourceCallback.builder()
        .setSourceModel(sourceModel)
        .setProcessingManager(messageProcessingManager)
        .setListener(messageProcessor)
        .setSource(messageSource)
        .setMuleContext(muleContext)
        .setProcessContextSupplier(() -> messageProcessContext)
        .setCompletionHandlerFactory(completionHandlerFactory)
        .setExceptionCallback(exceptionCallback)
        .setCursorStreamProviderFactory(cursorStreamProviderFactory)
        .build());

    when(sourceCallbackFactory.createSourceCallback(any())).thenReturn(sourceCallback);
  }

  @After
  public void after() throws MuleException {
    messageSource.stop();
    messageSource.dispose();
  }

  @Test
  public void handleMessage() throws Exception {
    doAnswer(invocationOnMock -> {
      sourceCallback.handle(result);
      return null;
    }).when(source).onStart(sourceCallback);

    doAnswer(invocation -> {
      ((Work) invocation.getArguments()[0]).run();
      return null;
    }).when(cpuLightScheduler).execute(any());

    messageSource.initialise();
    messageSource.start();

    verify(sourceCallback).handle(result);
  }

  @Test
  public void handleExceptionAndRestart() throws Exception {
    initialise();
    start();

    messageSource.onException(new ConnectionException(ERROR_MESSAGE));
    verify(source).onStop();
    verify(ioScheduler, never()).stop();
    verify(cpuLightScheduler, never()).stop();
    verify(source, times(2)).onStart(sourceCallback);
    handleMessage();
  }

  @Test
  public void initialise() throws Exception {
    messageSource.initialise();
    verify(source, never()).onStart(sourceCallback);
  }

  @Test
  public void sourceIsInstantiatedOnce() throws Exception {
    initialise();
    start();
    verify(sourceAdapterFactory, times(1)).createAdapter(any(), any(), any(), any(), any());
  }

  @Test
  public void failToStart() throws Exception {
    MuleException e = new DefaultMuleException(new Exception());
    doThrow(e).when(source).onStart(any());
    expectedException.expect(is(instanceOf(RetryPolicyExhaustedException.class)));

    messageSource.initialise();
    messageSource.start();
  }

  @Test
  public void failWithConnectionExceptionWhenStartingAndGetRetryPolicyExhausted() throws Exception {
    final ConnectionException connectionException = new ConnectionException(ERROR_MESSAGE);
    doThrow(new RuntimeException(connectionException)).when(source).onStart(sourceCallback);

    final Throwable throwable = catchThrowable(messageSource::start);
    assertThat(throwable, is(instanceOf(RetryPolicyExhaustedException.class)));
    assertThat(throwable, is(exhaustedBecauseOf(connectionException)));
    verify(source, times(3)).onStart(sourceCallback);
  }

  @Test
  public void failWithNonConnectionExceptionWhenStartingAndGetRetryPolicyExhausted() throws Exception {
    doThrow(new DefaultMuleException(new IOException(ERROR_MESSAGE))).when(source).onStart(sourceCallback);

    final Throwable throwable = catchThrowable(messageSource::start);
    assertThat(throwable, is(instanceOf(RetryPolicyExhaustedException.class)));
    assertThat(getThrowables(throwable), hasItemInArray(instanceOf(IOException.class)));
    verify(source, times(3)).onStart(sourceCallback);
  }

  @Test
  public void failWithConnectionExceptionWhenStartingAndGetsReconnected() throws Exception {
    doThrow(new RuntimeException(new ConnectionException(ERROR_MESSAGE)))
        .doThrow(new RuntimeException(new ConnectionException(ERROR_MESSAGE))).doNothing().when(source).onStart(sourceCallback);

    messageSource.initialise();
    messageSource.start();
    verify(source, times(3)).onStart(sourceCallback);
    verify(source, times(2)).onStop();
  }

  @Test
  public void failOnExceptionWithConnectionExceptionAndGetsReconnected() throws Exception {
    messageSource.initialise();
    messageSource.start();
    messageSource.onException(new ConnectionException(ERROR_MESSAGE));

    verify(source, times(2)).onStart(sourceCallback);
    verify(source, times(1)).onStop();
  }

  @Test
  public void startFailsWithRandomException() throws Exception {
    Exception e = new RuntimeException();
    doThrow(e).when(source).onStart(sourceCallback);
    expectedException.expect(exhaustedBecauseOf(new BaseMatcher<Throwable>() {

      private Matcher<Exception> exceptionMatcher = hasCause(sameInstance(e));

      @Override
      public boolean matches(Object item) {
        return exceptionMatcher.matches(item);
      }

      @Override
      public void describeTo(Description description) {
        exceptionMatcher.describeTo(description);
      }
    }));

    initialise();
    messageSource.start();
  }

  @Test
  public void start() throws Exception {
    initialise();
    messageSource.start();

    verify(source).onStart(sourceCallback);
    verify(muleContext.getInjector()).inject(source);
  }

  @Test
  public void failedToCreateRetryScheduler() throws Exception {
    Exception e = new RuntimeException();

    SchedulerService schedulerService = muleContext.getSchedulerService();
    doThrow(e).when(schedulerService).cpuLightScheduler();

    final Throwable throwable = catchThrowable(messageSource::start);
    assertThat(throwable, is(sameInstance(e)));
  }

  @Test
  public void failedToCreateFlowTrigger() throws Exception {
    Exception e = new RuntimeException();

    SchedulerService schedulerService = muleContext.getSchedulerService();
    doThrow(e).when(schedulerService).cpuLightScheduler();

    final Throwable throwable = catchThrowable(messageSource::start);
    assertThat(throwable, is(sameInstance(e)));
  }

  @Test
  public void stop() throws Exception {
    messageSource.initialise();
    messageSource.start();

    messageSource.stop();
    verify(source).onStop();
  }

  @Test
  public void enrichExceptionWithSourceExceptionEnricher() throws Exception {
    when(enricherFactory.createHandler()).thenReturn(new HeisenbergConnectionExceptionEnricher());
    mockExceptionEnricher(sourceModel, enricherFactory);
    mockExceptionEnricher(sourceModel, enricherFactory);
    ExtensionMessageSource messageSource = getNewExtensionMessageSourceInstance();
    doThrow(new RuntimeException(ERROR_MESSAGE)).when(source).onStart(sourceCallback);
    Throwable t = catchThrowable(messageSource::start);

    assertThat(ExceptionUtils.containsType(t, ConnectionException.class), is(true));
    assertThat(t.getMessage(), containsString(ENRICHED_MESSAGE + ERROR_MESSAGE));

    messageSource.stop();
  }

  @Test
  public void enrichExceptionWithExtensionEnricher() throws Exception {
    final String enrichedErrorMessage = "Enriched: " + ERROR_MESSAGE;
    ExceptionHandler exceptionEnricher = mock(ExceptionHandler.class);
    when(exceptionEnricher.enrichException(any(Exception.class))).thenReturn(new Exception(enrichedErrorMessage));
    when(enricherFactory.createHandler()).thenReturn(exceptionEnricher);
    mockExceptionEnricher(extensionModel, enricherFactory);
    ExtensionMessageSource messageSource = getNewExtensionMessageSourceInstance();
    doThrow(new RuntimeException(ERROR_MESSAGE)).when(source).onStart(sourceCallback);
    Throwable t = catchThrowable(messageSource::start);

    assertThat(t.getMessage(), containsString(enrichedErrorMessage));

    messageSource.stop();
  }

  @Test
  public void workManagerDisposedIfSourceFailsToStart() throws Exception {
    initialise();
    start();

    Exception e = new RuntimeException();
    doThrow(e).when(source).onStop();
    expectedException.expect(new BaseMatcher<Throwable>() {

      @Override
      public boolean matches(Object item) {
        Exception exception = (Exception) item;
        return exception.getCause() instanceof MuleException && exception.getCause().getCause() == e;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Exception was not wrapped as expected");
      }
    });
  }

  private ExtensionMessageSource getNewExtensionMessageSourceInstance() throws MuleException {

    ExtensionMessageSource messageSource =
        new ExtensionMessageSource(extensionModel, sourceModel, sourceAdapterFactory, configurationProvider,
                                   retryPolicyTemplate, cursorStreamProviderFactory, extensionManager);
    messageSource.setListener(messageProcessor);
    messageSource.setAnnotations(getAppleFlowComponentLocationAnnotations());
    muleContext.getInjector().inject(messageSource);
    return messageSource;
  }

  private BaseMatcher<Throwable> exhaustedBecauseOf(Throwable cause) {
    return exhaustedBecauseOf(sameInstance(cause));
  }

  private SourceAdapter createSourceAdapter() {
    return new SourceAdapter(extensionModel,
                             sourceModel,
                             source,
                             of(configurationInstance),
                             new NullCursorStreamProviderFactory(new SimpleByteBufferManager(), streamingManager),
                             sourceCallbackFactory,
                             mock(ComponentLocation.class),
                             mock(SourceConnectionManager.class),
                             null, callbackParameters, null,
                             mock(MessagingExceptionResolver.class));
  }

  private BaseMatcher<Throwable> exhaustedBecauseOf(Matcher<Throwable> causeMatcher) {
    return new BaseMatcher<Throwable>() {

      @Override
      public boolean matches(Object item) {
        Throwable exception = (Throwable) item;
        return causeMatcher.matches(exception.getCause());
      }

      @Override
      public void describeTo(Description description) {
        causeMatcher.describeTo(description);
      }
    };
  }

  @Test
  public void getMetadataKeyIdObjectValue() throws Exception {
    final String person = "person";
    source = new DummySource(person);
    sourceAdapter = createSourceAdapter();
    when(sourceAdapterFactory.createAdapter(any(), any(), any(), any(), any())).thenReturn(sourceAdapter);
    messageSource = getNewExtensionMessageSourceInstance();
    messageSource.initialise();
    messageSource.start();
    final Object metadataKeyValue = messageSource.getParameterValueResolver().getParameterValue(METADATA_KEY);
    assertThat(metadataKeyValue, is(person));
  }

  private class DummySource extends Source {

    DummySource(String metadataKey) {

      this.metadataKey = metadataKey;
    }

    private String metadataKey;

    @Override
    public void onStart(SourceCallback sourceCallback) throws MuleException {}

    @Override
    public void onStop() {}
  }
}
