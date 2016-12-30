/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.authorizationcode;

import static java.util.Collections.emptyList;
import static org.mule.extension.oauth2.internal.OAuthConstants.ACCESS_TOKEN_EXPRESSION;
import static org.mule.extension.oauth2.internal.OAuthConstants.EXPIRATION_TIME_EXPRESSION;
import static org.mule.extension.oauth2.internal.OAuthConstants.REFRESH_TOKEN_EXPRESSION;

import org.mule.extension.oauth2.internal.ParameterExtractor;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.List;
import java.util.function.Function;

/**
 * Provides configuration to handle a token url call response.
 */
public class TokenResponseConfiguration {

  /**
   * MEL expression to extract the access token parameter.
   */
  @Parameter
  @Optional(defaultValue = ACCESS_TOKEN_EXPRESSION)
  private Function<Event, String> accessToken;

  @Parameter
  @Optional(defaultValue = REFRESH_TOKEN_EXPRESSION)
  private Function<Event, String> refreshToken;

  /**
   * MEL expression to extract the expired in parameter.
   */
  @Parameter
  @Optional(defaultValue = EXPIRATION_TIME_EXPRESSION)
  private Function<Event, String> expiresIn;

  private List<ParameterExtractor> parameterExtractors = emptyList();

  public void setParameterExtractors(final List<ParameterExtractor> parameterExtractors) {
    this.parameterExtractors = parameterExtractors;
  }

  public Function<Event, String> getAccessToken() {
    return accessToken;
  }

  public Function<Event, String> getRefreshToken() {
    return refreshToken;
  }

  public Function<Event, String> getExpiresIn() {
    return expiresIn;
  }

  public List<ParameterExtractor> getParameterExtractors() {
    return parameterExtractors;
  }
}
