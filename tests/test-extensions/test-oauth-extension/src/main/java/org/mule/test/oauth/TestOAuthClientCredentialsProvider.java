/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.runtime.extension.api.runtime.parameter.HttpParameterPlacement.HEADERS;
import static org.mule.sdk.api.security.CredentialsPlacement.QUERY_PARAMS;
import static org.mule.sdk.api.runtime.parameter.HttpParameterPlacement.BODY;
import static org.mule.test.oauth.TestOAuthConnectionProvider.ACCESS_TOKEN_URL;
import static org.mule.test.oauth.TestOAuthConnectionProvider.DEFAULT_SCOPE;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.OAuthParameter;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsState;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthState;
import org.mule.sdk.api.annotation.connectivity.oauth.ClientCredentials;

import java.util.Map;

@ClientCredentials(tokenUrl = ACCESS_TOKEN_URL, defaultScopes = DEFAULT_SCOPE, credentialsPlacement = QUERY_PARAMS)
@Alias("client-credentials")
public class TestOAuthClientCredentialsProvider extends LegacyTestOAuthConnectionState
    implements ConnectionProvider<TestOAuthConnection> {

  private ClientCredentialsState state;

  @OAuthParameter(placement = HEADERS)
  @Optional
  private String knownCustomHeader;

  @org.mule.sdk.api.annotation.connectivity.oauth.OAuthParameter(placement = BODY)
  @Optional
  private String bodyParameter;

  @OAuthParameter(placement = HEADERS)
  @Optional
  @NullSafe
  private Map<String, String> randomHeaders;

  @Override
  public TestOAuthConnection connect() throws ConnectionException {
    return new TestOAuthConnection(this);
  }

  public void disconnect(TestOAuthConnection connection) {

  }

  @Override
  public OAuthState getState() {
    return state;
  }

  @Override
  public ConnectionValidationResult validate(TestOAuthConnection connection) {
    return success();
  }
}
