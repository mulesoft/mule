/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context.thread.notification;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.thread.notification.ThreadNotificationService;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.leftPad;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_LOGGING_INTERVAL_SCHEDULERS_LATENCY_REPORT;

/**
 * Implementation for {@link ThreadNotificationService}. It collects {@link ThreadNotificationElement}' and
 * every X seconds logs statistics from them, using the {@link ThreadsStatistics}.
 *
 * @since 4.2
 */
public class DefaultThreadNotificationService implements ThreadNotificationService, Startable, Stoppable {

  private static final String newLine = System.getProperty("line.separator");
  private ThreadsStatistics stats = new ThreadsStatistics();
  private int interval;
  private ScheduledFuture fixedRate;

  @Inject
  private SchedulerService schedulerService;
  private Scheduler scheduler;

  @Inject
  private MuleContext muleContext;

  public DefaultThreadNotificationService() {
    this(Integer.getInteger(MULE_LOGGING_INTERVAL_SCHEDULERS_LATENCY_REPORT, -1));
  }

  public DefaultThreadNotificationService(int interval) {
    this.interval = interval;
  }

  @Override
  public void addThreadNotificationElement(ThreadNotificationElement notification) {
    stats.addThreadNotificationElement(notification);
  }

  @Override
  public void addThreadNotificationElements(Collection<ThreadNotificationElement> notifications) {
    stats.addThreadNotificationElements(notifications);
  }

  @Override
  public String getNotification() {
    String msg = "";
    for (Pair<String, String> transition : stats.getPossibleTransitions()) {
      msg += getNotificationFor(transition);
    }
    return msg;
  }

  private String getNotificationFor(Pair<String, String> transition) {
    String count = formattedNumber(stats.getCount(transition));
    String mean = formattedNumber(stats.getMean(transition));
    String std = formattedNumber(stats.getStdDeviation(transition));
    String percentile = formattedNumber(stats.percentile(transition, 0.9));

    Function<String, String> pad = getPad(count, mean, std, percentile);

    String msg = newLine + "Stats for transition " + transition.getFirst() + " - " + transition.getSecond() + newLine;
    msg += " Transitions count: " + pad.apply(count) + newLine;
    msg += "              Mean: " + pad.apply(mean) + " nSecs" + newLine;
    msg += "Standard Deviation: " + pad.apply(std) + " nSecs" + newLine;
    msg += "    Percentile 90%: " + pad.apply(percentile) + " nSecs" + newLine;
    return msg;
  }

  @Override
  public void clear() {
    stats.clear();
  }

  private void logStats() {
    for (Pair<String, String> transition : stats.getPossibleTransitions()) {
      // We set this to warn logging level because the entire pipeline is already created, so even if the
      // logging level changes, the pipeline should be defined using a system property (the ThreadNotificationLogger
      // needs to know whether to add logging phases or not).
      REPORT_LOGGER.warn(getNotificationFor(transition));
    }
  }

  private Function<String, String> getPad(String... elements) {
    final Integer size = Arrays.stream(elements).map(element -> element.length()).max(Integer::compare).get();
    return value -> leftPad(value, size);
  }

  private String formattedNumber(double nanos) {
    return format("%,d", new Double(nanos).intValue());
  }

  @Override
  public void start() throws MuleException {
    if (muleContext.getConfiguration().isThreadLoggingEnabled() && interval > 0 && schedulerService != null) {
      scheduler = schedulerService.cpuLightScheduler();
      fixedRate = scheduler.scheduleAtFixedRate(() -> logStats(), interval, interval, SECONDS);
    }
  }

  @Override
  public void stop() throws MuleException {
    if (fixedRate != null) {
      fixedRate.cancel(true);
      scheduler.stop();
    }
  }
}
