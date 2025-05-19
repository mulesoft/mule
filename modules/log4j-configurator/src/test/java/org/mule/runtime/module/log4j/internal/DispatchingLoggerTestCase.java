/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.log4j.internal;

import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.artifact.api.classloader.BlockingLoggerResolutionClassRegistry.getBlockingLoggerResolutionClassRegistry;
import static org.mule.test.allure.AllureConstants.Logging.LOGGING;
import static org.mule.test.allure.AllureConstants.Logging.LoggingStory.CONTEXT_FACTORY;

import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.util.Reference;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.size.SmallTest;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.message.MessageFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@SmallTest
@Feature(LOGGING)
@Story(CONTEXT_FACTORY)
public class DispatchingLoggerTestCase extends AbstractMuleTestCase {

  private static final String LOGGER_NAME = DispatchingLoggerTestCase.class.getName();

  private static final String MESSAGE = "Hello Log!";

  private static final long PROBER_POLLING_TIMEOUT = 5000;
  private static final long PROBER_POLLING_INTERVAL = 100;

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  private ClassLoader currentClassLoader;

  @Mock
  private ClassLoader additionalClassLoader;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private Logger originalLogger;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private LoggerContext containerLoggerContext;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ContextSelector contextSelector;

  @Mock
  private ArtifactAwareContextSelector artifactAwareContextSelector;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private MessageFactory messageFactory;

  @Mock
  RegionClassLoader regionClassLoader;

  @Mock(answer = RETURNS_DEEP_STUBS)
  LoggerContext regionClassLoaderLoggerContext;

  private Logger logger;

  private LoggerContextCache loggerContextCache;

  @Before
  public void before() {
    currentClassLoader = Thread.currentThread().getContextClassLoader();
    when(containerLoggerContext.getConfiguration().getLoggerConfig(anyString()).getLevel()).thenReturn(Level.INFO);
    logger =
        new DispatchingLogger(originalLogger, currentClassLoader.hashCode(), containerLoggerContext, contextSelector,
                              messageFactory) {

          @Override
          public String getName() {
            return LOGGER_NAME;
          }
        };
    loggerContextCache = spy(new LoggerContextCache(artifactAwareContextSelector, mock(ClassLoader.class, RETURNS_DEEP_STUBS)));
  }

  @After
  public void after() {
    loggerContextCache.dispose();
  }

  @Test
  @Issue("W-18388443")
  public void noParallelizationWhenGettingLoggerFirstForClassNeedingBlockingAndThenForRegularClass() throws Exception {
    Logger regionClassLoaderLogger = getRegionClassLoader();

    final Latch latch = new Latch();
    final AtomicInteger getLoggerContextTimesCalled = new AtomicInteger(0);
    final AtomicBoolean getLoggerBlockingCalled = new AtomicBoolean(false);
    final AtomicBoolean getLoggerNonBlockingCalled = new AtomicBoolean(false);

    ClassOwningLoggerNeedingBlocking blockingLoggerOwner = new ClassOwningLoggerNeedingBlocking();
    ClassOwningLogger nonBlockingLoggerOwner = new ClassOwningLogger();
    DispatchingLogger blockingLogger = blockingLoggerOwner.getDispatchingLogger();
    DispatchingLogger nonBlockingLogger = nonBlockingLoggerOwner.getDispatchingLogger();

    doAnswer(invocation -> {
      assertThat(getLoggerBlockingCalled.compareAndSet(false, true), is(true));
      // Make the call wait here so the call for the non-blocking logger owner class isn't made because of the lock of the
      // `LoggerContextCache` instance being taken in `DispatchingLogger#getLogger(ClassLoader, Reference)`
      assertThat(latch.await(1000, MILLISECONDS), is(true));

      return invocation.callRealMethod();
    }).when(blockingLogger).getLogger(any(ClassLoader.class), any(Reference.class));

    doAnswer(invocation -> {
      assertThat(getLoggerNonBlockingCalled.compareAndSet(false, true), is(true));

      return invocation.callRealMethod();
    }).when(nonBlockingLogger).getLogger(any(ClassLoader.class), any(Reference.class));

    doAnswer(invocation -> {
      getLoggerContextTimesCalled.incrementAndGet();

      return regionClassLoaderLoggerContext;
    }).when(loggerContextCache).doGetLoggerContext(eq(regionClassLoader), any());

    String log1 = "Log 1";
    String log2 = "Log 2";

    Thread blockingLogThread = new Thread(() -> blockingLogger.info(log1));
    Thread nonBlockingLogThread = new Thread(() -> nonBlockingLogger.info(log2));

    blockingLogThread.setContextClassLoader(regionClassLoader);
    nonBlockingLogThread.setContextClassLoader(regionClassLoader);

    blockingLogThread.start();

    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      assertThat(getLoggerBlockingCalled.get(), is(true));
      return true;
    }));

    verify(regionClassLoaderLogger, times(0)).info(log1);
    verify(regionClassLoaderLogger, times(0)).info(log2);

    nonBlockingLogThread.start();

    // The second (non-blocking) logger will be allowed to get into the `getLogger` method, but won't be able to take the
    // synchronize over the `LoggerContextCache` instance and thus get into the `getLoggerContext` method of the latter
    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      assertThat(getLoggerNonBlockingCalled.get(), is(true));
      return true;
    }));

    // Give some time to check second logger didn't get into `getLogger` method due to the lock
    sleep(200);
    assertThat(getLoggerContextTimesCalled.get(), is(0));

    // Let the first logger continue
    latch.countDown();

    // Check the first message is logged
    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      verify(regionClassLoaderLogger, times(1)).info(log1);
      return true;
    }));

    // Now the second message should be logged
    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      assertThat(getLoggerContextTimesCalled.get(), is(2));
      return true;
    }));

    // Check the second message is logged
    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      verify(regionClassLoaderLogger, times(1)).info(log2);
      return true;
    }));

    blockingLogThread.join();
    nonBlockingLogThread.join();
  }

  @Test
  @Issue("W-18388443")
  public void noParallelizationWhenGettingLoggerFirstForRegularClassAndThenForClassNeedingBlocking() throws Exception {
    Logger regionClassLoaderLogger = getRegionClassLoader();

    final Latch latch = new Latch();
    final AtomicInteger getLoggerContextTimesCalled = new AtomicInteger(0);
    final AtomicBoolean getLoggerBlockingCalled = new AtomicBoolean(false);

    ClassOwningLogger nonBlockingLoggerOwner = new ClassOwningLogger();
    ClassOwningLoggerNeedingBlocking blockingLoggerOwner = new ClassOwningLoggerNeedingBlocking();
    DispatchingLogger nonBlockingLogger = nonBlockingLoggerOwner.getDispatchingLogger();
    DispatchingLogger blockingLogger = blockingLoggerOwner.getDispatchingLogger();

    doAnswer(invocation -> {
      if (getLoggerContextTimesCalled.incrementAndGet() == 1) {
        // Make the first call wait here so the second logger doesn't get to `DispatchingLogger#getLogger(ClassLoader, Reference)`
        // because of the lock of the `LoggerContextCache` instance being taken in
        // `LoggerContextCache#getLoggerContext(ClassLoader)`
        assertThat(latch.await(1000, MILLISECONDS), is(true));
      }

      return regionClassLoaderLoggerContext;
    }).when(loggerContextCache).doGetLoggerContext(eq(regionClassLoader), any());

    doAnswer(invocation -> {
      assertThat(getLoggerBlockingCalled.compareAndSet(false, true), is(true));

      return invocation.callRealMethod();
    }).when(blockingLogger).getLogger(any(ClassLoader.class), any(Reference.class));

    String log1 = "Log 1";
    String log2 = "Log 2";

    Thread nonBlockingLogThread = new Thread(() -> nonBlockingLogger.info(log1));
    Thread blockingLogThread = new Thread(() -> blockingLogger.info(log2));

    nonBlockingLogThread.setContextClassLoader(regionClassLoader);
    blockingLogThread.setContextClassLoader(regionClassLoader);

    nonBlockingLogThread.start();

    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      assertThat(getLoggerContextTimesCalled.get(), is(1));
      return true;
    }));

    verify(regionClassLoaderLogger, times(0)).info(log1);
    verify(regionClassLoaderLogger, times(0)).info(log2);

    blockingLogThread.start();

    // Give some time to check second invocation didn't get into `getLogger` method due to the lock
    sleep(200);
    assertThat(getLoggerBlockingCalled.get(), is(false));

    // Let the first logger continue
    latch.countDown();

    // Check the first message is logged
    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      verify(regionClassLoaderLogger, times(1)).info(log1);
      return true;
    }));

    // Now the second message should be logged
    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      assertThat(getLoggerBlockingCalled.get(), is(true));
      return true;
    }));

    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      assertThat(getLoggerContextTimesCalled.get(), is(2));
      return true;
    }));

    // Check the second message is logged
    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      verify(regionClassLoaderLogger, times(1)).info(log2);
      return true;
    }));

    nonBlockingLogThread.join();
    blockingLogThread.join();
  }

  private class ClassOwningLogger {

    private final Logger logger = getLogger();

    private Logger getLogger() {
      Logger logger = mock(Logger.class, RETURNS_DEEP_STUBS);
      when(logger.getName()).thenReturn(this.getClass().getName());

      return logger;
    }

    public DispatchingLogger getDispatchingLogger() {

      return spy(new DispatchingLogger(logger, currentClassLoader.hashCode(), containerLoggerContext,
                                       artifactAwareContextSelector,
                                       messageFactory) {

        @Override
        public String getName() {
          return LOGGER_NAME;
        }
      });
    }

  }

  private class ClassOwningLoggerNeedingBlocking extends ClassOwningLogger {

    static {
      getBlockingLoggerResolutionClassRegistry()
          .registerClassNeedingBlockingLoggerResolution(ClassOwningLoggerNeedingBlocking.class);
    }

  }

  private Logger getRegionClassLoader() throws ExecutionException {
    Logger containerLogger = mock(Logger.class);
    Logger regionClassLoaderLogger = mock(Logger.class);
    when(containerLoggerContext.getLogger(anyString(), any(MessageFactory.class))).thenReturn(containerLogger);
    when(regionClassLoaderLoggerContext.getLogger(anyString(), any(MessageFactory.class)))
        .thenReturn(regionClassLoaderLogger);
    // Triggers of the expected Loggers
    when(artifactAwareContextSelector.getContextWithResolvedContextClassLoader(any()))
        .thenAnswer(invocation -> loggerContextCache.getLoggerContext(invocation.getArgument(0)));
    when(artifactAwareContextSelector.getLoggerContextCache()).thenReturn(loggerContextCache);
    doAnswer(invocationOnMock -> containerLoggerContext)
        .when(loggerContextCache).doGetLoggerContext(eq(currentClassLoader), any());

    return regionClassLoaderLogger;
  }

  @Test
  public void currentClassLoader() {
    logger.info(MESSAGE);
    verify(originalLogger).info(MESSAGE);
  }

  @Test
  public void anotherClassLoader() {
    withContextClassLoader(additionalClassLoader, () -> {
      logger.info(MESSAGE);
      verify(originalLogger).info(MESSAGE);
    });
  }

  @Test
  public void regionClassLoader() {
    RegionClassLoader regionClassLoader = mock(RegionClassLoader.class);
    withContextClassLoader(regionClassLoader, () -> {
      logger.info(MESSAGE);
      verify(contextSelector).getContext(LOGGER_NAME, regionClassLoader, true);
    });
  }

  @Test
  public void whenRecursiveLoggerContextInstantiationExceptionExpectFallbackUsingContainerClassLoader() {
    // Expected Loggers
    Logger containerLogger = mock(Logger.class);
    Logger regionClassLoaderLogger = mock(Logger.class);
    when(containerLoggerContext.getLogger(anyString(), any(MessageFactory.class))).thenReturn(containerLogger);
    when(regionClassLoaderLoggerContext.getLogger(anyString(), any(MessageFactory.class))).thenReturn(regionClassLoaderLogger);
    when(artifactAwareContextSelector.getContextWithResolvedContextClassLoader(currentClassLoader))
        .thenAnswer(invocation -> containerLoggerContext);
    // Triggers of the expected Loggers
    when(artifactAwareContextSelector.getContextWithResolvedContextClassLoader(regionClassLoader))
        .thenThrow(RecursiveLoggerContextInstantiationException.class)
        .thenAnswer(invocation -> regionClassLoaderLoggerContext);
    // Class under test
    DispatchingLogger dispatchingLogger = new DispatchingLogger(originalLogger, currentClassLoader.hashCode(),
                                                                containerLoggerContext, artifactAwareContextSelector,
                                                                messageFactory) {

      @Override
      public String getName() {
        return LOGGER_NAME;
      }
    };
    // Test and assertions
    withContextClassLoader(regionClassLoader, () -> {
      dispatchingLogger.info("Fallback Test Message");
      dispatchingLogger.info("Test Message");
    });
    verify(containerLogger, times(1)).info("Fallback Test Message");
    verify(regionClassLoaderLogger, times(1)).info("Test Message");
  }

  @Test
  public void whenFallbackToContainerClassLoaderFailsReturnOriginalLogger() {
    // Expected Loggers
    Logger containerLogger = mock(Logger.class);
    Logger regionClassLoaderLogger = mock(Logger.class);
    when(containerLoggerContext.getLogger(anyString(), any(MessageFactory.class))).thenReturn(containerLogger);
    when(regionClassLoaderLoggerContext.getLogger(anyString(), any(MessageFactory.class))).thenReturn(regionClassLoaderLogger);
    when(artifactAwareContextSelector.getContextWithResolvedContextClassLoader(currentClassLoader))
        .thenThrow(RecursiveLoggerContextInstantiationException.class)
        .thenAnswer(invocation -> containerLoggerContext);
    // Triggers of the expected Loggers
    when(artifactAwareContextSelector.getContextWithResolvedContextClassLoader(regionClassLoader))
        .thenThrow(RecursiveLoggerContextInstantiationException.class)
        .thenAnswer(invocation -> regionClassLoaderLoggerContext);
    // Class under test
    DispatchingLogger dispatchingLogger = new DispatchingLogger(originalLogger, currentClassLoader.hashCode(),
                                                                containerLoggerContext, artifactAwareContextSelector,
                                                                messageFactory) {

      @Override
      public String getName() {
        return LOGGER_NAME;
      }
    };
    // Test and assertions
    withContextClassLoader(regionClassLoader, () -> {
      dispatchingLogger.info("Fallback Test Message");
      dispatchingLogger.info("Test Message");
    });
    verify(originalLogger, times(1)).info("Fallback Test Message");
    verify(regionClassLoaderLogger, times(1)).info("Test Message");
  }

}
