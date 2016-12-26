/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.authorizationcode;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.mule.extension.http.api.HttpConstants.HttpStatus.MOVED_TEMPORARILY;
import static org.mule.extension.http.api.HttpConstants.Methods.GET;
import static org.mule.extension.http.api.HttpHeaders.Names.LOCATION;
import static org.mule.extension.oauth2.internal.DynamicFlowFactory.createDynamicFlow;
import static org.mule.extension.oauth2.internal.authorizationcode.RequestHandlerUtils.addRequestHandler;

import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.extension.oauth2.internal.StateEncoder;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.Event.Builder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.util.AttributeEvaluator;
import org.mule.runtime.module.http.internal.listener.matcher.DefaultMethodRequestMatcher;
import org.mule.runtime.module.http.internal.listener.matcher.ListenerRequestMatcher;
import org.mule.service.http.api.domain.ParameterMap;
import org.mule.service.http.api.server.RequestHandlerManager;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the call to the localAuthorizationUrl and redirects the user to the oauth authentication server authorization url so
 * the user can grant access to the resources to the mule application.
 */
public class AuthorizationRequestHandler implements MuleContextAware, Startable, Stoppable {

  public static final String OAUTH_STATE_ID_FLOW_VAR_NAME = "resourceOwnerId";

  private Logger logger = LoggerFactory.getLogger(AuthorizationRequestHandler.class);
  private String scopes;
  private String state;
  private String localAuthorizationUrl;
  private String authorizationUrl;
  private Map<String, String> customParameters = new HashMap<>();
  private RequestHandlerManager redirectUrlHandlerManager;
  private MuleContext muleContext;
  private AuthorizationCodeGrantType oauthConfig;
  private AttributeEvaluator stateEvaluator;

  public void setScopes(final String scopes) {
    this.scopes = scopes;
  }

  public void setState(final String state) {
    this.state = state;
  }

  public void setLocalAuthorizationUrl(final String localAuthorizationUrl) {
    this.localAuthorizationUrl = localAuthorizationUrl;
  }

  public void setAuthorizationUrl(final String authorizationUrl) {
    this.authorizationUrl = authorizationUrl;
  }

  public Map<String, String> getCustomParameters() {
    return customParameters;
  }

  public void setCustomParameters(final Map<String, String> customParameters) {
    this.customParameters = customParameters;
  }

  public void init() throws MuleException {
    try {
      stateEvaluator = new AttributeEvaluator(state).initialize(muleContext.getExpressionManager());

      this.redirectUrlHandlerManager =
          addRequestHandler(getOauthConfig().getServer(),
                            new ListenerRequestMatcher(new DefaultMethodRequestMatcher(GET.name()),
                                                       new URL(localAuthorizationUrl).getPath()),
                            createDynamicFlow(muleContext, "authorization-request-handler-" + localAuthorizationUrl,
                                              createLocalAuthorizationUrlListener()),
                            logger);
    } catch (MalformedURLException e) {
      logger.warn("Could not parse provided url %s. Validate that the url is correct", localAuthorizationUrl);
      throw new DefaultMuleException(e);
    }
  }

  private List<Processor> createLocalAuthorizationUrlListener() {
    final Processor listenerMessageProcessor = muleEvent -> {
      final Builder builder = Event.builder(muleEvent);

      final String onCompleteRedirectToValue =
          ((HttpRequestAttributes) muleEvent.getMessage().getAttributes()).getQueryParams().get("onCompleteRedirectTo");
      final String resourceOwnerId =
          getOauthConfig().getLocalAuthorizationUrlResourceOwnerIdEvaluator().resolveStringValue(muleEvent);
      muleEvent = builder.addVariable(OAUTH_STATE_ID_FLOW_VAR_NAME, resourceOwnerId).build();
      final String stateValue = stateEvaluator.resolveStringValue(muleEvent);
      final StateEncoder stateEncoder = new StateEncoder(stateValue);
      if (resourceOwnerId != null) {
        stateEncoder.encodeResourceOwnerIdInState(resourceOwnerId);
      }
      if (onCompleteRedirectToValue != null) {
        stateEncoder.encodeOnCompleteRedirectToInState(onCompleteRedirectToValue);
      }
      final String authorizationUrlWithParams = new AuthorizationRequestUrlBuilder().setAuthorizationUrl(authorizationUrl)
          .setClientId(oauthConfig.getClientId()).setClientSecret(oauthConfig.getClientSecret())
          .setCustomParameters(customParameters).setRedirectUrl(oauthConfig.getExternalCallbackUrl())
          .setState(stateEncoder.getEncodedState()).setScope(scopes).buildUrl();

      return builder.message(InternalMessage.builder(muleEvent.getMessage())
          .attributes(new HttpResponseAttributes(MOVED_TEMPORARILY.getStatusCode(), MOVED_TEMPORARILY.getReasonPhrase(),
                                                 new ParameterMap(singletonMap(LOCATION, authorizationUrlWithParams))))
          .build()).build();
    };
    return asList(listenerMessageProcessor);
  }

  @Override
  public void setMuleContext(final MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  public void setOauthConfig(final AuthorizationCodeGrantType oauthConfig) {
    this.oauthConfig = oauthConfig;
  }

  public AuthorizationCodeGrantType getOauthConfig() {
    return oauthConfig;
  }

  @Override
  public void start() throws MuleException {
    redirectUrlHandlerManager.start();
  }

  @Override
  public void stop() {
    redirectUrlHandlerManager.stop();
  }

}
