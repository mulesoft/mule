/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.asserter;

import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.el.ExpressionManager;

import org.hamcrest.core.Is;

public class OAuthContextFunctionAsserter {

  private final ExpressionManager expressionManager;
  private final String configName;
  private final String userId;

  private OAuthContextFunctionAsserter(ExpressionManager expressionManager, String configName, String userId) {
    this.expressionManager = expressionManager;
    this.configName = configName;
    this.userId = userId;
  }

  public static OAuthContextFunctionAsserter createFrom(ExpressionManager expressionManager, String configName) {
    return new OAuthContextFunctionAsserter(expressionManager, configName, null);
  }

  public static OAuthContextFunctionAsserter createFrom(ExpressionManager expressionManager, String configName, String userId) {
    return new OAuthContextFunctionAsserter(expressionManager, configName, userId);
  }

  public OAuthContextFunctionAsserter assertAccessTokenIs(String expectedAccessToken) {
    if (userId != null) {
      assertThat(expressionManager.evaluate(String.format("oauthContext('%s','%s').accessToken", configName, userId)).getValue(),
                 Is.<Object>is(expectedAccessToken));
    } else {
      assertThat(expressionManager.evaluate(String.format("oauthContext('%s').accessToken", configName)).getValue(),
                 Is.<Object>is(expectedAccessToken));
    }
    return this;
  }

  public OAuthContextFunctionAsserter assertRefreshTokenIs(String expectedRefreshToken) {
    if (userId != null) {
      assertThat(expressionManager.evaluate(String.format("oauthContext('%s','%s').refreshToken", configName, userId))
          .getValue(),
                 Is.<Object>is(expectedRefreshToken));
    } else {
      assertThat(expressionManager.evaluate(String.format("oauthContext('%s').refreshToken", configName)).getValue(),
                 Is.<Object>is(expectedRefreshToken));
    }
    return this;
  }

  public OAuthContextFunctionAsserter assertState(String expectedState) {
    if (userId != null) {
      assertThat(expressionManager.evaluate(String.format("oauthContext('%s','%s').state", configName, userId)).getValue(),
                 Is.<Object>is(expectedState));
    } else {
      assertThat(expressionManager.evaluate(String.format("oauthContext('%s').state", configName)).getValue(),
                 Is.<Object>is(expectedState));
    }
    return this;
  }

  public OAuthContextFunctionAsserter assertExpiresInIs(String expectedExpiresIs) {
    if (userId != null) {
      assertThat(expressionManager.evaluate(String.format("oauthContext('%s','%s').expiresIn", configName, userId)).getValue(),
                 Is.<Object>is(expectedExpiresIs));
    } else {
      assertThat(expressionManager.evaluate(String.format("oauthContext('%s').expiresIn", configName)).getValue(),
                 Is.<Object>is(expectedExpiresIs));
    }
    return this;
  }

  public OAuthContextFunctionAsserter assertContainsCustomTokenResponseParam(String paramName, String paramValue) {
    if (userId != null) {
      assertThat(expressionManager
          .evaluate(String.format("oauthContext('%s','%s').tokenResponseParameters['%s']", configName, userId, paramName))
          .getValue(),
                 Is.<Object>is(paramValue));
    } else {
      assertThat(expressionManager
          .evaluate(String.format("oauthContext('%s').tokenResponseParameters['%s']", configName, paramName)).getValue(),
                 Is.<Object>is(paramValue));
    }
    return this;
  }
}
