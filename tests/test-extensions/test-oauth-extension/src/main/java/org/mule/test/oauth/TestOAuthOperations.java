/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth;

import static java.util.Arrays.asList;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.connectivity.oauth.AccessTokenExpiredException;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthState;
import org.mule.runtime.extension.api.error.MuleErrors;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;

import java.util.List;

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

  public PagingProvider<TestOAuthConnection, String> pagedOperation(@Optional(defaultValue = "0") Integer failAt) {
    return new PagingProvider<TestOAuthConnection, String>() {

      private int count = 1;
      private boolean done = false;

      @Override
      public List<String> getPage(TestOAuthConnection connection) {
        if (done) {
          return null;
        }

        final OAuthState state = connection.getState().getState();

        if (count >= failAt) {
          if (!state.getAccessToken().endsWith("refreshed")) {
            if (state instanceof AuthorizationCodeState) {
              throw new ModuleException(MuleErrors.CONNECTIVITY,
                                        new AccessTokenExpiredException(((AuthorizationCodeState) state).getResourceOwnerId()));
            } else {
              throw new ModuleException(MuleErrors.CONNECTIVITY, new AccessTokenExpiredException());
            }
          } else {
            done = true;
          }
        }

        return asList("item " + count++);
      }

      @Override
      public java.util.Optional<Integer> getTotalResults(TestOAuthConnection connection) {
        return java.util.Optional.empty();
      }

      @Override
      public void close(TestOAuthConnection connection) throws MuleException {

      }
    };
  }
}
