/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth;

import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.connectivity.oauth.AccessTokenExpiredException;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthState;

public class TestOAuthOperations {

  public TestOAuthConnection getConnection(@Connection TestOAuthConnection connection) {
    return connection;
  }

  public void tokenExpired(@Connection TestOAuthConnection connection) {
    final OAuthState state = connection.getState().getState();
    if (state != null && !state.getAccessToken().endsWith("refreshed")) {
      if (state instanceof AuthorizationCodeState) {
        throw new AccessTokenExpiredException(((AuthorizationCodeState) state).getResourceOwnerId());
      } else {
        throw new AccessTokenExpiredException();
      }
    }
  }
}
