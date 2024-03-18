/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth;

import static org.mule.sdk.api.connectivity.ConnectionValidationResult.success;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.AuthorizationCode;
import org.mule.sdk.api.connectivity.ConnectionValidationResult;
import org.mule.sdk.api.connectivity.PoolingConnectionProvider;

import java.util.LinkedList;
import java.util.Queue;

@AuthorizationCode(accessTokenUrl = TestOAuthConnectionProvider.ACCESS_TOKEN_URL,
    authorizationUrl = TestOAuthConnectionProvider.AUTH_URL)
@Alias("pooled")
public class TestOAuthPooledProvider extends LegacyTestOAuthConnectionState
    implements PoolingConnectionProvider<TestOAuthConnection> {

  public static String BORROWED = "Connection borrowed";
  public static String RETURNED = "Connection returned";
  public static Queue<String> CALLBACK_ACTIONS = new LinkedList<>();

  @Override
  public void onBorrow(TestOAuthConnection connection) {
    CALLBACK_ACTIONS.add(BORROWED);
  }

  @Override
  public void onReturn(TestOAuthConnection connection) {
    CALLBACK_ACTIONS.offer(RETURNED);
  }

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
