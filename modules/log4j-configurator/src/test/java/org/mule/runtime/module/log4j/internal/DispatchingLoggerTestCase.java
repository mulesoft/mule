/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.log4j.internal;

import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.test.allure.AllureConstants.Logging.LOGGING;
import static org.mule.test.allure.AllureConstants.Logging.LoggingStory.CONTEXT_FACTORY;

import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.util.Reference;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.size.SmallTest;

import java.util.concurrent.atomic.AtomicInteger;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.message.MessageFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Answers;
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
  }

  @Test
  public void parallelizationWhenGettingLoggerForSameClassAndDifferentCtxClassLoader() throws InterruptedException {
    Logger originalLogger = getLogger("org.mule.runtime.module.artifact.api.classloader.FineGrainedControlClassLoader");
    Logger regionClassLoaderLogger = getRegionClassLoader();

    DispatchingLogger logger =
        spy(new DispatchingLogger(originalLogger, currentClassLoader.hashCode(), containerLoggerContext,
                                  artifactAwareContextSelector,
                                  messageFactory) {

          @Override
          public String getName() {
            return LOGGER_NAME;
          }
        });

    final Latch latch = new Latch();
    final AtomicInteger timesCalled = new AtomicInteger(0);

    doAnswer(invocation -> {
      if (timesCalled.compareAndSet(0, 1)) {
        // Make the first call wait here so the second one doesn't get into this method due to the lock
        assertThat(latch.await(1000, MILLISECONDS), is(true));
      }

      return invocation.callRealMethod();
    }).when(logger).getLogger(any(ClassLoader.class), any(Reference.class));

    String log1 = "Log 1";
    String log2 = "Log 2";

    Thread firstLogThread = new Thread(() -> logger.info(log1));
    Thread secondLogThread = new Thread(() -> logger.info(log2));

    firstLogThread.setContextClassLoader(regionClassLoader);

    firstLogThread.start();

    // Check the `getLogger` method has been called for the first thread
    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      assertThat(timesCalled.get(), is(1));
      return true;
    }));

    // Start the second thread and verify it logs
    secondLogThread.start();

    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      verify(originalLogger, times(1)).info(log2);
      return true;
    }));

    verify(regionClassLoaderLogger, times(0)).info(log1);

    // Let the first log continue
    latch.countDown();

    // Check the first message is logged
    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      verify(regionClassLoaderLogger, times(1)).info(log1);
      return true;
    }));

    firstLogThread.join();
    secondLogThread.join();
  }

  @Test
  public void noParallelizationWhenGettingLoggerForSameClassAndSameCtxClassLoader() throws InterruptedException {
    Logger originalLogger = getLogger("org.mule.runtime.module.artifact.api.classloader.FineGrainedControlClassLoader");
    Logger regionClassLoaderLogger = getRegionClassLoader();

    DispatchingLogger logger =
        spy(new DispatchingLogger(originalLogger, currentClassLoader.hashCode(), containerLoggerContext,
                                  artifactAwareContextSelector,
                                  messageFactory) {

          @Override
          public String getName() {
            return LOGGER_NAME;
          }
        });

    final Latch latch = new Latch();
    final AtomicInteger timesCalled = new AtomicInteger(0);

    doAnswer(invocation -> {
      if (timesCalled.compareAndSet(0, 1)) {
        // Make the first call wait here so the second one doesn't get into this method due to the lock
        assertThat(latch.await(1000, MILLISECONDS), is(true));
      } else {
        timesCalled.compareAndSet(1, 2);
      }

      return invocation.callRealMethod();
    }).when(logger).getLogger(any(ClassLoader.class), any(Reference.class));

    String log1 = "Log 1";
    String log2 = "Log 2";

    Thread firstLogThread = new Thread(() -> logger.info(log1));
    Thread secondLogThread = new Thread(() -> logger.info(log2));

    firstLogThread.setContextClassLoader(regionClassLoader);
    secondLogThread.setContextClassLoader(regionClassLoader);

    firstLogThread.start();
    secondLogThread.start();

    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      assertThat(timesCalled.get(), is(1));
      return true;
    }));

    verify(regionClassLoaderLogger, times(0)).info(log1);
    verify(regionClassLoaderLogger, times(0)).info(log2);

    // Give some time to check second invocation didn't get into `getLogger` method due to the lock
    sleep(200);
    assertThat(timesCalled.get(), is(1));

    // Let the first log continue
    latch.countDown();

    // Check the first message is logged
    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      verify(regionClassLoaderLogger, times(1)).info(log1);
      return true;
    }));

    // Now the second message should be logged
    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      assertThat(timesCalled.get(), is(2));
      return true;
    }));

    // Check the second message is logged
    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      verify(regionClassLoaderLogger, times(1)).info(log2);
      return true;
    }));

    firstLogThread.join();
    secondLogThread.join();
  }

  private Logger getLogger(String loggerClassName) {
    Logger logger = mock(Logger.class, RETURNS_DEEP_STUBS);
    when(logger.getName()).thenReturn(loggerClassName);

    return logger;
  }

  private Logger getRegionClassLoader() {
    Logger containerLogger = mock(Logger.class);
    Logger regionClassLoaderLogger = mock(Logger.class);
    when(containerLoggerContext.getLogger(anyString(), any(MessageFactory.class))).thenReturn(containerLogger);
    when(regionClassLoaderLoggerContext.getLogger(anyString(), any(MessageFactory.class)))
        .thenReturn(regionClassLoaderLogger);
    // Triggers of the expected Loggers
    when(artifactAwareContextSelector.getContextWithResolvedContextClassLoader(regionClassLoader))
        .thenAnswer(invocation -> regionClassLoaderLoggerContext);
    when(artifactAwareContextSelector.getContextWithResolvedContextClassLoader(currentClassLoader))
        .thenAnswer(invocation -> containerLoggerContext);

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
