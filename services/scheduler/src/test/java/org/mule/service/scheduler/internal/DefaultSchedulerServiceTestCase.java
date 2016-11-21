/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.scheduler.ThreadType.COMPUTATION;
import static org.mule.runtime.core.api.scheduler.ThreadType.CPU_LIGHT;
import static org.mule.runtime.core.api.scheduler.ThreadType.IO;
import static org.mule.runtime.core.api.scheduler.ThreadType.UNKNOWN;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;

@Features("SchedulerService")
public class DefaultSchedulerServiceTestCase extends AbstractMuleTestCase {

  @Test
  @Description("Tests that the threads of the SchedulerService are correcly created and destroyed.")
  public void serviceStop() throws MuleException {
    final DefaultSchedulerService service = new DefaultSchedulerService();

    service.start();
    assertThat(collectThreadNames(), hasItem(startsWith(SchedulerService.class.getSimpleName())));

    service.stop();

    new PollingProber(500, 50).check(new JUnitLambdaProbe(() -> {
      assertThat(collectThreadNames(), not(hasItem(startsWith(SchedulerService.class.getSimpleName()))));
      return true;
    }));
  }

  @Test
  @Description("Tests that SchedulerService#getCurrentThreadType() works correctly")
  public void getCurrentThreadType() throws MuleException, InterruptedException, ExecutionException {
    final ExecutorService executor = newSingleThreadExecutor();
    final DefaultSchedulerService service = new DefaultSchedulerService();

    service.start();

    service.cpuLightScheduler().submit(() -> assertThat(service.currentThreadType(), is(CPU_LIGHT))).get();
    service.ioScheduler().submit(() -> assertThat(service.currentThreadType(), is(IO))).get();
    service.computationScheduler().submit(() -> assertThat(service.currentThreadType(), is(COMPUTATION))).get();
    executor.submit(() -> assertThat(service.currentThreadType(), is(UNKNOWN))).get();

    service.stop();
    executor.shutdownNow();
  }

  @Test
  @Description("Tests that the Scheduler.getThreadsType() works correctly")
  public void getThreadsType() throws MuleException, InterruptedException, ExecutionException {
    final DefaultSchedulerService service = new DefaultSchedulerService();

    service.start();

    assertThat(service.cpuLightScheduler().getType(), is(CPU_LIGHT));
    assertThat(service.ioScheduler().getType(), is(IO));
    assertThat(service.computationScheduler().getType(), is(COMPUTATION));

    service.stop();
  }
}
