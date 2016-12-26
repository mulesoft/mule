/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.clientcredentials;

import static java.lang.String.format;
import static org.mule.extension.http.api.HttpHeaders.Names.AUTHORIZATION;
import static org.mule.extension.http.internal.HttpConnectorConstants.TLS_CONFIGURATION;
import static org.mule.extension.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.SECURITY_TAB;

import org.mule.extension.oauth2.api.RequestAuthenticationException;
import org.mule.extension.oauth2.internal.AbstractGrantType;
import org.mule.extension.oauth2.internal.tokenmanager.TokenManagerConfig;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.service.http.api.domain.message.request.HttpRequestBuilder;

import java.util.function.Function;

/**
 * Authorization element for client credentials oauth grant type
 */
public class ClientCredentialsGrantType extends AbstractGrantType implements Initialisable, Startable, Stoppable {

  /**
   * Application identifier as defined in the oauth authentication server.
   */
  @Parameter
  private String clientId;

  /**
   * Application secret as defined in the oauth authentication server.
   */
  @Parameter
  private String clientSecret;

  /**
   * This element configures an automatic flow created by mule that listens in the configured url by the redirectUrl attribute and
   * process the request to retrieve an access token from the oauth authentication server.
   */
  @Parameter
  @ParameterGroup("Token Request")
  private ClientCredentialsTokenRequestHandler tokenRequestHandler;

  /**
   * References a TLS config that will be used to receive incoming HTTP request and do HTTP request during the OAuth dance.
   */
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  @DisplayName(TLS_CONFIGURATION)
  @Placement(tab = SECURITY_TAB)
  private TlsContextFactory tlsContextFactory;

  public TlsContextFactory getTlsContext() {
    return tlsContextFactory;
  }

  @Override
  public void start() throws MuleException {
    tokenRequestHandler.start();
    tokenRequestHandler.refreshAccessToken();
  }

  @Override
  public void stop() throws MuleException {
    tokenRequestHandler.stop();
  }

  @Override
  public String getClientSecret() {
    return clientSecret;
  }

  @Override
  public String getClientId() {
    return clientId;
  }

  @Override
  public void initialise() throws InitialisationException {
    if (tokenManager == null) {
      this.tokenManager = TokenManagerConfig.createDefault(muleContext);
    }

    tokenRequestHandler.setApplicationCredentials(this);
    tokenRequestHandler.setTokenManager(tokenManager);
    if (tlsContextFactory != null) {
      initialiseIfNeeded(tlsContextFactory);
      tokenRequestHandler.setTlsContextFactory(tlsContextFactory);
    }
    tokenRequestHandler.setMuleContext(muleContext);
    tokenRequestHandler.initialise();
  }

  @Override
  public void authenticate(Event muleEvent, HttpRequestBuilder builder) throws MuleException {
    final String accessToken =
        tokenManager.getConfigOAuthContext().getContextForResourceOwner(DEFAULT_RESOURCE_OWNER_ID).getAccessToken();
    if (accessToken == null) {
      throw new RequestAuthenticationException(createStaticMessage(format("No access token found. "
          + "Verify that you have authenticated before trying to execute an operation to the API.")));
    }
    builder.addHeader(AUTHORIZATION, buildAuthorizationHeaderContent(accessToken));
  }

  @Override
  public boolean shouldRetry(final Event firstAttemptResponseEvent) {
    final Boolean shouldRetryRequest = tokenRequestHandler.getRefreshTokenWhen().apply(firstAttemptResponseEvent);
    if (shouldRetryRequest) {
      try {
        tokenRequestHandler.refreshAccessToken();
      } catch (MuleException e) {
        throw new MuleRuntimeException(e);
      }
    }
    return shouldRetryRequest;
  }

  public void setTokenManager(TokenManagerConfig tokenManager) {
    this.tokenManager = tokenManager;
  }
}
