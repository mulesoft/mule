/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Streaming;
import org.mule.runtime.extension.api.annotation.notification.Fires;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.source.EmitsResponse;
import org.mule.runtime.extension.api.runtime.connectivity.Reconnectable;
import org.mule.runtime.extension.api.runtime.connectivity.ReconnectionCallback;

@Alias("ReconnectableListenPayments")
@EmitsResponse
@Fires(SourceNotificationProvider.class)
@Streaming
@MediaType(TEXT_PLAIN)
public class ReconnectableHeisenbergSource extends SdkHeisenbergSource implements Reconnectable {

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
