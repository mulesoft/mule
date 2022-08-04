/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth;

import org.mule.runtime.extension.api.connectivity.oauth.OAuthState;
import org.mule.sdk.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.sdk.api.connectivity.oauth.ClientCredentialsState;

import java.util.Optional;

public abstract class TestSdkOAuthConnectionState extends TestOAuthConnectionState {

  @Override
  public OAuthState getState() {
    org.mule.sdk.api.connectivity.oauth.OAuthState sdkOAuthState = getSdkState();

    if (sdkOAuthState instanceof AuthorizationCodeState) {
      AuthorizationCodeState sdkAuthohrizationCodeState = (AuthorizationCodeState) sdkOAuthState;
      return new org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState() {

        @Override
        public Optional<String> getRefreshToken() {
          return sdkAuthohrizationCodeState.getRefreshToken();
        }

        @Override
        public String getResourceOwnerId() {
          return sdkAuthohrizationCodeState.getResourceOwnerId();
        }

        @Override
        public Optional<String> getState() {
          return sdkAuthohrizationCodeState.getState();
        }

        @Override
        public String getAuthorizationUrl() {
          return sdkAuthohrizationCodeState.getAuthorizationUrl();
        }

        @Override
        public String getAccessTokenUrl() {
          return sdkAuthohrizationCodeState.getAccessTokenUrl();
        }

        @Override
        public String getConsumerKey() {
          return sdkAuthohrizationCodeState.getConsumerKey();
        }

        @Override
        public String getConsumerSecret() {
          return sdkAuthohrizationCodeState.getConsumerSecret();
        }

        @Override
        public Optional<String> getExternalCallbackUrl() {
          return sdkAuthohrizationCodeState.getExternalCallbackUrl();
        }

        @Override
        public String getAccessToken() {
          return sdkAuthohrizationCodeState.getAccessToken();
        }

        @Override
        public Optional<String> getExpiresIn() {
          return sdkAuthohrizationCodeState.getExpiresIn();
        }
      };
    } else if (sdkOAuthState instanceof ClientCredentialsState) {
      ClientCredentialsState sdkClientCredentialsState = (ClientCredentialsState) sdkOAuthState;
      return new org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsState() {

        @Override
        public String getAccessToken() {
          return sdkClientCredentialsState.getAccessToken();
        }

        @Override
        public Optional<String> getExpiresIn() {
          return sdkClientCredentialsState.getExpiresIn();
        }
      };
    }

    return null;
  }

  protected abstract org.mule.sdk.api.connectivity.oauth.OAuthState getSdkState();
}
