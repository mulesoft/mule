/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api.builder;

import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.http.api.client.HttpClient;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.function.Function;

/**
 * Builder that allows to configure the common attributes for any grant type.
 *
 * @since 4.0
 */
public interface OAuthDancerBuilder<D> {

  /**
   * @param clientId the application identifier as defined in the OAuth authentication server.
   * @param clientSecret the application secret as defined in the OAuth authentication server.
   * 
   * @return this builder
   */
  OAuthDancerBuilder<D> clientCredentials(String clientId, String clientSecret);

  /**
   * Mule, after receiving the authentication code from the OAuth server (through the redirectUrl) will call this url to get the
   * access token.
   * 
   * @param tokenUrl The OAuth authentication server url to get access to the token.
   * 
   * @return this builder
   */
  OAuthDancerBuilder<D> tokenUrl(String tokenUrl);

  /**
   * Mule, after receiving the authentication code from the OAuth server (through the redirectUrl) will call this url to get the
   * access token.
   * 
   * @param httpClient the {@link HttpClient} that will be used to do the HTTP request for the token during the OAuth dance.
   * @param tokenUrl The OAuth authentication server url to get access to the token.
   * 
   * @return this builder
   */
  OAuthDancerBuilder<D> tokenUrl(HttpClient httpClient, String tokenUrl);

  /**
   * Mule, after receiving the authentication code from the OAuth server (through the redirectUrl) will call this url to get the
   * access token.
   * 
   * @param tokenUrl The OAuth authentication server url to get access to the token.
   * @param tlsContextFactory References a TLS config that will be used to do HTTP request during the OAuth dance.
   * 
   * @return this builder
   */
  OAuthDancerBuilder<D> tokenUrl(String tokenUrl, TlsContextFactory tlsContextFactory);

  /**
   * Scopes define permissions over resources.
   * 
   * @param scopes required by this application to execute.
   * 
   * @return this builder
   */
  OAuthDancerBuilder<D> scopes(String scopes);

  /**
   * @param encoding the encoding to use when processing the incoming requests and responses of the OAuth dance.
   * 
   * @return this builder
   */
  OAuthDancerBuilder<D> encoding(Charset encoding);

  /**
   * @param responseAccessTokenExpr an expression to extract the {@code access token} parameter from the response of the call to
   *        {@link #tokenUrl(String) token-url}.
   * 
   * @return this builder
   */
  OAuthDancerBuilder<D> responseAccessTokenExpr(String responseAccessTokenExpr);

  /**
   * @param responseRefreshTokenExpr an expression to extract the {@code refresh token} parameter from the response of the call to
   *        {@link #tokenUrl(String) token-url}.
   * 
   * @return this builder
   */
  OAuthDancerBuilder<D> responseRefreshTokenExpr(String responseRefreshTokenExpr);

  /**
   * @param responseExpiresInExpr an expression to extract the {@code expiresIn} parameter from the response of the call to
   *        {@link #tokenUrl(String) token-url}.
   * 
   * @return this builder
   */
  OAuthDancerBuilder<D> responseExpiresInExpr(String responseExpiresInExpr);


  /**
   * @param customParamsExtractorsExprs a map of {@code paramName} to an expression to extract the custom parameters from the
   *        response of the call to {@link #tokenUrl(String) token-url}.
   * 
   * @return this builder
   */
  OAuthDancerBuilder<D> customParametersExtractorsExprs(Map<String, String> customParamsExtractorsExprs);

  /**
   * Allows to partition a {@code tokensStore} and reuse it among different dancers, as long as each dancer has its own proper
   * {@code resourceOwnerIdStoreTransformer} and ensures there can be no collissions between the transformed
   * {@code respurceOwnerIds} for different dancers.
   * <p>
   * Providing this transformer only affects how the dancer puts the contexts associated to a {@code reosurceOwner} in the
   * {@code tokensStore} and the name of the locks generated from the {@code lockProvider} The un-transformed value still has to
   * be used when calling dancer methods that receive the {@code resourceOwnerId} as a parameter, and will be used when sending
   * data out as part of the OAuth dance or the token refresh.
   * 
   * @param resourceOwnerIdTransformer a transformer to apply to the {@code resourceOwnerId} before using it to access the
   *        provided {@code tokensStore}.
   * @return this builder
   */
  OAuthDancerBuilder<D> resourceOwnerIdTransformer(Function<String, String> resourceOwnerIdTransformer);

  /**
   * Uses the configuration provided to this builder to create a new dancer.
   * 
   * @return a fresh instance of a dancer.
   */
  D build();

}
