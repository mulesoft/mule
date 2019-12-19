/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.log4j2;

import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mule.tck.size.SmallTest;
import org.apache.logging.log4j.core.LoggerContext;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class LoggerContextCacheTestCase {

  private ClassLoader currentClassLoader;
  private LoggerContextCache loggerContextCache;
  @Mock
  private RegionClassLoader regionClassLoader;
  @Mock
  private ArtifactAwareContextSelector contextSelector;

  @Before
  public void before() {
    loggerContextCache = new LoggerContextCache(contextSelector, currentClassLoader);
    currentClassLoader = Thread.currentThread().getContextClassLoader();
  }

  @After
  public void after() {
    loggerContextCache.dispose();
  }

  @Test
  public void whenRecursiveMuleLoggerContextInstantiationExpectRecursiveLoggerContextInstantiationExceptionAndRecovery() {
    LoggerContext expectedLoggerContext = mock(MuleLoggerContext.class);
    when(contextSelector.buildContext(currentClassLoader))
        .thenAnswer(invocation -> loggerContextCache.getLoggerContext(currentClassLoader))
        .thenReturn(expectedLoggerContext);
    LoggerContext actualLoggerContext;
    try {
      actualLoggerContext = loggerContextCache.getLoggerContext(currentClassLoader);
      fail("Recursive instantiation should throw RecursiveInstantiationException");
    } catch (MuleRuntimeException e) {
      assertThat("Exception should be caused by RecursiveContextInstantiation", e.getClass(),
                 equalTo(RecursiveLoggerContextInstantiationException.class));
      actualLoggerContext = loggerContextCache.getLoggerContext(currentClassLoader);
    }
    assertThat("Invalid LoggerContext", actualLoggerContext, equalTo(expectedLoggerContext));
  }

  @Test
  public void whenMuleRuntimeExceptionDuringMuleLoggerContextInstantiationExpectRecovery() {
    LoggerContext expectedLoggerContext = mock(MuleLoggerContext.class);
    when(contextSelector.buildContext(currentClassLoader))
        .thenThrow(MuleRuntimeException.class)
        .thenAnswer(invocation -> expectedLoggerContext);
    LoggerContext actualLoggerContext = null;
    try {
      loggerContextCache.getLoggerContext(currentClassLoader);
    } catch (MuleRuntimeException mre) {
      actualLoggerContext = loggerContextCache.getLoggerContext(currentClassLoader);
    }
    assertThat("Invalid loggerContext", actualLoggerContext, equalTo(expectedLoggerContext));
  }

  @Test
  public void whenMuleLoggerContextInstantiationExpectCacheStore() {
    LoggerContext firstExpectedLoggerContext = mock(MuleLoggerContext.class);
    LoggerContext secondExpectedLoggerContext = mock(MuleLoggerContext.class);
    when(contextSelector.buildContext(currentClassLoader)).thenReturn(firstExpectedLoggerContext);
    when(contextSelector.buildContext(regionClassLoader)).thenReturn(secondExpectedLoggerContext);
    loggerContextCache.getLoggerContext(currentClassLoader);
    withContextClassLoader(regionClassLoader, () -> {
      loggerContextCache.getLoggerContext(regionClassLoader);
    });
    assertThat("Additional or missing LoggerContext instances found in cache", loggerContextCache.getAllLoggerContexts(),
               hasSize(2));
    assertThat(loggerContextCache.getAllLoggerContexts(), hasItem(firstExpectedLoggerContext));
    assertThat(loggerContextCache.getAllLoggerContexts(), hasItem(secondExpectedLoggerContext));
  }

}
