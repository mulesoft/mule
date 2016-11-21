/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.service.scheduler;

import static java.lang.Thread.currentThread;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

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
}
