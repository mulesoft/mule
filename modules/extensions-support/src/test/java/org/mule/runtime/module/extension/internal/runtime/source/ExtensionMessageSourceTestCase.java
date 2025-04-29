/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.mule.runtime.api.util.MuleSystemProperties.COMPUTE_CONNECTION_ERRORS_IN_STATS_PROPERTY;
import static org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy.FAIL;
import static org.mule.runtime.core.internal.logger.LoggingTestUtils.verifyLogMessage;
import static org.mule.runtime.core.internal.logger.LoggingTestUtils.verifyLogRegex;
import static org.mule.tck.probe.PollingProber.checkNot;
import static org.mule.test.heisenberg.extension.exception.HeisenbergConnectionExceptionEnricher.ENRICHED_MESSAGE;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockExceptionEnricher;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.SECONDS;

import static org.apache.commons.lang3.exception.ExceptionUtils.getThrowables;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.internal.matchers.ThrowableCauseMatcher.hasCause;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.ERROR;
import static org.slf4j.event.Level.INFO;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.notification.ExceptionNotification;
import org.mule.runtime.api.notification.ExceptionNotificationListener;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.retry.async.AsynchronousRetryTemplate;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;
import org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate;
import org.mule.runtime.core.api.util.ExceptionUtils;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.logger.CustomLogger;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.sdk.api.connectivity.oauth.AccessTokenExpiredException;
import org.mule.sdk.api.runtime.exception.ExceptionHandler;
import org.mule.sdk.api.runtime.source.Source;
import org.mule.sdk.api.runtime.source.SourceCallback;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.heisenberg.extension.exception.SdkHeisenbergConnectionExceptionEnricher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.qameta.allure.Issue;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InOrder;
import org.slf4j.LoggerFactory;

@RunWith(Parameterized.class)
public class ExtensionMessageSourceTestCase extends AbstractExtensionMessageSourceTestCase {

  private static final CustomLogger logger = (CustomLogger) LoggerFactory.getLogger(ExtensionMessageSource.class);

  protected static final int TEST_TIMEOUT = 3000;
  protected static final int TEST_POLL_DELAY = 1000;

  protected String property;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"primary node only sync", true, false},
        {"primary node only async", true, true},
        {"all nodes sync", false, false},
        {"all nodes async", false, true}
    });
  }

  public ExtensionMessageSourceTestCase(String name, boolean primaryNodeOnly, boolean isAsync) {
    this.primaryNodeOnly = primaryNodeOnly;
    if (isAsync) {
      this.retryPolicyTemplate = new AsynchronousRetryTemplate(new SimpleRetryPolicyTemplate(0, 2));
    } else {
      SimpleRetryPolicyTemplate template = new SimpleRetryPolicyTemplate(0, 2);
      template.setNotificationFirer(notificationDispatcher);
      this.retryPolicyTemplate = template;
    }
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() {
    property = System.setProperty(COMPUTE_CONNECTION_ERRORS_IN_STATS_PROPERTY, "true");
  }

  @After
  public void restoreProperty() {
    System.clearProperty(COMPUTE_CONNECTION_ERRORS_IN_STATS_PROPERTY);
    logger.resetLevel();
  }

  @Test
  public void handleMessage() throws Exception {
    reset(sourceCallbackFactory);
    when(sourceCallbackFactory.createSourceCallback(any())).thenReturn(sourceCallback);

    AtomicBoolean handled = new AtomicBoolean(false);
    Latch latch = new Latch();

    doAnswer(invocationOnMock -> {
      sourceCallback.handle(result);
      handled.set(true);
      latch.release();
      return null;
    }).when(source).onStart(sourceCallback);

    doAnswer(invocation -> {
      ((Runnable) invocation.getArguments()[0]).run();
      return null;
    }).when(cpuLightScheduler).execute(any());

    start();

    latch.await();
    assertThat(handled.get(), is(true));
  }

  @Test
  public void handleExceptionAndRestart() throws Exception {
    start();
    messageSource.onException(new ConnectionException(ERROR_MESSAGE));
    new PollingProber(TEST_TIMEOUT, TEST_POLL_DELAY).check(new JUnitLambdaProbe(() -> {
      verify(source).onStop();
      return true;
    }));
    verify(ioScheduler, never()).stop();
    verify(cpuLightScheduler, never()).stop();

    new PollingProber(TEST_TIMEOUT, TEST_POLL_DELAY).check(new JUnitLambdaProbe(() -> {
      verify(source, times(2)).onStart(sourceCallback);
      return true;
    }));

  }

  @Test
  public void initialise() throws Exception {
    if (!messageSource.getLifecycleState().isInitialised()) {
      messageSource.initialise();
      verify(muleContext.getInjector()).inject(source);
      verify((Initialisable) source).initialise();
      verify(source, never()).onStart(sourceCallback);
    }
  }

  @Test
  @Issue("W-15923796")
  public void doInitialise() throws Exception {
    executedClassloader.set(Thread.currentThread().getContextClassLoader());
    ExtensionMessageSource messageSource = getNewExtensionMessageSourceOverriddenInstance();
    messageSource.initialise();
    assertThat(executedClassloader.get(), instanceOf(MuleArtifactClassLoader.class));
    assertThat(executedClassloader.get(), is(artifactClassLoader));
  }

  @Test
  public void sourceIsInstantiatedOnce() throws Exception {
    initialise();
    start();
    verify(sourceAdapterFactory, times(1)).createAdapter(any(), any(), any(), any(), anyBoolean());
  }

  @Test
  public void failToStart() throws Exception {
    ConnectionException connectionException = new ConnectionException(ERROR_MESSAGE);
    MuleException e = new DefaultMuleException(connectionException);
    doThrow(e).when(source).onStart(any());

    messageSource.initialise();
    if (!this.retryPolicyTemplate.isAsync()) {
      var thrown = assertThrows(RetryPolicyExhaustedException.class, () -> messageSource.start());
      assertThat(thrown.getCause(), is(connectionException));
    } else {
      messageSource.start();
    }
  }

  @Test
  public void dispatchNotificationWhenFailToStart() throws Exception {
    final Latch latch = new Latch();
    final List<ExceptionNotification> notifications = new ArrayList<>();
    final ExceptionNotificationListener listener = notification -> {
      notifications.add(notification);
      latch.release();
    };
    registerNotificationListener(listener);

    ConnectionException connectionException = new ConnectionException(ERROR_MESSAGE);
    MuleException e = new DefaultMuleException(connectionException);
    doThrow(e).when(source).onStart(any());

    messageSource.initialise();
    try {
      messageSource.start();
      latch.await(5, SECONDS);
      assertThat(notifications.get(0), is(instanceOf(ExceptionNotification.class)));
      assertThat(notifications.get(0).getException(), instanceOf(RetryPolicyExhaustedException.class));
    } catch (Exception ex) {
      latch.await(5, SECONDS);
      assertThat(notifications.get(0), is(instanceOf(ExceptionNotification.class)));
      assertThat(notifications.get(0).getException(), instanceOf(RetryPolicyExhaustedException.class));
    }
  }

  @Test
  public void failToStartAndStopFails() throws Exception {
    final Latch latch = new Latch();
    final List<ExceptionNotification> notifications = new ArrayList<>();
    final ExceptionNotificationListener listener = notification -> {
      notifications.add(notification);
      latch.release();
    };
    registerNotificationListener(listener);

    ConnectionException connectionException = new ConnectionException(ERROR_MESSAGE);
    MuleException e = new DefaultMuleException(connectionException);
    doThrow(e).when(source).onStart(any());
    doThrow(new NullPointerException()).when(source).onStop();

    messageSource.initialise();

    // Non async retries will make the test thread fail.
    if (!this.retryPolicyTemplate.isAsync()) {
      var thrown = assertThrows(RetryPolicyExhaustedException.class, () -> messageSource.start());
      assertThat(thrown.getCause(), is(connectionException));
    } else {
      messageSource.start();

      // Async retries will fail on a different thread but must send an error notification.
      latch.await(TEST_TIMEOUT, SECONDS);
      assertThat(notifications.get(0).getException(), is(instanceOf(RetryPolicyExhaustedException.class)));
      assertThat(notifications.get(0).getException().getCause(), is(connectionException));
    }
  }

  @Test
  public void failWithConnectionExceptionWhenStartingAndGetRetryPolicyExhausted() throws Exception {
    messageSource.initialise();
    final ConnectionException connectionException = new ConnectionException(ERROR_MESSAGE);
    doThrow(new RuntimeException(connectionException)).when(source).onStart(sourceCallback);

    final Throwable throwable = catchThrowable(messageSource::start);
    if (!this.retryPolicyTemplate.isAsync()) {
      assertThat(throwable, is(instanceOf(RetryPolicyExhaustedException.class)));
      assertThat(throwable, is(exhaustedBecauseOf(connectionException)));
    } else {
      new PollingProber(TEST_TIMEOUT, TEST_POLL_DELAY).check(new JUnitLambdaProbe(() -> {
        assertNull(throwable);
        return true;
      }));
    }

    new PollingProber(TEST_TIMEOUT, TEST_POLL_DELAY).check(new JUnitLambdaProbe(() -> {
      verify(source, times(3)).onStart(sourceCallback);
      return true;
    }));
  }

  @Test
  public void failWithNonConnectionExceptionWhenStartingAndGetRetryPolicyExhausted() throws Exception {
    doThrow(new DefaultMuleException(new IOException(ERROR_MESSAGE))).when(source).onStart(sourceCallback);

    messageSource.initialise();
    final Throwable throwable = catchThrowable(messageSource::start);
    if (!this.retryPolicyTemplate.isAsync()) {
      assertThat(throwable, is(instanceOf(RetryPolicyExhaustedException.class)));
      assertThat(getThrowables(throwable), hasItemInArray(instanceOf(IOException.class)));
    } else {
      new PollingProber(TEST_TIMEOUT, TEST_POLL_DELAY).check(new JUnitLambdaProbe(() -> {
        assertNull(throwable);
        return true;
      }));
    }
    new PollingProber(TEST_TIMEOUT, TEST_POLL_DELAY).check(new JUnitLambdaProbe(() -> {
      verify(source, times(3)).onStart(sourceCallback);
      return true;
    }));
  }

  @Test
  public void failWithConnectionExceptionWhenStartingAndGetsReconnected() throws Exception {
    doThrow(new RuntimeException(new ConnectionException(ERROR_MESSAGE)))
        .doThrow(new RuntimeException(new ConnectionException(ERROR_MESSAGE))).doNothing().when(source).onStart(sourceCallback);

    messageSource.initialise();
    messageSource.start();

    new PollingProber(TEST_TIMEOUT, TEST_POLL_DELAY).check(new JUnitLambdaProbe(() -> {
      verify(source, times(3)).onStart(sourceCallback);
      verify(source, times(2)).onStop();
      return true;
    }));
  }

  @Test
  public void getBackPressureStrategy() {
    assertThat(messageSource.getBackPressureStrategy(), is(FAIL));
  }

  @Test
  public void failOnExceptionWithConnectionExceptionAndGetsReconnected() throws Exception {
    messageSource.initialise();
    messageSource.start();
    messageSource.onException(new ConnectionException(ERROR_MESSAGE));

    new PollingProber(TEST_TIMEOUT, TEST_POLL_DELAY).check(new JUnitLambdaProbe(() -> {
      verify(source, times(2)).onStart(sourceCallback);
      verify(source, times(1)).onStop();
      return true;
    }));
  }

  @Test
  public void failOnExceptionWithAccessTokenExpiredExceptionInConnectionExceptionAndGetsReconnected() throws Exception {
    messageSource.initialise();
    messageSource.start();
    when(configurationInstance.getConnectionProvider()).thenReturn(of(mock(ConnectionProvider.class)));
    messageSource.onException(new ConnectionException(new AccessTokenExpiredException(ERROR_MESSAGE)));

    new PollingProber(TEST_TIMEOUT, TEST_POLL_DELAY).check(new JUnitLambdaProbe(() -> {
      verify(source, times(2)).onStart(sourceCallback);
      verify(source, times(1)).onStop();
      return true;
    }));
  }

  @Test
  public void startFailsWithRandomException() throws Exception {
    Exception e = new RuntimeException();
    doThrow(e).when(source).onStart(sourceCallback);
    initialise();

    if (!this.retryPolicyTemplate.isAsync()) {
      var thrown = assertThrows(Exception.class, () -> messageSource.start());
      assertThat(thrown, exhaustedBecauseOf(new BaseMatcher<Throwable>() {

        private final Matcher<Exception> exceptionMatcher = hasCause(sameInstance(e));

        @Override
        public boolean matches(Object item) {
          return exceptionMatcher.matches(item);
        }

        @Override
        public void describeTo(Description description) {
          exceptionMatcher.describeTo(description);
        }
      }));
    } else {
      messageSource.start();
    }


    new PollingProber(TEST_TIMEOUT, TEST_POLL_DELAY).check(new JUnitLambdaProbe(() -> {
      verify(source, times(3)).onStart(sourceCallback);
      verify(source, times(3)).onStop();
      return true;
    }));
  }

  @Test
  public void start() throws Exception {
    initialise();
    logger.resetLogs();
    logger.setLevel(INFO);
    if (!messageSource.getLifecycleState().isStarted()) {
      messageSource.start();
    }

    final Injector injector = muleContext.getInjector();
    new PollingProber(TEST_TIMEOUT, TEST_POLL_DELAY).check(new JUnitLambdaProbe(() -> {
      InOrder inOrder = inOrder(injector, source);
      inOrder.verify(injector).inject(source);
      inOrder.verify((Initialisable) source).initialise();
      inOrder.verify(source).onStart(sourceCallback);
      return true;
    }));

    new PollingProber(TEST_TIMEOUT, TEST_POLL_DELAY).check(new JUnitLambdaProbe(() -> {
      verifyLogMessage(logger.getMessages(), "Message source 'source' on flow 'appleFlow' successfully started");
      return true;
    }));
    checkNot(TEST_POLL_DELAY, TEST_TIMEOUT, () -> {
      verifyLogMessage(logger.getMessages(), "Message source 'source' on flow 'appleFlow' successfully reconnected");
      return true;
    });
  }

  @Test
  public void failedToCreateRetryScheduler() throws Exception {
    messageSource.initialise();
    Exception e = new RuntimeException();

    SchedulerService schedulerService = muleContext.getSchedulerService();
    doThrow(e).when(schedulerService).ioScheduler();

    final Throwable throwable = catchThrowable(messageSource::start);
    assertThat(throwable.getCause(), is(sameInstance(e)));
  }

  @Test
  public void stop() throws Exception {
    messageSource.initialise();
    messageSource.start();

    messageSource.stop();
    verify(source).onStop();
  }

  @Test
  public void dispose() throws Exception {
    messageSource.initialise();
    messageSource.start();
    messageSource.stop();

    messageSource.dispose();
    verify((Disposable) source).dispose();
  }

  @Test
  public void enrichExceptionWithSourceExceptionEnricher() throws Exception {
    when(enricherFactory.createHandler()).thenReturn(new SdkHeisenbergConnectionExceptionEnricher());
    mockExceptionEnricher(sourceModel, enricherFactory);
    mockExceptionEnricher(sourceModel, enricherFactory);
    ExtensionMessageSource messageSource = getNewExtensionMessageSourceInstance();
    messageSource.initialise();

    doThrow(new RuntimeException(ERROR_MESSAGE)).when(source).onStart(sourceCallback);
    Throwable t = catchThrowable(messageSource::start);

    if (!this.retryPolicyTemplate.isAsync()) {
      assertThat(ExceptionUtils.containsType(t, ConnectionException.class), is(true));
      assertThat(t.getMessage(), containsString(ENRICHED_MESSAGE + ERROR_MESSAGE));
    } else {
      new PollingProber(TEST_TIMEOUT, TEST_POLL_DELAY).check(new JUnitLambdaProbe(() -> {
        assertNull(t);
        return true;
      }));
    }

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
    messageSource.initialise();

    doThrow(new RuntimeException(ERROR_MESSAGE)).when(source).onStart(sourceCallback);
    Throwable t = catchThrowable(messageSource::start);

    if (!this.retryPolicyTemplate.isAsync()) {
      assertThat(t.getMessage(), containsString(enrichedErrorMessage));
    } else {
      new PollingProber(TEST_TIMEOUT, TEST_POLL_DELAY).check(new JUnitLambdaProbe(() -> {
        assertNull(t);
        return true;
      }));
    }
    messageSource.stop();
  }

  @Test
  public void workManagerDisposedIfSourceFailsToStop() throws Exception {
    start();

    Exception e = new RuntimeException();
    doThrow(e).when(source).onStop();

    var thrown = assertThrows(Exception.class, () -> stop());
    assertThat(thrown, new BaseMatcher<Throwable>() {

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

  @Test
  public void actualSourceStoppedIfMessageSourceFailsToStop() throws Exception {
    when(configurationProvider.isDynamic()).thenReturn(true);
    start();

    Exception e = new RuntimeException();
    doThrow(e).when(configurationProvider).get(any(CoreEvent.class));

    var thrown = assertThrows(Exception.class, () -> messageSource.stop());
    assertThat(thrown.getCause(), sameInstance(e));
    verify(source).onStop();
  }

  @Test
  public void reconnectTwice() throws Exception {
    start();

    messageSource.onException(new ConnectionException(ERROR_MESSAGE));
    new PollingProber(TEST_TIMEOUT, TEST_POLL_DELAY).check(new JUnitLambdaProbe(() -> !messageSource.isReconnecting()));

    verify(source, times(2)).onStart(sourceCallback);
    verify(source, times(1)).onStop();

    messageSource.onException(new ConnectionException(ERROR_MESSAGE));
    new PollingProber(TEST_TIMEOUT, TEST_POLL_DELAY).check(new JUnitLambdaProbe(() -> !messageSource.isReconnecting()));

    new PollingProber(TEST_TIMEOUT, TEST_POLL_DELAY).check(new JUnitLambdaProbe(() -> {
      verify(source, times(3)).onStart(sourceCallback);
      verify(source, times(2)).onStop();
      return true;
    }));
  }

  @Test
  public void failToReconnect() throws Exception {
    start();
    ConnectionException connectionException = new ConnectionException(ERROR_MESSAGE);
    doThrow(connectionException).when(source).onStart(any());

    messageSource.onException(connectionException);
    new PollingProber(TEST_TIMEOUT, TEST_POLL_DELAY).check(new JUnitLambdaProbe(() -> !messageSource.isReconnecting()));

    new PollingProber(TEST_TIMEOUT, TEST_POLL_DELAY).check(new JUnitLambdaProbe(() -> {
      verify(source, times(4)).onStart(sourceCallback);
      verify(source, times(4)).onStop();
      return true;
    }));
  }

  private BaseMatcher<Throwable> exhaustedBecauseOf(Throwable cause) {
    return exhaustedBecauseOf(sameInstance(cause));
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
    when(sourceAdapterFactory.createAdapter(any(), any(), any(), any(), anyBoolean())).thenReturn(sourceAdapter);
    messageSource = getNewExtensionMessageSourceInstance();
    messageSource.initialise();
    messageSource.start();
    final Object metadataKeyValue = messageSource.getParameterValueResolver().getParameterValue(METADATA_KEY);
    assertThat(metadataKeyValue, is(person));
  }

  @Test
  public void getRetryPolicyExhaustedAndConnectionErrorsAreComputed() throws Exception {

    muleContext.getStatistics().setEnabled(true);
    messageSource.initialise();
    final ConnectionException connectionException = new ConnectionException(ERROR_MESSAGE);
    doThrow(new RuntimeException(connectionException)).when(source).onStart(sourceCallback);
    final Throwable throwable = catchThrowable(messageSource::start);

    new PollingProber(TEST_TIMEOUT, TEST_POLL_DELAY).check(new JUnitLambdaProbe(() -> {
      assertThat(muleContext.getStatistics().getApplicationStatistics().getConnectionErrors(), equalTo(2l));
      assertThat(muleContext.getStatistics().getApplicationStatistics().getExecutionErrors(), equalTo(2l));
      return true;
    }));
  }

  @Test
  public void sourceInitializedLogMessage() throws Exception {
    logger.resetLogs();
    logger.setLevel(DEBUG);
    messageSource.initialise();
    if (primaryNodeOnly) {
      verifyLogMessage(logger.getMessages(),
                       "Message source 'source' on flow 'appleFlow' running on the primary node is initializing. Note that this Message source must run on the primary node only.");
    } else {
      verifyLogMessage(logger.getMessages(),
                       "Message source 'source' on flow 'appleFlow' is initializing. This is the primary node of the cluster.");
    }
  }

  @Test
  public void sourceStartedLogMessage() throws Exception {
    ArrayList<String> debugMessages = new ArrayList<>();
    logger.resetLogs();
    logger.setLevel(DEBUG);
    messageSource.initialise();
    messageSource.start();
    verifyLogMessage(logger.getMessages(), "Message source 'source' on flow 'appleFlow' is starting");
  }

  @Test
  public void sourceStoppedLogMessage() throws Exception {
    logger.resetLogs();
    logger.setLevel(DEBUG);
    messageSource.initialise();
    messageSource.start();
    messageSource.stop();
    verifyLogMessage(logger.getMessages(), "Message source 'source' on flow 'appleFlow' is stopping");
  }


  @Test
  public void getRetryPolicyExhaustedAndLogShutdownMessage() throws Exception {
    ArrayList<String> errorMessages = new ArrayList<>();
    logger.resetLogs();
    logger.setLevel(ERROR);
    start();
    ConnectionException e = new ConnectionException(ERROR_MESSAGE);
    doThrow(e).when(source).onStart(any());
    messageSource.onException(e);
    new PollingProber(TEST_TIMEOUT, TEST_POLL_DELAY).check(new JUnitLambdaProbe(() -> {
      verifyLogRegex(logger.getMessages(),
                     "Message source 'source' on flow 'appleFlow' could not be reconnected. Will be shutdown. (.*)");
      return true;
    }));
  }

  @Test
  public void reconnectAndLogSuccessMessage() throws Exception {
    start();
    logger.resetLogs();
    logger.setLevel(INFO);
    ConnectionException e = new ConnectionException(ERROR_MESSAGE);
    messageSource.onException(e);
    new PollingProber(TEST_TIMEOUT, TEST_POLL_DELAY).check(new JUnitLambdaProbe(() -> {
      verifyLogMessage(logger.getMessages(), "Message source 'source' on flow 'appleFlow' successfully reconnected");
      return true;
    }));
  }

  private void registerNotificationListener(ExceptionNotificationListener exceptionNotificationListener)
      throws RegistrationException {
    MuleRegistry muleRegistry = ((MuleContextWithRegistry) muleContext).getRegistry();
    NotificationListenerRegistry notificationListenerRegistry = muleRegistry.lookupObject(NotificationListenerRegistry.class);
    notificationListenerRegistry.registerListener(exceptionNotificationListener);
  }

  private class DummySource extends Source {

    DummySource(String metadataKey) {

      this.metadataKey = metadataKey;
    }

    private final String metadataKey;

    @Override
    public void onStart(SourceCallback sourceCallback) throws MuleException {}

    @Override
    public void onStop() {}
  }
}
