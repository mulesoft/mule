/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Extends the {@link ProcessingStrategy} contract with implementation methods which should not be exposed
 * as part of the core API
 *
 * @since 4.3.0
 */
public interface ProcessingStrategyAdapter extends ProcessingStrategy {

  /**
   * Returns a {@link Function} that implementations will use to decorate {@link ScheduledExecutorService} instances.
   * <p>
   * Notice that which schedulers get decorated and which are not is up to each implementation. No guarantees should be
   * expected around that.
   *
   * @return a decorator function
   */
  Function<ScheduledExecutorService, ScheduledExecutorService> getSchedulerDecorator();

  /**
   * Sets the {@link Function} that implementations will use to decorate {@link ScheduledExecutorService} instances.
   * <p>
   * Notice that which schedulers get decorated and which are not is up to each implementation. No guarantees should be
   * expected around that.
   *
   * @param schedulerDecorator a decoration function
   */
  void setSchedulerDecorator(Function<ScheduledExecutorService, ScheduledExecutorService> schedulerDecorator);

  /**
   * Sets a {@link Consumer} that will be invoked each time a new message is dispatch through {@code this} processing strategy
   * but before the pipeline processes it.
   *
   * @param eventConsumer a {@link CoreEvent} {@link Consumer}
   */
  void setOnEventConsumer(Consumer<CoreEvent> eventConsumer);

}
