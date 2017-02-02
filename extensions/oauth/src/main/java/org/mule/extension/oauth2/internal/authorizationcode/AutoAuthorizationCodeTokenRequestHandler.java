/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.authorizationcode;

import static java.lang.String.valueOf;
import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableMap;
import static org.mule.extension.oauth2.internal.OAuthConstants.CLIENT_ID_PARAMETER;
import static org.mule.extension.oauth2.internal.OAuthConstants.CLIENT_SECRET_PARAMETER;
import static org.mule.extension.oauth2.internal.OAuthConstants.CODE_PARAMETER;
import static org.mule.extension.oauth2.internal.OAuthConstants.GRANT_TYPE_AUTHENTICATION_CODE;
import static org.mule.extension.oauth2.internal.OAuthConstants.GRANT_TYPE_PARAMETER;
import static org.mule.extension.oauth2.internal.OAuthConstants.GRANT_TYPE_REFRESH_TOKEN;
import static org.mule.extension.oauth2.internal.OAuthConstants.REDIRECT_URI_PARAMETER;
import static org.mule.extension.oauth2.internal.OAuthConstants.STATE_PARAMETER;
import static org.mule.extension.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;
import static org.mule.runtime.api.metadata.MediaType.TEXT;
import static org.mule.service.http.api.HttpConstants.HttpStatus.BAD_REQUEST;
import static org.mule.service.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.mule.service.http.api.HttpConstants.HttpStatus.MOVED_TEMPORARILY;
import static org.mule.service.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.service.http.api.HttpHeaders.Names.LOCATION;
import static org.mule.service.http.api.utils.HttpEncoderDecoderUtils.appendQueryParam;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.extension.oauth2.api.exception.TokenNotFoundException;
import org.mule.extension.oauth2.internal.MuleEventLogger;
import org.mule.extension.oauth2.internal.OAuthConstants;
import org.mule.extension.oauth2.internal.StateDecoder;
import org.mule.extension.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.service.http.api.HttpConstants.HttpStatus;
import org.mule.service.http.api.domain.ParameterMap;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;

/**
 * Represents the Token request and response handling behaviour of the OAuth 2.0 dance. It provides support for standard OAuth
 * server implementations of the token acquisition part plus a couple of configuration attributes to customize behaviour.
 */
public class AutoAuthorizationCodeTokenRequestHandler extends AbstractAuthorizationCodeTokenRequestHandler {

  private static final Logger LOGGER = getLogger(AutoAuthorizationCodeTokenRequestHandler.class);

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
    muleEventLogger = new MuleEventLogger(LOGGER);
  }

  @Override
  protected Function<Result<Object, HttpRequestAttributes>, Result<String, HttpResponseAttributes>> createRedirectUrlProcessor() {
    return in -> {
      int authorizationStatus = 0;
      HttpStatus statusCodeToReturn = OK;
      String responseMessage = "Successfully retrieved access token";

      final HttpRequestAttributes callbackHttpRequestAttributes = in.getAttributes().get();

      final String state = callbackHttpRequestAttributes.getQueryParams().get(STATE_PARAMETER);
      final StateDecoder stateDecoder = new StateDecoder(state);
      try {
        final String authorizationCode = processAuthorizationCode(callbackHttpRequestAttributes);

        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Redirect url request state: " + state);
          LOGGER.debug("Redirect url request code: " + authorizationCode);
        }

        Result<Object, HttpResponseAttributes> tokenUrlResponse =
            invokeTokenUrl(setMapPayloadWithTokenRequestParameters(authorizationCode), null);

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
        LOGGER.error("Could not extract authorization code from OAuth provider HTTP request done to the redirect URL");
        muleEventLogger.logContent(in);
        authorizationStatus = NO_AUTHORIZATION_CODE_STATUS;
        statusCodeToReturn = BAD_REQUEST;
        responseMessage =
            "Failure retrieving access token.\n OAuth Server uri from callback: " + callbackHttpRequestAttributes.getRequestUri();
      } catch (TokenUrlResponseException e) {
        LOGGER.error(e.getMessage());
        LOGGER.error("Token URL response body was: " + e.getTokenUrlResponse());
        authorizationStatus = TOKEN_URL_CALL_FAILED_STATUS;
        statusCodeToReturn = INTERNAL_SERVER_ERROR;
        responseMessage = String.format("Failure calling token url %s. Exception message is %s", getTokenUrl(), e.getMessage());
      } catch (TokenNotFoundException e) {
        LOGGER.error(e.getMessage());
        authorizationStatus = TOKEN_NOT_FOUND_STATUS;
        statusCodeToReturn = INTERNAL_SERVER_ERROR;
        responseMessage = "Failed getting access token or refresh token from token URL response. See logs for details.";
      } catch (MuleException e4) {
        LOGGER.error("Fail processing redirect URL request", e4);
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

      return Result.<String, HttpResponseAttributes>builder().output(responseMessage).attributes(responseAttributes)
          .mediaType(TEXT).build();
    };
  }

  private String processAuthorizationCode(final HttpRequestAttributes callbackHttpRequestAttributes)
      throws NoAuthorizationCodeException {
    final String authorizationCode = callbackHttpRequestAttributes.getQueryParams().get(CODE_PARAMETER);
    if (authorizationCode == null) {
      LOGGER.info("HTTP Request to redirect URL done by the OAuth provider does not contains a code query parameter. "
          + "Code query parameter is required to get the access token.");
      throw new NoAuthorizationCodeException();
    }
    return authorizationCode;
  }

  private Map<String, String> setMapPayloadWithTokenRequestParameters(final String authorizationCode) {
    final Map<String, String> formData = new HashMap<>();
    formData.put(CODE_PARAMETER, authorizationCode);
    formData.put(CLIENT_ID_PARAMETER, getOauthConfig().getClientId());
    formData.put(CLIENT_SECRET_PARAMETER, getOauthConfig().getClientSecret());
    formData.put(GRANT_TYPE_PARAMETER, GRANT_TYPE_AUTHENTICATION_CODE);
    formData.put(REDIRECT_URI_PARAMETER, getOauthConfig().getExternalCallbackUrl());
    return unmodifiableMap(formData);
  }

  private Map<String, String> setMapPayloadWithRefreshTokenRequestParameters(final String refreshToken) {
    final Map<String, String> formData = new HashMap<>();
    formData.put(OAuthConstants.REFRESH_TOKEN_PARAMETER, refreshToken);
    formData.put(CLIENT_ID_PARAMETER, getOauthConfig().getClientId());
    formData.put(CLIENT_SECRET_PARAMETER, getOauthConfig().getClientSecret());
    formData.put(GRANT_TYPE_PARAMETER, GRANT_TYPE_REFRESH_TOKEN);
    formData.put(REDIRECT_URI_PARAMETER, getOauthConfig().getExternalCallbackUrl());
    return unmodifiableMap(formData);
  }

  private void logResourceOwnerOAuthContextBeforeUpdate(ResourceOwnerOAuthContext resourceOwnerOAuthContext) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Update OAuth Context for resourceOwnerId %s", resourceOwnerOAuthContext.getResourceOwnerId());
    }
  }

  private TokenResponse processTokenUrlResponse(Result<Object, HttpResponseAttributes> tokenUrlResponse)
      throws TokenNotFoundException, TransformerException {
    TokenResponse tokenResponse = processTokenResponse(tokenUrlResponse, true);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Retrieved access token, refresh token and expires from token url are: %s, %s, %s",
                   tokenResponse.getAccessToken(), tokenResponse.getRefreshToken(),
                   tokenResponse.getExpiresIn());
    }

    if (!tokenResponseContentIsValid(tokenResponse)) {
      throw new TokenNotFoundException(tokenUrlResponse);
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

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("New OAuth State for resourceOwnerId %s is: accessToken(%s), refreshToken(%s), expiresIn(%s), state(%s)",
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
   * @param resourceOwnerOAuthContext oauth context for who we need to update the access token.
   */
  @Override
  public void doRefreshToken(final ResourceOwnerOAuthContext resourceOwnerOAuthContext) {
    try {
      final String userRefreshToken = resourceOwnerOAuthContext.getRefreshToken();
      if (userRefreshToken == null) {
        throw new DefaultMuleException(CoreMessages.createStaticMessage(
                                                                        "The user with user id %s has no refresh token in his OAuth state so we can't execute the refresh token call",
                                                                        resourceOwnerOAuthContext.getResourceOwnerId()));
      }
      final Result<Object, HttpResponseAttributes> refreshTokenResponse =
          invokeTokenUrl(setMapPayloadWithRefreshTokenRequestParameters(userRefreshToken), null);

      logResourceOwnerOAuthContextBeforeUpdate(resourceOwnerOAuthContext);
      TokenResponse tokenResponse = processTokenUrlResponse(refreshTokenResponse);
      updateResourceOwnerState(resourceOwnerOAuthContext, null, tokenResponse);
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
