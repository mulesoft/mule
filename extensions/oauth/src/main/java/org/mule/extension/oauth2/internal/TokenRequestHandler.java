/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal;

import static java.util.Collections.emptyList;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.operation.ParameterResolver;

import java.util.List;

/**
 * Represents the Token request and response handling behaviour of the OAuth 2.0 dance. It provides support for standard OAuth
 * server implementations of the token acquisition part plus a couple of configuration attributes to customize behaviour.
 */
public class TokenRequestHandler {

  // Expressions to extract parameters from standard token url response.
  private static final String ACCESS_TOKEN_EXPRESSION = "#[(payload match /.*\"access_token\"[ ]*:[ ]*\"([^\\\"]*)\".*/)[1]]";
  private static final String REFRESH_TOKEN_EXPRESSION = "#[(payload match /.*\"refresh_token\"[ ]*:[ ]*\"([^\\\"]*)\".*/)[1]]";
  private static final String EXPIRATION_TIME_EXPRESSION = "#[(payload match /.*\"expires_in\"[ ]*:[ ]*\"([^\\\"]*)\".*/)[1]]";

  /**
   * MEL expression to extract the access token parameter from the response of the call to tokenUrl.
   */
  @Parameter
  @Optional(defaultValue = ACCESS_TOKEN_EXPRESSION)
  protected ParameterResolver<String> responseAccessToken;

  @Parameter
  @Optional(defaultValue = REFRESH_TOKEN_EXPRESSION)
  protected ParameterResolver<String> responseRefreshToken;

  /**
   * MEL expression to extract the expiresIn parameter from the response of the call to tokenUrl.
   */
  @Parameter
  @Optional(defaultValue = EXPIRATION_TIME_EXPRESSION)
  protected ParameterResolver<String> responseExpiresIn;

  @Parameter
  @Alias("custom-parameter-extractors")
  @Optional
  protected List<ParameterExtractor> parameterExtractors;

  /**
   * After executing an API call authenticated with OAuth it may be that the access token used was expired, so this attribute
   * allows for an expressions that will be evaluated against the http response of the API callback to determine if the request
   * failed because it was done using an expired token. In case the evaluation returns true (access token expired) then mule will
   * automatically trigger a refresh token flow and retry the API callback using a new access token. Default value evaluates if
   * the response status code was 401 or 403.
   */
  @Parameter
  @Optional(defaultValue = "#[attributes.statusCode == 401 or attributes.statusCode == 403]")
  private ParameterResolver<Boolean> refreshTokenWhen;

  /**
   * The oauth authentication server url to get access to the token. Mule, after receiving the authentication code from the oauth
   * server (through the redirectUrl) will call this url to get the access token.
   */
  @Parameter
  private String tokenUrl;

  public ParameterResolver<Boolean> getRefreshTokenWhen() {
    return refreshTokenWhen;
  }

  public void setTokenUrl(String tokenUrl) {
    this.tokenUrl = tokenUrl;
  }

  public String getTokenUrl() {
    return tokenUrl;
  }

  public ParameterResolver<String> getResponseAccessToken() {
    return responseAccessToken;
  }


  public ParameterResolver<String> getResponseRefreshToken() {
    return responseRefreshToken;
  }


  public ParameterResolver<String> getResponseExpiresIn() {
    return responseExpiresIn;
  }

  public List<ParameterExtractor> getCustomParameterExtractors() {
    return parameterExtractors != null ? parameterExtractors : emptyList();
  }
}
