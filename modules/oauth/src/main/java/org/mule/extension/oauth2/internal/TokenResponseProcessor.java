/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal;

import org.mule.extension.oauth2.internal.authorizationcode.TokenResponseConfiguration;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.Event.Builder;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process a token url response and extracts all the oauth context variables based on the user configuration.
 */
public class TokenResponseProcessor {

  protected Logger logger = LoggerFactory.getLogger(getClass());
  private final TokenResponseConfiguration tokenResponseConfiguration;
  private final ExtendedExpressionManager expressionManager;
  private final boolean retrieveRefreshToken;
  private String accessToken;
  private String refreshToken;
  private String expiresIn;
  private Map<String, Object> customResponseParameters;

  public static TokenResponseProcessor createAuthorizationCodeProcessor(final TokenResponseConfiguration tokenResponseConfiguration,
                                                                        final ExtendedExpressionManager expressionManager) {
    return new TokenResponseProcessor(tokenResponseConfiguration, expressionManager, true);
  }

  public static TokenResponseProcessor createClientCredentialsProcessor(final TokenResponseConfiguration tokenResponseConfiguration,
                                                                        final ExtendedExpressionManager expressionManager) {
    return new TokenResponseProcessor(tokenResponseConfiguration, expressionManager, false);
  }

  private TokenResponseProcessor(final TokenResponseConfiguration tokenResponseConfiguration,
                                 final ExtendedExpressionManager expressionManager, boolean retrieveRefreshToken) {
    this.tokenResponseConfiguration = tokenResponseConfiguration;
    this.expressionManager = expressionManager;
    this.retrieveRefreshToken = retrieveRefreshToken;
  }

  public void process(Event muleEvent) {
    Builder builder = Event.builder(muleEvent);
    accessToken = expressionManager.parse(tokenResponseConfiguration.getAccessToken(), muleEvent, builder, null);
    muleEvent = builder.build();
    accessToken = isEmpty(accessToken) ? null : accessToken;
    if (accessToken == null) {
      logger.error("Could not extract access token from token URL. Expressions used to retrieve access token was "
          + tokenResponseConfiguration.getAccessToken());
    }
    if (retrieveRefreshToken) {
      builder = Event.builder(muleEvent);
      refreshToken = expressionManager.parse(tokenResponseConfiguration.getRefreshToken(), muleEvent, builder, null);
      muleEvent = builder.build();
      refreshToken = isEmpty(refreshToken) ? null : refreshToken;
    }
    builder = Event.builder(muleEvent);
    expiresIn = expressionManager.parse(tokenResponseConfiguration.getExpiresIn(), muleEvent, builder, null);
    muleEvent = builder.build();
    customResponseParameters = new HashMap<>();
    for (ParameterExtractor parameterExtractor : tokenResponseConfiguration.getParameterExtractors()) {
      builder = Event.builder(muleEvent);
      customResponseParameters.put(parameterExtractor.getParamName(),
                                   expressionManager.evaluate(parameterExtractor.getValue(), muleEvent, builder, null));
      muleEvent = builder.build();
    }
  }

  public String getAccessToken() {
    return accessToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public String getExpiresIn() {
    return expiresIn;
  }

  public Map<String, Object> getCustomResponseParameters() {
    return customResponseParameters;
  }

  private boolean isEmpty(String value) {
    // TODO remove "null" check when MULE-8281 gets fixed.
    return value == null || StringUtils.isEmpty(value) || "null".equals(value);
  }
}
