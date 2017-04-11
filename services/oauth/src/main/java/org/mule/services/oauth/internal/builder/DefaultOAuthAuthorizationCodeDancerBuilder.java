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
import org.mule.runtime.api.el.MuleExpressionLanguage;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.oauth.api.AuthorizationCodeOAuthDancer;
import org.mule.runtime.oauth.api.AuthorizationCodeRequest;
import org.mule.runtime.oauth.api.builder.AuthorizationCodeDanceCallbackContext;
import org.mule.runtime.oauth.api.builder.OAuthAuthorizationCodeDancerBuilder;
import org.mule.runtime.oauth.api.state.DefaultResourceOwnerOAuthContext;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.server.HttpServer;
import org.mule.service.http.api.server.HttpServerConfiguration;
import org.mule.service.http.api.server.RequestHandler;
import org.mule.service.http.api.server.RequestHandlerManager;
import org.mule.service.http.api.server.ServerAddress;
import org.mule.services.oauth.internal.DefaultAuthorizationCodeOAuthDancer;
import org.mule.services.oauth.internal.OAuthCallbackServersManager;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;


public class DefaultOAuthAuthorizationCodeDancerBuilder extends AbstractOAuthDancerBuilder<AuthorizationCodeOAuthDancer>
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

  private Function<AuthorizationCodeRequest, AuthorizationCodeDanceCallbackContext> beforeDanceCallback = r -> null;
  private BiConsumer<AuthorizationCodeDanceCallbackContext, ResourceOwnerOAuthContext> afterDanceCallback = (vars, ctx) -> {
  };

  public DefaultOAuthAuthorizationCodeDancerBuilder(OAuthCallbackServersManager httpServersManager,
                                                    SchedulerService schedulerService, LockFactory lockProvider,
                                                    Map<String, DefaultResourceOwnerOAuthContext> tokensStore,
                                                    HttpService httpService,
                                                    MuleExpressionLanguage expressionEvaluator) {
    super(lockProvider, tokensStore, httpService, expressionEvaluator);
    this.httpServersManager = httpServersManager;
  }

  @Override
  public OAuthAuthorizationCodeDancerBuilder localCallback(URL localCallbackUrl) {
    localCallbackServerFactory = (serversManager) -> {
      final HttpServerConfiguration.Builder serverConfigBuilder = new HttpServerConfiguration.Builder();
      serverConfigBuilder.setHost(localCallbackUrl.getHost()).setPort(localCallbackUrl.getPort());
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
    localCallbackServerFactory = (serversManager) -> {
      return new HttpServer() {

        @Override
        public void stop() {
          // Nothing to do. The lifecycle of this object is handled by whoever passed me the client.
        }

        @Override
        public void start() throws IOException {
          // Nothing to do. The lifecycle of this object is handled by whoever passed me the client.
        }

        @Override
        public boolean isStopping() {
          return server.isStopping();
        }

        @Override
        public boolean isStopped() {
          return server.isStopped();
        }

        @Override
        public ServerAddress getServerAddress() {
          return server.getServerAddress();
        }

        @Override
        public void dispose() {
          // Nothing to do. The lifecycle of this object is handled by whoever passed me the client.
        }

        @Override
        public RequestHandlerManager addRequestHandler(String path, RequestHandler requestHandler) {
          return server.addRequestHandler(localCallbackConfigPath, requestHandler);
        }

        @Override
        public RequestHandlerManager addRequestHandler(Collection<String> methods, String path, RequestHandler requestHandler) {
          return server.addRequestHandler(methods, localCallbackConfigPath, requestHandler);
        }
      };
    };
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
  public OAuthAuthorizationCodeDancerBuilder beforeDanceCallback(Function<AuthorizationCodeRequest, AuthorizationCodeDanceCallbackContext> beforeDanceCallback) {
    requireNonNull(beforeDanceCallback, "beforeDanceCallback cannot be null");
    this.beforeDanceCallback = beforeDanceCallback;
    return this;
  }

  @Override
  public OAuthAuthorizationCodeDancerBuilder afterDanceCallback(BiConsumer<AuthorizationCodeDanceCallbackContext, ResourceOwnerOAuthContext> afterDanceCallback) {
    requireNonNull(afterDanceCallback, "afterDanceCallback cannot be null");
    this.afterDanceCallback = afterDanceCallback;
    return this;
  }

  @Override
  public AuthorizationCodeOAuthDancer build() {
    checkArgument(isNotBlank(clientId), "clientId cannot be blank");
    checkArgument(isNotBlank(clientSecret), "clientSecret cannot be blank");
    checkArgument(isNotBlank(tokenUrl), "tokenUrl cannot be blank");
    checkArgument(isNotBlank(authorizationUrl), "authorizationUrl cannot be blank");
    requireNonNull(localCallbackServerFactory, "localCallback must be configured");

    return new DefaultAuthorizationCodeOAuthDancer(localCallbackServerFactory.apply(httpServersManager), clientId, clientSecret,
                                                   tokenUrl, scopes, externalCallbackUrl, encoding, localCallbackUrlPath,
                                                   localAuthorizationUrlPath, localAuthorizationUrlResourceOwnerId, state,
                                                   authorizationUrl, responseAccessTokenExpr, responseRefreshTokenExpr,
                                                   responseExpiresInExpr, customParameters, customParametersExtractorsExprs,
                                                   lockProvider, tokensStore, httpClientFactory.get(), expressionEvaluator,
                                                   beforeDanceCallback, afterDanceCallback);
  }

}
