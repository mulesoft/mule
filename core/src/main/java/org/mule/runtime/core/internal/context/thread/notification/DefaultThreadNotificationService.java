/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context.thread.notification;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.util.Pair;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.leftPad;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation for {@link ThreadNotificationService}. It collects {@link ThreadNotificationElement}' and
 * every X seconds logs statistics from them, using the {@link ThreadsStatistics}.
 *
 * @since 4.2
 */
public class DefaultThreadNotificationService implements ThreadNotificationService, Runnable, Lifecycle {

  private static final int DEFAULT_INTERVAL = 5;
  private static final String newLine = System.getProperty("line.separator").toString();
  private ThreadsStatistics stats = new ThreadsStatistics();
  private int interval;
  private boolean stopped = false;

  public DefaultThreadNotificationService() {
    this(DEFAULT_INTERVAL);
  }

  public DefaultThreadNotificationService(int interval) {
    this.interval = interval;
    if (THREAD_LOGGING) {
      new Thread(this).start();
    }
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
  public void clear() {
    stats.clear();
  }

  private void logStats() {
    REPORT_LOGGER.error("HEEELOOOOOOOO");
    for (Pair<String, String> transition : stats.getPossibleTransitions()) {
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
      // We set this to warn logging level because the entire pipeline is already created, so even if the
      // logging level changes, the pipeline should be defined using a system property (the ThreadNotificationLogger
      // needs to know whether to add logging phases or not).
      REPORT_LOGGER.error(msg);
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
  public void run() {
    while (!Thread.interrupted() && !stopped) {
      try {
        Thread.sleep(interval * 1000);
      } catch (InterruptedException e) {
        break;
      }
      logStats();
    }
  }

  @Override
  public void dispose() {

  }

  @Override
  public void initialise() throws InitialisationException {

  }

  @Override
  public void start() throws MuleException {

  }

  @Override
  public void stop() throws MuleException {
    stopped = true;
  }
}
