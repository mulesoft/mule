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

import org.mule.runtime.core.api.source.polling.ScheduledPollFactory;
import org.mule.runtime.core.source.polling.schedule.ScheduledPoll;

import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Factory of the Cron {@link ScheduledPoll}.
 * </p>
 *
 * @since 3.5.0
 */
public class CronScheduledPollFactory extends ScheduledPollFactory {

  private static final Logger logger = LoggerFactory.getLogger(CronScheduledPollFactory.class);

  private static final String TZ_GMT_ID = "GMT";

  private String expression;

  private String timeZone;

  @Override
  protected ScheduledPoll doCreate(String name, Runnable job) {
    return new ScheduledPoll(context.getSchedulerService(), name, job,
                             executor -> executor.scheduleWithCronExpression(job, expression, resolveTimeZone(timeZone)));
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
