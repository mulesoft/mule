/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.oauth.internal;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.api.metadata.MediaType.parse;
import static org.mule.service.http.api.HttpConstants.HttpStatus.BAD_REQUEST;
import static org.mule.service.http.api.HttpConstants.Method.POST;
import static org.mule.service.http.api.HttpHeaders.Names.AUTHORIZATION;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.service.http.api.HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED;
import static org.mule.service.http.api.utils.HttpEncoderDecoderUtils.decodeUrlEncodedBody;
import static org.mule.service.http.api.utils.HttpEncoderDecoderUtils.encodeString;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionEvaluator;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.MapUtils;
import org.mule.runtime.oauth.api.exception.RequestAuthenticationException;
import org.mule.runtime.oauth.api.exception.TokenNotFoundException;
import org.mule.runtime.oauth.api.exception.TokenUrlResponseException;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;
import org.mule.service.http.api.client.HttpClient;
import org.mule.service.http.api.domain.ParameterMap;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.request.HttpRequestBuilder;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.oauth.internal.state.TokenResponse;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;

public abstract class AbstractOAuthDancer implements Startable, Stoppable {

  private static final int TOKEN_REQUEST_TIMEOUT_MILLIS = 60000;

  protected final String clientId;
  protected final String clientSecret;
  protected final String tokenUrl;
  protected final Charset encoding;
  protected final String scopes;

  protected final String responseAccessTokenExpr;
  protected final String responseRefreshTokenExpr;
  protected final String responseExpiresInExpr;
  protected final Map<String, String> customParametersExtractorsExprs;

  private final LockFactory lockProvider;
  private final Map<String, ResourceOwnerOAuthContext> tokensStore;
  private final HttpClient httpClient;
  private final ExpressionEvaluator expressionEvaluator;

  protected AbstractOAuthDancer(String clientId, String clientSecret, String tokenUrl, Charset encoding, String scopes,
                                String responseAccessTokenExpr, String responseRefreshTokenExpr, String responseExpiresInExpr,
                                Map<String, String> customParametersExtractorsExprs, LockFactory lockProvider,
                                Map<String, ResourceOwnerOAuthContext> tokensStore,
                                HttpClient httpClient, ExpressionEvaluator expressionEvaluator) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.tokenUrl = tokenUrl;
    this.encoding = encoding;
    this.scopes = scopes;
    this.responseAccessTokenExpr = responseAccessTokenExpr;
    this.responseRefreshTokenExpr = responseRefreshTokenExpr;
    this.responseExpiresInExpr = responseExpiresInExpr;
    this.customParametersExtractorsExprs = customParametersExtractorsExprs;

    this.lockProvider = lockProvider;
    this.tokensStore = tokensStore;
    this.httpClient = httpClient;
    this.expressionEvaluator = expressionEvaluator;
  }

  @Override
  public void start() throws MuleException {
    httpClient.start();
  }

  @Override
  public void stop() throws MuleException {
    httpClient.stop();
  }

  public String accessToken(String resourceOwner) throws RequestAuthenticationException {
    final String accessToken = getContextForResourceOwner(resourceOwner).getAccessToken();
    if (accessToken == null) {
      throw new RequestAuthenticationException(createStaticMessage(format("No access token found. "
          + "Verify that you have authenticated before trying to execute an operation to the API.")));
    }

    return accessToken;
  }

  protected TokenResponse invokeTokenUrl(String tokenUrl, Map<String, String> tokenRequestFormToSend, String authorization,
                                         boolean retrieveRefreshToken, Charset encoding)
      throws TokenUrlResponseException, TokenNotFoundException {
    try {
      final HttpRequestBuilder requestBuilder = HttpRequest.builder()
          .setUri(tokenUrl).setMethod(POST.name())
          .setEntity(new ByteArrayHttpEntity(encodeString(tokenRequestFormToSend, encoding).getBytes()))
          .addHeader(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED.toRfcString());

      if (authorization != null) {
        requestBuilder.addHeader(AUTHORIZATION, authorization);
      }

      // TODO MULE-11272 Support doing non-blocking requests
      final HttpResponse response = httpClient.send(requestBuilder.build(), TOKEN_REQUEST_TIMEOUT_MILLIS, true, null);

      MediaType responseContentType =
          response.getHeaderValueIgnoreCase(CONTENT_TYPE) != null ? parse(response.getHeaderValueIgnoreCase(CONTENT_TYPE)) : ANY;
      ParameterMap headers = new ParameterMap();
      for (String headerName : response.getHeaderNames()) {
        headers.put(headerName, response.getHeaderValues(headerName));
      }

      String readBody = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
      Object body = readBody;
      if (responseContentType.withoutParameters().matches(APPLICATION_X_WWW_FORM_URLENCODED)) {
        body = decodeUrlEncodedBody(readBody, responseContentType.getCharset().orElse(encoding));
      }

      if (response.getStatusCode() >= BAD_REQUEST.getStatusCode()) {
        throw new TokenUrlResponseException(tokenUrl);
      }

      TokenResponse tokenResponse = new TokenResponse();

      tokenResponse
          .setAccessToken(resolveExpression(responseAccessTokenExpr, body, headers, responseContentType));
      if (tokenResponse.getAccessToken() == null) {
        throw new TokenNotFoundException(body);
      }
      if (retrieveRefreshToken) {
        tokenResponse
            .setRefreshToken(resolveExpression(responseRefreshTokenExpr, body, headers, responseContentType));
      }
      tokenResponse.setExpiresIn(resolveExpression(responseExpiresInExpr, body, headers, responseContentType));

      if (!MapUtils.isEmpty(customParametersExtractorsExprs)) {
        Map<String, Object> customParams = new HashMap<>();
        for (Entry<String, String> customParamExpr : customParametersExtractorsExprs.entrySet()) {
          customParams.put(customParamExpr.getKey(),
                           resolveExpression(customParamExpr.getValue(), body, headers, responseContentType));
        }
        tokenResponse.setCustomResponseParameters(customParams);
      }

      return tokenResponse;
    } catch (IOException e) {
      throw new TokenUrlResponseException(tokenUrl, e);
    } catch (TimeoutException e) {
      throw new TokenUrlResponseException(tokenUrl, e);
    }
  }

  protected <T> T resolveExpression(String expr, Object body, ParameterMap headers, MediaType responseContentType) {
    if (expr == null) {
      return null;
    } else if (!expressionEvaluator.isExpression(expr)) {
      return (T) expr;
    } else {
      BindingContext resultContext = BindingContext.builder()
          .addBinding("payload",
                      new TypedValue(body, DataType.builder().fromObject(body)
                          .mediaType(responseContentType).build()))

          .addBinding("attributes", new TypedValue(Collections.singletonMap("headers", headers.toImmutableParameterMap()),
                                                   DataType.fromType(Map.class)))
          .addBinding("dataType",
                      new TypedValue(DataType.builder().fromObject(body).mediaType(responseContentType)
                          .build(), DataType.fromType(DataType.class)))
          .build();

      return (T) expressionEvaluator.evaluate(expr, resultContext).getValue();
    }
  }

  protected <T> T resolveExpression(String expr, Object body, ParameterMap headers, ParameterMap queryParams,
                                    MediaType responseContentType) {
    if (expr == null) {
      return null;
    } else if (!expressionEvaluator.isExpression(expr)) {
      return (T) expr;
    } else {
      Map<Object, Object> attributes = new HashMap<>(2);
      attributes.put("headers", headers.toImmutableParameterMap());
      attributes.put("queryParams", queryParams.toImmutableParameterMap());

      BindingContext resultContext = BindingContext.builder()
          .addBinding("payload",
                      new TypedValue(body, DataType.builder().fromObject(body)
                          .mediaType(responseContentType).build()))

          .addBinding("attributes", new TypedValue(attributes, DataType.fromType(Map.class)))
          .addBinding("dataType",
                      new TypedValue(DataType.builder().fromObject(body).mediaType(responseContentType)
                          .build(), DataType.fromType(DataType.class)))
          .build();

      return (T) expressionEvaluator.evaluate(expr, resultContext).getValue();
    }
  }

  /**
   * Retrieves the oauth context for a particular user. If there's no state for that user a new state is retrieve so never returns
   * null.
   *
   * @param resourceOwnerId id of the user.
   * @return oauth state
   */
  protected ResourceOwnerOAuthContext getContextForResourceOwner(final String resourceOwnerId) {
    ResourceOwnerOAuthContext resourceOwnerOAuthContext = null;
    if (!tokensStore.containsKey(resourceOwnerId)) {
      final Lock lock = lockProvider.createLock(toString() + "-config-oauth-context");
      lock.lock();
      try {
        if (!tokensStore.containsKey(resourceOwnerId)) {
          resourceOwnerOAuthContext = new ResourceOwnerOAuthContext(createLockForResourceOwner(resourceOwnerId), resourceOwnerId);
          tokensStore.put(resourceOwnerId, resourceOwnerOAuthContext);
        }
      } finally {
        lock.unlock();
      }
    }
    if (resourceOwnerOAuthContext == null) {
      resourceOwnerOAuthContext = tokensStore.get(resourceOwnerId);
      resourceOwnerOAuthContext.setRefreshUserOAuthContextLock(createLockForResourceOwner(resourceOwnerId));
    }
    return resourceOwnerOAuthContext;
  }

  private Lock createLockForResourceOwner(String resourceOwnerId) {
    return lockProvider.createLock(toString() + "-" + resourceOwnerId);
  }

  /**
   * Updates the resource owner oauth context information
   *
   * @param resourceOwnerOAuthContext
   */
  protected void updateResourceOwnerOAuthContext(ResourceOwnerOAuthContext resourceOwnerOAuthContext) {
    final Lock resourceOwnerContextLock = resourceOwnerOAuthContext.getRefreshUserOAuthContextLock();
    resourceOwnerContextLock.lock();
    try {
      tokensStore.put(resourceOwnerOAuthContext.getResourceOwnerId(), resourceOwnerOAuthContext);
    } finally {
      resourceOwnerContextLock.unlock();
    }
  }

}
