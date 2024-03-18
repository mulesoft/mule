/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.sdk.api.runtime.parameter.HttpParameterPlacement.BODY;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.AuthorizationCode;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.OAuthParameter;
import org.mule.runtime.extension.api.runtime.parameter.HttpParameterPlacement;
import org.mule.sdk.api.annotation.param.Optional;

@AuthorizationCode(accessTokenUrl = TestOAuthConnectionProvider.ACCESS_TOKEN_URL,
    authorizationUrl = TestOAuthConnectionProvider.AUTH_URL,
    defaultScopes = TestOAuthConnectionProvider.DEFAULT_SCOPE)
@Alias("with-params-connection")
public class TestWithOAuthParamsConnectionProvider extends LegacyTestOAuthConnectionState
    implements ConnectionProvider<TestOAuthConnection> {

  public static final String ACCESS_TOKEN_URL = "accessTokenUrl";
  public static final String AUTH_URL = "authUrl";
  public static final String DEFAULT_SCOPE = "defaultScope";

  @OAuthParameter(requestAlias = "with_alias")
  String withAlias;

  @OAuthParameter()
  String defaultPlacement;

  @OAuthParameter(placement = HttpParameterPlacement.QUERY_PARAMS)
  String queryParam;

  @OAuthParameter(placement = HttpParameterPlacement.HEADERS)
  String header;

  @org.mule.sdk.api.annotation.connectivity.oauth.OAuthParameter(placement = BODY)
  @Optional
  String bodyParameter;

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
