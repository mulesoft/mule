/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.test.allure.AllureConstants.SchedulerServiceFeature.SCHEDULER_SERVICE;
import static org.mule.test.allure.AllureConstants.SchedulerServiceFeature.SchedulerServiceStory.SOURCE_MANAGEMENT;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.fail;

import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.source.SchedulerMessageSource;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.source.scheduler.DefaultSchedulerMessageSource;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(SCHEDULER_SERVICE)
@Stories(SOURCE_MANAGEMENT)
public class SchedulerManagementTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/scheduler-management-config.xml";
  }

  @Description("scheduler that never runs due to configuration but works by triggering it manually")
  @Test
  public void triggerSchedulerManually() {
    SchedulerMessageSource scheduler = (DefaultSchedulerMessageSource) muleContext.getConfigurationComponentLocator()
        .find(builder().globalName("neverRunningScheduler").addSourcePart().build()).get();
    scheduler.trigger();
    new PollingProber(10000, 100).check(new Probe() {

      @Override
      public boolean isSatisfied() {
        try {
          Either<Error, Optional<Message>> response =
              muleContext.getClient().request("test://neverRunningSchedulerQueue", 100);
          return response.isRight() && response.getRight().isPresent();
        } catch (MuleException e) {
          return false;
        }
      }

      @Override
      public String describeFailure() {
        return "Message expected by triggering flow";
      }
    });
  }

  @Description("scheduler that runs once, gets stopped by a functional component within the same flow and the it's triggered manually")
  @Test
  public void stopSchedulerWithinFlowAndTriggerItManually() throws Exception {
    FunctionalTestComponent functionalTestComponent = getFunctionalTestComponent("schedulerControlledFromSameFlow");
    SchedulerMessageSource scheduler = (DefaultSchedulerMessageSource) muleContext.getConfigurationComponentLocator()
        .find(builder().globalName("schedulerControlledFromSameFlow").addSourcePart().build()).get();
    AtomicInteger atomicInteger = new AtomicInteger(0);

    Latch componentExecutedLatch = new Latch();
    functionalTestComponent.setEventCallback((eventContext, component, muleContext) -> {
      scheduler.stop();
      atomicInteger.incrementAndGet();
      componentExecutedLatch.release();
    });
    if (!componentExecutedLatch.await(RECEIVE_TIMEOUT, MILLISECONDS)) {
      fail("test component never executed");
    }

    scheduler.trigger();
    new PollingProber(10000, 100).check(new Probe() {

      @Override
      public boolean isSatisfied() {
        return atomicInteger.get() == 2;
      }

      @Override
      public String describeFailure() {
        return "Executed two total executions of the flow but received " + atomicInteger.get();
      }
    });
  }

  @Description("scheduler start twice does not fail")
  @Test
  public void startTwiceDoesNotFail() throws MuleException {
    SchedulerMessageSource scheduler = (DefaultSchedulerMessageSource) muleContext.getConfigurationComponentLocator()
        .find(builder().globalName("schedulerControlledFromSameFlow").addSourcePart().build()).get();
    scheduler.start();
    scheduler.start();
  }

  @Description("scheduler stop twice does not fail")
  @Test
  public void stopTwiceDoesNotFail() throws MuleException {
    SchedulerMessageSource scheduler = (DefaultSchedulerMessageSource) muleContext.getConfigurationComponentLocator()
        .find(builder().globalName("schedulerControlledFromSameFlow").addSourcePart().build()).get();
    scheduler.stop();
    scheduler.stop();
  }

}
