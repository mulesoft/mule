/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.profiling.tracing;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.junit.MockitoJUnit.rule;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_PROFILING_SERVICE;

import org.mule.runtime.api.profiling.tracing.ExecutionContext;
import org.mule.runtime.api.profiling.tracing.TracingService;
import org.mule.runtime.core.internal.profiling.tracing.ThreadLocalTracingService;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;
import org.junit.Before;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_PROFILING_SERVICE)
public class ThreadLocalTracingServiceTestCase {

  @Rule
  public MockitoRule mockitoRule = rule();

  private TracingService threadLocalTracingService;

  @Mock
  private ExecutionContext executionContext;

  ExecutorService executorService = newSingleThreadExecutor();

  @Before
  public void before() {
    this.threadLocalTracingService = new ThreadLocalTracingService();
  }

  @Test
  public void setAndGetConsistency() {
    assertThat("Initial execution context should be null.", threadLocalTracingService.getCurrentExecutionContext(),
               is(nullValue()));
    threadLocalTracingService.setCurrentExecutionContext(executionContext);
    assertThat(threadLocalTracingService.getCurrentExecutionContext(), equalTo(executionContext));
    threadLocalTracingService.deleteCurrentExecutionContext();
    assertThat(threadLocalTracingService.getCurrentExecutionContext(), is(nullValue()));
  }

  @Test
  public void threadLocality() throws ExecutionException, InterruptedException {
    threadLocalTracingService.setCurrentExecutionContext(executionContext);
    AtomicReference<ExecutionContext> assertedContext = new AtomicReference<>(executionContext);
    executorService.submit(() -> assertedContext.set(threadLocalTracingService.getCurrentExecutionContext())).get();
    assertThat(assertedContext.get(), is(nullValue()));
    assertThat(threadLocalTracingService.getCurrentExecutionContext(), equalTo(executionContext));
  }

}
