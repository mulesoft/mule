/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.annotation.source.EmitsResponse;
import org.mule.sdk.api.annotation.Alias;
import org.mule.sdk.api.annotation.Streaming;
import org.mule.sdk.api.annotation.notification.Fires;
import org.mule.sdk.api.annotation.param.MediaType;
import org.mule.sdk.api.annotation.param.Parameter;
import org.mule.sdk.api.runtime.connectivity.Reconnectable;
import org.mule.sdk.api.runtime.connectivity.ReconnectionCallback;

@Alias("SdkReconnectableListenPayments")
@EmitsResponse
@Fires(SdkSourceNotificationProvider.class)
@Streaming
@MediaType(TEXT_PLAIN)
public class ReconnectableHeisenbergSdkSource extends SdkHeisenbergSource implements Reconnectable {

  public static int succesfulReconnectionsSdk = 0;
  public static int failedReconnectionsSdk = 0;

  @Parameter
  private boolean reconnectable;

  @Override
  public void reconnect(ConnectionException exception, ReconnectionCallback reconnectionCallback) {
    if (reconnectable) {
      succesfulReconnectionsSdk++;
      reconnectionCallback.success();
    } else {
      failedReconnectionsSdk++;
      reconnectionCallback.failed(new ConnectionException("Boss said no", exception));
    }
  }
}
