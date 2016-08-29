/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.oauth2.internal.clientcredentials;

import static org.mule.runtime.core.DefaultMessageContext.create;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;

import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.module.http.api.HttpHeaders;
import org.mule.runtime.module.oauth2.internal.AbstractTokenRequestHandler;
import org.mule.runtime.module.oauth2.internal.ApplicationCredentials;
import org.mule.runtime.module.oauth2.internal.MuleEventLogger;
import org.mule.runtime.module.oauth2.internal.OAuthConstants;
import org.mule.runtime.module.oauth2.internal.TokenNotFoundException;
import org.mule.runtime.module.oauth2.internal.TokenResponseProcessor;
import org.mule.runtime.module.oauth2.internal.authorizationcode.TokenResponseConfiguration;
import org.mule.runtime.module.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext;
import org.mule.runtime.module.oauth2.internal.tokenmanager.TokenManagerConfig;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

/**
 * Handler for calling the token url, parsing the response and storing the oauth context data.
 */
public class ClientCredentialsTokenRequestHandler extends AbstractTokenRequestHandler {

  private String scopes;
  private ApplicationCredentials applicationCredentials;
  private TokenResponseConfiguration tokenResponseConfiguration = new TokenResponseConfiguration();
  private TokenManagerConfig tokenManager;
  private boolean encodeClientCredentialsInBody = false;
  private MuleEventLogger muleEventLogger;

  public void setApplicationCredentials(ApplicationCredentials applicationCredentials) {
    this.applicationCredentials = applicationCredentials;
  }

  public void setScopes(String scopes) {
    this.scopes = scopes;
  }

  public void setTokenResponseConfiguration(TokenResponseConfiguration tokenResponseConfiguration) {
    this.tokenResponseConfiguration = tokenResponseConfiguration;
  }

  @Override
  public void setMuleContext(MuleContext muleContext) {
    super.setMuleContext(muleContext);
    muleEventLogger = new MuleEventLogger(logger, muleContext);
  }

  private void setMapPayloadWithTokenRequestParameters(final MuleEvent event) throws MuleException {
    final HashMap<String, String> formData = new HashMap<>();
    formData.put(OAuthConstants.GRANT_TYPE_PARAMETER, OAuthConstants.GRANT_TYPE_CLIENT_CREDENTIALS);
    String clientId = applicationCredentials.getClientId();
    String clientSecret = applicationCredentials.getClientSecret();

    MuleMessage.Builder builder = MuleMessage.builder(event.getMessage());
    if (encodeClientCredentialsInBody) {
      formData.put(OAuthConstants.CLIENT_ID_PARAMETER, clientId);
      formData.put(OAuthConstants.CLIENT_SECRET_PARAMETER, clientSecret);
    } else {
      String encodedCredentials = Base64.encodeBase64String(String.format("%s:%s", clientId, clientSecret).getBytes());
      builder.addOutboundProperty(HttpHeaders.Names.AUTHORIZATION, "Basic " + encodedCredentials);
    }
    if (scopes != null) {
      formData.put(OAuthConstants.SCOPE_PARAMETER, scopes);
    }
    event.setMessage(builder.payload(formData).build());
  }

  public void refreshAccessToken() throws MuleException {
    try {
      Flow flow = new Flow("test", getMuleContext());
      final MuleEvent accessTokenEvent = MuleEvent.builder(create(flow, "ClientCredentialsTokenRequestHandler"))
          .message(MuleMessage.builder().nullPayload().build()).exchangePattern(REQUEST_RESPONSE).flow(flow).build();
      setMapPayloadWithTokenRequestParameters(accessTokenEvent);
      final MuleEvent response;
      response = invokeTokenUrl(accessTokenEvent);
      final TokenResponseProcessor tokenResponseProcessor = TokenResponseProcessor
          .createClientCredentialsProcessor(tokenResponseConfiguration, getMuleContext().getExpressionManager());
      tokenResponseProcessor.process(response);

      if (logger.isDebugEnabled()) {
        logger.debug("Retrieved access token, refresh token and expires from token url are: %s, %s, %s",
                     tokenResponseProcessor.getAccessToken(), tokenResponseProcessor.getRefreshToken(),
                     tokenResponseProcessor.getExpiresIn());
      }

      if (!tokenResponseContentIsValid(tokenResponseProcessor)) {
        throw new TokenNotFoundException(response, tokenResponseProcessor);
      }

      final ResourceOwnerOAuthContext defaultUserState =
          tokenManager.getConfigOAuthContext().getContextForResourceOwner(ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID);
      defaultUserState.setAccessToken(tokenResponseProcessor.getAccessToken());
      defaultUserState.setExpiresIn(tokenResponseProcessor.getExpiresIn());
      final Map<String, Object> customResponseParameters = tokenResponseProcessor.getCustomResponseParameters();
      for (String paramName : customResponseParameters.keySet()) {
        defaultUserState.getTokenResponseParameters().put(paramName, customResponseParameters.get(paramName));
      }
      tokenManager.getConfigOAuthContext().updateResourceOwnerOAuthContext(defaultUserState);
    } catch (TokenNotFoundException e) {
      logger.error(String
          .format("Could not extract access token or refresh token from token URL. Access token is %s, Refresh token is %s",
                  e.getTokenResponseProcessor().getAccessToken(), e.getTokenResponseProcessor().getRefreshToken()));
      muleEventLogger.logContent(e.getTokenUrlResponse());
      throw new DefaultMuleException(e);
    } catch (TokenUrlResponseException e) {
      logger.error((String.format("HTTP response from token URL %s returned a failure status code", getTokenUrl())));
      muleEventLogger.logContent(e.getTokenUrlResponse());
      throw new DefaultMuleException(e);
    }
  }

  private boolean tokenResponseContentIsValid(TokenResponseProcessor tokenResponseProcessor) {
    return tokenResponseProcessor.getAccessToken() != null;
  }

  public void setTokenManager(TokenManagerConfig tokenManager) {
    this.tokenManager = tokenManager;
  }

  public void setEncodeClientCredentialsInBody(boolean encodeClientCredentialsInBody) {
    this.encodeClientCredentialsInBody = encodeClientCredentialsInBody;
  }
}
