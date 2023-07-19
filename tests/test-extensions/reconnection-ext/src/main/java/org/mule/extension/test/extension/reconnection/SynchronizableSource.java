/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.extension.test.extension.reconnection;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.PollContext;
import org.mule.runtime.extension.api.runtime.source.PollingSource;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

@Alias("sync-source")
@MediaType("text/plain")
public class SynchronizableSource extends PollingSource<String, Void> {

  public static boolean first = true;

  @Connection
  private ConnectionProvider<SynchronizableConnection> connectionProvider;

  @Override
  protected void doStart() throws MuleException {}

  @Override
  protected void doStop() {}

  @Override
  public void poll(PollContext<String, Void> pollContext) {
    if (!first) {
      return;
    }
    first = false;
    SynchronizableConnection connection = null;
    try {
      connection = connectionProvider.connect();
    } catch (ConnectionException e) {
    }
    Latch latch = connection.getDisconnectionLatch();
    pollContext.onConnectionException(new ConnectionException(
                                                              new RuntimeException("Poll failed"),
                                                              connection));
    latch.release();
  }

  @Override
  public void onRejectedItem(Result<String, Void> result, SourceCallbackContext sourceCallbackContext) {

  }
}
