/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.oauth2.internal;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.Event.Builder;
import org.mule.runtime.core.api.el.ExpressionLanguage;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.module.oauth2.internal.authorizationcode.TokenResponseConfiguration;

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
  private final ExpressionLanguage expressionLanguage;
  private final boolean retrieveRefreshToken;
  private String accessToken;
  private String refreshToken;
  private String expiresIn;
  private Map<String, Object> customResponseParameters;

  public static TokenResponseProcessor createAuthorizationCodeProcessor(final TokenResponseConfiguration tokenResponseConfiguration,
                                                                        final ExpressionLanguage expressionLanguage) {
    return new TokenResponseProcessor(tokenResponseConfiguration, expressionLanguage, true);
  }

  public static TokenResponseProcessor createClientCredentialsProcessor(final TokenResponseConfiguration tokenResponseConfiguration,
                                                                        final ExpressionLanguage expressionLanguage) {
    return new TokenResponseProcessor(tokenResponseConfiguration, expressionLanguage, false);
  }

  private TokenResponseProcessor(final TokenResponseConfiguration tokenResponseConfiguration,
                                 final ExpressionLanguage expressionLanguage, boolean retrieveRefreshToken) {
    this.tokenResponseConfiguration = tokenResponseConfiguration;
    this.expressionLanguage = expressionLanguage;
    this.retrieveRefreshToken = retrieveRefreshToken;
  }

  public void process(Event muleEvent) {
    Builder builder = Event.builder(muleEvent);
    accessToken = expressionLanguage.parse(tokenResponseConfiguration.getAccessToken(), muleEvent, builder, null);
    muleEvent = builder.build();
    accessToken = isEmpty(accessToken) ? null : accessToken;
    if (accessToken == null) {
      logger.error("Could not extract access token from token URL. Expressions used to retrieve access token was "
          + tokenResponseConfiguration.getAccessToken());
    }
    if (retrieveRefreshToken) {
      builder = Event.builder(muleEvent);
      refreshToken = expressionLanguage.parse(tokenResponseConfiguration.getRefreshToken(), muleEvent, builder, null);
      muleEvent = builder.build();
      refreshToken = isEmpty(refreshToken) ? null : refreshToken;
    }
    builder = Event.builder(muleEvent);
    expiresIn = expressionLanguage.parse(tokenResponseConfiguration.getExpiresIn(), muleEvent, builder, null);
    muleEvent = builder.build();
    customResponseParameters = new HashMap<>();
    for (ParameterExtractor parameterExtractor : tokenResponseConfiguration.getParameterExtractors()) {
      builder = Event.builder(muleEvent);
      customResponseParameters.put(parameterExtractor.getParamName(),
                                   expressionLanguage.evaluate(parameterExtractor.getValue(), muleEvent, builder, null));
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
