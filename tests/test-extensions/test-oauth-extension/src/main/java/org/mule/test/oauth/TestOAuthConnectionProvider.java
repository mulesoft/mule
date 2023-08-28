/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.oauth;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.sdk.api.annotation.connectivity.oauth.AuthorizationCode;

@AuthorizationCode(accessTokenUrl = TestOAuthConnectionProvider.ACCESS_TOKEN_URL,
    authorizationUrl = TestOAuthConnectionProvider.AUTH_URL,
    defaultScopes = TestOAuthConnectionProvider.DEFAULT_SCOPE)
public class TestOAuthConnectionProvider extends LegacyTestOAuthConnectionState
    implements ConnectionProvider<TestOAuthConnection> {

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
    return success();
  }
}
