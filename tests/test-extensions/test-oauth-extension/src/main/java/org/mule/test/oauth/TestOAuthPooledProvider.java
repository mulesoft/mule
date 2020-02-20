/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.AuthorizationCode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@AuthorizationCode(accessTokenUrl = TestOAuthConnectionProvider.ACCESS_TOKEN_URL,
    authorizationUrl = TestOAuthConnectionProvider.AUTH_URL,
    defaultScopes = TestOAuthConnectionProvider.DEFAULT_SCOPE)
@Alias("pooled")
public class TestOAuthPooledProvider extends TestOAuthConnectionState implements PoolingConnectionProvider<TestOAuthConnection> {

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
