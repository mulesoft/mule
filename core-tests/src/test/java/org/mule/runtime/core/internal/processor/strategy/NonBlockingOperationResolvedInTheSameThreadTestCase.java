/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Thread.currentThread;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.internal.processor.strategy.AbstractStreamProcessingStrategyFactory.CORES;
import static reactor.util.concurrent.Queues.XS_BUFFER_SIZE;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.tck.SimpleUnitTestSupportSchedulerService;

import java.util.concurrent.Future;

import io.qameta.allure.Description;
import org.junit.Test;

public class NonBlockingOperationResolvedInTheSameThreadTestCase extends AbstractProcessingStrategyTestCase {

  SchedulerService mockSchedulerService;

  public NonBlockingOperationResolvedInTheSameThreadTestCase(
                                                             Mode mode) {
    super(mode);
    mockSchedulerService = mock(SimpleUnitTestSupportSchedulerService.class);
    when(mockSchedulerService.cpuLightScheduler()).thenReturn(cpuLight);
    when(mockSchedulerService.isCurrentThreadForCpuWork()).thenCallRealMethod();
  }

  @Override
  protected ProcessingStrategy createProcessingStrategy(MuleContext muleContext, String schedulersNamePrefix) {
    return createProcessingStrategy(muleContext, MAX_VALUE);
  }

  protected ProcessingStrategy createProcessingStrategy(MuleContext muleContext,
                                                        int maxConcurrency) {
    return new ProactorStreamEmitterProcessingStrategyFactory.ProactorStreamEmitterProcessingStrategy(XS_BUFFER_SIZE,
                                                                                                      2,
                                                                                                      () -> cpuLight,
                                                                                                      () -> blocking,
                                                                                                      () -> cpuIntensive,
                                                                                                      CORES,
                                                                                                      maxConcurrency, true,
                                                                                                      mockSchedulerService);
  }

  @Override
  public void tx() throws Exception {

  }

  @Test
  @Description("When ")
  public void asyncCpuLightWithoutThreadSwitch() throws Exception {
    threadSwitch = false;

    final Future<?> submit = cpuLight.submit(() -> {
      try {
        super.asyncCpuLight();
      } catch (Exception e) {
        currentThread().interrupt();
      }
    });

    submit.get();
    assertThat(threads, hasSize(1));
    assertThat(threads, hasItem(startsWith(CPU_LIGHT)));
    assertThat(threads, not(hasItem(startsWith(IO))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
    assertThat(threads, not(hasItem(startsWith(RING_BUFFER))));
    assertThat(threads, not(hasItem(startsWith(EXECUTOR))));
  }
}
