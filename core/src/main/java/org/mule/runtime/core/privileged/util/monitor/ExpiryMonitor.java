/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.util.monitor;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.propertyHasInvalidValue;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

/**
 * <code>ExpiryMonitor</code> can monitor objects beased on an expiry time and can invoke a callback method once the object time
 * has expired. If the object does expire it is removed from this monitor.
 */
public class ExpiryMonitor implements Runnable, Disposable {

  /**
   * logger used by this class
   */
  private static final Logger LOGGER = getLogger(ExpiryMonitor.class);

  private Scheduler scheduler;

  private Map<Expirable, ExpirableHolder> monitors;

  private long monitorFrequency;

  private String name;

  private MuleContext muleContext;

  private boolean onPollingNodeOnly;

  public ExpiryMonitor(MuleContext muleContext, boolean onPollingNodeOnly) {
    this.muleContext = muleContext;
    this.onPollingNodeOnly = onPollingNodeOnly;
  }

  public ExpiryMonitor(String name, long monitorFrequency, MuleContext muleContext, boolean onPollingNodeOnly) {
    this(muleContext, onPollingNodeOnly);
    this.name = name;
    this.monitorFrequency = monitorFrequency;
    init();
  }

  protected void init() {
    if (monitorFrequency <= 0) {
      throw new IllegalArgumentException(propertyHasInvalidValue("monitorFrequency", Long.valueOf(monitorFrequency))
          .toString());
    }
    monitors = new ConcurrentHashMap<>();
    if (scheduler == null) {
      this.scheduler = muleContext.getSchedulerService()
          .customScheduler(muleContext.getSchedulerBaseConfig().withName(name + ".expiry.monitor").withMaxConcurrentTasks(1));
      scheduler.scheduleWithFixedDelay(this, 0, monitorFrequency, MILLISECONDS);
    }
  }

  /**
   * Adds an expirable object to monitor. If the Object is already being monitored it will be reset and the millisecond timeout
   * will be ignored
   *
   * @param value the expiry value
   * @param timeUnit The time unit of the Expiry value
   * @param expirable the object that will expire
   */
  public void addExpirable(long value, TimeUnit timeUnit, Expirable expirable) {
    if (isRegistered(expirable)) {
      resetExpirable(expirable);
    } else {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Adding new expirable: " + expirable);
      }
      monitors.put(expirable, new ExpirableHolder(timeUnit.toMillis(value), expirable));
    }
  }

  public boolean isRegistered(Expirable expirable) {
    return monitors.containsKey(expirable);
  }

  public void removeExpirable(Expirable expirable) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Removing expirable: " + expirable);
    }
    monitors.remove(expirable);
  }

  public void resetExpirable(Expirable expirable) {
    ExpirableHolder eh = monitors.get(expirable);
    if (eh != null) {
      eh.reset();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Reset expirable: " + expirable);
      }
    }
  }

  /**
   * The action to be performed by this timer task.
   */
  @Override
  public void run() {
    if (!onPollingNodeOnly || muleContext == null || muleContext.isPrimaryPollingInstance()) {
      synchronized (monitors) {
        for (ExpirableHolder holder : monitors.values()) {
          if (holder.isExpired()) {
            removeExpirable(holder.getExpirable());
            holder.getExpirable().expired();
          }
        }
      }
    }
  }

  @Override
  public void dispose() {
    LOGGER.info("disposing monitor");
    scheduler.stop();
    ExpirableHolder holder;
    synchronized (monitors) {
      for (Object element : monitors.values()) {
        holder = (ExpirableHolder) element;
        removeExpirable(holder.getExpirable());
        try {
          holder.getExpirable().expired();
        } catch (Exception e) {
          LOGGER.warn(e.getMessage());
        }
      }
    }
  }

  @Override
  public String toString() {
    return format("ExpiryMonitor {monitorFrequency: %d, monitors: %s}", monitorFrequency, monitors.toString());
  }

  private static class ExpirableHolder {

    private Expirable expirable;
    private long milliseconds;
    private long created;

    public ExpirableHolder(long milliseconds, Expirable expirable) {
      this.milliseconds = milliseconds;
      this.expirable = expirable;
      created = currentTimeMillis();
    }

    public Expirable getExpirable() {
      return expirable;
    }

    public boolean isExpired() {
      return (currentTimeMillis() - milliseconds) > created;
    }

    public void reset() {
      created = currentTimeMillis();
    }

    @Override
    public String toString() {
      return format("ExpirableHolder {expirable: %s, milliseconds: %d, created: %d}", expirable.toString(), milliseconds,
                    created);
    }
  }
}
