/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.oauth.internal;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.lang.Thread.currentThread;
import static java.util.Collections.singleton;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.api.metadata.MediaType.parse;
import static org.mule.runtime.oauth.api.OAuthAuthorizationStatusCode.AUTHORIZATION_CODE_RECEIVED_STATUS;
import static org.mule.runtime.oauth.api.OAuthAuthorizationStatusCode.AUTHORIZATION_STATUS_QUERY_PARAM_KEY;
import static org.mule.runtime.oauth.api.OAuthAuthorizationStatusCode.NO_AUTHORIZATION_CODE_STATUS;
import static org.mule.runtime.oauth.api.OAuthAuthorizationStatusCode.TOKEN_NOT_FOUND_STATUS;
import static org.mule.runtime.oauth.api.OAuthAuthorizationStatusCode.TOKEN_URL_CALL_FAILED_STATUS;
import static org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;
import static org.mule.service.http.api.HttpConstants.HttpStatus.BAD_REQUEST;
import static org.mule.service.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.mule.service.http.api.HttpConstants.HttpStatus.MOVED_TEMPORARILY;
import static org.mule.service.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.service.http.api.HttpConstants.Method.GET;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_LENGTH;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.service.http.api.HttpHeaders.Names.LOCATION;
import static org.mule.service.http.api.utils.HttpEncoderDecoderUtils.appendQueryParam;
import static org.mule.services.oauth.internal.OAuthConstants.CLIENT_ID_PARAMETER;
import static org.mule.services.oauth.internal.OAuthConstants.CLIENT_SECRET_PARAMETER;
import static org.mule.services.oauth.internal.OAuthConstants.CODE_PARAMETER;
import static org.mule.services.oauth.internal.OAuthConstants.GRANT_TYPE_AUTHENTICATION_CODE;
import static org.mule.services.oauth.internal.OAuthConstants.GRANT_TYPE_PARAMETER;
import static org.mule.services.oauth.internal.OAuthConstants.GRANT_TYPE_REFRESH_TOKEN;
import static org.mule.services.oauth.internal.OAuthConstants.REDIRECT_URI_PARAMETER;
import static org.mule.services.oauth.internal.OAuthConstants.REFRESH_TOKEN_PARAMETER;
import static org.mule.services.oauth.internal.OAuthConstants.STATE_PARAMETER;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.el.MuleExpressionLanguage;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.oauth.api.AuthorizationCodeOAuthDancer;
import org.mule.runtime.oauth.api.exception.TokenNotFoundException;
import org.mule.runtime.oauth.api.exception.TokenUrlResponseException;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;
import org.mule.service.http.api.HttpConstants;
import org.mule.service.http.api.HttpConstants.HttpStatus;
import org.mule.service.http.api.HttpConstants.Method;
import org.mule.service.http.api.client.HttpClient;
import org.mule.service.http.api.domain.ParameterMap;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.EmptyHttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.service.http.api.domain.message.response.HttpResponseBuilder;
import org.mule.service.http.api.server.HttpServer;
import org.mule.service.http.api.server.RequestHandler;
import org.mule.service.http.api.server.RequestHandlerManager;
import org.mule.service.http.api.server.async.HttpResponseReadyCallback;
import org.mule.service.http.api.server.async.ResponseStatusCallback;
import org.mule.services.oauth.internal.authorizationcode.AuthorizationRequestUrlBuilder;
import org.mule.services.oauth.internal.state.StateDecoder;
import org.mule.services.oauth.internal.state.StateEncoder;
import org.mule.services.oauth.internal.state.TokenResponse;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

/**
 * Provides OAuth dance support for authorization-code grant-type.
 * 
 * @since 4.0
 */
public class DefaultAuthorizationCodeOAuthDancer extends AbstractOAuthDancer implements AuthorizationCodeOAuthDancer, Lifecycle {

  private static final Logger LOGGER = getLogger(DefaultAuthorizationCodeOAuthDancer.class);

  private final HttpServer httpServer;

  private final String localCallbackUrlPath;

  private final String localAuthorizationUrlPath;
  private final String localAuthorizationUrlResourceOwnerId;

  private final String externalCallbackUrl;

  private final String state;
  private final String authorizationUrl;
  private final Map<String, String> customParameters;

  private RequestHandlerManager redirectUrlHandlerManager;
  private RequestHandlerManager localAuthorizationUrlHandlerManager;


  public DefaultAuthorizationCodeOAuthDancer(HttpServer httpServer, String clientId, String clientSecret,
                                             String tokenUrl, String scopes, String externalCallbackUrl, Charset encoding,
                                             String localCallbackUrlPath, String localAuthorizationUrlPath,
                                             String localAuthorizationUrlResourceOwnerId, String state, String authorizationUrl,
                                             String responseAccessTokenExpr, String responseRefreshTokenExpr,
                                             String responseExpiresInExpr, Map<String, String> customParameters,
                                             Map<String, String> customParametersExtractorsExprs,
                                             LockFactory lockProvider, Map<String, ResourceOwnerOAuthContext> tokensStore,
                                             HttpClient httpClient, MuleExpressionLanguage expressionEvaluator) {
    super(clientId, clientSecret, tokenUrl, encoding, scopes, responseAccessTokenExpr, responseRefreshTokenExpr,
          responseExpiresInExpr, customParametersExtractorsExprs, lockProvider, tokensStore, httpClient, expressionEvaluator);

    this.httpServer = httpServer;
    this.localCallbackUrlPath = localCallbackUrlPath;
    this.localAuthorizationUrlPath = localAuthorizationUrlPath;
    this.localAuthorizationUrlResourceOwnerId = localAuthorizationUrlResourceOwnerId;
    this.externalCallbackUrl = externalCallbackUrl;
    this.state = state;
    this.authorizationUrl = authorizationUrl;
    this.customParameters = customParameters;
  }

  @Override
  public void initialise() throws InitialisationException {
    redirectUrlHandlerManager = addRequestHandler(httpServer, GET, localCallbackUrlPath, createRedirectUrlListener());
    localAuthorizationUrlHandlerManager =
        addRequestHandler(httpServer, GET, localAuthorizationUrlPath, createLocalAuthorizationUrlListener());
  }

  private static <T> RequestHandlerManager addRequestHandler(HttpServer server, Method method, String path,
                                                             RequestHandler callbackHandler) {
    // MULE-11277 Support non-blocking in OAuth http listeners
    return server.addRequestHandler(singleton(method.name()), path, (requestContext, responseCallback) -> {
      final ClassLoader previousCtxClassLoader = currentThread().getContextClassLoader();
      try {
        currentThread().setContextClassLoader(DefaultAuthorizationCodeOAuthDancer.class.getClassLoader());

        callbackHandler.handleRequest(requestContext, responseCallback);
      } catch (Exception e) {
        LOGGER.error("Uncaught Exception on OAuth listener", e);
        sendErrorResponse(INTERNAL_SERVER_ERROR, e.getMessage(), responseCallback);
      } finally {
        currentThread().setContextClassLoader(previousCtxClassLoader);
      }
    });
  }

  private static void sendErrorResponse(final HttpConstants.HttpStatus status, String message,
                                        HttpResponseReadyCallback responseCallback) {
    responseCallback.responseReady(HttpResponse.builder()
        .setStatusCode(status.getStatusCode())
        .setReasonPhrase(status.getReasonPhrase())
        .setEntity(message != null ? new ByteArrayHttpEntity(message.getBytes()) : new EmptyHttpEntity())
        .addHeader(CONTENT_LENGTH, message != null ? valueOf(message.length()) : "0")
        .build(), new ResponseStatusCallback() {

          @Override
          public void responseSendFailure(Throwable exception) {
            LOGGER.warn("Error while sending {} response {}", status.getStatusCode(), exception.getMessage());
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Exception thrown", exception);
            }
          }

          @Override
          public void responseSendSuccessfully() {}
        });
  }

  private RequestHandler createRedirectUrlListener() {
    return (requestContext, responseCallback) -> {
      final HttpRequest request = requestContext.getRequest();
      final ParameterMap queryParams = request.getQueryParams();

      final String state = queryParams.get(STATE_PARAMETER);
      final StateDecoder stateDecoder = new StateDecoder(state);
      final String authorizationCode = queryParams.get(CODE_PARAMETER);
      if (authorizationCode == null) {
        LOGGER.info("HTTP Request to redirect URL done by the OAuth provider does not contains a code query parameter. "
            + "Code query parameter is required to get the access token.");
        LOGGER.error("Could not extract authorization code from OAuth provider HTTP request done to the redirect URL");

        sendResponse(stateDecoder, responseCallback, BAD_REQUEST,
                     "Failure retrieving access token.\n OAuth Server uri from callback: " + request.getUri(),
                     NO_AUTHORIZATION_CODE_STATUS);
        return;
      }

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Redirect url request state: " + state);
        LOGGER.debug("Redirect url request code: " + authorizationCode);
      }

      final Map<String, String> formData = new HashMap<>();
      formData.put(CODE_PARAMETER, authorizationCode);
      formData.put(CLIENT_ID_PARAMETER, clientId);
      formData.put(CLIENT_SECRET_PARAMETER, clientSecret);
      formData.put(GRANT_TYPE_PARAMETER, GRANT_TYPE_AUTHENTICATION_CODE);
      formData.put(REDIRECT_URI_PARAMETER, externalCallbackUrl);

      try {
        TokenResponse tokenResponse = invokeTokenUrl(tokenUrl, formData, null, true, encoding);

        String encodedResourceOwnerId = stateDecoder.decodeResourceOwnerId();
        final ResourceOwnerOAuthContext resourceOwnerOAuthContext =
            getContextForResourceOwner(encodedResourceOwnerId == null ? DEFAULT_RESOURCE_OWNER_ID : encodedResourceOwnerId);

        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Update OAuth Context for resourceOwnerId %s", resourceOwnerOAuthContext.getResourceOwnerId());
          LOGGER.debug("Retrieved access token, refresh token and expires from token url are: %s, %s, %s",
                       tokenResponse.getAccessToken(), tokenResponse.getRefreshToken(),
                       tokenResponse.getExpiresIn());
        }

        updateResourceOwnerState(resourceOwnerOAuthContext, stateDecoder.decodeOriginalState(), tokenResponse);
        updateResourceOwnerOAuthContext(resourceOwnerOAuthContext);

        sendResponse(stateDecoder, responseCallback, OK, "Successfully retrieved access token",
                     AUTHORIZATION_CODE_RECEIVED_STATUS);
      } catch (TokenUrlResponseException e) {
        LOGGER.error(e.getMessage());

        sendResponse(stateDecoder, responseCallback, INTERNAL_SERVER_ERROR,
                     format("Failure calling token url %s. Exception message is %s", tokenUrl, e.getMessage()),
                     TOKEN_URL_CALL_FAILED_STATUS);
        return;
      } catch (TokenNotFoundException e) {
        LOGGER.error(e.getMessage());

        sendResponse(stateDecoder, responseCallback, INTERNAL_SERVER_ERROR,
                     "Failed getting access token or refresh token from token URL response. See logs for details.",
                     TOKEN_NOT_FOUND_STATUS);
        return;
      }
    };
  }

  private static void sendResponse(StateDecoder stateDecoder, HttpResponseReadyCallback responseCallback,
                                   HttpStatus statusEmptyState, String message, int authorizationStatus) {
    String onCompleteRedirectToValue = stateDecoder.decodeOnCompleteRedirectTo();
    if (!isEmpty(onCompleteRedirectToValue)) {
      sendResponse(responseCallback, MOVED_TEMPORARILY, message, appendQueryParam(onCompleteRedirectToValue,
                                                                                  AUTHORIZATION_STATUS_QUERY_PARAM_KEY,
                                                                                  valueOf(authorizationStatus)));
    } else {
      sendResponse(responseCallback, statusEmptyState, message);
    }
  }

  private static void sendResponse(HttpResponseReadyCallback responseCallback, HttpStatus status, String message,
                                   String locationHeader) {
    HttpResponseBuilder httpResponseBuilder = HttpResponse.builder();
    httpResponseBuilder.setStatusCode(status.getStatusCode());
    httpResponseBuilder.setReasonPhrase(status.getReasonPhrase());
    httpResponseBuilder.setEntity(new ByteArrayHttpEntity(message.getBytes()));
    httpResponseBuilder.addHeader(CONTENT_LENGTH, valueOf(message.length()));
    httpResponseBuilder.addHeader(LOCATION, locationHeader);
    responseCallback.responseReady(httpResponseBuilder.build(), new ResponseStatusCallback() {

      @Override
      public void responseSendFailure(Throwable exception) {
        LOGGER.warn("Error while sending {} response {}", status.getStatusCode(), exception.getMessage());
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Exception thrown", exception);
        }
      }

      @Override
      public void responseSendSuccessfully() {}
    });
  }

  private static void sendResponse(HttpResponseReadyCallback responseCallback, HttpStatus status, String message) {
    HttpResponseBuilder httpResponseBuilder = HttpResponse.builder();
    httpResponseBuilder.setStatusCode(status.getStatusCode());
    httpResponseBuilder.setReasonPhrase(status.getReasonPhrase());
    httpResponseBuilder.setEntity(new ByteArrayHttpEntity(message.getBytes()));
    httpResponseBuilder.addHeader(CONTENT_LENGTH, valueOf(message.length()));
    responseCallback.responseReady(httpResponseBuilder.build(), new ResponseStatusCallback() {

      @Override
      public void responseSendFailure(Throwable exception) {
        LOGGER.warn("Error while sending {} response {}", status.getStatusCode(), exception.getMessage());
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Exception thrown", exception);
        }
      }

      @Override
      public void responseSendSuccessfully() {}
    });
  }

  private static boolean isEmpty(String value) {
    return value == null || org.mule.runtime.core.util.StringUtils.isEmpty(value) || "null".equals(value);
  }

  private RequestHandler createLocalAuthorizationUrlListener() {
    return (requestContext, responseCallback) -> {
      handleLocalAuthorizationRequest(requestContext.getRequest(), responseCallback);
    };
  }

  @Override
  public void handleLocalAuthorizationRequest(HttpRequest request, HttpResponseReadyCallback responseCallback) {
    final String body = readBody(request);
    final ParameterMap headers = readHeaders(request);
    final MediaType mediaType = getMediaType(request);
    final ParameterMap queryParams = request.getQueryParams();

    final String originalState = resolveExpression(state, body, headers, queryParams, mediaType);
    final StateEncoder stateEncoder = new StateEncoder(originalState);

    final String resourceOwnerId =
        resolveExpression(localAuthorizationUrlResourceOwnerId, body, headers, queryParams, mediaType);
    if (resourceOwnerId != null) {
      stateEncoder.encodeResourceOwnerIdInState(resourceOwnerId);
    }

    final String onCompleteRedirectToValue = queryParams.get("onCompleteRedirectTo");
    if (onCompleteRedirectToValue != null) {
      stateEncoder.encodeOnCompleteRedirectToInState(onCompleteRedirectToValue);
    }

    final String authorizationUrlWithParams = new AuthorizationRequestUrlBuilder()
        .setAuthorizationUrl(authorizationUrl)
        .setClientId(clientId)
        .setClientSecret(clientSecret)
        .setCustomParameters(customParameters)
        .setRedirectUrl(externalCallbackUrl)
        .setState(stateEncoder.getEncodedState())
        .setScope(scopes)
        .setEncoding(encoding)
        .buildUrl();

    sendResponse(responseCallback, MOVED_TEMPORARILY, body, authorizationUrlWithParams);
  }

  private String readBody(final HttpRequest request) {
    try {
      InputStreamHttpEntity inputStreamEntity = request.getInputStreamEntity();
      return inputStreamEntity != null ? IOUtils.toString(inputStreamEntity.getInputStream()) : "";
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
  }

  private ParameterMap readHeaders(final HttpRequest request) {
    ParameterMap headers = new ParameterMap();
    for (String headerName : request.getHeaderNames()) {
      headers.put(headerName, request.getHeaderValues(headerName));
    }
    return headers;
  }

  private MediaType getMediaType(final HttpRequest request) {
    String contentType = request.getHeaderValueIgnoreCase(CONTENT_TYPE);
    return contentType != null ? parse(contentType) : ANY;
  }

  @Override
  public void start() throws MuleException {
    super.start();
    try {
      httpServer.start();
    } catch (IOException e) {
      throw new DefaultMuleException(e);
    }
    redirectUrlHandlerManager.start();
    localAuthorizationUrlHandlerManager.start();
  }

  @Override
  public void stop() throws MuleException {
    redirectUrlHandlerManager.stop();
    localAuthorizationUrlHandlerManager.stop();
    httpServer.stop();
    super.stop();
  }

  @Override
  public void dispose() {
    redirectUrlHandlerManager.dispose();
    localAuthorizationUrlHandlerManager.dispose();
    httpServer.dispose();
  }

  @Override
  public CompletableFuture<Void> refreshToken(String resourceOwner) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Executing refresh token for user " + resourceOwner);
    }
    final ResourceOwnerOAuthContext resourceOwnerOAuthContext = getContextForResourceOwner(resourceOwner);
    final boolean lockWasAcquired = resourceOwnerOAuthContext.getRefreshUserOAuthContextLock().tryLock();
    try {
      if (lockWasAcquired) {
        final String userRefreshToken = resourceOwnerOAuthContext.getRefreshToken();
        if (userRefreshToken == null) {
          throw new MuleRuntimeException(createStaticMessage("The user with user id %s has no refresh token in his OAuth state so we can't execute the refresh token call",
                                                             resourceOwnerOAuthContext.getResourceOwnerId()));
        }

        final Map<String, String> formData = new HashMap<>();
        formData.put(REFRESH_TOKEN_PARAMETER, userRefreshToken);
        formData.put(CLIENT_ID_PARAMETER, clientId);
        formData.put(CLIENT_SECRET_PARAMETER, clientSecret);
        formData.put(GRANT_TYPE_PARAMETER, GRANT_TYPE_REFRESH_TOKEN);
        formData.put(REDIRECT_URI_PARAMETER, externalCallbackUrl);

        try {
          TokenResponse tokenResponse = invokeTokenUrl(tokenUrl, formData, null, true, encoding);
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Update OAuth Context for resourceOwnerId %s", resourceOwnerOAuthContext.getResourceOwnerId());
          }
          updateResourceOwnerState(resourceOwnerOAuthContext, null, tokenResponse);
          updateResourceOwnerOAuthContext(resourceOwnerOAuthContext);
        } catch (TokenUrlResponseException | TokenNotFoundException e) {
          final CompletableFuture<Void> exceptionFuture = new CompletableFuture<>();
          exceptionFuture.completeExceptionally(e);
          return exceptionFuture;
        }
      }
    } finally {
      if (lockWasAcquired) {
        resourceOwnerOAuthContext.getRefreshUserOAuthContextLock().unlock();
      }
    }
    if (!lockWasAcquired) {
      // if we couldn't acquire the lock then we wait until the other thread updates the token.
      resourceOwnerOAuthContext.getRefreshUserOAuthContextLock().lock();
      resourceOwnerOAuthContext.getRefreshUserOAuthContextLock().unlock();
    }
    return completedFuture(null);
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

}
