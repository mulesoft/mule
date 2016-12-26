/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.authorizationcode;

import static java.lang.String.valueOf;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.mule.extension.http.api.HttpConstants.HttpStatus.BAD_REQUEST;
import static org.mule.extension.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.mule.extension.http.api.HttpConstants.HttpStatus.MOVED_TEMPORARILY;
import static org.mule.extension.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.extension.http.api.HttpHeaders.Names.LOCATION;
import static org.mule.extension.oauth2.internal.OAuthConstants.CLIENT_ID_PARAMETER;
import static org.mule.extension.oauth2.internal.OAuthConstants.CLIENT_SECRET_PARAMETER;
import static org.mule.extension.oauth2.internal.OAuthConstants.CODE_PARAMETER;
import static org.mule.extension.oauth2.internal.OAuthConstants.GRANT_TYPE_AUTHENTICATION_CODE;
import static org.mule.extension.oauth2.internal.OAuthConstants.GRANT_TYPE_PARAMETER;
import static org.mule.extension.oauth2.internal.OAuthConstants.GRANT_TYPE_REFRESH_TOKEN;
import static org.mule.extension.oauth2.internal.OAuthConstants.REDIRECT_URI_PARAMETER;
import static org.mule.extension.oauth2.internal.OAuthConstants.STATE_PARAMETER;
import static org.mule.extension.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;
import static org.mule.runtime.module.http.internal.HttpParser.appendQueryParam;

import org.mule.extension.http.api.HttpConstants.HttpStatus;
import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.extension.oauth2.internal.MuleEventLogger;
import org.mule.extension.oauth2.internal.OAuthConstants;
import org.mule.extension.oauth2.internal.StateDecoder;
import org.mule.extension.oauth2.internal.TokenNotFoundException;
import org.mule.extension.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.session.DefaultMuleSession;
import org.mule.runtime.core.util.StringUtils;
import org.mule.service.http.api.domain.ParameterMap;

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
  private MuleEventLogger muleEventLogger;

  /**
   * Starts the http listener for the redirect url callback. This will create a flow with an endpoint on the provided OAuth
   * redirect uri parameter. The OAuth Server will call this url to provide the authentication code required to get the access
   * token.
   *
   * @throws MuleException if the listener couldn't be created.
   */
  @Override
  public void init() throws MuleException {
    createListenerForCallbackUrl();
    muleEventLogger = new MuleEventLogger(logger, muleContext);
  }

  @Override
  protected Processor createRedirectUrlProcessor() {
    return event -> {
      int authorizationStatus = 0;
      HttpStatus statusCodeToReturn = OK;
      String responseMessage = "Successfully retrieved access token";
      final Map<String, String> queryParams = ((HttpRequestAttributes) event.getMessage().getAttributes()).getQueryParams();
      final String state = queryParams.get(STATE_PARAMETER);
      final StateDecoder stateDecoder = new StateDecoder(state);
      try {
        final String authorizationCode = processAuthorizationCode(event);

        if (logger.isDebugEnabled()) {
          logger.debug("Redirect url request state: " + state);
          logger.debug("Redirect url request code: " + authorizationCode);
        }

        Event tokenUrlResponse = callTokenUrl(event, authorizationCode);

        String decodedState = stateDecoder.decodeOriginalState();
        String encodedResourceOwnerId = stateDecoder.decodeResourceOwnerId();
        String resourceOwnerId = encodedResourceOwnerId == null ? DEFAULT_RESOURCE_OWNER_ID : encodedResourceOwnerId;

        final ResourceOwnerOAuthContext resourceOwnerOAuthContext =
            getOauthConfig().getUserOAuthContext().getContextForResourceOwner(resourceOwnerId);

        logResourceOwnerOAuthContextBeforeUpdate(resourceOwnerOAuthContext);

        TokenResponse tokenResponse = processTokenUrlResponse(tokenUrlResponse);

        updateResourceOwnerState(resourceOwnerOAuthContext, decodedState, tokenResponse);
        getOauthConfig().getUserOAuthContext().updateResourceOwnerOAuthContext(resourceOwnerOAuthContext);
      } catch (NoAuthorizationCodeException e1) {
        logger.error("Could not extract authorization code from OAuth provider HTTP request done to the redirect URL");
        muleEventLogger.logContent(event);
        authorizationStatus = NO_AUTHORIZATION_CODE_STATUS;
        statusCodeToReturn = BAD_REQUEST;
        responseMessage = "Failure retrieving access token.\n OAuth Server uri from callback: "
            + ((HttpRequestAttributes) event.getMessage().getAttributes()).getRequestUri();
      } catch (TokenUrlResponseException e2) {
        logger.error((String.format("HTTP response from token URL %s returned a failure status code", getTokenUrl())));
        muleEventLogger.logContent(e2.getTokenUrlResponse());
        authorizationStatus = TOKEN_URL_CALL_FAILED_STATUS;
        statusCodeToReturn = INTERNAL_SERVER_ERROR;
        responseMessage = String.format("Failure calling token url %s. Exception message is %s", getTokenUrl(), e2.getMessage());
      } catch (TokenNotFoundException e3) {
        logger.error(String.format("Could not extract access token from token URL. Access token is %s, Refresh token is %s",
                                   e3.getTokenResponseAccessToken(),
                                   StringUtils.isBlank(e3.getTokenResponseRefreshToken()) ? "(Not issued)"
                                       : e3.getTokenResponseRefreshToken()));
        muleEventLogger.logContent(e3.getTokenUrlResponse());
        authorizationStatus = TOKEN_NOT_FOUND_STATUS;
        statusCodeToReturn = INTERNAL_SERVER_ERROR;
        responseMessage = "Failed getting access token or refresh token from token URL response. See logs for details.";
      } catch (MuleException e4) {
        logger.error("Fail processing redirect URL request", e4);
        authorizationStatus = FAILURE_PROCESSING_REDIRECT_URL_REQUEST_STATUS;
        statusCodeToReturn = INTERNAL_SERVER_ERROR;
        responseMessage = "Failed processing redirect URL request done from OAuth provider. See logs for details.";
      }

      final HttpResponseAttributes responseAttributes;

      String onCompleteRedirectToValue = stateDecoder.decodeOnCompleteRedirectTo();
      if (!isEmpty(onCompleteRedirectToValue)) {
        responseAttributes = new HttpResponseAttributes(MOVED_TEMPORARILY.getStatusCode(), MOVED_TEMPORARILY.getReasonPhrase(),
                                                        new ParameterMap(singletonMap(LOCATION,
                                                                                      appendQueryParam(onCompleteRedirectToValue,
                                                                                                       AUTHORIZATION_STATUS_QUERY_PARAM_KEY,
                                                                                                       valueOf(authorizationStatus)))));
      } else {
        responseAttributes =
            new HttpResponseAttributes(statusCodeToReturn.getStatusCode(), statusCodeToReturn.getReasonPhrase(),
                                       new ParameterMap());
      }

      InternalMessage.Builder builder =
          InternalMessage.builder(event.getMessage()).payload(responseMessage).attributes(responseAttributes);
      return Event.builder(event).message(builder.build()).build();
    };
  }

  private Event callTokenUrl(Event event, String authorizationCode) throws MuleException, TokenUrlResponseException {
    event = setMapPayloadWithTokenRequestParameters(event, authorizationCode);
    return invokeTokenUrl(event);
  }

  private String processAuthorizationCode(final Event event) throws NoAuthorizationCodeException {
    final Map<String, String> queryParams = ((HttpRequestAttributes) event.getMessage().getAttributes()).getQueryParams();
    final String authorizationCode = queryParams.get(CODE_PARAMETER);
    if (authorizationCode == null) {
      logger.info("HTTP Request to redirect URL done by the OAuth provider does not contains a code query parameter. "
          + "Code query parameter is required to get the access token.");
      throw new NoAuthorizationCodeException();
    }
    return authorizationCode;
  }

  private Event setMapPayloadWithTokenRequestParameters(final Event event, final String authorizationCode) {
    final Map<String, String> formData = new HashMap<>();
    formData.put(CODE_PARAMETER, authorizationCode);
    formData.put(CLIENT_ID_PARAMETER, getOauthConfig().getClientId());
    formData.put(CLIENT_SECRET_PARAMETER, getOauthConfig().getClientSecret());
    formData.put(GRANT_TYPE_PARAMETER, GRANT_TYPE_AUTHENTICATION_CODE);
    formData.put(REDIRECT_URI_PARAMETER, getOauthConfig().getExternalCallbackUrl());
    return Event.builder(event).message(InternalMessage.builder(event.getMessage()).payload(formData).build()).build();
  }

  private Event setMapPayloadWithRefreshTokenRequestParameters(final Event event, final String refreshToken) {
    final Map<String, String> formData = new HashMap<>();
    formData.put(OAuthConstants.REFRESH_TOKEN_PARAMETER, refreshToken);
    formData.put(CLIENT_ID_PARAMETER, getOauthConfig().getClientId());
    formData.put(CLIENT_SECRET_PARAMETER, getOauthConfig().getClientSecret());
    formData.put(GRANT_TYPE_PARAMETER, GRANT_TYPE_REFRESH_TOKEN);
    formData.put(REDIRECT_URI_PARAMETER, getOauthConfig().getExternalCallbackUrl());
    return Event.builder(event).message(InternalMessage.builder(event.getMessage()).payload(formData).build()).build();
  }

  private void logResourceOwnerOAuthContextBeforeUpdate(ResourceOwnerOAuthContext resourceOwnerOAuthContext) {
    if (logger.isDebugEnabled()) {
      logger.debug("Update OAuth Context for resourceOwnerId %s", resourceOwnerOAuthContext.getResourceOwnerId());
    }
  }

  private TokenResponse processTokenUrlResponse(Event tokenUrlResponse)
      throws TokenNotFoundException, TransformerException {
    TokenResponse tokenResponse = processTokenResponse(tokenUrlResponse, true);

    if (logger.isDebugEnabled()) {
      logger.debug("Retrieved access token, refresh token and expires from token url are: %s, %s, %s",
                   tokenResponse.getAccessToken(), tokenResponse.getRefreshToken(),
                   tokenResponse.getExpiresIn());
    }

    if (!tokenResponseContentIsValid(tokenResponse)) {
      throw new TokenNotFoundException(tokenUrlResponse, tokenResponse.getAccessToken(), tokenResponse.getRefreshToken());
    }
    return tokenResponse;
  }

  private void updateResourceOwnerState(ResourceOwnerOAuthContext resourceOwnerOAuthContext, String newState,
                                        TokenResponse tokenResponse) {
    resourceOwnerOAuthContext.setAccessToken(tokenResponse.getAccessToken());
    if (tokenResponse.getRefreshToken() != null) {
      resourceOwnerOAuthContext.setRefreshToken(tokenResponse.getRefreshToken());
    }
    resourceOwnerOAuthContext.setExpiresIn(tokenResponse.getExpiresIn());

    // State may be null because there's no state or because this was called after refresh token.
    if (newState != null) {
      resourceOwnerOAuthContext.setState(newState);
    }

    final Map<String, Object> customResponseParameters = tokenResponse.getCustomResponseParameters();
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
  public void doRefreshToken(final Event currentEvent, final ResourceOwnerOAuthContext resourceOwnerOAuthContext) {
    try {
      Event muleEvent = Event.builder(currentEvent)
          .message(InternalMessage.builder(currentEvent.getMessage()).outboundProperties(emptyMap()).build())
          .session(new DefaultMuleSession(currentEvent.getSession())).build();
      final String userRefreshToken = resourceOwnerOAuthContext.getRefreshToken();
      if (userRefreshToken == null) {
        throw new DefaultMuleException(CoreMessages.createStaticMessage(
                                                                        "The user with user id %s has no refresh token in his OAuth state so we can't execute the refresh token call",
                                                                        resourceOwnerOAuthContext.getResourceOwnerId()));
      }
      muleEvent = setMapPayloadWithRefreshTokenRequestParameters(muleEvent, userRefreshToken);
      final Event refreshTokenResponse = invokeTokenUrl(muleEvent);

      logResourceOwnerOAuthContextBeforeUpdate(resourceOwnerOAuthContext);
      TokenResponse tokenResponse = processTokenUrlResponse(refreshTokenResponse);
      updateResourceOwnerState(resourceOwnerOAuthContext, null, tokenResponse);
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
