/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.log4j2;

import org.apache.logging.log4j.core.LoggerContext;
import org.hamcrest.collection.IsCollectionWithSize;
import org.hamcrest.core.IsCollectionContaining;
import org.hamcrest.core.IsEqual;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.tck.size.SmallTest;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

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
  public void when_recursive_MuleLoggerContext_instantiation_expect_RecursiveLoggerContextInstantiationException_and_recovery() {
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
                 IsEqual.equalTo(RecursiveLoggerContextInstantiationException.class));
      actualLoggerContext = loggerContextCache.getLoggerContext(currentClassLoader);
    }
    assertThat("Invalid LoggerContext", actualLoggerContext, IsEqual.equalTo(expectedLoggerContext));
  }

  @Test
  public void when_MuleRuntimeException_during_MuleLoggerContext_instantiation_expect_recovery() {
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
    assertThat("Invalid loggerContext", actualLoggerContext, IsEqual.equalTo(expectedLoggerContext));
  }

  @Test
  public void when_MuleLoggerContext_instantiation_expect_cache_store() {
    LoggerContext firstExpectedLoggerContext = mock(MuleLoggerContext.class);
    LoggerContext secondExpectedLoggerContext = mock(MuleLoggerContext.class);
    when(contextSelector.buildContext(currentClassLoader)).thenReturn(firstExpectedLoggerContext);
    when(contextSelector.buildContext(regionClassLoader)).thenReturn(secondExpectedLoggerContext);
    loggerContextCache.getLoggerContext(currentClassLoader);
    ClassUtils.withContextClassLoader(regionClassLoader, () -> {
      loggerContextCache.getLoggerContext(regionClassLoader);
    });
    assertThat("Additional or missing LoggerContext instances found in cache", loggerContextCache.getAllLoggerContexts(),
               IsCollectionWithSize.hasSize(2));
    assertThat("Cache should contain firstExpectedLogger", loggerContextCache.getAllLoggerContexts(),
               IsCollectionContaining.hasItem(firstExpectedLoggerContext));
    assertThat("Cache should contain secondExpectedLogger", loggerContextCache.getAllLoggerContexts(),
               IsCollectionContaining.hasItem(secondExpectedLoggerContext));
  }

}
