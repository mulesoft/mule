/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.source.scheduler.FixedFrequencyScheduler;
import org.mule.runtime.core.api.source.scheduler.Scheduler;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.dsl.xml.ParameterDsl;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class PetAdoptionPolling {

  private static final long DEFAULT_FREQUENCY = 1000L;
  private static final long DEFAULT_START_DELAY = 1000L;
  private static final TimeUnit DEFAULT_TIME_UNIT = MILLISECONDS;

  @Parameter
  @Summary("Configures the scheduler that triggers the polling")
  @ParameterDsl(allowReferences = false)
  @Expression(ExpressionSupport.NOT_SUPPORTED)
  private Scheduler schedulingStrategy;

  public Scheduler getSchedulingStrategy() {
    return schedulingStrategy;
  }

  public void setSchedulingStrategy(Scheduler schedulingStrategy) {
    this.schedulingStrategy = schedulingStrategy;
  }

  public org.mule.runtime.api.scheduler.Scheduler createExecutor(String subscriberName, SchedulerService schedulerService) {
    initaliseStrategy();

    return schedulerService.customScheduler(SchedulerConfig.config()
        .withName(subscriberName)
        .withMaxConcurrentTasks(1)
        .withWaitAllowed(false));
  }

  private void initaliseStrategy() {
    if (schedulingStrategy == null) {
      FixedFrequencyScheduler strategy = new FixedFrequencyScheduler();
      strategy.setTimeUnit(DEFAULT_TIME_UNIT);
      strategy.setFrequency(DEFAULT_FREQUENCY);
      strategy.setStartDelay(DEFAULT_START_DELAY);
      schedulingStrategy = strategy;
    }
  }

  public Optional<Long> getFrequency() {
    return schedulingStrategy instanceof FixedFrequencyScheduler
        ? Optional.of(((FixedFrequencyScheduler) schedulingStrategy).getFrequency())
        : Optional.empty();
  }
}
