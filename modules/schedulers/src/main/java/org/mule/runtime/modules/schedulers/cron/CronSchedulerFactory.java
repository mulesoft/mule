/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.modules.schedulers.cron;

import static java.lang.String.format;
import static java.util.TimeZone.getDefault;
import static java.util.TimeZone.getTimeZone;
import org.mule.runtime.core.api.schedule.Scheduler;
import org.mule.runtime.core.api.schedule.SchedulerFactory;
import org.mule.runtime.core.source.polling.PollingWorker;

import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Factory of the Cron Scheduler for poll
 * </p>
 *
 * @since 3.5.0
 */
public class CronSchedulerFactory extends SchedulerFactory<PollingWorker> {

  private static final Logger logger = LoggerFactory.getLogger(CronSchedulerFactory.class);

  private static final String TZ_GMT_ID = "GMT";

  private String expression;

  private String timeZone;

  @Override
  protected Scheduler doCreate(String name, PollingWorker job) {
    CronScheduler cronScheduler = new CronScheduler(name, job, expression, resolveTimeZone(name));
    cronScheduler.setMuleContext(context);
    return cronScheduler;
  }

  protected TimeZone resolveTimeZone(String name) {
    TimeZone resolvedTimeZone = timeZone == null ? getDefault() : getTimeZone(timeZone);
    if (!TZ_GMT_ID.equals(timeZone) && resolvedTimeZone.equals(getTimeZone(TZ_GMT_ID))) {
      logger.warn(format("Configured timezone '%s' is invalid in scheduler '%s'. Defaulting to %s", timeZone, name, TZ_GMT_ID));
    }
    return resolvedTimeZone;
  }

  public void setExpression(String expression) {
    this.expression = expression;
  }

  public void setTimeZone(String timeZone) {
    this.timeZone = timeZone;
  }
}
