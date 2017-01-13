/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.clientcredentials;

import static java.lang.String.format;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.mule.extension.oauth2.internal.OAuthConstants.CLIENT_ID_PARAMETER;
import static org.mule.extension.oauth2.internal.OAuthConstants.CLIENT_SECRET_PARAMETER;
import static org.mule.extension.oauth2.internal.OAuthConstants.GRANT_TYPE_CLIENT_CREDENTIALS;
import static org.mule.extension.oauth2.internal.OAuthConstants.GRANT_TYPE_PARAMETER;
import static org.mule.extension.oauth2.internal.OAuthConstants.SCOPE_PARAMETER;
import static org.mule.extension.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;
import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.oauth2.api.exception.TokenNotFoundException;
import org.mule.extension.oauth2.internal.AbstractTokenRequestHandler;
import org.mule.extension.oauth2.internal.ApplicationCredentials;
import org.mule.extension.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext;
import org.mule.extension.oauth2.internal.tokenmanager.TokenManagerConfig;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.UseConfig;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

/**
 * Handler for calling the token url, parsing the response and storing the oauth context data.
 */
public class ClientCredentialsTokenRequestHandler extends AbstractTokenRequestHandler implements Initialisable {

  private static final Logger LOGGER = getLogger(ClientCredentialsTokenRequestHandler.class);

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

  public void setApplicationCredentials(ApplicationCredentials applicationCredentials) {
    this.applicationCredentials = applicationCredentials;
  }

  public void setScopes(String scopes) {
    this.scopes = scopes;
  }

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    initialiseIfNeeded(tokenManager, muleContext);
  }

  private Event setMapPayloadWithTokenRequestParameters(final Event event) throws MuleException {
    final Map<String, String> formData = new HashMap<>();
    formData.put(GRANT_TYPE_PARAMETER, GRANT_TYPE_CLIENT_CREDENTIALS);
    String clientId = applicationCredentials.getClientId();
    String clientSecret = applicationCredentials.getClientSecret();

    InternalMessage.Builder builder = InternalMessage.builder(event.getMessage());
    if (encodeClientCredentialsInBody) {
      formData.put(CLIENT_ID_PARAMETER, clientId);
      formData.put(CLIENT_SECRET_PARAMETER, clientSecret);
    } else {
      String encodedCredentials = encodeBase64String(format("%s:%s", clientId, clientSecret).getBytes());
      builder.attributes(new OAuthAuthorizationAttributes("Basic " + encodedCredentials));
    }
    if (scopes != null) {
      formData.put(SCOPE_PARAMETER, scopes);
    }
    return Event.builder(event).message(builder.payload(formData).build()).build();
  }

  public void refreshAccessToken() throws MuleException {
    Flow flow = new Flow("test", getMuleContext());
    Event accessTokenEvent = Event.builder(create(flow, "ClientCredentialsTokenRequestHandler"))
        .message(InternalMessage.builder().nullPayload().build()).flow(flow).build();
    accessTokenEvent = setMapPayloadWithTokenRequestParameters(accessTokenEvent);
    final Event response;
    response = invokeTokenUrl(accessTokenEvent);
    TokenResponse tokenResponse = processTokenResponse(response, false);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Retrieved access token, refresh token and expires from token url are: %s, %s, %s",
                   tokenResponse.getAccessToken(), tokenResponse.getRefreshToken(), tokenResponse.getExpiresIn());
    }

    if (!tokenResponseContentIsValid(tokenResponse)) {
      throw new TokenNotFoundException(response);
    }

    final ResourceOwnerOAuthContext defaultUserState =
        tokenManager.getConfigOAuthContext().getContextForResourceOwner(DEFAULT_RESOURCE_OWNER_ID);
    defaultUserState.setAccessToken(tokenResponse.getAccessToken());
    defaultUserState.setExpiresIn(tokenResponse.getExpiresIn());
    final Map<String, Object> customResponseParameters = tokenResponse.getCustomResponseParameters();
    for (String paramName : customResponseParameters.keySet()) {
      defaultUserState.getTokenResponseParameters().put(paramName, customResponseParameters.get(paramName));
    }
    tokenManager.getConfigOAuthContext().updateResourceOwnerOAuthContext(defaultUserState);
  }

  public void setTokenManager(TokenManagerConfig tokenManager) {
    this.tokenManager = tokenManager;
  }

  public void setEncodeClientCredentialsInBody(boolean encodeClientCredentialsInBody) {
    this.encodeClientCredentialsInBody = encodeClientCredentialsInBody;
  }
}
