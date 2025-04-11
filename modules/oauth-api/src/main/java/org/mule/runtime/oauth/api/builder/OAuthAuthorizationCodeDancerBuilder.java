/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api.builder;

import org.mule.api.annotation.NoImplement;
import org.mule.oauth.client.api.AuthorizationCodeRequest;
import org.mule.oauth.client.api.builder.AuthorizationCodeDanceCallbackContext;
import org.mule.oauth.client.api.builder.ClientCredentialsLocation;
import org.mule.oauth.client.api.listener.AuthorizationCodeListener;
import org.mule.oauth.client.api.state.ResourceOwnerOAuthContext;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.http.api.server.HttpServer;

import java.net.URL;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Provides compatibility with version 1.x of the mule-oauth-client, which is a transitive api of the service api.
 *
 * @deprecated since 1.5, use {@link org.mule.oauth.client.api.builder.OAuthAuthorizationCodeDancerBuilder} from
 *             {@code mule-oauth-client 2.x}.
 */
@Deprecated
@NoImplement
public interface OAuthAuthorizationCodeDancerBuilder
    extends org.mule.oauth.client.api.builder.OAuthAuthorizationCodeDancerBuilder,
    org.mule.runtime.oauth.api.builder.OAuthDancerBuilder<org.mule.oauth.client.api.AuthorizationCodeOAuthDancer> {

  /**
   * @deprecated since 4.2.0. Use {@link OAuthAuthorizationCodeDancerBuilder#withClientCredentialsIn(ClientCredentialsLocation)}
   *             instead.
   */
  @Override
  @Deprecated
  OAuthAuthorizationCodeDancerBuilder encodeClientCredentialsInBody(boolean encodeClientCredentialsInBody);

  @Override
  OAuthAuthorizationCodeDancerBuilder localCallback(URL localCallbackUrl);

  OAuthAuthorizationCodeDancerBuilder localCallback(URL localCallbackUrl, TlsContextFactory tlsContextFactory);

  @Override
  OAuthAuthorizationCodeDancerBuilder localCallback(HttpServer server, String localCallbackConfigPath);

  @Override
  OAuthAuthorizationCodeDancerBuilder localAuthorizationUrlPath(String path);

  @Override
  OAuthAuthorizationCodeDancerBuilder localAuthorizationUrlResourceOwnerId(String localAuthorizationUrlResourceOwnerIdExpr);

  @Override
  OAuthAuthorizationCodeDancerBuilder customParameters(Map<String, String> customParameters);

  @Override
  OAuthAuthorizationCodeDancerBuilder customParameters(Supplier<Map<String, String>> customParameters);

  @Override
  OAuthAuthorizationCodeDancerBuilder customHeaders(Map<String, String> customHeaders);

  @Override
  OAuthAuthorizationCodeDancerBuilder customHeaders(Supplier<Map<String, String>> customHeaders);

  @Override
  OAuthAuthorizationCodeDancerBuilder state(String stateExpr);

  @Override
  OAuthAuthorizationCodeDancerBuilder authorizationUrl(String authorizationUrl);

  @Override
  OAuthAuthorizationCodeDancerBuilder externalCallbackUrl(String externalCallbackUrl);

  @Override
  OAuthAuthorizationCodeDancerBuilder beforeDanceCallback(Function<AuthorizationCodeRequest, AuthorizationCodeDanceCallbackContext> callback);

  @Override
  OAuthAuthorizationCodeDancerBuilder afterDanceCallback(BiConsumer<AuthorizationCodeDanceCallbackContext, ResourceOwnerOAuthContext> callback);

  @Override
  OAuthAuthorizationCodeDancerBuilder addListener(AuthorizationCodeListener listener);

  OAuthAuthorizationCodeDancerBuilder addListener(org.mule.runtime.oauth.api.listener.AuthorizationCodeListener listener);

  @Override
  OAuthAuthorizationCodeDancerBuilder addAdditionalRefreshTokenRequestParameters(MultiMap<String, String> additionalParameters);

  @Override
  OAuthAuthorizationCodeDancerBuilder addAdditionalRefreshTokenRequestHeaders(MultiMap<String, String> additionalHeaders);

  @Override
  OAuthAuthorizationCodeDancerBuilder includeRedirectUriInRefreshTokenRequest(boolean includeRedirectUriInRefreshTokenRequest);
}
