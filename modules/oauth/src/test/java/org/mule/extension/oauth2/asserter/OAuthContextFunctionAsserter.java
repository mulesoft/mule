/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.asserter;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.extension.oauth2.internal.tokenmanager.TokenManagerConfig;

public class OAuthContextFunctionAsserter {

  private final TokenManagerConfig tokenManagerConfig;
  private final String userId;

  private OAuthContextFunctionAsserter(TokenManagerConfig tokenManagerConfig, String userId) {
    this.tokenManagerConfig = tokenManagerConfig;
    this.userId = userId;
  }

  public static OAuthContextFunctionAsserter createFrom(TokenManagerConfig tokenManagerConfig) {
    return new OAuthContextFunctionAsserter(tokenManagerConfig, null);
  }

  public static OAuthContextFunctionAsserter createFrom(TokenManagerConfig tokenManagerConfig, String userId) {
    return new OAuthContextFunctionAsserter(tokenManagerConfig, userId);
  }

  public OAuthContextFunctionAsserter assertAccessTokenIs(String expectedAccessToken) {
    if (userId != null) {
      assertThat(tokenManagerConfig.processOauthContextFunctionACall(userId).getAccessToken(), is(expectedAccessToken));
    } else {
      assertThat(tokenManagerConfig.processOauthContextFunctionACall().getAccessToken(), is(expectedAccessToken));
    }
    return this;
  }

  public OAuthContextFunctionAsserter assertRefreshTokenIs(String expectedRefreshToken) {
    if (userId != null) {
      assertThat(tokenManagerConfig.processOauthContextFunctionACall(userId).getRefreshToken(), is(expectedRefreshToken));
    } else {
      assertThat(tokenManagerConfig.processOauthContextFunctionACall().getRefreshToken(), is(expectedRefreshToken));
    }
    return this;
  }

  public OAuthContextFunctionAsserter assertState(String expectedState) {
    if (userId != null) {
      assertThat(tokenManagerConfig.processOauthContextFunctionACall(userId).getState(), is(expectedState));
    } else {
      assertThat(tokenManagerConfig.processOauthContextFunctionACall().getState(), is(expectedState));
    }
    return this;
  }

  public OAuthContextFunctionAsserter assertExpiresInIs(String expectedExpiresIs) {
    if (userId != null) {
      assertThat(tokenManagerConfig.processOauthContextFunctionACall(userId).getExpiresIn(), is(expectedExpiresIs));
    } else {
      assertThat(tokenManagerConfig.processOauthContextFunctionACall().getExpiresIn(), is(expectedExpiresIs));
    }
    return this;
  }

  public OAuthContextFunctionAsserter assertContainsCustomTokenResponseParam(String paramName, String paramValue) {
    if (userId != null) {
      assertThat(tokenManagerConfig.processOauthContextFunctionACall(userId).getTokenResponseParameters().get(paramName),
                 is(paramValue));
    } else {
      assertThat(tokenManagerConfig.processOauthContextFunctionACall().getTokenResponseParameters().get(paramName),
                 is(paramValue));
    }
    return this;
  }
}
