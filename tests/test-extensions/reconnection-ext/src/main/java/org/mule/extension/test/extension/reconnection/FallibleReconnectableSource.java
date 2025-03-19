/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.test.extension.reconnection;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.source.Source;
import org.mule.sdk.api.runtime.source.SourceCallback;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

/**
 * This class represents a {@link Source} that can fail starting repeatedly and after all reconnect successfully. Also keeps in
 * track if multiple instances were starting at the same time.
 */
public class FallibleReconnectableSource extends Source<Void, Void> {

  public static volatile boolean fail = false;
  public static volatile boolean simultaneouslyStartedSources = false;

  @Connection
  ConnectionProvider<ReconnectableConnection> connectionProvider;

  @Inject
  SchedulerService schedulerService;

  private Scheduler scheduler;
  private ScheduledFuture<?> scheduleWithFixedDelay;

  private static final AtomicInteger countStartedSources = new AtomicInteger(0);
  private static final Latch latch = new Latch();

  @Override
  public void onStart(SourceCallback<Void, Void> sourceCallback) throws MuleException {
    if (countStartedSources.addAndGet(1) > 1) {
      simultaneouslyStartedSources = true;
    }
    try {
      doStart(sourceCallback);
    } finally {
      countStartedSources.addAndGet(-1);
    }
  }

  private void doStart(SourceCallback<Void, Void> sourceCallback) throws MuleException {
    if (fail) {
      await();
      fail = false;
      throw new RuntimeException("Fail starting source");
    }

    ReconnectableConnection connection = connectionProvider.connect();
    this.scheduler = schedulerService.ioScheduler();

    scheduleWithFixedDelay = this.scheduler.scheduleWithFixedDelay(() -> {
      if (fail) {
        sourceCallback.onConnectionException(new ConnectionException(new RuntimeException(), connection));
      } else {
        sourceCallback.handle(Result.<Void, Void>builder().build());
      }
    }, 0, 250, MILLISECONDS);
  }

  @Override
  public void onStop() {
    if (this.scheduleWithFixedDelay != null) {
      this.scheduleWithFixedDelay.cancel(true);
    }
    if (this.scheduler != null) {
      this.scheduler.stop();
    }
  }

  private void await() {
    try {
      latch.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static void release() {
    latch.release();
  }
}
