/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.extension.test.extension.reconnection;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

@MediaType("text/plain")
public class ReconectionSource extends Source<ReconnectableConnection, Void> {

  @Connection
  ConnectionProvider<ReconnectableConnection> connectionProvider;

  @Inject
  SchedulerService schedulerService;

  private final AtomicReference<ScheduledFuture<?>> scheduleWithFixedDelay = new AtomicReference<>();

  private Scheduler scheduler;

  @Override
  public void onStart(SourceCallback<ReconnectableConnection, Void> sourceCallback) throws MuleException {
    ReconnectableConnection connection = connectionProvider.connect();
    scheduler = schedulerService.ioScheduler();
    scheduleWithFixedDelay.set(scheduler.scheduleWithFixedDelay(() -> {
      if (ReconnectableConnectionProvider.fail) {
        sourceCallback.onConnectionException(new ConnectionException(new RuntimeException(), connection));
        scheduleWithFixedDelay.get().cancel(true);
      } else {
        sourceCallback.handle(Result.<ReconnectableConnection, Void>builder().output(connection).build());
      }
    }, 0, 1000, MILLISECONDS));

  }

  @Override
  public void onStop() {
    if (scheduler != null) {
      scheduleWithFixedDelay.get().cancel(true);
      scheduler.stop();
    }
  }
}
