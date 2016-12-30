/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.clientcredentials;

import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;

import org.mule.extension.oauth2.internal.AbstractTokenRequestHandler;
import org.mule.extension.oauth2.internal.ApplicationCredentials;
import org.mule.extension.oauth2.internal.MuleEventLogger;
import org.mule.extension.oauth2.internal.OAuthConstants;
import org.mule.extension.oauth2.internal.TokenNotFoundException;
import org.mule.extension.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext;
import org.mule.extension.oauth2.internal.tokenmanager.TokenManagerConfig;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.UseConfig;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for calling the token url, parsing the response and storing the oauth context data.
 */
public class ClientCredentialsTokenRequestHandler extends AbstractTokenRequestHandler implements Initialisable {

  private static final Logger logger = LoggerFactory.getLogger(ClientCredentialsTokenRequestHandler.class);

  /**
   * Scope required by this application to execute. Scopes define permissions over resources.
   */
  @Parameter
  @Optional
  private String scopes;
  private ApplicationCredentials applicationCredentials;

  @UseConfig
  private TokenManagerConfig tokenManager;

  /**
   * If true, the client id and client secret will be sent in the request body. Otherwise, they will be sent as basic
   * authentication.
   */
  @Parameter
  @Optional(defaultValue = "false")
  private boolean encodeClientCredentialsInBody;
  private MuleEventLogger muleEventLogger;

  public void setApplicationCredentials(ApplicationCredentials applicationCredentials) {
    this.applicationCredentials = applicationCredentials;
  }

  public void setScopes(String scopes) {
    this.scopes = scopes;
  }

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    muleEventLogger = new MuleEventLogger(logger, muleContext);
    initialiseIfNeeded(tokenManager, muleContext);
  }

  private Event setMapPayloadWithTokenRequestParameters(final Event event) throws MuleException {
    final Map<String, String> formData = new HashMap<>();
    formData.put(OAuthConstants.GRANT_TYPE_PARAMETER, OAuthConstants.GRANT_TYPE_CLIENT_CREDENTIALS);
    String clientId = applicationCredentials.getClientId();
    String clientSecret = applicationCredentials.getClientSecret();

    InternalMessage.Builder builder = InternalMessage.builder(event.getMessage());
    if (encodeClientCredentialsInBody) {
      formData.put(OAuthConstants.CLIENT_ID_PARAMETER, clientId);
      formData.put(OAuthConstants.CLIENT_SECRET_PARAMETER, clientSecret);
    } else {
      String encodedCredentials = Base64.encodeBase64String(String.format("%s:%s", clientId, clientSecret).getBytes());
      builder.attributes(new OAuthAuthorizationAttributes("Basic " + encodedCredentials));
    }
    if (scopes != null) {
      formData.put(OAuthConstants.SCOPE_PARAMETER, scopes);
    }
    return Event.builder(event).message(builder.payload(formData).build()).build();
  }

  public void refreshAccessToken() throws MuleException {
    try {
      Flow flow = new Flow("test", getMuleContext());
      Event accessTokenEvent = Event.builder(create(flow, "ClientCredentialsTokenRequestHandler"))
          .message(InternalMessage.builder().nullPayload().build()).exchangePattern(REQUEST_RESPONSE).flow(flow).build();
      accessTokenEvent = setMapPayloadWithTokenRequestParameters(accessTokenEvent);
      final Event response;
      response = invokeTokenUrl(accessTokenEvent);
      TokenResponse tokenResponse = processTokenResponse(response, false);

      if (logger.isDebugEnabled()) {
        logger.debug("Retrieved access token, refresh token and expires from token url are: %s, %s, %s",
                     tokenResponse.getAccessToken(), tokenResponse.getRefreshToken(), tokenResponse.getExpiresIn());
      }

      if (!tokenResponseContentIsValid(tokenResponse)) {
        throw new TokenNotFoundException(response, tokenResponse.getAccessToken(), tokenResponse.getRefreshToken());
      }

      final ResourceOwnerOAuthContext defaultUserState =
          tokenManager.getConfigOAuthContext().getContextForResourceOwner(ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID);
      defaultUserState.setAccessToken(tokenResponse.getAccessToken());
      defaultUserState.setExpiresIn(tokenResponse.getExpiresIn());
      final Map<String, Object> customResponseParameters = tokenResponse.getCustomResponseParameters();
      for (String paramName : customResponseParameters.keySet()) {
        defaultUserState.getTokenResponseParameters().put(paramName, customResponseParameters.get(paramName));
      }
      tokenManager.getConfigOAuthContext().updateResourceOwnerOAuthContext(defaultUserState);
    } catch (TokenNotFoundException e) {
      logger.error(String
          .format("Could not extract access token or refresh token from token URL. Access token is %s, Refresh token is %s",
                  e.getTokenResponseAccessToken(), e.getTokenResponseRefreshToken()));
      muleEventLogger.logContent(e.getTokenUrlResponse());
      throw new DefaultMuleException(e);
    } catch (TokenUrlResponseException e) {
      logger.error((String.format("HTTP response from token URL %s returned a failure status code", getTokenUrl())));
      muleEventLogger.logContent(e.getTokenUrlResponse());
      throw new DefaultMuleException(e);
    }
  }

  public void setTokenManager(TokenManagerConfig tokenManager) {
    this.tokenManager = tokenManager;
  }

  public void setEncodeClientCredentialsInBody(boolean encodeClientCredentialsInBody) {
    this.encodeClientCredentialsInBody = encodeClientCredentialsInBody;
  }
}
