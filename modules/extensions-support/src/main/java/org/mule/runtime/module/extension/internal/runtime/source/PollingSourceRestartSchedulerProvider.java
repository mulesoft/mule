/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.source.scheduler.FixedFrequencyScheduler;
import org.mule.runtime.core.api.source.scheduler.Scheduler;
import org.mule.runtime.extension.api.runtime.source.PollContext;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;

/**
 * Class for providing schedulers for a polling source that was restarted.
 */
public class PollingSourceRestartSchedulerProvider {

  private PollingSourceRestartSchedulerProvider() {}

  /**
   *  This method provides the scheduler that a polling source must use to trigger the invocations of the
   *  {@link org.mule.runtime.extension.api.runtime.source.PollingSource#poll(PollContext)} method in case of a restart
   *
   * @param scheduler the scheduler configured for the polling source
   * @return the scheduler that must be used by the polling source in case of a restart.
   * @throws MuleException
   */
  public static Scheduler getScheduler(Scheduler scheduler) {
    if (scheduler instanceof FixedFrequencyScheduler) {
      ((FixedFrequencyScheduler) scheduler).setStartDelay(0);
    }
    return scheduler;
  }
}
