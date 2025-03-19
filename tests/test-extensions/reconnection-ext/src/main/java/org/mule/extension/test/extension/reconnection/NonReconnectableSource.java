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
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.source.Source;
import org.mule.sdk.api.runtime.source.SourceCallback;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

/**
 * This class represents a {@link Source} that after failure never reconnects. It also keeps track of how many attempts were made.
 */
public class NonReconnectableSource extends Source<Void, Void> {

  public static volatile boolean fail = false;

  @Connection
  ConnectionProvider<ReconnectableConnection> connectionProvider;

  @Inject
  SchedulerService schedulerService;

  private Scheduler scheduler;
  private ScheduledFuture<?> scheduleWithFixedDelay;

  public static final AtomicInteger attempts = new AtomicInteger(0);

  @Override
  public void onStart(SourceCallback<Void, Void> sourceCallback) throws MuleException {
    if (fail) {
      attempts.getAndIncrement();
      throw new RuntimeException("Fail starting source");
    }

    attempts.set(0);

    ReconnectableConnection connection = connectionProvider.connect();
    this.scheduler = schedulerService.ioScheduler();

    scheduleWithFixedDelay = this.scheduler.scheduleWithFixedDelay(() -> {
      if (fail) {
        sourceCallback.onConnectionException(new ConnectionException(new RuntimeException(), connection));
      } else {
        sourceCallback.handle(Result.<Void, Void>builder().build());
      }
    }, 0, 1000, MILLISECONDS);
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
}
