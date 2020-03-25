/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.exception.ExceptionUtils.getThrowables;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
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
import static org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy.FAIL;
import static org.mule.test.heisenberg.extension.exception.HeisenbergConnectionExceptionEnricher.ENRICHED_MESSAGE;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockExceptionEnricher;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;
import org.mule.runtime.core.api.util.ExceptionUtils;
import org.mule.runtime.extension.api.runtime.exception.ExceptionHandler;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.heisenberg.extension.exception.HeisenbergConnectionExceptionEnricher;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.resource.spi.work.Work;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InOrder;

@RunWith(Parameterized.class)
public class ExtensionMessageSourceTestCase extends AbstractExtensionMessageSourceTestCase {

  private static final int TEST_TIMEOUT = 2000;
  private static final int TEST_POLL_DELAY = 10;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"primary node only", true},
        {"all nodes", false}
    });
  }

  public ExtensionMessageSourceTestCase(String name, boolean primaryNodeOnly) {
    this.primaryNodeOnly = primaryNodeOnly;
  }

  @Test
  public void handleMessage() throws Exception {
    reset(sourceCallbackFactory);
    when(sourceCallbackFactory.createSourceCallback(any())).thenReturn(sourceCallback);

    AtomicBoolean handled = new AtomicBoolean(false);

    doAnswer(invocationOnMock -> {
      sourceCallback.handle(result);
      handled.set(true);
      return null;
    }).when(source).onStart(sourceCallback);

    doAnswer(invocation -> {
      ((Work) invocation.getArguments()[0]).run();
      return null;
    }).when(cpuLightScheduler).execute(any());

    start();

    assertThat(handled.get(), is(true));
  }

  @Test
  public void handleExceptionAndRestart() throws Exception {
    start();
    messageSource.onException(new ConnectionException(ERROR_MESSAGE));
    verify(source).onStop();
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
    expectedException.expect(is(instanceOf(RetryPolicyExhaustedException.class)));
    expectedException.expectCause(is(connectionException));

    messageSource.initialise();
    messageSource.start();
  }

  @Test
  public void failToStartAndStopFails() throws Exception {
    ConnectionException connectionException = new ConnectionException(ERROR_MESSAGE);
    MuleException e = new DefaultMuleException(connectionException);
    doThrow(e).when(source).onStart(any());
    doThrow(new NullPointerException()).when(source).onStop();
    expectedException.expect(is(instanceOf(RetryPolicyExhaustedException.class)));
    expectedException.expectCause(is(connectionException));

    messageSource.initialise();
    messageSource.start();
  }

  @Test
  public void failWithConnectionExceptionWhenStartingAndGetRetryPolicyExhausted() throws Exception {
    messageSource.initialise();
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

    messageSource.initialise();
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
  public void startFailsWithRandomException() throws Exception {
    Exception e = new RuntimeException();
    doThrow(e).when(source).onStart(sourceCallback);
    expectedException.expect(exhaustedBecauseOf(new BaseMatcher<Throwable>() {

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

    initialise();
    messageSource.start();
  }

  @Test
  public void start() throws Exception {
    initialise();
    if (!messageSource.getLifecycleState().isStarted()) {
      messageSource.start();
    }

    final Injector injector = muleContext.getInjector();
    InOrder inOrder = inOrder(injector, source);
    inOrder.verify(injector).inject(source);
    inOrder.verify((Initialisable) source).initialise();
    inOrder.verify(source).onStart(sourceCallback);
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
    when(enricherFactory.createHandler()).thenReturn(new HeisenbergConnectionExceptionEnricher());
    mockExceptionEnricher(sourceModel, enricherFactory);
    mockExceptionEnricher(sourceModel, enricherFactory);
    ExtensionMessageSource messageSource = getNewExtensionMessageSourceInstance();
    messageSource.initialise();

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
    messageSource.initialise();

    doThrow(new RuntimeException(ERROR_MESSAGE)).when(source).onStart(sourceCallback);
    Throwable t = catchThrowable(messageSource::start);

    assertThat(t.getMessage(), containsString(enrichedErrorMessage));

    messageSource.stop();
  }

  @Test
  public void workManagerDisposedIfSourceFailsToStop() throws Exception {
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

  @Test
  public void actualSourceStoppedIfMessageSourceFailsToStop() throws Exception {
    when(configurationProvider.isDynamic()).thenReturn(true);
    start();

    Exception e = new RuntimeException();
    doThrow(e).when(configurationProvider).get(any(CoreEvent.class));

    expectedException.expectCause(sameInstance(e));

    try {
      messageSource.stop();
    } finally {
      verify(source).onStop();
    }
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
