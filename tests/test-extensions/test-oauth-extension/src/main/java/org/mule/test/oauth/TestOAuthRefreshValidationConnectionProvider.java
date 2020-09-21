/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth;

import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.AuthorizationCode;
import org.mule.runtime.extension.api.connectivity.oauth.AccessTokenExpiredException;

@AuthorizationCode(accessTokenUrl = TestOAuthConnectionProvider.ACCESS_TOKEN_URL,
    authorizationUrl = TestOAuthConnectionProvider.AUTH_URL,
    defaultScopes = TestOAuthConnectionProvider.DEFAULT_SCOPE)
@Alias("validated-connection")
public class TestOAuthRefreshValidationConnectionProvider extends TestOAuthConnectionProvider {

  @Override
  public ConnectionValidationResult validate(TestOAuthConnection connection) {
    if (!connection.getState().getState().getAccessToken().contains("refreshed")) {
      return failure("Token is expired!", new AccessTokenExpiredException());
    }
    return success();
  }
}
