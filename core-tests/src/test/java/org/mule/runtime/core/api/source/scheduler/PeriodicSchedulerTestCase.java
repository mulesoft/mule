/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.source.scheduler;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.source.scheduler.PeriodicScheduler;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.concurrent.ScheduledFuture;

import org.junit.Test;

/**
 * Test to validate the interface {@link PeriodicScheduler} interface
 */
@SmallTest
public class PeriodicSchedulerTestCase extends AbstractMuleTestCase {

  /**
   * If the {@link Scheduler} created is null then throw an {@link ScheduledPollCreationException}
   */
  @Test(expected = NullPointerException.class)
  public void checkCreationOfNullScheduler() {
    factory(null, null).schedule(null, this.newRunnable());
  }

  private PeriodicScheduler factory(ScheduledFuture schedulerToReturn, MuleContext muleContext) {
    PeriodicScheduler pollFactory = mock(PeriodicScheduler.class);
    when(pollFactory.doSchedule(any(), any())).thenReturn(schedulerToReturn);
    return pollFactory;
  }

  private Runnable newRunnable() {
    return () -> {
      // no-op
    };
  }
}
