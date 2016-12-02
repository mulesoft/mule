/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static java.lang.Thread.currentThread;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.core.api.scheduler.SchedulerConfig.config;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.util.concurrent.RejectedExecutionException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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

  @Rule
  public ExpectedException expected = none();

  @Test
  public void executorRejects() throws MuleException {
    final Latch latch = new Latch();
    final DefaultSchedulerService service = new DefaultSchedulerService();

    service.start();

    final Scheduler custom = service.customScheduler(config().withMaxConcurrentTasks(1));

    custom.execute(() -> {
      try {
        latch.await();
      } catch (InterruptedException e) {
        currentThread().interrupt();
      }
    });

    expected.expect(RejectedExecutionException.class);

    final Runnable task = () -> {
    };
    try {
      custom.submit(task);
    } finally {
      assertThat(custom.shutdownNow(), not(hasItem(task)));
      service.stop();
    }

  }
}
