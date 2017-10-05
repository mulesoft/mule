/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_PERSISTENT_OBJECT_STORE_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.util.SystemUtils.getDefaultEncoding;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils.toAuthorizationCodeState;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.el.MuleExpressionLanguage;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.core.internal.util.LazyLookup;
import org.mule.runtime.extension.api.connectivity.oauth.AuthCodeRequest;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.server.HttpServer;
import org.mule.runtime.http.api.server.ServerNotFoundException;
import org.mule.runtime.module.extension.api.runtime.connectivity.oauth.ImmutableAuthCodeRequest;
import org.mule.runtime.module.extension.internal.store.LazyObjectStoreToMapAdapter;
import org.mule.runtime.oauth.api.AuthorizationCodeOAuthDancer;
import org.mule.runtime.oauth.api.AuthorizationCodeRequest;
import org.mule.runtime.oauth.api.OAuthService;
import org.mule.runtime.oauth.api.builder.AuthorizationCodeDanceCallbackContext;
import org.mule.runtime.oauth.api.builder.OAuthAuthorizationCodeDancerBuilder;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Default implementation of {@Link ExtensionsOAuthManager}
 *
 * @since 4.0
 */
public class DefaultExtensionsOAuthManager implements Initialisable, Startable, Stoppable, ExtensionsOAuthManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultExtensionsOAuthManager.class);
  private static final String DANCE_CALLBACK_EVENT_KEY = "event";

  @Inject
  private MuleContext muleContext;

  @Inject
  private LockFactory lockFactory;

  @Inject
  @Named(OBJECT_STORE_MANAGER)
  private ObjectStoreManager objectStoreManager;

  @Inject
  private MuleExpressionLanguage expressionEvaluator;

  @Inject
  private Registry registry;

  // TODO: MULE-10837 this should be a plain old @Inject
  private LazyValue<HttpService> httpService;

  // TODO: MULE-10837 this should be a plain old @Inject
  private LazyValue<OAuthService> oauthService;

  private final Map<String, AuthorizationCodeOAuthDancer> dancers = new ConcurrentHashMap<>();
  private boolean started = false;

  @Override
  public void initialise() throws InitialisationException {
    httpService = new LazyLookup<>(HttpService.class, muleContext);
    oauthService = new LazyLookup<>(OAuthService.class, muleContext);
  }

  @Override
  public void start() throws MuleException {
    for (AuthorizationCodeOAuthDancer dancer : dancers.values()) {
      start(dancer);
    }
    started = true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void register(OAuthConfig config) throws MuleException {
    dancers.computeIfAbsent(config.getOwnerConfigName(),
                            (CheckedFunction<String, AuthorizationCodeOAuthDancer>) k -> createDancer(config));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void invalidate(String ownerConfigName, String resourceOwnerId) {
    AuthorizationCodeOAuthDancer dancer = dancers.get(ownerConfigName);
    if (dancer == null) {
      return;
    }

    dancer.invalidateContext(resourceOwnerId);
  }

  private void disable(String ownerConfigName, AuthorizationCodeOAuthDancer dancer) {
    try {
      stopIfNeeded(dancer);
    } catch (Exception e) {
      LOGGER.warn("Found exception trying to Stop OAuth dancer for config " + ownerConfigName, e);
    } finally {
      disposeIfNeeded(dancer, LOGGER);
    }
  }

  @Override
  public void stop() throws MuleException {
    dancers.forEach((key, dancer) -> {
      try {
        disable(key, dancer);
      } catch (Exception e) {
        LOGGER.warn("Found exception while trying to stop OAuth callback for config " + key, e);
      }
    });
    dancers.clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void refreshToken(String ownerConfigName, String resourceOwnerId, OAuthConnectionProviderWrapper connectionProvider) {
    AuthorizationCodeOAuthDancer dancer = dancers.get(ownerConfigName);

    try {
      dancer.refreshToken(resourceOwnerId).get();
      connectionProvider.updateAuthState();
    } catch (Exception e) {
      throw new MuleRuntimeException(
                                     createStaticMessage(format("Could not refresh token for resourceOwnerId '%s' using config '%s'",
                                                                resourceOwnerId, ownerConfigName)),
                                     e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ResourceOwnerOAuthContext> getOAuthContext(OAuthConfig config) {
    AuthorizationCodeOAuthDancer dancer = dancers.get(config.getOwnerConfigName());
    if (dancer == null) {
      return empty();
    }

    ResourceOwnerOAuthContext contextForResourceOwner =
        dancer.getContextForResourceOwner(config.getAuthCodeConfig().getResourceOwnerId());

    if (contextForResourceOwner == null || contextForResourceOwner.getAccessToken() == null) {
      return empty();
    }

    return of(contextForResourceOwner);
  }

  private AuthorizationCodeOAuthDancer createDancer(OAuthConfig config) throws MuleException {
    OAuthAuthorizationCodeDancerBuilder dancerBuilder =
        oauthService.get().authorizationCodeGrantTypeDancerBuilder(lockId -> lockFactory.createLock(lockId),
                                                                   new LazyObjectStoreToMapAdapter(
                                                                                                   getObjectStoreSupplier(config)),
                                                                   expressionEvaluator);
    final AuthCodeConfig authCodeConfig = config.getAuthCodeConfig();
    final AuthorizationCodeGrantType grantType = config.getGrantType();
    final OAuthCallbackConfig callbackConfig = config.getCallbackConfig();

    dancerBuilder
        .encoding(getDefaultEncoding(muleContext))
        .clientCredentials(authCodeConfig.getConsumerKey(), authCodeConfig.getConsumerSecret())
        .tokenUrl(authCodeConfig.getAccessTokenUrl())
        .responseExpiresInExpr(grantType.getExpirationRegex())
        .responseRefreshTokenExpr(grantType.getRefreshTokenExpr())
        .responseAccessTokenExpr(grantType.getAccessTokenExpr())
        .resourceOwnerIdTransformer(ownerId -> ownerId + "-" + config.getOwnerConfigName());

    String scopes = authCodeConfig.getScope()
        .orElseGet(() -> grantType.getDefaultScope().orElse(null));

    if (scopes != null) {
      dancerBuilder.scopes(scopes);
    }

    HttpServer httpServer;
    try {
      httpServer = httpService.get().getServerFactory().lookup(callbackConfig.getListenerConfig());
    } catch (ServerNotFoundException e) {
      throw new MuleRuntimeException(createStaticMessage(format(
                                                                "Connector '%s' defines '%s' as the http:listener-config to use for provisioning callbacks, but no such definition "
                                                                    + "exists in the application configuration",
                                                                config.getOwnerConfigName(), callbackConfig.getListenerConfig())),
                                     e);
    }

    dancerBuilder
        .localCallback(httpServer, callbackConfig.getCallbackPath())
        .externalCallbackUrl(getExternalCallback(httpServer, callbackConfig))
        .authorizationUrl(authCodeConfig.getAuthorizationUrl())
        .localAuthorizationUrlPath(callbackConfig.getLocalAuthorizePath())
        .localAuthorizationUrlResourceOwnerId(
                                              "#[if (attributes.queryParams.resourceOwnerId != null) attributes.queryParams.resourceOwnerId else '']")
        .state("#[if (attributes.queryParams.state != null) attributes.queryParams.state else '']")
        .customParameters(config.getCustomParameters())
        .customParametersExtractorsExprs(getParameterExtractors(config));

    Pair<Optional<Flow>, Optional<Flow>> listenerFlows = getListenerFlows(config);
    listenerFlows.getFirst().ifPresent(flow -> dancerBuilder.beforeDanceCallback(beforeCallback(config, flow)));
    listenerFlows.getSecond().ifPresent(flow -> dancerBuilder.afterDanceCallback(afterCallback(config, flow)));

    AuthorizationCodeOAuthDancer dancer = dancerBuilder.build();

    if (started) {
      start(dancer);
    }

    return dancer;
  }

  private String getExternalCallback(HttpServer httpServer, OAuthCallbackConfig callbackConfig) {
    return callbackConfig.getExternalCallbackUrl().orElseGet(() -> {
      try {
        return new URL(httpServer.getProtocol().getScheme(),
                       httpServer.getServerAddress().getIp(),
                       httpServer.getServerAddress().getPort(),
                       callbackConfig.getCallbackPath())
                           .toExternalForm();
      } catch (MalformedURLException e) {
        throw new MuleRuntimeException(createStaticMessage(format(
                                                                  "Could not derive a external callback url from <http:listener-config> '%s'",
                                                                  callbackConfig.getListenerConfig())),
                                       e);
      }
    });
  }

  private void start(AuthorizationCodeOAuthDancer dancer) throws MuleException {
    initialiseIfNeeded(dancer, muleContext);
    startIfNeeded(dancer);
  }

  private Supplier<ObjectStore> getObjectStoreSupplier(OAuthConfig config) {
    return () -> {
      String storeName = config.getStoreConfig()
          .map(OAuthObjectStoreConfig::getObjectStoreName)
          .orElse(BASE_PERSISTENT_OBJECT_STORE_KEY);

      return objectStoreManager.getObjectStore(storeName);
    };
  }

  private Map<String, String> getParameterExtractors(OAuthConfig config) {
    return config.getParameterExtractors().entrySet().stream()
        .collect(toMap(entry -> entry.getKey().getName(), Map.Entry::getValue));
  }

  private Pair<Optional<Flow>, Optional<Flow>> getListenerFlows(OAuthConfig oauthConfig) {
    AuthCodeConfig config = oauthConfig.getAuthCodeConfig();
    try {
      return new Pair<>(lookupFlow(config.getBefore()), lookupFlow(config.getAfter()));
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not obtain 'before' and 'after' OAuth flows defined by "
          + "config " + oauthConfig.getOwnerConfigName(), e));
    }
  }

  private Optional<Flow> lookupFlow(Optional<String> flowName) {
    return flowName.map(this::lookupFlow);
  }

  private Flow lookupFlow(String flowName) {
    return registry.<Flow>lookupByName(flowName)
        .orElseThrow(() -> new IllegalArgumentException("Flow " + flowName + " doesn't exist"));
  }

  private Function<AuthorizationCodeRequest, AuthorizationCodeDanceCallbackContext> beforeCallback(OAuthConfig config,
                                                                                                   Flow flow) {
    return (AuthorizationCodeRequest danceRequest) -> {
      final AuthCodeRequest request = new ImmutableAuthCodeRequest(danceRequest.getResourceOwnerId(),
                                                                   danceRequest.getScopes(),
                                                                   danceRequest.getState().orElse(null),
                                                                   config.getCallbackConfig().getExternalCallbackUrl());

      CoreEvent event = runFlow(flow, createEvent(request, config, flow), config, "before");
      return paramKey -> DANCE_CALLBACK_EVENT_KEY.equals(paramKey) ? of(event) : empty();
    };
  }

  private BiConsumer<AuthorizationCodeDanceCallbackContext, ResourceOwnerOAuthContext> afterCallback(OAuthConfig config,
                                                                                                     Flow flow) {
    return (callbackContext, oauthContext) -> {
      AuthorizationCodeState state = toAuthorizationCodeState(config, oauthContext);
      CoreEvent event = (CoreEvent) callbackContext.getParameter(DANCE_CALLBACK_EVENT_KEY)
          .orElseGet(() -> createEvent(state, config, flow));

      event = CoreEvent.builder(event).message(Message.builder().value(state).build()).build();
      runFlow(flow, event, config, "after");
    };
  }

  private CoreEvent createEvent(Object payload, OAuthConfig config, Flow flow) {
    return CoreEvent.builder(create(flow, fromSingleComponent(config.getOwnerConfigName())))
        .message(Message.builder().value(payload).build()).build();
  }

  private CoreEvent runFlow(Flow flow, CoreEvent event, OAuthConfig config, String callbackType) {
    try {
      return flow.process(event);
    } catch (MuleException e) {
      throw new MuleRuntimeException(
                                     createStaticMessage(format("Error found while execution flow '%s' which is configured in the '%s' parameter "
                                         + "of the '%s' config", flow.getName(), callbackType, config.getOwnerConfigName()), e));
    }
  }
}
