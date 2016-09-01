/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.oauth2.internal.authorizationcode;

import static java.util.Collections.emptyMap;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.BAD_REQUEST;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.MOVED_TEMPORARILY;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.runtime.module.http.api.HttpConstants.RequestProperties.HTTP_QUERY_PARAMS;
import static org.mule.runtime.module.http.api.HttpConstants.RequestProperties.HTTP_REQUEST_URI;
import static org.mule.runtime.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.LOCATION;
import static org.mule.runtime.module.oauth2.internal.OAuthConstants.CLIENT_ID_PARAMETER;
import static org.mule.runtime.module.oauth2.internal.OAuthConstants.CLIENT_SECRET_PARAMETER;
import static org.mule.runtime.module.oauth2.internal.OAuthConstants.CODE_PARAMETER;
import static org.mule.runtime.module.oauth2.internal.OAuthConstants.GRANT_TYPE_AUTHENTICATION_CODE;
import static org.mule.runtime.module.oauth2.internal.OAuthConstants.GRANT_TYPE_PARAMETER;
import static org.mule.runtime.module.oauth2.internal.OAuthConstants.REDIRECT_URI_PARAMETER;
import static org.mule.runtime.module.oauth2.internal.OAuthConstants.STATE_PARAMETER;
import static org.mule.runtime.module.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;
import static org.springframework.util.StringUtils.isEmpty;

import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.session.DefaultMuleSession;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.module.http.internal.HttpParser;
import org.mule.runtime.module.oauth2.internal.MuleEventLogger;
import org.mule.runtime.module.oauth2.internal.OAuthConstants;
import org.mule.runtime.module.oauth2.internal.StateDecoder;
import org.mule.runtime.module.oauth2.internal.TokenNotFoundException;
import org.mule.runtime.module.oauth2.internal.TokenResponseProcessor;
import org.mule.runtime.module.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the Token request and response handling behaviour of the OAuth 2.0 dance. It provides support for standard OAuth
 * server implementations of the token acquisition part plus a couple of configuration attributes to customize behaviour.
 */
public class AutoAuthorizationCodeTokenRequestHandler extends AbstractAuthorizationCodeTokenRequestHandler {

  public static final String AUTHORIZATION_STATUS_QUERY_PARAM_KEY = "authorizationStatus";
  public static final int NO_AUTHORIZATION_CODE_STATUS = 100;
  public static final int TOKEN_URL_CALL_FAILED_STATUS = 200;
  public static final int TOKEN_NOT_FOUND_STATUS = 201;
  public static final int FAILURE_PROCESSING_REDIRECT_URL_REQUEST_STATUS = 300;
  private TokenResponseConfiguration tokenResponseConfiguration = new TokenResponseConfiguration();
  private MuleEventLogger muleEventLogger;

  public void setTokenResponseConfiguration(final TokenResponseConfiguration tokenResponseConfiguration) {
    this.tokenResponseConfiguration = tokenResponseConfiguration;
  }

  /**
   * Starts the http listener for the redirect url callback. This will create a flow with an endpoint on the provided OAuth
   * redirect uri parameter. The OAuth Server will call this url to provide the authentication code required to get the access
   * token.
   *
   * @throws MuleException if the listener couldn't be created.
   */
  @Override
  public void init() throws MuleException {
    createListenerForRedirectUrl();
    muleEventLogger = new MuleEventLogger(logger, muleContext);
  }

  @Override
  protected MessageProcessor createRedirectUrlProcessor() {
    return event -> {
      int authorizationStatus = 0;
      int statusCodeToReturn = OK.getStatusCode();
      String responseMessage = "Successfully retrieved access token";
      final Map<String, String> queryParams = event.getMessage().getInboundProperty(HTTP_QUERY_PARAMS);
      final String state = queryParams.get(STATE_PARAMETER);
      final StateDecoder stateDecoder = new StateDecoder(state);
      try {
        final String authorizationCode = processAuthorizationCode(event);

        if (logger.isDebugEnabled()) {
          logger.debug("Redirect url request state: " + state);
          logger.debug("Redirect url request code: " + authorizationCode);
        }

        MuleEvent tokenUrlResponse = callTokenUrl(event, authorizationCode);

        String decodedState = stateDecoder.decodeOriginalState();
        String encodedResourceOwnerId = stateDecoder.decodeResourceOwnerId();
        String resourceOwnerId = encodedResourceOwnerId == null ? DEFAULT_RESOURCE_OWNER_ID : encodedResourceOwnerId;

        final ResourceOwnerOAuthContext resourceOwnerOAuthContext =
            getOauthConfig().getUserOAuthContext().getContextForResourceOwner(resourceOwnerId);

        logResourceOwnerOAuthContextBeforeUpdate(resourceOwnerOAuthContext);

        TokenResponseProcessor tokenResponseProcessor = processTokenUrlResponse(tokenUrlResponse);

        updateResourceOwnerState(resourceOwnerOAuthContext, decodedState, tokenResponseProcessor);
        getOauthConfig().getUserOAuthContext().updateResourceOwnerOAuthContext(resourceOwnerOAuthContext);
      } catch (NoAuthorizationCodeException e1) {
        logger.error("Could not extract authorization code from OAuth provider HTTP request done to the redirect URL");
        muleEventLogger.logContent(event);
        authorizationStatus = NO_AUTHORIZATION_CODE_STATUS;
        statusCodeToReturn = BAD_REQUEST.getStatusCode();
        responseMessage = "Failure retrieving access token.\n OAuth Server uri from callback: "
            + event.getMessage().getInboundProperty(HTTP_REQUEST_URI);
      } catch (TokenUrlResponseException e2) {
        logger.error((String.format("HTTP response from token URL %s returned a failure status code", getTokenUrl())));
        muleEventLogger.logContent(e2.getTokenUrlResponse());
        authorizationStatus = TOKEN_URL_CALL_FAILED_STATUS;
        statusCodeToReturn = INTERNAL_SERVER_ERROR.getStatusCode();
        responseMessage = String.format("Failure calling token url %s. Exception message is %s", getTokenUrl(), e2.getMessage());
      } catch (TokenNotFoundException e3) {
        logger.error(String.format("Could not extract access token from token URL. Access token is %s, Refresh token is %s",
                                   e3.getTokenResponseProcessor().getAccessToken(),
                                   StringUtils.isBlank(e3.getTokenResponseProcessor().getRefreshToken()) ? "(Not issued)"
                                       : e3.getTokenResponseProcessor().getRefreshToken()));
        muleEventLogger.logContent(e3.getTokenUrlResponse());
        authorizationStatus = TOKEN_NOT_FOUND_STATUS;
        statusCodeToReturn = INTERNAL_SERVER_ERROR.getStatusCode();
        responseMessage = "Failed getting access token or refresh token from token URL response. See logs for details.";
      } catch (MuleException e4) {
        logger.error("Fail processing redirect URL request", e4);
        authorizationStatus = FAILURE_PROCESSING_REDIRECT_URL_REQUEST_STATUS;
        statusCodeToReturn = INTERNAL_SERVER_ERROR.getStatusCode();
        responseMessage = "Failed processing redirect URL request done from OAuth provider. See logs for details.";
      }
      final String finalResponseMessage = responseMessage;
      final int finalStatusCodeToReturn = statusCodeToReturn;
      final int finalAuthorizationStatus = authorizationStatus;
      MuleMessage.Builder builder = MuleMessage.builder(event.getMessage()).payload(finalResponseMessage)
          .addOutboundProperty(HTTP_STATUS_PROPERTY, finalStatusCodeToReturn);
      String onCompleteRedirectToValue = stateDecoder.decodeOnCompleteRedirectTo();
      if (!isEmpty(onCompleteRedirectToValue)) {
        builder.addOutboundProperty(HTTP_STATUS_PROPERTY, MOVED_TEMPORARILY.getStatusCode());
        builder.addOutboundProperty(LOCATION,
                                    HttpParser.appendQueryParam(onCompleteRedirectToValue, AUTHORIZATION_STATUS_QUERY_PARAM_KEY,
                                                                String.valueOf(finalAuthorizationStatus)));
      }

      event.setMessage(builder.build());
      return event;
    };
  }

  private MuleEvent callTokenUrl(MuleEvent event, String authorizationCode) throws MuleException, TokenUrlResponseException {
    setMapPayloadWithTokenRequestParameters(event, authorizationCode);
    return invokeTokenUrl(event);
  }

  private String processAuthorizationCode(final MuleEvent event) throws NoAuthorizationCodeException {
    final Map<String, String> queryParams = event.getMessage().getInboundProperty(HTTP_QUERY_PARAMS);
    final String authorizationCode = queryParams.get(CODE_PARAMETER);
    if (authorizationCode == null) {
      logger
          .info("HTTP Request to redirect URL done by the OAuth provider does not contains a code query parameter. Code query parameter is required to get the access token.");
      throw new NoAuthorizationCodeException();
    }
    return authorizationCode;
  }

  private boolean tokenResponseContentIsValid(TokenResponseProcessor tokenResponseProcessor) {
    return tokenResponseProcessor.getAccessToken() != null;
  }

  private void setMapPayloadWithTokenRequestParameters(final MuleEvent event, final String authorizationCode) {
    final HashMap<String, String> formData = new HashMap<>();
    formData.put(CODE_PARAMETER, authorizationCode);
    formData.put(CLIENT_ID_PARAMETER, getOauthConfig().getClientId());
    formData.put(CLIENT_SECRET_PARAMETER, getOauthConfig().getClientSecret());
    formData.put(GRANT_TYPE_PARAMETER, GRANT_TYPE_AUTHENTICATION_CODE);
    formData.put(REDIRECT_URI_PARAMETER, getOauthConfig().getRedirectionUrl());
    event.setMessage(MuleMessage.builder(event.getMessage()).payload(formData).build());
  }

  private void setMapPayloadWithRefreshTokenRequestParameters(final MuleEvent event, final String refreshToken) {
    final HashMap<String, String> formData = new HashMap<>();
    formData.put(OAuthConstants.REFRESH_TOKEN_PARAMETER, refreshToken);
    formData.put(CLIENT_ID_PARAMETER, getOauthConfig().getClientId());
    formData.put(CLIENT_SECRET_PARAMETER, getOauthConfig().getClientSecret());
    formData.put(GRANT_TYPE_PARAMETER, OAuthConstants.GRANT_TYPE_REFRESH_TOKEN);
    formData.put(REDIRECT_URI_PARAMETER, getOauthConfig().getRedirectionUrl());
    event.setMessage(MuleMessage.builder(event.getMessage()).payload(formData).build());
  }

  private void logResourceOwnerOAuthContextBeforeUpdate(ResourceOwnerOAuthContext resourceOwnerOAuthContext) {
    if (logger.isDebugEnabled()) {
      logger.debug("Update OAuth Context for resourceOwnerId %s", resourceOwnerOAuthContext.getResourceOwnerId());
    }
  }

  private TokenResponseProcessor processTokenUrlResponse(MuleEvent tokenUrlResponse)
      throws TokenNotFoundException, TransformerException {
    final TokenResponseProcessor tokenResponseProcessor = TokenResponseProcessor
        .createAuthorizationCodeProcessor(tokenResponseConfiguration, getMuleContext().getExpressionManager());
    tokenResponseProcessor.process(tokenUrlResponse);

    if (logger.isDebugEnabled()) {
      logger.debug("Retrieved access token, refresh token and expires from token url are: %s, %s, %s",
                   tokenResponseProcessor.getAccessToken(), tokenResponseProcessor.getRefreshToken(),
                   tokenResponseProcessor.getExpiresIn());
    }

    if (!tokenResponseContentIsValid(tokenResponseProcessor)) {
      throw new TokenNotFoundException(tokenUrlResponse, tokenResponseProcessor);
    }
    return tokenResponseProcessor;
  }

  private void updateResourceOwnerState(ResourceOwnerOAuthContext resourceOwnerOAuthContext, String newState,
                                        TokenResponseProcessor tokenResponseProcessor) {
    resourceOwnerOAuthContext.setAccessToken(tokenResponseProcessor.getAccessToken());
    if (tokenResponseProcessor.getRefreshToken() != null) {
      resourceOwnerOAuthContext.setRefreshToken(tokenResponseProcessor.getRefreshToken());
    }
    resourceOwnerOAuthContext.setExpiresIn(tokenResponseProcessor.getExpiresIn());

    // State may be null because there's no state or because this was called after refresh token.
    if (newState != null) {
      resourceOwnerOAuthContext.setState(newState);
    }

    final Map<String, Object> customResponseParameters = tokenResponseProcessor.getCustomResponseParameters();
    for (String paramName : customResponseParameters.keySet()) {
      final Object paramValue = customResponseParameters.get(paramName);
      if (paramValue != null) {
        resourceOwnerOAuthContext.getTokenResponseParameters().put(paramName, paramValue);
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("New OAuth State for resourceOwnerId %s is: accessToken(%s), refreshToken(%s), expiresIn(%s), state(%s)",
                   resourceOwnerOAuthContext.getResourceOwnerId(), resourceOwnerOAuthContext.getAccessToken(),
                   StringUtils.isBlank(resourceOwnerOAuthContext.getRefreshToken()) ? "Not issued"
                       : resourceOwnerOAuthContext.getRefreshToken(),
                   resourceOwnerOAuthContext.getExpiresIn(), resourceOwnerOAuthContext.getState());
    }
  }

  /**
   * Executes a refresh token for a particular user. It will call the OAuth Server token url and provide the refresh token to get
   * a new access token.
   *
   * @param currentEvent the event being processed when the refresh token was required.
   * @param resourceOwnerOAuthContext oauth context for who we need to update the access token.
   */
  @Override
  public void doRefreshToken(final MuleEvent currentEvent, final ResourceOwnerOAuthContext resourceOwnerOAuthContext) {
    try {
      final MuleEvent muleEvent = MuleEvent.builder(currentEvent)
          .message(MuleMessage.builder(currentEvent.getMessage()).outboundProperties(emptyMap()).build())
          .session(new DefaultMuleSession(currentEvent.getSession())).build();
      final String userRefreshToken = resourceOwnerOAuthContext.getRefreshToken();
      if (userRefreshToken == null) {
        throw new DefaultMuleException(CoreMessages.createStaticMessage(
                                                                        "The user with user id %s has no refresh token in his OAuth state so we can't execute the refresh token call",
                                                                        resourceOwnerOAuthContext.getResourceOwnerId()));
      }
      setMapPayloadWithRefreshTokenRequestParameters(muleEvent, userRefreshToken);
      final MuleEvent refreshTokenResponse = invokeTokenUrl(muleEvent);

      logResourceOwnerOAuthContextBeforeUpdate(resourceOwnerOAuthContext);
      TokenResponseProcessor tokenResponseProcessor = processTokenUrlResponse(refreshTokenResponse);
      updateResourceOwnerState(resourceOwnerOAuthContext, null, tokenResponseProcessor);
    } catch (TokenNotFoundException e) {
      throw new MuleRuntimeException(CoreMessages
          .createStaticMessage("Access token was not found from the refresh token oauth call"), e);
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  /**
   * The HTTP request received in redirect URL did not a query parameter 'code'
   */
  private class NoAuthorizationCodeException extends Exception {
  }

}
