/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.oauth.internal;

import static java.lang.String.format;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;
import static org.mule.services.oauth.internal.OAuthConstants.CLIENT_ID_PARAMETER;
import static org.mule.services.oauth.internal.OAuthConstants.CLIENT_SECRET_PARAMETER;
import static org.mule.services.oauth.internal.OAuthConstants.GRANT_TYPE_CLIENT_CREDENTIALS;
import static org.mule.services.oauth.internal.OAuthConstants.GRANT_TYPE_PARAMETER;
import static org.mule.services.oauth.internal.OAuthConstants.SCOPE_PARAMETER;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.el.ExpressionEvaluator;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.oauth.api.OAuthDancer;
import org.mule.runtime.oauth.api.exception.TokenNotFoundException;
import org.mule.runtime.oauth.api.exception.TokenUrlResponseException;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;
import org.mule.service.http.api.client.HttpClient;
import org.mule.services.oauth.internal.state.TokenResponse;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;

/**
 * Provides OAuth dance support for client-credentials grant-type.
 * 
 * @since 4.0
 */
public class ClientCredentialsOAuthDancer extends AbstractOAuthDancer implements OAuthDancer, Startable {

  private static final Logger LOGGER = getLogger(ClientCredentialsOAuthDancer.class);

  private final boolean encodeClientCredentialsInBody;

  public ClientCredentialsOAuthDancer(String clientId, String clientSecret, String tokenUrl, String scopes,
                                      boolean encodeClientCredentialsInBody, Charset encoding, String responseAccessTokenExpr,
                                      String responseRefreshTokenExpr, String responseExpiresInExpr,
                                      Map<String, String> customParametersExprs, LockFactory lockProvider,
                                      Map<String, ResourceOwnerOAuthContext> tokensStore, HttpClient httpClient,
                                      ExpressionEvaluator expressionEvaluator) {
    super(clientId, clientSecret, tokenUrl, encoding, scopes, responseAccessTokenExpr, responseRefreshTokenExpr,
          responseExpiresInExpr, customParametersExprs, lockProvider, tokensStore, httpClient, expressionEvaluator);
    this.encodeClientCredentialsInBody = encodeClientCredentialsInBody;
  }

  @Override
  public void start() throws MuleException {
    super.start();
    try {
      refreshToken(null);
    } catch (Exception e) {
      super.stop();
      throw e;
    }
  }

  @Override
  public void refreshToken(String resourceOwner) {
    final Map<String, String> formData = new HashMap<>();

    formData.put(GRANT_TYPE_PARAMETER, GRANT_TYPE_CLIENT_CREDENTIALS);
    if (scopes != null) {
      formData.put(SCOPE_PARAMETER, scopes);
    }
    String authorization = null;
    if (encodeClientCredentialsInBody) {
      formData.put(CLIENT_ID_PARAMETER, clientId);
      formData.put(CLIENT_SECRET_PARAMETER, clientSecret);
    } else {
      authorization = "Basic " + encodeBase64String(format("%s:%s", clientId, clientSecret).getBytes());
    }

    try {
      TokenResponse tokenResponse = invokeTokenUrl(tokenUrl, formData, authorization, false, encoding);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Retrieved access token, refresh token and expires from token url are: %s, %s, %s",
                     tokenResponse.getAccessToken(), tokenResponse.getRefreshToken(), tokenResponse.getExpiresIn());
      }

      final ResourceOwnerOAuthContext defaultUserState = getContextForResourceOwner(DEFAULT_RESOURCE_OWNER_ID);
      defaultUserState.setAccessToken(tokenResponse.getAccessToken());
      defaultUserState.setExpiresIn(tokenResponse.getExpiresIn());
      for (Entry<String, Object> customResponseParameterEntry : tokenResponse.getCustomResponseParameters().entrySet()) {
        defaultUserState.getTokenResponseParameters().put(customResponseParameterEntry.getKey(),
                                                          customResponseParameterEntry.getValue());
      }

      updateResourceOwnerOAuthContext(defaultUserState);
    } catch (TokenUrlResponseException | TokenNotFoundException e) {
      throw new MuleRuntimeException(e);
    }

  }

}
