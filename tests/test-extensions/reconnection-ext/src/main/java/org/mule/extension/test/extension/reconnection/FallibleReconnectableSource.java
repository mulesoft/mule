/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

public class FallibleReconnectableSource extends Source<Integer, Void> {

  public static volatile boolean fail;

  @Connection
  ConnectionProvider<ReconnectableConnection> connectionProvider;

  @Inject
  SchedulerService schedulerService;

  private Latch latch = new Latch();
  private Scheduler scheduler;
  private ScheduledFuture<?> scheduleWithFixedDelay;

  private static final AtomicInteger countStartedSources = new AtomicInteger(0);

  @Override
  public void onStart(SourceCallback<Integer, Void> sourceCallback) throws MuleException {
    countStartedSources.addAndGet(1);
    doStart(sourceCallback);
  }

  private void doStart(SourceCallback<Integer, Void> sourceCallback) throws MuleException {
    ReconnectableConnection connection = connectionProvider.connect();
    this.scheduler = schedulerService.ioScheduler();

    if (fail) {
      await();
      sourceCallback.onConnectionException(new ConnectionException(new RuntimeException(), connection));
      fail = false;
      latch = new Latch();
      throw new RuntimeException("Fail starting source");
    }

    scheduleWithFixedDelay = this.scheduler.scheduleWithFixedDelay(() -> {
      if (fail) {
        sourceCallback.onConnectionException(new ConnectionException(new RuntimeException(), connection));
        latch.release();
      } else {
        sourceCallback.handle(Result.<Integer, Void>builder().output(countStartedSources.get()).build());
      }
    }, 0, 1000, MILLISECONDS);
  }

  @Override
  public void onStop() {
    countStartedSources.addAndGet(-1);
    if (this.scheduleWithFixedDelay != null) {
      this.scheduleWithFixedDelay.cancel(true);
    }
    if (this.scheduler != null) {
      this.scheduler.stop();
    }
  }

  private void await() {
    try {
      latch.await(5000, MILLISECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
