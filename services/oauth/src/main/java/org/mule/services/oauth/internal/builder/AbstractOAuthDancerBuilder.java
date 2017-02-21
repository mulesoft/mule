/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.oauth.internal.builder;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import org.mule.runtime.api.el.ExpressionEvaluator;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.oauth.api.OAuthDancer;
import org.mule.runtime.oauth.api.builder.OAuthDancerBuilder;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.client.HttpClient;
import org.mule.service.http.api.client.HttpClientConfiguration;
import org.mule.service.http.api.client.HttpClientConfiguration.Builder;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractOAuthDancerBuilder<D extends OAuthDancer> implements OAuthDancerBuilder<D> {

  protected final LockFactory lockProvider;
  protected final Map<String, ResourceOwnerOAuthContext> tokensStore;
  protected final HttpService httpService;
  protected final ExpressionEvaluator expressionEvaluator;

  protected String clientId;
  protected String clientSecret;
  protected String tokenUrl;
  protected Optional<TlsContextFactory> tlsContextFactory = empty();

  protected Charset encoding = UTF_8;
  protected String responseAccessTokenExpr = "#[(payload match /.*\"access_token\"[ ]*:[ ]*\"([^\\\"]*)\".*/)[1]]";
  protected String responseRefreshTokenExpr = "#[(payload match /.*\"refresh_token\"[ ]*:[ ]*\"([^\\\"]*)\".*/)[1]]";
  protected String responseExpiresInExpr = "#[(payload match /.*\"expires_in\"[ ]*:[ ]*\"([^\\\"]*)\".*/)[1]]";
  protected String scopes = null;
  protected Map<String, String> customParametersExtractorsExprs;

  public AbstractOAuthDancerBuilder(LockFactory lockProvider, Map<String, ResourceOwnerOAuthContext> tokensStore,
                                    HttpService httpService, ExpressionEvaluator expressionEvaluator) {
    this.lockProvider = lockProvider;
    this.tokensStore = tokensStore;
    this.httpService = httpService;
    this.expressionEvaluator = expressionEvaluator;
  }

  protected final HttpClient createHttpClient(HttpService httpService, Optional<TlsContextFactory> tlsContextFactory) {
    final Builder clientConfigBuilder =
        new HttpClientConfiguration.Builder().setThreadNamePrefix(format("oauthToken.requester[%s]", tokenUrl));
    tlsContextFactory.ifPresent(tcf -> clientConfigBuilder.setTlsContextFactory(tcf));
    return httpService.getClientFactory().create(clientConfigBuilder.build());

  }

  @Override
  public OAuthDancerBuilder clientCredentials(String clientId, String clientSecret) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    return this;
  }

  @Override
  public OAuthDancerBuilder tokenUrl(String tokenUrl) {
    this.tokenUrl = tokenUrl;
    return this;
  }

  @Override
  public OAuthDancerBuilder scopes(String scopes) {
    this.scopes = scopes;
    return this;
  }

  @Override
  public OAuthDancerBuilder encoding(Charset encoding) {
    this.encoding = encoding;
    return this;
  }

  @Override
  public OAuthDancerBuilder tlsContextFactory(TlsContextFactory tlsContextFactory) {
    this.tlsContextFactory = ofNullable(tlsContextFactory);
    return this;
  }

  @Override
  public OAuthDancerBuilder responseAccessTokenExpr(String responseAccessTokenExpr) {
    this.responseAccessTokenExpr = responseAccessTokenExpr;
    return this;
  }

  @Override
  public OAuthDancerBuilder responseRefreshTokenExpr(String responseRefreshTokenExpr) {
    this.responseRefreshTokenExpr = responseRefreshTokenExpr;
    return this;
  }

  @Override
  public OAuthDancerBuilder responseExpiresInExpr(String responseExpiresInExpr) {
    this.responseExpiresInExpr = responseExpiresInExpr;
    return this;
  }

  @Override
  public OAuthDancerBuilder customParametersExtractorsExprs(Map<String, String> customParamsExtractorsExprs) {
    this.customParametersExtractorsExprs = customParamsExtractorsExprs;
    return this;
  }

}
