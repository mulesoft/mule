/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.source.scheduler;

import static java.lang.String.format;
import static java.util.TimeZone.getDefault;
import static java.util.TimeZone.getTimeZone;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.TimeZone;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduler for Cron scheduled jobs.
 *
 * @since 3.5.0, moved from {@link org.mule.runtime.modules.schedulers.cron.CronSchedulerFactory}.
 */
@Alias("cron")
public class CronScheduler extends PeriodicScheduler {

  private static final Logger logger = LoggerFactory.getLogger(CronScheduler.class);

  private static final String TZ_GMT_ID = "GMT";

  @Parameter
  private String expression;

  /**
   * The ID of the time zone in which the expression will be based.
   * Refer to {@code java.util.TimeZone} for the format and possible values of the timeZone ID.
   */
  @Parameter
  private String timeZone;

  @Override
  protected ScheduledFuture<?> doSchedule(Scheduler executor, Runnable job) {
    return executor.scheduleWithCronExpression(job, expression, resolveTimeZone(timeZone));
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
