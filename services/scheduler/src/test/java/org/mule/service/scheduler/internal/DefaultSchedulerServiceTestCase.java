/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.util.concurrent.ExecutionException;

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
  @Description("Tests that the isCurrentThreadCpuLight() works correctly")
  public void cpuLightThreadTypes() throws MuleException, InterruptedException, ExecutionException {
    final DefaultSchedulerService service = new DefaultSchedulerService();

    service.start();

    service.cpuLightScheduler().submit(() -> assertThat(service.isCurrentThreadCpuLight(), is(true))).get();
    service.ioScheduler().submit(() -> assertThat(service.isCurrentThreadCpuLight(), is(false))).get();
    service.computationScheduler().submit(() -> assertThat(service.isCurrentThreadCpuLight(), is(false))).get();

    service.stop();
  }

  @Test
  @Description("Tests that the isCurrentThreadIo() works correctly")
  public void ioThreadTypes() throws MuleException, InterruptedException, ExecutionException {
    final DefaultSchedulerService service = new DefaultSchedulerService();

    service.start();

    service.cpuLightScheduler().submit(() -> assertThat(service.isCurrentThreadIo(), is(false))).get();
    service.ioScheduler().submit(() -> assertThat(service.isCurrentThreadIo(), is(true))).get();
    service.computationScheduler().submit(() -> assertThat(service.isCurrentThreadIo(), is(false))).get();

    service.stop();
  }

  @Test
  @Description("Tests that the isCurrentThreadComputation() works correctly")
  public void computationThreadTypes() throws MuleException, InterruptedException, ExecutionException {
    final DefaultSchedulerService service = new DefaultSchedulerService();

    service.start();

    service.cpuLightScheduler().submit(() -> assertThat(service.isCurrentThreadComputation(), is(false))).get();
    service.ioScheduler().submit(() -> assertThat(service.isCurrentThreadComputation(), is(false))).get();
    service.computationScheduler().submit(() -> assertThat(service.isCurrentThreadComputation(), is(true))).get();

    service.stop();
  }

}
