/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.modules.schedulers.cron;

import static java.util.TimeZone.getTimeZone;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.schedule.Scheduler;
import org.mule.runtime.core.api.schedule.Schedulers;
import org.mule.functional.junit4.FunctionalTestCase;

import java.util.Collection;

import org.junit.Test;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class CronsTimeZoneSchedulerTest extends FunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "cron-timezone-scheduler-config.xml";
  }

  @Test
  public void timeZoneInScheduler() throws Exception {
    Collection<Scheduler> schedulers =
        muleContext.getRegistry().lookupScheduler(Schedulers.flowConstructPollingSchedulers("pollWithTimeZone"));

    assertThat(schedulers, hasSize(1));
    for (Scheduler scheduler : schedulers) {
      assertThat(scheduler, instanceOf(CronScheduler.class));
      // Just can assert that the tx in the config gets to the mule scheduler. To instrospect the quatz object to
      // check its there would be very complex.
      assertThat(((CronScheduler) scheduler).getTimeZone(), is(getTimeZone("America/Argentina/Buenos_Aires")));
    }
  }

  @Test
  public void invalidTimeZoneInScheduler() throws Exception {
    Collection<Scheduler> schedulers =
        muleContext.getRegistry().lookupScheduler(Schedulers.flowConstructPollingSchedulers("pollWithInvalidTimeZone"));

    assertThat(schedulers, hasSize(1));
    for (Scheduler scheduler : schedulers) {
      assertThat(scheduler, instanceOf(CronScheduler.class));
      // Just can assert that the tx in the config gets to the mule scheduler. To instrospect the quatz object to
      // check its there would be very complex.
      assertThat(((CronScheduler) scheduler).getTimeZone(), is(getTimeZone("GMT")));
    }
  }
}
