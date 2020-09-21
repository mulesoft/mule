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
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

public class OtherReconnectionSource extends Source<Integer, Void> {

  @Connection
  ConnectionProvider<ReconnectableConnection> connectionProvider;

  @Inject
  SchedulerService schedulerService;

  private final AtomicReference<ScheduledFuture<?>> scheduleWithFixedDelay = new AtomicReference<>();

  private Scheduler scheduler;

  private static final AtomicInteger countStartedSources = new AtomicInteger(0);

  @Override
  public void onStart(SourceCallback<Integer, Void> sourceCallback) throws MuleException {
    countStartedSources.accumulateAndGet(1, Integer::sum);
    delay(1000L);
    doStart(sourceCallback);
  }

  private void doStart(SourceCallback<Integer, Void> sourceCallback) throws MuleException {
    ReconnectableConnection connection = connectionProvider.connect();
    this.scheduler = schedulerService.ioScheduler();

    if (ReconnectableConnectionProvider.otherFail) {
      delay(500L);
      sourceCallback.onConnectionException(new ConnectionException(new RuntimeException(), connection));
      ReconnectableConnectionProvider.otherFail = !ReconnectableConnectionProvider.otherFail;
      throw new RuntimeException("Fail starting source");
    }

    scheduleWithFixedDelay.set(this.scheduler.scheduleWithFixedDelay(() -> {
      if (ReconnectableConnectionProvider.otherFail) {
        sourceCallback.onConnectionException(new ConnectionException(new RuntimeException(), connection));
      } else {
        sourceCallback.handle(Result.<Integer, Void>builder().output(countStartedSources.get()).build());
      }
    }, 0, 1000, MILLISECONDS));
  }

  @Override
  public void onStop() {
    countStartedSources.accumulateAndGet(-1, Integer::sum);
    delay(1000L);
    if (this.scheduler != null) {
      if (scheduleWithFixedDelay.get() != null) {
        scheduleWithFixedDelay.get().cancel(true);
      }
      this.scheduler.stop();
    }
  }

  private void delay(Long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
