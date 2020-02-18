/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.log4j2;

import static java.lang.Thread.currentThread;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.test.allure.AllureConstants.ComponentsFeature.CORE_COMPONENTS;
import static org.mule.test.allure.AllureConstants.ComponentsFeature.LoggerStory.LOGGER;

import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.message.MessageFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
@Feature(CORE_COMPONENTS)
@Story(LOGGER)
public class DispatchingLoggerTestCase extends AbstractMuleTestCase {

  private static final String LOGGER_NAME = DispatchingLoggerTestCase.class.getName();

  private static final String MESSAGE = "Hello Log!";

  private ClassLoader currentClassLoader;

  @Mock
  private ClassLoader additionalClassLoader;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Logger originalLogger;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private LoggerContext containerLoggerContext;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ContextSelector contextSelector;

  @Mock
  private ArtifactAwareContextSelector artifactAwareContextSelector;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MessageFactory messageFactory;

  @Mock
  RegionClassLoader regionClassLoader;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  LoggerContext regionClassLoaderLoggerContext;

  private Logger logger;

  @Before
  public void before() {
    currentClassLoader = currentThread().getContextClassLoader();
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
  public void currentClassLoader() {
    logger.info(MESSAGE);
    verify(originalLogger).info(MESSAGE);
  }

  @Test
  public void anotherClassLoader() {
    Thread currentThread = currentThread();
    ClassLoader originalClassLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(additionalClassLoader);
    try {
      logger.info(MESSAGE);
      verify(originalLogger).info(MESSAGE);
    } finally {
      currentThread.setContextClassLoader(originalClassLoader);
    }
  }

  @Test
  public void regionClassLoader() {
    RegionClassLoader regionClassLoader = mock(RegionClassLoader.class);
    Thread currentThread = currentThread();
    ClassLoader originalClassLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(regionClassLoader);
    try {
      logger.info(MESSAGE);
      verify(contextSelector).getContext(LOGGER_NAME, regionClassLoader, true);
    } finally {
      currentThread.setContextClassLoader(originalClassLoader);
    }
  }

  @Test
  public void whenRecursiveLoggerContextInstantiationExceptionExpectFallbackUsingContainerClassLoader() {
    // Expected Loggers
    Logger containerLogger = mock(Logger.class);
    Logger regionClassLoaderLogger = mock(Logger.class);
    when(containerLoggerContext.getLogger(any(), any())).thenReturn(containerLogger);
    when(regionClassLoaderLoggerContext.getLogger(any(), any())).thenReturn(regionClassLoaderLogger);
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
    Thread currentThread = currentThread();
    ClassLoader originalClassLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(regionClassLoader);
    try {
      dispatchingLogger.info("Fallback Test Message");
      dispatchingLogger.info("Test Message");
    } finally {
      currentThread.setContextClassLoader(originalClassLoader);
    }
    verify(containerLogger, times(1)).info("Fallback Test Message");
    verify(regionClassLoaderLogger, times(1)).info("Test Message");
  }

}
