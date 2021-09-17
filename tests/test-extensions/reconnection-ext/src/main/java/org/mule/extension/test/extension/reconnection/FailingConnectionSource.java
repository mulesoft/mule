/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.test.extension.reconnection;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Alias("failing-connection-source")
public class FailingConnectionSource extends Source<List<FailingConnection>, Void> {

  public static Integer failures = 0;
  private static List<FailingConnection> connections = Collections.synchronizedList(new ArrayList<>());

  @Connection
  ConnectionProvider<FailingConnection> connectionProvider;

  FailingConnection connection;

  @Override
  public void onStart(SourceCallback<List<FailingConnection>, Void> sourceCallback) throws MuleException {
    connection = connectionProvider.connect();
    try {
      connections.add(connection);
      if (failures < 4) {
        failures += 1;
        connection.sendWithFailure("failure");
      } else {
        connection.send("successful");
        sourceCallback.handle(Result.<List<FailingConnection>, Void>builder().output(connections).build());
      }
    } catch (Exception e) {
      throw new ConnectionException(e, connection);
    }
  }

  @Override
  public void onStop() {
    connectionProvider.disconnect(connection);
  }
}
