/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.service.scheduler;

import static java.lang.Runtime.getRuntime;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.runtime.core.api.scheduler.SchedulerConfig.config;

import org.mule.functional.functional.SkeletonSource;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;

import javax.inject.Inject;

import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;

@Features("Scheduler Service")
public class SchedulerServiceTestCase extends AbstractIntegrationTestCase {

  private static long mem = getRuntime().maxMemory() / 1024;

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
  public void schedulerDefaultName() {
    SchedulerService schedulerService = muleContext.getSchedulerService();
    assertThat(schedulerService.ioScheduler().getName(),
               startsWith("SchedulerService_io@" + SchedulerServiceTestCase.class.getName() + ".schedulerDefaultName:"));
  }

  @Test
  public void schedulerCustomName() {
    SchedulerService schedulerService = muleContext.getSchedulerService();
    assertThat(schedulerService.ioScheduler(config().withName("myPreciousScheduler")).getName(),
               startsWith("myPreciousScheduler"));
  }

  @Test
  @Description("Tests that the exception that happens when a thread pool is full is properly handled.")
  public void overloadErrorHandling() throws Exception {
    // This has to match the default value returned by ThreadPoolsConfig#getIoMaxPoolSize() + ThreadPoolsConfig#getIoQueueSize().
    for (int i = 0; i < 256 + 1024; ++i) {
      flowRunner("delaySchedule").run();
    }

    MessagingException exception = flowRunner("delaySchedule").runExpectingException();

    assertThat(exception.getEvent().getError().isPresent(), is(true));
    assertThat(exception.getEvent().getError().get().getErrorType().getIdentifier(), is("OVERLOAD"));
    assertThat(exception.getCause(), instanceOf(RejectedExecutionException.class));

    WaitingProcessor.latch.countDown();
  }

  @Rule
  public ExpectedException expected = none();

  @Test
  @Description("Tests that an OVERLOAD error is handled only by the message source."
      + " This assumes org.mule.test.integration.exceptions.ErrorHandlerTestCase#criticalNotHandled")
  public void overloadErrorHandlingFromSource() throws Exception {
    FlowConstruct delayScheduleFlow = getFlowConstruct("delaySchedule");
    MessageSource messageSource = ((Flow) delayScheduleFlow).getMessageSource();

    // This has to match the default value returned by ThreadPoolsConfig#getIoMaxPoolSize() + ThreadPoolsConfig#getIoQueueSize().
    for (int i = 0; i < 256 + 1024; ++i) {
      ((SkeletonSource) messageSource).getListener()
          .process(Event.builder(create(delayScheduleFlow, SchedulerServiceTestCase.class.getSimpleName())).build());
    }

    expected.expect(instanceOf(MessagingException.class));
    expected.expect(new TypeSafeMatcher<MessagingException>() {

      private String errorTypeId;

      @Override
      public void describeTo(org.hamcrest.Description description) {
        description.appendValue(errorTypeId);
      }

      @Override
      protected boolean matchesSafely(MessagingException item) {
        errorTypeId = item.getEvent().getError().get().getErrorType().getIdentifier();
        return "OVERLOAD".equals(errorTypeId);
      }
    });
    expected.expectCause(instanceOf(RejectedExecutionException.class));

    try {
      ((SkeletonSource) messageSource).getListener()
          .process(Event.builder(create(delayScheduleFlow, SchedulerServiceTestCase.class.getSimpleName())).build());
    } finally {
      WaitingProcessor.latch.countDown();
    }
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

  public static class WaitingProcessor implements Processor, Initialisable, Disposable {

    public static Latch latch = new Latch();

    @Inject
    private SchedulerService schedulerService;

    private volatile Scheduler scheduler;

    @Override
    public void initialise() throws InitialisationException {
      latch = new Latch();
      scheduler = schedulerService.ioScheduler();
    }

    @Override
    public Event process(Event event) throws MuleException {
      scheduler.submit(() -> {
        try {
          latch.await(DEFAULT_TEST_TIMEOUT_SECS, SECONDS);
        } catch (InterruptedException e) {
          currentThread().interrupt();
        }
      });
      return event;
    }

    @Override
    public void dispose() {
      scheduler.shutdownNow();
    }
  }
}
