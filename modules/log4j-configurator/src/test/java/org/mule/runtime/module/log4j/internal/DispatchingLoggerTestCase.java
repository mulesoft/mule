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
import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
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

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.message.MessageFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@SmallTest
@ExtendWith(MockitoExtension.class)
@Feature(LOGGING)
@Story(CONTEXT_FACTORY)
public class DispatchingLoggerTestCase extends AbstractMuleTestCase {

  private static final String LOGGER_NAME = DispatchingLoggerTestCase.class.getName();

  private static final String MESSAGE = "Hello Log!";

  private static final long PROBER_POLLING_TIMEOUT = 5000;
  private static final long PROBER_POLLING_INTERVAL = 100;

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

  @BeforeEach
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

  @AfterEach
  public void after() {
    loggerContextCache.dispose();
  }

  @Test
  @Issue("W-18388443")
  void noParallelizationWhenGettingLoggerFirstForClassNeedingBlockingAndThenForRegularClass() throws Exception {
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
  void noParallelizationWhenGettingLoggerFirstForRegularClassAndThenForClassNeedingBlocking() throws Exception {
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
    lenient().doAnswer(invocationOnMock -> containerLoggerContext)
        .when(loggerContextCache).doGetLoggerContext(eq(currentClassLoader), any());

    return regionClassLoaderLogger;
  }

  @Test
  void currentClassLoader() {
    logger.info(MESSAGE);
    verify(originalLogger).info(MESSAGE);
  }

  @Test
  void anotherClassLoader() {
    withContextClassLoader(additionalClassLoader, () -> {
      logger.info(MESSAGE);
      verify(originalLogger).info(MESSAGE);
    });
  }

  @Test
  void regionClassLoader() {
    RegionClassLoader regionClassLoader = mock(RegionClassLoader.class);
    withContextClassLoader(regionClassLoader, () -> {
      logger.info(MESSAGE);
      verify(contextSelector).getContext(LOGGER_NAME, regionClassLoader, true);
    });
  }

  @Test
  void whenRecursiveLoggerContextInstantiationExceptionExpectFallbackUsingContainerClassLoader() {
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
  void whenFallbackToContainerClassLoaderFailsReturnOriginalLogger() {
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

  static Stream<Method> log4jOverridableMethods() {
    final Class<?> log4jLoggerClass = org.apache.logging.log4j.core.Logger.class;

    // Collect the signature of every method that originates from a non-Log4j type so that we can
    // filter out Log4j methods overriding them (e.g. java.util.function.Supplier#get()).
    final Set<String> nonLog4jSignatures = new HashSet<>();
    // Add java.lang.Object methods explicitly so classic overrides like equals/hashCode/toString are ignored.
    Stream.of(Object.class.getDeclaredMethods())
        .map(DispatchingLoggerTestCase::signature)
        .forEach(nonLog4jSignatures::add);

    collectNonLog4jMethodSignatures(log4jLoggerClass, nonLog4jSignatures);

    return Stream.of(log4jLoggerClass.getMethods())
        // Keep only methods declared within the Log4j code-base.
        .filter(m -> m.getDeclaringClass().getPackageName().startsWith("org.apache.logging.log4j"))
        .filter(m -> isPublic(m.getModifiers()) && !isFinal(m.getModifiers()) && !isStatic(m.getModifiers()))
        .filter(m -> !nonLog4jSignatures.contains(signature(m)))
        // getName of the logger does not need delegation
        .filter(m -> !m.getName().equals("getName"))
        .distinct();
  }

  /**
   * Recursively walks the hierarchy of the provided class (super-classes and interfaces) collecting the signatures for every
   * method that does not belong to log4j.
   */
  private static void collectNonLog4jMethodSignatures(Class<?> type, Set<String> signatures) {
    if (type == null || type == Object.class) {
      return;
    }

    // Add signatures not coming from log4j classes / interfaces.
    if (!type.getPackageName().startsWith("org.apache.logging.log4j")) {
      Stream.of(type.getDeclaredMethods())
          .map(DispatchingLoggerTestCase::signature)
          .forEach(signatures::add);
    }

    // Traverse interfaces first.
    for (Class<?> iface : type.getInterfaces()) {
      collectNonLog4jMethodSignatures(iface, signatures);
    }

    // Then superclass.
    collectNonLog4jMethodSignatures(type.getSuperclass(), signatures);
  }

  private static String signature(Method m) {
    return m.getName() + Arrays.toString(m.getParameterTypes());
  }

  public static boolean isOverridden(Method method, Method superMethod) {
    if (superMethod.getName().equals(method.getName()) &&
        Arrays.equals(superMethod.getParameterTypes(), method.getParameterTypes()) &&
        superMethod.getReturnType().isAssignableFrom(method.getReturnType())) {
      return true;
    }
    return false;
  }

  @Issue("W-10622326")
  @ParameterizedTest
  @MethodSource("log4jOverridableMethods")
  void properLog4jDelegation(Method overridableMethod) throws IllegalAccessException, InvocationTargetException {
    final Object[] params = Stream.of(overridableMethod.getParameterTypes())
        .map(p -> {
          if (p.isPrimitive()) {
            if (p.equals(boolean.class)) {
              return true;
            }
          } else if (p.isArray()) {
            return Array.newInstance(p.getComponentType(), 0);
          } else if (p.equals(Object.class)) {
            return new Object();
          } else if (p.equals(String.class)) {
            return "string";
          }
          return mock(p);
        })
        .toArray();

    overridableMethod.invoke(logger, params);
    overridableMethod.invoke(verify(originalLogger), params);
  }

}
