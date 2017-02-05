/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api.builder;

import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.oauth.api.OAuthDancer;
import org.mule.service.http.api.server.HttpServer;

import java.net.URL;
import java.util.Map;

public interface OAuthAuthorizationCodeDancerBuilder extends OAuthDancerBuilder {

  /**
   * The produced {@link OAuthDancer} will create an {@link HttpServer} to listen on the provided {@code localCallbackUrl}.
   * 
   * @param localCallbackUrl
   */
  OAuthAuthorizationCodeDancerBuilder localCallback(URL localCallbackUrl);

  /**
   * The produced {@link OAuthDancer} will create an {@link HttpServer} with the provided {@code tlsContextFactory} to listen on
   * the provided {@code localCallbackUrl}.
   * 
   * @param localCallbackUrl
   * @param tlsContextFactory
   */
  OAuthAuthorizationCodeDancerBuilder localCallback(URL localCallbackUrl, TlsContextFactory tlsContextFactory);

  /**
   * The produced {@link OAuthDancer} will use an existing {@link HttpServer} to listen on the provided
   * {@code localCallbackConfigPath}.
   * 
   * @param localCallbackConfigPath
   */
  OAuthAuthorizationCodeDancerBuilder localCallback(HttpServer server, String localCallbackConfigPath);

  /**
   * If this attribute is provided mule will automatically create and endpoint in the host server that the user can hit to
   * authenticate and grant access to the application for his account.
   * 
   * @param path the path to listen for the callback
   */
  OAuthAuthorizationCodeDancerBuilder localAuthorizationUrlPath(String path);

  /**
   * This attribute is only required when the applications needs to access resources from more than one user in the OAuth
   * authentication server.
   * 
   * @param localAuthorizationUrlResourceOwnerIdExpr expression to get the identifier under which the oauth authentication
   *        attributes are stored (accessToken, refreshToken, etc).
   */
  OAuthAuthorizationCodeDancerBuilder localAuthorizationUrlResourceOwnerId(String localAuthorizationUrlResourceOwnerIdExpr);

  /**
   * @param customParameters
   */
  OAuthAuthorizationCodeDancerBuilder customParameters(Map<String, String> customParameters);

  /**
   * @param stateExpr parameter for holding state between the authentication request and the callback done by the OAuth
   *        authorization server to the {@code redirectUrl}.
   */
  OAuthAuthorizationCodeDancerBuilder state(String stateExpr);

  /**
   * @param authorizationUrl The OAuth authentication server url to authorize the app for a certain user.
   */
  OAuthAuthorizationCodeDancerBuilder authorizationUrl(String authorizationUrl);

  /**
   * The oauth authentication server will use this url to provide the authentication code to the Mule server so the mule server
   * can retrieve the access token.
   * <p>
   * Note that this must be the externally visible address of the callback, not the local one.
   * 
   * @param externalCallbackUrl the callback url where the authorization code will be received.
   */
  OAuthAuthorizationCodeDancerBuilder externalCallbackUrl(String externalCallbackUrl);



}
