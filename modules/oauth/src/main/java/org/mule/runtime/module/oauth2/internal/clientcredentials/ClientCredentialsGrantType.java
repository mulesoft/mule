/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.oauth2.internal.clientcredentials;

import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.module.http.api.HttpHeaders;
import org.mule.runtime.module.http.internal.domain.request.HttpRequestBuilder;
import org.mule.runtime.module.oauth2.api.RequestAuthenticationException;
import org.mule.runtime.module.oauth2.internal.AbstractGrantType;
import org.mule.runtime.module.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext;
import org.mule.runtime.module.oauth2.internal.tokenmanager.TokenManagerConfig;
import org.mule.runtime.api.tls.TlsContextFactory;

/**
 * Authorization element for client credentials oauth grant type
 */
public class ClientCredentialsGrantType extends AbstractGrantType implements Initialisable, Startable, MuleContextAware {

  private String clientId;
  private String clientSecret;
  private ClientCredentialsTokenRequestHandler tokenRequestHandler;
  private MuleContext muleContext;
  private TokenManagerConfig tokenManager;
  private TlsContextFactory tlsContextFactory;

  public void setClientId(final String clientId) {
    this.clientId = clientId;
  }

  public void setClientSecret(final String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public void setTokenRequestHandler(final ClientCredentialsTokenRequestHandler tokenRequestHandler) {
    this.tokenRequestHandler = tokenRequestHandler;
  }

  public TlsContextFactory getTlsContext() {
    return tlsContextFactory;
  }

  public void setTlsContext(TlsContextFactory tlsContextFactory) {
    this.tlsContextFactory = tlsContextFactory;
  }

  @Override
  public void start() throws MuleException {
    tokenRequestHandler.refreshAccessToken();
  }

  public String getClientSecret() {
    return clientSecret;
  }

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
      tokenRequestHandler.setTlsContextFactory(tlsContextFactory);
    }
  }

  @Override
  public void setMuleContext(final MuleContext context) {
    this.muleContext = context;
  }

  public String getRefreshTokenWhen() {
    return tokenRequestHandler.getRefreshTokenWhen();
  }

  public void refreshAccessToken() throws MuleException {
    tokenRequestHandler.refreshAccessToken();
  }

  @Override
  public void authenticate(MuleEvent muleEvent, HttpRequestBuilder builder) throws MuleException {
    final String accessToken = tokenManager.getConfigOAuthContext()
        .getContextForResourceOwner(ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID).getAccessToken();
    if (accessToken == null) {
      throw new RequestAuthenticationException(createStaticMessage(String
          .format("No access token found. Verify that you have authenticated before trying to execute an operation to the API.")));
    }
    builder.addHeader(HttpHeaders.Names.AUTHORIZATION, buildAuthorizationHeaderContent(accessToken));
  }

  @Override
  public boolean shouldRetry(final MuleEvent firstAttemptResponseEvent) {
    final Object value = muleContext.getExpressionManager().evaluate(getRefreshTokenWhen(), firstAttemptResponseEvent);
    if (!(value instanceof Boolean)) {
      throw new MuleRuntimeException(createStaticMessage("Expression %s should return a boolean but return %s",
                                                         getRefreshTokenWhen(), value));
    }
    final Boolean shouldRetryRequest = (Boolean) value;
    if (shouldRetryRequest) {
      try {
        refreshAccessToken();
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
