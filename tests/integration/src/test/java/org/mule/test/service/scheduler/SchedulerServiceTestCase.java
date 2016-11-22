/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.service.scheduler;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.scheduler.Scheduler;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;

import javax.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;

@Features("Scheduler Service")
public class SchedulerServiceTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/service/scheduler/scheduler-service.xml";
  }

  @Test
  @Description("Test that the scheduler service is properly injected into a Mule component")
  public void useSchedulingService() throws Exception {
    flowRunner("willSchedule").run();
  }

  @Test
  public void schedulerName() {
    SchedulerService schedulerService = muleContext.getSchedulerService();
    assertThat(schedulerService.ioScheduler().getName(),
               startsWith("SchedulerService_io@" + SchedulerServiceTestCase.class.getName() + ".schedulerName:"));
  }

  @Test
  @Ignore("This is flaky because when the RejectedExcecutionException occurs when scheduling a task as part of a processing strategy, the Mono in Flow.process remians blocked forever.")
  @Description("Tests that the exception that happens when a thread pool is full is properly handled.")
  public void overloadErrorHandling() throws Exception {
    for (int i = 0; i < 100; ++i) {
      // TODO MULE-10585 this 64 below is based on the value set on DefaultSchedulerService.
      if (i >= 63) {
        MessagingException exception = flowRunner("delaySchedule").runExpectingException();

        assertThat(exception.getEvent().getError().isPresent(), is(true));
        assertThat(exception.getEvent().getError().get().getErrorType().getIdentifier(), is("OVERLOAD"));
        assertThat(exception.getCause(), instanceOf(RejectedExecutionException.class));
      } else {
        flowRunner("delaySchedule").run();
      }
    }

    WaitingProcessor.latch.countDown();
  }

  public static class HasSchedulingService implements Processor {

    @Inject
    private SchedulerService scheduler;

    @Override
    public Event process(Event event) throws MuleException {
      try {
        // just exercise the scheduler.
        return scheduler.cpuLightScheduler().submit(() -> event).get();
      } catch (InterruptedException e) {
        currentThread().interrupt();
        return event;
      } catch (ExecutionException e) {
        throw new MuleRuntimeException(e.getCause());
      }
    }
  }

  public static class WaitingProcessor implements Processor {

    public static final Latch latch = new Latch();

    @Inject
    private SchedulerService schedulerService;

    private volatile Scheduler scheduler;

    @Override
    public Event process(Event event) throws MuleException {
      if (scheduler == null) {
        synchronized (this) {
          if (scheduler == null) {
            scheduler = schedulerService.ioScheduler();
          }
        }
      }

      scheduler.submit(() -> {
        try {
          latch.await(DEFAULT_TEST_TIMEOUT_SECS, SECONDS);
        } catch (InterruptedException e) {
          currentThread().interrupt();
        }
      });
      return event;
    }
  }
}
