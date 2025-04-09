/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.internal.event.DefaultEventContext.child;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContext;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.from;
import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils.toAuthorizationCodeState;
import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils.toCredentialsLocation;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import static reactor.core.publisher.Mono.from;

import org.mule.oauth.client.api.AuthorizationCodeOAuthDancer;
import org.mule.oauth.client.api.AuthorizationCodeRequest;
import org.mule.oauth.client.api.builder.AuthorizationCodeDanceCallbackContext;
import org.mule.oauth.client.api.builder.OAuthAuthorizationCodeDancerBuilder;
import org.mule.oauth.client.api.listener.AuthorizationCodeListener;
import org.mule.oauth.client.api.state.ResourceOwnerOAuthContext;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.config.ArtifactEncoding;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.extension.api.connectivity.oauth.AuthCodeRequest;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.server.HttpServer;
import org.mule.runtime.http.api.server.ServerNotFoundException;
import org.mule.runtime.module.extension.api.runtime.connectivity.oauth.ImmutableAuthCodeRequest;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.LazyLookup;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.OAuthConfig;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.OAuthHandler;
import org.mule.runtime.module.extension.internal.store.LazyObjectStoreToMapAdapter;
import org.mule.runtime.oauth.api.OAuthService;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import jakarta.inject.Inject;

import org.reactivestreams.Publisher;

/**
 * {@link OAuthHandler} implementation for the authorization code grant type
 *
 * @since 4.2.1
 */
public class AuthorizationCodeOAuthHandler extends OAuthHandler<AuthorizationCodeOAuthDancer> {

  private static final String DANCE_CALLBACK_EVENT_KEY = "event";

  @Inject
  private ArtifactEncoding artifactEncoding;

  @Inject
  private Registry registry;

  // TODO: MULE-10837 this should be a plain old @Inject
  protected LazyValue<HttpService> httpService;

  private boolean forceInvalidateStatusRetrieval;

  /**
   * Becomes aware of the given {@code config} and makes sure that the access token callback and authorization endpoints are
   * provisioned.
   *
   * @param config an {@link AuthorizationCodeConfig}
   */
  public AuthorizationCodeOAuthDancer register(AuthorizationCodeConfig config) {
    return register(config, emptyList());
  }

  public AuthorizationCodeOAuthDancer register(AuthorizationCodeConfig config, List<AuthorizationCodeListener> listeners) {
    return dancers.computeIfAbsent(config.getOwnerConfigName(),
                                   (CheckedFunction<String, AuthorizationCodeOAuthDancer>) k -> createDancer(config, listeners));

  }

  /**
   * Performs the refresh token flow
   *
   * @param ownerConfigName the name of the extension config which obtained the token
   * @param resourceOwnerId the id of the user to be invalidated
   */
  public void refreshToken(String ownerConfigName, String resourceOwnerId) {
    AuthorizationCodeOAuthDancer dancer = dancers.get(ownerConfigName);

    try {
      dancer.refreshToken(resourceOwnerId).get();
    } catch (Exception e) {
      throw new MuleRuntimeException(
                                     createStaticMessage(format("Could not refresh token for resourceOwnerId '%s' using config '%s'",
                                                                resourceOwnerId, ownerConfigName)),
                                     e);
    }
  }

  /**
   * @param config an {@link OAuthConfig}
   * @return the {@link ResourceOwnerOAuthContext} for the given {@code config} or {@link Optional#empty()} if authorization
   *         hasn't yet taken place or has been invalidated
   */
  public Optional<ResourceOwnerOAuthContext> getOAuthContext(AuthorizationCodeConfig config) {
    AuthorizationCodeOAuthDancer dancer = dancers.get(config.getOwnerConfigName());
    if (dancer == null) {
      return empty();
    }

    ResourceOwnerOAuthContext contextForResourceOwner =
        dancer.getContextForResourceOwner(config.getResourceOwnerId());

    if (contextForResourceOwner == null || contextForResourceOwner.getAccessToken() == null) {
      return empty();
    }

    return of(contextForResourceOwner);
  }

  /**
   * Invalidates the OAuth information of a particular resourceOwnerId
   *
   * @param ownerConfigName the name of the extension config which obtained the token
   * @param resourceOwnerId the id of the user to be invalidated
   */
  public void invalidate(String ownerConfigName, String resourceOwnerId) {
    AuthorizationCodeOAuthDancer dancer = dancers.get(ownerConfigName);
    if (dancer == null) {
      return;
    }
    dancer.invalidateContext(resourceOwnerId, forceInvalidateStatusRetrieval);
  }

  protected void setForceInvalidateStatusRetrieval(boolean forceInvalidateStatusRetrieval) {
    this.forceInvalidateStatusRetrieval = forceInvalidateStatusRetrieval;
  }

  private AuthorizationCodeOAuthDancer createDancer(AuthorizationCodeConfig config, List<AuthorizationCodeListener> listeners)
      throws MuleException {
    checkArgument(listeners != null, "listeners cannot be null");

    OAuthAuthorizationCodeDancerBuilder dancerBuilder =
        getOAuthService().get().authorizationCodeGrantTypeDancerBuilder(lockFactory,
                                                                        new LazyObjectStoreToMapAdapter(
                                                                                                        () -> objectStoreLocator
                                                                                                            .apply(config)),
                                                                        expressionEvaluator);
    final AuthorizationCodeGrantType grantType = config.getGrantType();
    final OAuthCallbackConfig callbackConfig = config.getCallbackConfig();

    dancerBuilder
        .name(config.getOwnerConfigName())
        .encoding(artifactEncoding.getDefaultEncoding())
        .clientCredentials(config.getConsumerKey(), config.getConsumerSecret())
        .tokenUrl(config.getAccessTokenUrl())
        .responseExpiresInExpr(grantType.getExpirationRegex())
        .responseRefreshTokenExpr(grantType.getRefreshTokenExpr())
        .responseAccessTokenExpr(grantType.getAccessTokenExpr())
        .resourceOwnerIdTransformer(ownerId -> ownerId + "-" + config.getOwnerConfigName())
        .withClientCredentialsIn(toCredentialsLocation(grantType.getCredentialsPlacement()));

    String scopes = config.getScope()
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
        .authorizationUrl(config.getAuthorizationUrl())
        .localAuthorizationUrlPath(callbackConfig.getLocalAuthorizePath())
        .localAuthorizationUrlResourceOwnerId("#[attributes.queryParams.resourceOwnerId]")
        .state("#[attributes.queryParams.state]")
        .customParameters(config.getCustomQueryParameters())
        .customHeaders(config.getCustomHeaders())
        .customBodyParameters(config.getCustomBodyParameters())
        .customParametersExtractorsExprs(getParameterExtractors(config));

    dancerBuilder.includeRedirectUriInRefreshTokenRequest(grantType.includeRedirectUriInRefreshTokenRequest());

    Pair<Optional<Flow>, Optional<Flow>> listenerFlows = getListenerFlows(config);
    listenerFlows.getFirst().ifPresent(flow -> dancerBuilder.beforeDanceCallback(beforeCallback(config, flow)));
    listenerFlows.getSecond().ifPresent(flow -> dancerBuilder.afterDanceCallback(afterCallback(config, flow)));

    listeners.forEach(dancerBuilder::addListener);

    AuthorizationCodeOAuthDancer dancer = dancerBuilder.build();

    if (started) {
      start(dancer);
    }

    return dancer;
  }

  protected String getExternalCallback(HttpServer httpServer, OAuthCallbackConfig callbackConfig) {
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

  protected Pair<Optional<Flow>, Optional<Flow>> getListenerFlows(AuthorizationCodeConfig config) {
    try {
      return new Pair<>(lookupFlow(config.getBefore()), lookupFlow(config.getAfter()));
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not obtain 'before' and 'after' OAuth flows defined by "
          + "config " + config.getOwnerConfigName(), e));
    }
  }

  protected Optional<Flow> lookupFlow(Optional<String> flowName) {
    return flowName.map(this::lookupFlow);
  }

  protected Flow lookupFlow(String flowName) {
    return registry.<Flow>lookupByName(flowName)
        .orElseThrow(() -> new IllegalArgumentException("Flow " + flowName + " doesn't exist"));
  }

  protected Function<AuthorizationCodeRequest, AuthorizationCodeDanceCallbackContext> beforeCallback(AuthorizationCodeConfig config,
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

  protected BiConsumer<AuthorizationCodeDanceCallbackContext, ResourceOwnerOAuthContext> afterCallback(
                                                                                                       AuthorizationCodeConfig config,
                                                                                                       Flow flow) {

    return (callbackContext, oauthContext) -> {
      AuthorizationCodeState state = toAuthorizationCodeState(config, oauthContext);
      CoreEvent event = (CoreEvent) callbackContext.getParameter(DANCE_CALLBACK_EVENT_KEY)
          .orElseGet(() -> createEvent(state, config, flow));

      event = CoreEvent.builder(event).message(Message.builder().value(state).build()).build();
      runFlow(flow, event, config, "after");
    };
  }

  protected CoreEvent createEvent(Object payload, OAuthConfig config, Flow flow) {
    return CoreEvent.builder(create(flow, from(config.getOwnerConfigName())))
        .message(Message.builder().value(payload).build()).build();
  }

  protected CoreEvent runFlow(Flow flow, CoreEvent event, OAuthConfig config, String callbackType) {
    final Publisher<CoreEvent> childPublisher =
        processWithChildContext(event, flow, child((BaseEventContext) event.getContext(), of(flow.getLocation())));
    return from(childPublisher)
        .onErrorMap(MuleException.class,
                    e -> new MuleRuntimeException(createStaticMessage(
                                                                      format("Error found while execution flow '%s' which is configured in the '%s' parameter "
                                                                          + "of the '%s' config", flow.getName(), callbackType,
                                                                             config.getOwnerConfigName()),
                                                                      e)))
        .block();
  }

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    httpService = new LazyLookup<>(HttpService.class, muleContext);
  }

  protected Map<String, AuthorizationCodeOAuthDancer> getDancers() {
    return dancers;
  }

  protected LazyValue<OAuthService> getOAuthService() {
    return oauthService;
  }
}
