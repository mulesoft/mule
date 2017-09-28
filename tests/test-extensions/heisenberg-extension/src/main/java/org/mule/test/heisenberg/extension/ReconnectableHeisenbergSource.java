/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Streaming;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.source.EmitsResponse;
import org.mule.runtime.extension.api.runtime.connectivity.Reconnectable;
import org.mule.runtime.extension.api.runtime.connectivity.ReconnectionCallback;

@Alias("ReconnectableListenPayments")
@EmitsResponse
@Streaming
@MediaType(TEXT_PLAIN)
public class ReconnectableHeisenbergSource extends HeisenbergSource implements Reconnectable {

  public static int succesfulReconnections = 0;
  public static int failedReconnections = 0;

  @Parameter
  private boolean reconnectable;

  @Override
  public void reconnect(ConnectionException exception, ReconnectionCallback reconnectionCallback) {
    if (reconnectable) {
      succesfulReconnections++;
      reconnectionCallback.success();
    } else {
      failedReconnections++;
      reconnectionCallback.failed(new ConnectionException("Boss said no", exception));
    }
  }


}
