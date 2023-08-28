/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.oauth;

import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;

import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.connectivity.oauth.AccessTokenExpiredException;
import org.mule.sdk.api.annotation.connectivity.oauth.AuthorizationCode;

@AuthorizationCode(accessTokenUrl = TestOAuthConnectionProvider.ACCESS_TOKEN_URL,
    authorizationUrl = TestOAuthConnectionProvider.AUTH_URL)
@Alias("refresh-validation")
public class TestOAuthRefreshValidationProvider extends LegacyTestOAuthConnectionState
    implements CachedConnectionProvider<TestOAuthConnection> {

  public static int TIMES_REFRESH_IS_NEEDED = 0;

  public static final String ACCESS_TOKEN_URL = "accessTokenUrl";
  public static final String AUTH_URL = "authUrl";
  public static final String DEFAULT_SCOPE = "defaultScope";

  @Override
  public TestOAuthConnection connect() throws ConnectionException {
    return new TestOAuthConnection(this);
  }

  public void disconnect(TestOAuthConnection connection) {

  }

  @Override
  public ConnectionValidationResult validate(TestOAuthConnection connection) {
    if (TIMES_REFRESH_IS_NEEDED > 0) {
      TIMES_REFRESH_IS_NEEDED--;
      return failure("The token needs to be refreshed", new AccessTokenExpiredException());
    }
    return success();
  }

}
