/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

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
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
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
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.tck.MuleTestUtils.spyInjector;
import static org.mule.test.heisenberg.extension.exception.HeisenbergConnectionExceptionEnricher.ENRICHED_MESSAGE;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockClassLoaderModelProperty;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockExceptionEnricher;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockMetadataResolverFactory;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockSubTypes;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.setRequires;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.java.api.JavaTypeLoader;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.api.scheduler.Scheduler;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.execution.ExceptionCallback;
import org.mule.runtime.core.execution.MessageProcessContext;
import org.mule.runtime.core.execution.MessageProcessingManager;
import org.mule.runtime.core.retry.RetryPolicyExhaustedException;
import org.mule.runtime.core.retry.policies.SimpleRetryPolicyTemplate;
import org.mule.runtime.core.util.ExceptionUtils;
import org.mule.runtime.extension.api.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.metadata.NullMetadataResolver;
import org.mule.runtime.extension.api.model.ImmutableOutputModel;
import org.mule.runtime.extension.api.model.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.exception.ExceptionEnricher;
import org.mule.runtime.extension.api.runtime.exception.ExceptionEnricherFactory;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.runtime.module.extension.internal.model.property.MetadataResolverFactoryModelProperty;
import org.mule.runtime.module.extension.internal.model.property.SourceCallbackModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.tck.SimpleUnitTestSupportSchedulerService;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.test.heisenberg.extension.exception.HeisenbergConnectionExceptionEnricher;
import org.mule.test.metadata.extension.resolver.TestNoConfigMetadataResolver;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import javax.resource.spi.work.Work;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ExtensionMessageSourceTestCase extends AbstractMuleContextTestCase {

  private static final String CONFIG_NAME = "myConfig";
  private static final String ERROR_MESSAGE = "ERROR";
  private static final String SOURCE_NAME = "source";
  private static final String METADATA_KEY = "metadataKey";
  private final RetryPolicyTemplate retryPolicyTemplate = new SimpleRetryPolicyTemplate(0, 2);
  private final JavaTypeLoader typeLoader = new JavaTypeLoader(this.getClass().getClassLoader());

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private ExtensionModel extensionModel;

  @Mock
  private SourceModel sourceModel;

  @Mock
  private SourceAdapterFactory sourceAdapterFactory;

  @Mock
  private SourceCallbackFactory sourceCallbackFactory;

  @Mock
  private Supplier<MessageProcessContext> processContextSupplier;

  @Mock
  private ThreadingProfile threadingProfile;

  // @Mock
  // private WorkManager workManager;

  @Mock
  Scheduler ioScheduler;

  @Mock
  Scheduler cpuLightScheduler;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private Processor messageProcessor;

  @Mock
  private SourceCompletionHandlerFactory completionHandlerFactory;

  @Mock
  private FlowConstruct flowConstruct;

  @Mock
  private Source source;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionManagerAdapter extensionManager;

  @Mock
  private MessageProcessingManager messageProcessingManager;

  @Mock
  private ExceptionCallback exceptionCallback;

  @Mock
  private ExceptionEnricherFactory enricherFactory;

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

  @Before
  public void before() throws Exception {
    spyInjector(muleContext);
    // when(muleContext.getSchedulerService().ioScheduler()).thenReturn(ioScheduler);
    // when(muleContext.getSchedulerService().cpuLightScheduler()).thenReturn(cpuLightScheduler);
    reset(muleContext.getSchedulerService());
    // when(threadingProfile.createWorkManager(anyString(), eq(muleContext.getConfiguration().getShutdownTimeout())))
    // .thenReturn(workManager);
    when(result.getMediaType()).thenReturn(of(ANY));
    when(result.getAttributes()).thenReturn(of(mock(Attributes.class)));

    sourceCallback = spy(DefaultSourceCallback.builder()
        .setConfigName(CONFIG_NAME)
        .setFlowConstruct(flowConstruct)
        .setProcessingManager(messageProcessingManager)
        .setListener(messageProcessor)
        .setProcessContextSupplier(processContextSupplier)
        .setCompletionHandlerFactory(completionHandlerFactory)
        .setExceptionCallback(exceptionCallback)
        .build());

    when(sourceCallbackFactory.createSourceCallback(any())).thenReturn(sourceCallback);
    sourceAdapter = createSourceAdapter();

    when(sourceAdapterFactory.createAdapter(any(), any())).thenReturn(sourceAdapter);

    mockExceptionEnricher(sourceModel, null);
    when(sourceModel.getName()).thenReturn(SOURCE_NAME);
    when(sourceModel.getModelProperty(MetadataResolverFactoryModelProperty.class)).thenReturn(empty());
    when(sourceModel.getModelProperty(SourceCallbackModelProperty.class)).thenReturn(empty());
    setRequires(sourceModel, true, true);
    mockExceptionEnricher(extensionModel, null);
    mockClassLoaderModelProperty(extensionModel, getClass().getClassLoader());

    initialiseIfNeeded(retryPolicyTemplate, muleContext);

    muleContext.getRegistry().registerObject(OBJECT_EXTENSION_MANAGER, extensionManager);

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

    messageSource = getNewExtensionMessageSourceInstance();
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
    verify(ioScheduler, never()).stop(anyLong(), any());
    verify(cpuLightScheduler, never()).stop(anyLong(), any());
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
    verify(sourceAdapterFactory, times(1)).createAdapter(any(), any());
  }

  @Test
  public void failToStart() throws Exception {
    MuleException e = new DefaultMuleException(new Exception());
    doThrow(e).when(source).onStart(any());
    expectedException.expectCause(is(instanceOf(RetryPolicyExhaustedException.class)));

    messageSource.initialise();
    messageSource.start();
  }

  @Test
  public void failWithConnectionExceptionWhenStartingAndGetRetryPolicyExhausted() throws Exception {
    final ConnectionException connectionException = new ConnectionException(ERROR_MESSAGE);
    doThrow(new RuntimeException(connectionException)).when(source).onStart(sourceCallback);

    final Throwable throwable = catchThrowable(messageSource::start);
    assertThat(throwable, is(instanceOf(MuleRuntimeException.class)));
    assertThat(throwable.getCause(), is(exhaustedBecauseOf(connectionException)));
    verify(source, times(3)).onStart(sourceCallback);
  }

  @Test
  public void failWithNonConnectionExceptionWhenStartingAndGetRetryPolicyExhausted() throws Exception {
    doThrow(new DefaultMuleException(new IOException(ERROR_MESSAGE))).when(source).onStart(sourceCallback);

    final Throwable throwable = catchThrowable(messageSource::start);
    assertThat(throwable, is(instanceOf(MuleRuntimeException.class)));
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
  public void failOnExceptionWithNonConnectionExceptionAndGetsExhausted() throws Exception {
    initialise();
    messageSource.start();
    messageSource.onException(new RuntimeException(ERROR_MESSAGE));

    verify(source, times(1)).onStart(sourceCallback);
    verify(source, times(1)).onStop();
  }

  @Test
  public void startFailsWithRandomException() throws Exception {
    Exception e = new RuntimeException();
    doThrow(e).when(source).onStart(sourceCallback);
    expectedException.expectCause(exhaustedBecauseOf(new BaseMatcher<Throwable>() {

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

    verify(muleContext.getSchedulerService()).ioScheduler();
    verify(muleContext.getSchedulerService()).cpuLightScheduler();
    verify(source).onStart(sourceCallback);
    verify(muleContext.getInjector()).inject(source);
  }

  @Test
  public void failedToCreateRetryScheduler() throws Exception {
    Exception e = new RuntimeException();

    SchedulerService schedulerService = muleContext.getSchedulerService();
    when(schedulerService.ioScheduler()).thenThrow(e);

    final Throwable throwable = catchThrowable(messageSource::start);
    assertThat(throwable, is(sameInstance(e)));
  }

  @Test
  public void failedToCreateFlowTrigger() throws Exception {
    Exception e = new RuntimeException();

    SchedulerService schedulerService = muleContext.getRegistry().lookupObject(SchedulerService.class);
    when(schedulerService.cpuLightScheduler()).thenThrow(e);

    final Throwable throwable = catchThrowable(messageSource::start);
    assertThat(throwable, is(sameInstance(e)));
  }

  @Test
  public void stop() throws Exception {
    ((SimpleUnitTestSupportSchedulerService) (muleContext.getSchedulerService())).clearCreatedSchedulers();
    messageSource.initialise();
    messageSource.start();

    List<Scheduler> createdSchedulers =
        ((SimpleUnitTestSupportSchedulerService) (muleContext.getSchedulerService())).getCreatedSchedulers();
    InOrder inOrder = inOrder(source, createdSchedulers.get(0), createdSchedulers.get(1));

    messageSource.stop();
    inOrder.verify(source).onStop();
    inOrder.verify(createdSchedulers.get(0)).stop(anyLong(), any());
    inOrder.verify(createdSchedulers.get(1)).stop(anyLong(), any());
  }

  @Test
  public void enrichExceptionWithSourceExceptionEnricher() throws Exception {
    when(enricherFactory.createEnricher()).thenReturn(new HeisenbergConnectionExceptionEnricher());
    mockExceptionEnricher(sourceModel, enricherFactory);
    mockExceptionEnricher(sourceModel, enricherFactory);
    ExtensionMessageSource messageSource = getNewExtensionMessageSourceInstance();
    doThrow(new RuntimeException(ERROR_MESSAGE)).when(source).onStart(sourceCallback);
    Throwable t = catchThrowable(messageSource::start);

    assertThat(ExceptionUtils.containsType(t, ConnectionException.class), is(true));
    assertThat(t.getMessage(), containsString(ENRICHED_MESSAGE + ERROR_MESSAGE));
  }

  @Test
  public void enrichExceptionWithExtensionEnricher() throws Exception {
    final String enrichedErrorMessage = "Enriched: " + ERROR_MESSAGE;
    ExceptionEnricher exceptionEnricher = mock(ExceptionEnricher.class);
    when(exceptionEnricher.enrichException(any(Exception.class))).thenReturn(new Exception(enrichedErrorMessage));
    when(enricherFactory.createEnricher()).thenReturn(exceptionEnricher);
    mockExceptionEnricher(extensionModel, enricherFactory);
    ExtensionMessageSource messageSource = getNewExtensionMessageSourceInstance();
    doThrow(new RuntimeException(ERROR_MESSAGE)).when(source).onStart(sourceCallback);
    Throwable t = catchThrowable(messageSource::start);

    assertThat(t.getMessage(), containsString(enrichedErrorMessage));
  }

  @Test
  public void workManagerDisposedIfSourceFailsToStart() throws Exception {
    ((SimpleUnitTestSupportSchedulerService) (muleContext.getSchedulerService())).clearCreatedSchedulers();
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

    messageSource.stop();
    List<Scheduler> createdSchedulers =
        ((SimpleUnitTestSupportSchedulerService) (muleContext.getSchedulerService())).getCreatedSchedulers();
    verify(createdSchedulers.get(0)).stop(anyLong(), any());
    verify(createdSchedulers.get(1)).stop(anyLong(), any());
  }

  private ExtensionMessageSource getNewExtensionMessageSourceInstance() throws MuleException {

    ExtensionMessageSource messageSource =
        new ExtensionMessageSource(extensionModel, sourceModel, sourceAdapterFactory, configurationProvider,
                                   retryPolicyTemplate, extensionManager);
    messageSource.setListener(messageProcessor);
    messageSource.setFlowConstruct(flowConstruct);
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
                             sourceCallbackFactory,
                             callbackParameters,
                             callbackParameters);
  }

  private BaseMatcher<Throwable> exhaustedBecauseOf(Matcher<Throwable> causeMatcher) {
    return new BaseMatcher<Throwable>() {

      @Override
      public boolean matches(Object item) {
        Throwable exception = (Throwable) item;
        return exception instanceof RetryPolicyExhaustedException && causeMatcher.matches(exception.getCause());
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
    when(sourceAdapterFactory.createAdapter(any(), any())).thenReturn(sourceAdapter);
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
