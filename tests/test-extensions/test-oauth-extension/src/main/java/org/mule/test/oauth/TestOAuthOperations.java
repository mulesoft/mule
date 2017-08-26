/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth;

import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.connectivity.oauth.AccessTokenExpiredException;
import org.mule.runtime.extension.api.connectivity.oauth.AuthCodeRequest;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;

public class TestOAuthOperations {

  public TestOAuthConnection getConnection(@Connection TestOAuthConnection connection) {
    return connection;
  }

  public void captureCallbackPayloads(@Config TestOAuthExtension config,
                                      @Optional AuthCodeRequest request,
                                      @Optional AuthorizationCodeState state) {
    config.getCapturedAuthCodeRequests().add(request);
    config.getCapturedAuthCodeStates().add(state);
  }

  public void tokenExpired(@Connection TestOAuthConnection connection) {
    final AuthorizationCodeState state = connection.getState().getState();
    if (!state.getAccessToken().endsWith("refreshed")) {
      throw new AccessTokenExpiredException(state.getResourceOwnerId());
    }
  }
}
