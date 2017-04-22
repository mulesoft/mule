/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.service.scheduler;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SCHEDULER_BASE_CONFIG;
import static org.mule.test.allure.AllureConstants.SchedulerServiceFeature.SCHEDULER_SERVICE;

import org.mule.functional.functional.SkeletonSource;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.scheduler.SchedulerBusyException;
import org.mule.runtime.core.api.scheduler.SchedulerConfig;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.config.DefaultMuleConfiguration;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Named;

import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;

@Features(SCHEDULER_SERVICE)
public class SchedulerServiceTestCase extends AbstractIntegrationTestCase {

  private static final int CUSTOM_SCHEDULER_SIZE = 4;

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
    final Scheduler ioScheduler = schedulerService.ioScheduler();
    assertThat(ioScheduler.getName(),
               startsWith("SchedulerService_io@" + SchedulerServiceTestCase.class.getName() + ".schedulerDefaultName:"));
    ioScheduler.shutdownNow();
  }

  @Test
  public void customSchedulerDefaultName() {
    SchedulerService schedulerService = muleContext.getSchedulerService();
    final Scheduler ioScheduler =
        schedulerService.customScheduler(muleContext.getSchedulerBaseConfig().withMaxConcurrentTasks(1));
    assertThat(ioScheduler.getName(),
               startsWith("SchedulerService_custom@" + SchedulerServiceTestCase.class.getName()
                   + ".customSchedulerDefaultName:"));
    ioScheduler.shutdownNow();
  }

  @Test
  public void schedulerCustomName() {
    SchedulerService schedulerService = muleContext.getSchedulerService();
    final Scheduler ioScheduler =
        schedulerService.ioScheduler(muleContext.getSchedulerBaseConfig().withName("myPreciousScheduler"));
    assertThat(ioScheduler.getName(),
               startsWith("myPreciousScheduler"));
    ioScheduler.shutdownNow();
  }

  @Test
  public void configTimeoutChange() {
    Scheduler scheduler = muleContext.getSchedulerService().cpuLightScheduler();

    Latch testLatch = new Latch();
    scheduler.submit(() -> {
      try {
        testLatch.await();
      } catch (InterruptedException e) {
        currentThread().interrupt();
      }
    });

    final long stopRequestTime = currentTimeMillis();
    scheduler.stop();

    assertThat("gracefultShutdown flag in test not honored", muleContext.getConfiguration().getShutdownTimeout(), is(0L));
    // check for the actual timeout plus some margin
    assertThat(currentTimeMillis() - stopRequestTime, lessThan(1000L));
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    super.addBuilders(builders);
    builders.add(new AbstractConfigurationBuilder() {

      @Override
      protected void doConfigure(MuleContext muleContext) throws Exception {
        // Set an arbitrarily high value. This will be overridden by the context builder wen checking for the graceful shutdown
        // flag of the test.
        ((DefaultMuleConfiguration) muleContext.getConfiguration()).setShutdownTimeout(10000);
      }
    });
  }

  @Test
  @Description("Tests that the exception that happens when a thread pool is full is properly handled.")
  public void overloadErrorHandling() throws Exception {
    for (int i = 0; i < CUSTOM_SCHEDULER_SIZE; ++i) {
      flowRunner("delaySchedule").run();
    }

    Scheduler scheduler = muleContext.getSchedulerService().cpuLightScheduler();

    MessagingException exception =
        flowRunner("delaySchedule").withScheduler(scheduler).runExpectingException();

    assertThat(exception.getEvent().getError().isPresent(), is(true));
    assertThat(exception.getEvent().getError().get().getErrorType().getIdentifier(), is("OVERLOAD"));
    assertThat(exception.getCause(), instanceOf(SchedulerBusyException.class));

    WaitingProcessor.latch.countDown();
    scheduler.shutdownNow();
  }

  @Rule
  public ExpectedException expected = none();

  @Test
  @Description("Tests that an OVERLOAD error is handled only by the message source."
      + " This assumes org.mule.test.integration.exceptions.ErrorHandlerTestCase#criticalNotHandled")
  public void overloadErrorHandlingFromSource() throws Throwable {
    FlowConstruct delayScheduleFlow = getFlowConstruct("delaySchedule");
    MessageSource messageSource = ((Flow) delayScheduleFlow).getMessageSource();

    for (int i = 0; i < CUSTOM_SCHEDULER_SIZE; ++i) {
      ((SkeletonSource) messageSource).getListener()
          .process(Event.builder(create(delayScheduleFlow, SchedulerServiceTestCase.class.getSimpleName())).build());
    }

    expected.expect(MessagingException.class);
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
    expected.expectCause(instanceOf(SchedulerBusyException.class));

    Scheduler scheduler = muleContext.getSchedulerService().cpuLightScheduler();
    try {
      scheduler.submit(() -> ((SkeletonSource) messageSource).getListener()
          .process(Event.builder(create(delayScheduleFlow, SchedulerServiceTestCase.class.getSimpleName())).message(of(null))
              .build()))
          .get();
    } catch (ExecutionException executionException) {
      throw executionException.getCause();
    } finally {
      WaitingProcessor.latch.countDown();
      scheduler.shutdownNow();
    }
  }

  public static class HasSchedulingService implements Processor, Initialisable, Disposable {

    @Inject
    private SchedulerService schedulerService;

    private Scheduler scheduler;

    @Override
    public void initialise() throws InitialisationException {
      scheduler = schedulerService.cpuLightScheduler();
    }

    @Override
    public void dispose() {
      scheduler.shutdownNow();
    }

    @Override
    public Event process(Event event) throws MuleException {
      try {
        // just exercise the scheduler.
        return scheduler.submit(() -> event).get();
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
    @Named(OBJECT_SCHEDULER_BASE_CONFIG)
    private SchedulerConfig config;

    @Inject
    private SchedulerService schedulerService;

    private volatile Scheduler scheduler;

    @Override
    public void initialise() throws InitialisationException {
      latch = new Latch();
      scheduler = schedulerService.customScheduler(config.withMaxConcurrentTasks(CUSTOM_SCHEDULER_SIZE));
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
