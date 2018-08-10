/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context.thread.notification;

import static org.mule.runtime.core.api.context.thread.notification.ThreadNotificationService.ThreadNotificationElement;

import org.apache.commons.math3.stat.StatUtils;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.api.util.Pair;

import java.util.Collection;
import java.util.Set;

import static java.lang.Math.sqrt;

/**
 * Calculates Statistics from {@link ThreadNotificationElement}'s,
 * separated by the type of thread switch
 *
 * @since 4.2
 */
public class ThreadsStatistics {

  private MultiMap<Pair<String, String>, Double> times = new MultiMap<>();

  public synchronized void addThreadNotificationElement(ThreadNotificationElement notification) {
    Pair<String, String> key = new Pair<>(notification.getFromThreadType(), notification.getToThreadType());
    times.put(key, new Double(notification.getLatencyTime()));
  }

  public synchronized void addThreadNotificationElements(Collection<ThreadNotificationElement> notifications) {
    notifications.stream().forEach(notification -> addThreadNotificationElement(notification));
  }

  public Set<Pair<String, String>> getPossibleTransitions() {
    return times.keySet();
  }

  private double[] getTimes(String from, String to) {
    return times.getAll(new Pair<>(from, to)).stream().mapToDouble(i -> i).toArray();
  }

  public double getMean(String from, String to) {
    return StatUtils.mean(getTimes(from, to));
  }

  public double getStdDeviation(String from, String to) {
    return sqrt(StatUtils.variance(getTimes(from, to)));
  }

  public double percentile(String from, String to, double quantile) {
    return StatUtils.percentile(getTimes(from, to), quantile);
  }

  public int getCount(String from, String to) {
    return getTimes(from, to).length;
  }

  public int getCount(Pair<String, String> transition) {
    return times.getAll(transition).size();
  }

  public double getMean(Pair<String, String> transition) {
    return getMean(transition.getFirst(), transition.getSecond());
  }

  public double getStdDeviation(Pair<String, String> transition) {
    return getStdDeviation(transition.getFirst(), transition.getSecond());
  }

  public double percentile(Pair<String, String> transition, double quantile) {
    return percentile(transition.getFirst(), transition.getSecond(), quantile);
  }

  public void clear() {
    this.times.clear();
  }
}
