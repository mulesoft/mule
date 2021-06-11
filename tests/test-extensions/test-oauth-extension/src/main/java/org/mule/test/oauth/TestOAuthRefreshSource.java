/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.connectivity.oauth.AccessTokenExpiredException;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

import java.util.concurrent.ScheduledFuture;

import javax.inject.Inject;

@MediaType(TEXT_PLAIN)
@Alias("listener")
public class TestOAuthRefreshSource extends Source<String, String> {

  public static final String SOURCE_ITEM_PAYLOAD = "Item payload!";
  public static final String SOURCE_ITEM_ATTRIBUTE = "Item attribute!";

  @Inject
  private SchedulerService schedulerService;

  private Scheduler executor;
  private ScheduledFuture<?> scheduledFuture;

  @org.mule.sdk.api.annotation.param.Connection
  private ConnectionProvider<TestOAuthConnection> connectionProvider;
  private TestOAuthConnection connection;

  @Override
  public void onStart(SourceCallback<String, String> sourceCallback) throws MuleException {
    connection = connectionProvider.connect();
    executor = schedulerService.cpuLightScheduler();
    scheduledFuture = executor.scheduleAtFixedRate(() -> dispatchItem(sourceCallback), 2000, 500, MILLISECONDS);
  }

  private void dispatchItem(SourceCallback<String, String> sourceCallback) {
    if (!connection.getState().getState().getAccessToken().contains("refreshed")) {
      sourceCallback.onConnectionException(new ConnectionException(new AccessTokenExpiredException("Token is expired!")));
      return;
    }
    sourceCallback.handle(Result.<String, String>builder().output(SOURCE_ITEM_PAYLOAD).attributes(SOURCE_ITEM_ATTRIBUTE).build());
  }

  @Override
  public void onStop() {
    if (executor != null && scheduledFuture != null) {
      scheduledFuture.cancel(true);
      executor.stop();
    }

    if (connection != null && connectionProvider != null) {
      connectionProvider.disconnect(connection);
    }
  }
}
