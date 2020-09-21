/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.connectivity.oauth.AccessTokenExpiredException;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.PollContext;
import org.mule.runtime.extension.api.runtime.source.PollingSource;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

@MediaType(TEXT_PLAIN)
@Alias("poller")
public class TestOAuthRefreshPollingSource extends PollingSource<String, String> {

  public static final String SOURCE_ITEM_PAYLOAD = "Item payload!";
  public static final String SOURCE_ITEM_ATTRIBUTE = "Item attribute!";

  @Connection
  private ConnectionProvider<TestOAuthConnection> connectionProvider;

  @Override
  protected void doStart() throws MuleException {
    // Do nothing
  }

  @Override
  protected void doStop() {
    // Do nothing
  }

  @Override
  public void poll(PollContext<String, String> pollContext) {
    TestOAuthConnection testOAuthConnection = null;
    try {
      testOAuthConnection = connectionProvider.connect();
    } catch (ConnectionException e) {
      throw new MuleRuntimeException(e);
    }
    if (!testOAuthConnection.getState().getState().getAccessToken().contains("refreshed")) {
      pollContext.onConnectionException(new ConnectionException(new AccessTokenExpiredException("Token is expired!")));
      return;
    }
    pollContext.accept(pollItem -> pollItem
        .setResult(Result.<String, String>builder().output(SOURCE_ITEM_PAYLOAD).attributes(SOURCE_ITEM_ATTRIBUTE).build()));
    connectionProvider.disconnect(testOAuthConnection);
  }

  @Override
  public void onRejectedItem(Result<String, String> result, SourceCallbackContext callbackContext) {
    // Do nothing
  }

}
