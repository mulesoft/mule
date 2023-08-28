/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.oauth;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.test.oauth.TestOAuthConnectionProvider.ACCESS_TOKEN_URL;
import static org.mule.test.oauth.TestOAuthConnectionProvider.AUTH_URL;
import static org.mule.test.oauth.TestOAuthConnectionProvider.DEFAULT_SCOPE;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.sdk.api.annotation.connectivity.oauth.AuthorizationCode;
import org.mule.sdk.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.sdk.api.connectivity.oauth.OAuthState;

@AuthorizationCode(accessTokenUrl = ACCESS_TOKEN_URL,
    authorizationUrl = AUTH_URL,
    defaultScopes = DEFAULT_SCOPE)
@Alias("sdk-authcode-connection")
public class SdkTestOAuthConnectionProvider extends TestSdkOAuthConnectionState
    implements ConnectionProvider<TestOAuthConnection> {

  private AuthorizationCodeState state;

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

  @Override
  protected OAuthState getSdkState() {
    return state;
  }
}
