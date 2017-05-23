/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api.builder;

import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.http.api.server.HttpServer;
import org.mule.runtime.oauth.api.AuthorizationCodeOAuthDancer;
import org.mule.runtime.oauth.api.AuthorizationCodeRequest;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import java.net.URL;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Builder that allows to configure the attributes for the authorization code grant type.
 *
 * @since 4.0
 */
public interface OAuthAuthorizationCodeDancerBuilder extends OAuthDancerBuilder<AuthorizationCodeOAuthDancer> {

  /**
   * The produced {@link AuthorizationCodeOAuthDancer} will create an {@link HttpServer} to listen on the provided
   * {@code localCallbackUrl}.
   *
   * @param localCallbackUrl the url to listen on
   * @return this builder
   */
  OAuthAuthorizationCodeDancerBuilder localCallback(URL localCallbackUrl);

  /**
   * The produced {@link AuthorizationCodeOAuthDancer} will create an {@link HttpServer} with the provided
   * {@code tlsContextFactory} to listen on the provided {@code localCallbackUrl}.
   *
   * @param localCallbackUrl  the url to listen on
   * @param tlsContextFactory the TLS context to use for the listener
   * @return this builder
   */
  OAuthAuthorizationCodeDancerBuilder localCallback(URL localCallbackUrl, TlsContextFactory tlsContextFactory);

  /**
   * The produced {@link AuthorizationCodeOAuthDancer} will use an existing {@link HttpServer} to listen on the provided
   * {@code localCallbackConfigPath}.
   *
   * @param server                  a server listening on a specific host and port
   * @param localCallbackConfigPath the path on the {@code server} to listen on
   * @return this builder
   */
  OAuthAuthorizationCodeDancerBuilder localCallback(HttpServer server, String localCallbackConfigPath);

  /**
   * If this attribute is provided mule will automatically create and endpoint in the host server (the same configured for
   * {@link #localCallback}) that the user can hit to authenticate and grant access to the application for his account.
   *
   * @param path the path to listen for the callback
   * @return this builder
   */
  OAuthAuthorizationCodeDancerBuilder localAuthorizationUrlPath(String path);

  /**
   * This attribute is only required when the applications needs to access resources from more than one user in the OAuth
   * authentication server.
   *
   * @param localAuthorizationUrlResourceOwnerIdExpr expression to get the identifier under which the oauth authentication
   *                                                 attributes are stored (accessToken, refreshToken, etc).
   * @return this builder
   */
  OAuthAuthorizationCodeDancerBuilder localAuthorizationUrlResourceOwnerId(String localAuthorizationUrlResourceOwnerIdExpr);

  /**
   * There are OAuth implementations that require or allow extra query parameters to be sent when calling the Authentication URL
   * of the OAS.
   * <p>
   * Invoking this method overrides any prior invokations to {@link #customParameters(Supplier)}
   *
   * @param customParameters the extra parameters to be sent with the authorization request to {@link #authorizationUrl(String)}.
   * @return this builder
   */
  OAuthAuthorizationCodeDancerBuilder customParameters(Map<String, String> customParameters);

  /**
   * There are OAuth implementations that require or allow extra query parameters to be sent when calling the Authentication URL
   * of the OAS.
   * <p>
   * Invoking this method overrides any prior invokations to {@link #customParameters(Map)}
   *
   * @param customParameters A {@link Supplier} the extra parameters to be sent with the authorization request
   *                         to {@link #authorizationUrl(String)}.
   * @return this builder
   */
  OAuthAuthorizationCodeDancerBuilder customParameters(Supplier<Map<String, String>> customParameters);

  /**
   * Mule will add some internal stuff to the state that is sent to the Authorization server. When the callback is received, those
   * will be removed to be processed, and the {@code state} as specified in this method will be honored.
   *
   * @param stateExpr parameter for holding state between the authentication request and the callback done by the OAuth
   *                  authorization server to the {@link #externalCallbackUrl(String)}.
   * @return this builder
   */
  OAuthAuthorizationCodeDancerBuilder state(String stateExpr);

  /**
   * @param authorizationUrl The OAuth authentication server url to authorize the app for a certain user.
   * @return this builder
   */
  OAuthAuthorizationCodeDancerBuilder authorizationUrl(String authorizationUrl);

  /**
   * The oauth authentication server will use this url to provide the authentication code to the Mule server so the mule server
   * can retrieve the access token.
   * <p>
   * Note: this must be the externally visible address of the {@link #localCallback}.
   * <p>
   * TODO MULE-11861: Allow to infer the localCallback url based on the externalCallbackUrl for Auth-code grant-type
   *
   * @param externalCallbackUrl the callback url where the authorization code will be received.
   * @return this builder
   */
  OAuthAuthorizationCodeDancerBuilder externalCallbackUrl(String externalCallbackUrl);

  /**
   * Allows custom code to be run just before doing the request to the provided {@code tokenUrl}.
   * <p>
   * The map returned by the provided function will be passed to the {@link #afterDanceCallback(BiConsumer)}, if set.
   *
   * @param callback a {@link Function} that receives the parameters that will be used in the executing dance and returns an
   *                 {@link AuthorizationCodeDanceCallbackContext} to be then passed to the {@link #afterDanceCallback(BiConsumer)}. Not
   *                 null.
   * @return this builder
   */
  OAuthAuthorizationCodeDancerBuilder beforeDanceCallback(
                                                          Function<AuthorizationCodeRequest, AuthorizationCodeDanceCallbackContext> callback);

  /**
   * Allows custom code to be run after doing the request to the provided {@code tokenUrl} and processing its results.
   *
   * @param callback a {@link BiConsumer} that receives the {@link AuthorizationCodeDanceCallbackContext} returned by the callback
   *                 passed to {@link #beforeDanceCallback(Function)} and the OAuth context from the response of the call to
   *                 {@code tokenUrl}. Not null.
   * @return this builder
   */
  OAuthAuthorizationCodeDancerBuilder afterDanceCallback(
                                                         BiConsumer<AuthorizationCodeDanceCallbackContext, ResourceOwnerOAuthContext> callback);
}
