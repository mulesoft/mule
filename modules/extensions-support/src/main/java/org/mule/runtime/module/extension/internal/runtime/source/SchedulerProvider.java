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
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;

/**
 * Class for providing schedulers according to the context and if the source was restarted.
 */
public class SchedulerProvider {

  public static Scheduler getScheduler(ValueResolvingContext context, ValueResolver valueResolver, boolean restarting)
      throws MuleException {
    Scheduler scheduler = (Scheduler) valueResolver.resolve(context);
    if (restarting && scheduler instanceof FixedFrequencyScheduler) {
      ((FixedFrequencyScheduler) scheduler).setStartDelay(0);
    }
    return scheduler;
  }
}
