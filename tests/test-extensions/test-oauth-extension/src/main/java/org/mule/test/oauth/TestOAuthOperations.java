/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.connectivity.oauth.AccessTokenExpiredException;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthState;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;

import java.util.List;
import java.util.Optional;

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

  public PagingProvider<TestOAuthConnection, String> pagedOperation() {
    return new PagingProvider<TestOAuthConnection, String>() {

      @Override
      public List<String> getPage(TestOAuthConnection connection) {
        final OAuthState state = connection.getState().getState();
        if (state != null && !state.getAccessToken().endsWith("refreshed")) {
          if (state instanceof AuthorizationCodeState) {
            throw new AccessTokenExpiredException(((AuthorizationCodeState) state).getResourceOwnerId());
          } else {
            throw new AccessTokenExpiredException();
          }
        }
        return null;
      }

      @Override
      public Optional<Integer> getTotalResults(TestOAuthConnection connection) {
        return Optional.empty();
      }

      @Override
      public void close(TestOAuthConnection connection) throws MuleException {

      }
    };
  }
}
