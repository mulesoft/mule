/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.modules.schedulers.cron;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.mule.runtime.core.api.exception.SystemExceptionHandler;
import org.mule.runtime.core.api.schedule.Scheduler;
import org.mule.runtime.core.source.polling.PollingTask;
import org.mule.runtime.core.source.polling.PollingWorker;

import org.junit.Test;

public class CronSchedulerFactoryTest {

  @Test
  public void testSchedulerCreation() {
    CronSchedulerFactory factory = new CronSchedulerFactory();
    factory.setExpression("my expression");

    Scheduler scheduler = factory.create("name", new PollingWorker(mock(PollingTask.class), mock(SystemExceptionHandler.class)));

    assertTrue(scheduler instanceof CronScheduler);
    assertEquals("my expression", ((CronScheduler) scheduler).getCronExpression());
  }

}
