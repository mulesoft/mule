/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.source.polling;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.source.polling.ScheduledPollCreationException;
import org.mule.runtime.core.api.source.polling.ScheduledPollFactory;
import org.mule.runtime.core.source.polling.schedule.ScheduledPoll;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

/**
 * Test to validate the interface {@link ScheduledPollFactory} interface
 */
@SmallTest
public class ScheduledPollFactoryTest extends AbstractMuleTestCase {

  public static final String NAME = "test";

  /**
   * If the {@link Scheduler} created is null then throw an {@link ScheduledPollCreationException}
   */
  @Test(expected = ScheduledPollCreationException.class)
  public void checkCreationOfNullScheduler() {
    factory(null, null).create(NAME, this.newRunnable());
  }

  private ScheduledPollFactory factory(ScheduledPoll schedulerToReturn, MuleContext muleContext) {
    TestedSchedulerFactory factory = new TestedSchedulerFactory(schedulerToReturn);
    factory.setMuleContext(muleContext);
    return factory;
  }

  private Runnable newRunnable() {
    return () -> {
      // no-op
    };
  }

  private class TestedSchedulerFactory extends ScheduledPollFactory {

    private ScheduledPoll schedulerToReturn;

    private TestedSchedulerFactory(ScheduledPoll schedulerToReturn) {
      this.schedulerToReturn = schedulerToReturn;
    }

    @Override
    protected ScheduledPoll doCreate(String name, Runnable job) {
      return schedulerToReturn;
    }
  }

}
