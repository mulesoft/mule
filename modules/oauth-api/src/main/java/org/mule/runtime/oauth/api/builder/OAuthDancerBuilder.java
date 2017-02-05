/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api.builder;

import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.oauth.api.OAuthDancer;

import java.nio.charset.Charset;
import java.util.Map;


public interface OAuthDancerBuilder {

  /**
   * @param clientId the application identifier as defined in the OAuth authentication server.
   * @param clientSecret the application secret as defined in the OAuth authentication server.
   */
  OAuthDancerBuilder clientCredentials(String clientId, String clientSecret);

  /**
   * Mule, after receiving the authentication code from the OAuth server (through the redirectUrl) will call this url to get the
   * access token.
   * 
   * @param tokenUrl The OAuth authentication server url to get access to the token.
   */
  OAuthDancerBuilder tokenUrl(String tokenUrl);

  /**
   * Scopes define permissions over resources.
   * 
   * @param scopes required by this application to execute.
   */
  OAuthDancerBuilder scopes(String scopes);

  OAuthDancerBuilder encoding(Charset encoding);

  /**
   * @param tlsContextFactory References a TLS config that will be used to receive incoming HTTP request and do HTTP request
   *        during the OAuth dance.
   */
  OAuthDancerBuilder tlsContextFactory(TlsContextFactory tlsContextFactory);

  /**
   * @param responseAccessTokenExpr an expression to extract the {@code access token} parameter from the response of the call to
   *        {@link #getTokenUrl() token-url}.
   */
  OAuthDancerBuilder responseAccessTokenExpr(String responseAccessTokenExpr);

  OAuthDancerBuilder responseRefreshTokenExpr(String responseRefreshTokenExpr);

  /**
   * @param responseExpiresInExpr an expression to extract the {@code expiresIn} parameter from the response of the call to
   *        {@link #getTokenUrl() token-url}.
   */
  OAuthDancerBuilder responseExpiresInExpr(String responseExpiresInExpr);

  OAuthDancerBuilder customParametersExtractorsExprs(Map<String, String> customParamsExtractorsExprs);

  OAuthDancer build();

}
