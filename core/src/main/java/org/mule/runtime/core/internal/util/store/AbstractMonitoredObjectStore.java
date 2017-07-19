/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.store;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.store.TemplateObjectStore;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.util.UUID;

import java.io.Serializable;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO
 */
public abstract class AbstractMonitoredObjectStore<T extends Serializable>
    extends TemplateObjectStore<T> implements Runnable, MuleContextAware, Initialisable, Disposable {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  protected MuleContext context;
  private Scheduler scheduler;
  private ScheduledFuture<?> scheduledTask;

  /**
   * the maximum number of entries that this store keeps around. Specify <em>-1</em> if the store is supposed to be "unbounded".
   */
  protected int maxEntries = 4000;

  /**
   * The time-to-live for each message ID, specified in milliseconds, or <em>-1</em> for entries that should never expire. <b>DO
   * NOT</b> combine this with an unbounded store!
   */
  protected int entryTTL = -1;

  /**
   * The interval for periodic bounded size enforcement and entry expiration, specified in milliseconds. Arbitrary positive values
   * between 1 millisecond and several hours or days are possible, but should be chosen carefully according to the expected
   * message rate to prevent out of memory conditions.
   */
  protected int expirationInterval = 1000;

  /**
   * A name for this store, can be used for logging and identification purposes.
   */
  protected String name = null;

  @Override
  public void initialise() throws InitialisationException {
    if (name == null) {
      name = UUID.getUUID();
    }

    if (expirationInterval <= 0) {
      throw new IllegalArgumentException(CoreMessages
          .propertyHasInvalidValue("expirationInterval", new Integer(expirationInterval)).toString());
    }

    if (scheduler == null) {
      this.scheduler = context.getSchedulerService()
          .customScheduler(context.getSchedulerBaseConfig().withName(name + "-Monitor").withMaxConcurrentTasks(1));
      scheduledTask = scheduler.scheduleWithFixedDelay(this, 0, expirationInterval, MILLISECONDS);
    }
  }

  @Override
  public final void run() {
    if (context == null || context.isPrimaryPollingInstance()) {
      expire();
    }
  }

  @Override
  public void dispose() {
    if (scheduledTask != null) {
      scheduledTask.cancel(true);
      scheduler.stop();
    }
  }

  protected MuleContext getMuleContext() {
    return this.context;
  }

  public void setEntryTTL(int entryTTL) {
    this.entryTTL = entryTTL;
  }

  public void setExpirationInterval(int expirationInterval) {
    this.expirationInterval = expirationInterval;
  }

  public void setMaxEntries(int maxEntries) {
    this.maxEntries = maxEntries;
  }

  public void setName(String id) {
    this.name = id;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.context = context;
  }

  public int getEntryTTL() {
    return entryTTL;
  }

  public int getExpirationInterval() {
    return expirationInterval;
  }

  public int getMaxEntries() {
    return maxEntries;
  }

  public String getName() {
    return name;
  }

  protected abstract void expire();
}
