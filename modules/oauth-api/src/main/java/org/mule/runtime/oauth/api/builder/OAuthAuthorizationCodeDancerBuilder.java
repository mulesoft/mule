/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api.builder;

import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.oauth.api.AuthorizationCodeOAuthDancer;
import org.mule.runtime.oauth.api.OAuthDancer;
import org.mule.service.http.api.server.HttpServer;

import java.net.URL;
import java.util.Map;

/**
 * Builder that allows to configure the attributes for the authorization code grant type.
 *
 * @since 4.0
 */
public interface OAuthAuthorizationCodeDancerBuilder extends OAuthDancerBuilder<AuthorizationCodeOAuthDancer> {

  /**
   * The produced {@link OAuthDancer} will create an {@link HttpServer} to listen on the provided {@code localCallbackUrl}.
   * 
   * @param localCallbackUrl the url to listen on
   * 
   * @return this builder
   */
  OAuthAuthorizationCodeDancerBuilder localCallback(URL localCallbackUrl);

  /**
   * The produced {@link OAuthDancer} will create an {@link HttpServer} with the provided {@code tlsContextFactory} to listen on
   * the provided {@code localCallbackUrl}.
   * 
   * @param localCallbackUrl the url to listen on
   * @param tlsContextFactory the TLS context to use for the listener
   * 
   * @return this builder
   */
  OAuthAuthorizationCodeDancerBuilder localCallback(URL localCallbackUrl, TlsContextFactory tlsContextFactory);

  /**
   * The produced {@link OAuthDancer} will use an existing {@link HttpServer} to listen on the provided
   * {@code localCallbackConfigPath}.
   * 
   * @param server a server listening on a specifi host and port
   * @param localCallbackConfigPath the path on the {@code server} to listen on
   * 
   * @return this builder
   */
  OAuthAuthorizationCodeDancerBuilder localCallback(HttpServer server, String localCallbackConfigPath);

  /**
   * If this attribute is provided mule will automatically create and endpoint in the host server that the user can hit to
   * authenticate and grant access to the application for his account.
   * 
   * @param path the path to listen for the callback
   * 
   * @return this builder
   */
  OAuthAuthorizationCodeDancerBuilder localAuthorizationUrlPath(String path);

  /**
   * This attribute is only required when the applications needs to access resources from more than one user in the OAuth
   * authentication server.
   * 
   * @param localAuthorizationUrlResourceOwnerIdExpr expression to get the identifier under which the oauth authentication
   *        attributes are stored (accessToken, refreshToken, etc).
   * 
   * @return this builder
   */
  OAuthAuthorizationCodeDancerBuilder localAuthorizationUrlResourceOwnerId(String localAuthorizationUrlResourceOwnerIdExpr);

  /**
   * There are OAuth implementations that require or allow extra query parameters to be sent when calling the Authentication URL
   * of the OAS.
   * 
   * @param customParameters the extra parameters to be sent with the authorization request to {@link #authorizationUrl(String)}.
   * 
   * @return this builder
   */
  OAuthAuthorizationCodeDancerBuilder customParameters(Map<String, String> customParameters);

  /**
   * @param stateExpr parameter for holding state between the authentication request and the callback done by the OAuth
   *        authorization server to the {@code redirectUrl}.
   * 
   * @return this builder
   */
  OAuthAuthorizationCodeDancerBuilder state(String stateExpr);

  /**
   * @param authorizationUrl The OAuth authentication server url to authorize the app for a certain user.
   * 
   * @return this builder
   */
  OAuthAuthorizationCodeDancerBuilder authorizationUrl(String authorizationUrl);

  /**
   * The oauth authentication server will use this url to provide the authentication code to the Mule server so the mule server
   * can retrieve the access token.
   * <p>
   * Note that this must be the externally visible address of the callback, not the local one.
   * 
   * @param externalCallbackUrl the callback url where the authorization code will be received.
   * 
   * @return this builder
   */
  OAuthAuthorizationCodeDancerBuilder externalCallbackUrl(String externalCallbackUrl);

}
