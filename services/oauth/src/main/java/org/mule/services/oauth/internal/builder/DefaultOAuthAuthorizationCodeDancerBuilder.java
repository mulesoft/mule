/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.oauth.internal.builder;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.el.ExpressionEvaluator;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.oauth.api.OAuthDancer;
import org.mule.runtime.oauth.api.builder.OAuthAuthorizationCodeDancerBuilder;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.server.HttpServer;
import org.mule.service.http.api.server.HttpServerConfiguration;
import org.mule.services.oauth.internal.AuthorizationCodeOAuthDancer;
import org.mule.services.oauth.internal.OAuthCallbackServersManager;

import java.net.URL;
import java.util.Map;
import java.util.function.Function;


public class DefaultOAuthAuthorizationCodeDancerBuilder extends AbstractOAuthDancerBuilder
    implements OAuthAuthorizationCodeDancerBuilder {

  private OAuthCallbackServersManager httpServersManager;
  private Function<OAuthCallbackServersManager, HttpServer> localCallbackServerFactory;
  private String localCallbackUrlPath;
  private String localAuthorizationUrlPath;
  private String localAuthorizationUrlResourceOwnerId;
  private String externalCallbackUrl;

  private String state;
  private String authorizationUrl;

  private Map<String, String> customParameters = emptyMap();

  public DefaultOAuthAuthorizationCodeDancerBuilder(OAuthCallbackServersManager httpServersManager,
                                                    SchedulerService schedulerService, LockFactory lockProvider,
                                                    Map<String, ResourceOwnerOAuthContext> tokensStore, HttpService httpService,
                                                    ExpressionEvaluator expressionEvaluator) {
    super(lockProvider, tokensStore, httpService, expressionEvaluator);
    this.httpServersManager = httpServersManager;
  }

  @Override
  public OAuthAuthorizationCodeDancerBuilder localCallback(URL localCallbackUrl) {
    localCallbackServerFactory = (serversManager) -> {
      final HttpServerConfiguration.Builder serverConfigBuilder = new HttpServerConfiguration.Builder();
      serverConfigBuilder.setHost(localCallbackUrl.getHost()).setPort(localCallbackUrl.getPort());
      tlsContextFactory.ifPresent(tls -> serverConfigBuilder.setTlsContextFactory(tls));
      try {
        return serversManager.getServer(serverConfigBuilder.build());
      } catch (ConnectionException e) {
        throw new MuleRuntimeException(e);
      }
    };
    localCallbackUrlPath = localCallbackUrl.getPath();

    return this;
  }

  @Override
  public OAuthAuthorizationCodeDancerBuilder localCallback(URL localCallbackUrl, TlsContextFactory tlsContextFactory) {
    localCallbackServerFactory = (serversManager) -> {
      final HttpServerConfiguration.Builder serverConfigBuilder = new HttpServerConfiguration.Builder();
      serverConfigBuilder.setHost(localCallbackUrl.getHost()).setPort(localCallbackUrl.getPort());
      serverConfigBuilder.setTlsContextFactory(tlsContextFactory);
      try {
        return serversManager.getServer(serverConfigBuilder.build());
      } catch (ConnectionException e) {
        throw new MuleRuntimeException(e);
      }
    };
    localCallbackUrlPath = localCallbackUrl.getPath();

    return this;
  }

  @Override
  public OAuthAuthorizationCodeDancerBuilder localCallback(HttpServer server, String localCallbackConfigPath) {
    localCallbackServerFactory = (serversManager) -> server;
    localCallbackUrlPath = localCallbackConfigPath;
    return this;
  }

  @Override
  public OAuthAuthorizationCodeDancerBuilder localAuthorizationUrlPath(String path) {
    this.localAuthorizationUrlPath = path;
    return this;
  }

  @Override
  public OAuthAuthorizationCodeDancerBuilder localAuthorizationUrlResourceOwnerId(String localAuthorizationUrlResourceOwnerIdExpr) {
    this.localAuthorizationUrlResourceOwnerId = localAuthorizationUrlResourceOwnerIdExpr;
    return this;
  }

  @Override
  public OAuthAuthorizationCodeDancerBuilder customParameters(Map<String, String> customParameters) {
    requireNonNull(customParameters, "customParameters cannot be null");
    this.customParameters = customParameters;
    return this;
  }

  @Override
  public OAuthAuthorizationCodeDancerBuilder state(String stateExpr) {
    this.state = stateExpr;
    return this;
  }

  @Override
  public OAuthAuthorizationCodeDancerBuilder authorizationUrl(String authorizationUrl) {
    this.authorizationUrl = authorizationUrl;
    return this;
  }

  @Override
  public OAuthAuthorizationCodeDancerBuilder externalCallbackUrl(String externalCallbackUrl) {
    this.externalCallbackUrl = externalCallbackUrl;
    return this;
  }

  @Override
  public OAuthDancer build() {
    checkArgument(isNotBlank(clientId), "clientId cannot be blank");
    checkArgument(isNotBlank(clientSecret), "clientSecret cannot be blank");
    checkArgument(isNotBlank(tokenUrl), "tokenUrl cannot be blank");
    checkArgument(isNotBlank(authorizationUrl), "authorizationUrl cannot be blank");
    requireNonNull(localCallbackServerFactory, "localCallback must be configured");

    return new AuthorizationCodeOAuthDancer(localCallbackServerFactory.apply(httpServersManager), clientId, clientSecret,
                                            tokenUrl, scopes, externalCallbackUrl, encoding, localCallbackUrlPath,
                                            localAuthorizationUrlPath, localAuthorizationUrlResourceOwnerId, state,
                                            authorizationUrl, responseAccessTokenExpr, responseRefreshTokenExpr,
                                            responseExpiresInExpr, customParameters, customParametersExtractorsExprs,
                                            lockProvider, tokensStore,
                                            createHttpClient(httpService, tlsContextFactory), expressionEvaluator);
  }

}
