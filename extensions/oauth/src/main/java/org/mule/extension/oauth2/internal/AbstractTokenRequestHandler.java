/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal;

import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static org.mule.extension.http.api.HttpSendBodyMode.ALWAYS;
import static org.mule.extension.http.api.HttpStreamingType.NEVER;
import static org.mule.extension.oauth2.api.exception.OAuthErrors.TOKEN_URL_FAIL;
import static org.mule.extension.oauth2.internal.OAuthConstants.ACCESS_TOKEN_EXPRESSION;
import static org.mule.extension.oauth2.internal.OAuthConstants.EXPIRATION_TIME_EXPRESSION;
import static org.mule.extension.oauth2.internal.OAuthConstants.REFRESH_TOKEN_EXPRESSION;
import static org.mule.runtime.api.metadata.DataType.fromObject;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.core.util.concurrent.ThreadNameHelper.getPrefix;
import static org.mule.service.http.api.HttpConstants.HttpStatus.BAD_REQUEST;
import static org.mule.service.http.api.HttpConstants.Methods.POST;
import static org.mule.service.http.api.HttpHeaders.Names.AUTHORIZATION;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.extension.http.api.request.builder.HttpRequesterRequestBuilder;
import org.mule.extension.http.internal.request.HttpRequestFactory;
import org.mule.extension.http.internal.request.HttpRequesterCookieConfig;
import org.mule.extension.http.internal.request.HttpResponseToResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.util.CollectionUtils;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.ParameterResolver;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.client.HttpClient;
import org.mule.service.http.api.client.HttpClientConfiguration;
import org.mule.service.http.api.domain.message.response.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;

//TODO MULE-11412 Remove MuleContextAware
public abstract class AbstractTokenRequestHandler implements Initialisable, Startable, Stoppable, MuleContextAware {

  private static final Logger LOGGER = getLogger(AbstractTokenRequestHandler.class);

  // TODO MULE-11412 Add @Inject
  protected MuleContext muleContext;
  private DeferredExpressionResolver resolver;

  // TODO MULE-11412 Add @Inject
  private HttpService httpService;

  /**
   * MEL expression to extract the access token parameter from the response of the call to tokenUrl.
   */
  @Parameter
  @Optional(defaultValue = ACCESS_TOKEN_EXPRESSION)
  protected ParameterResolver<String> responseAccessToken;

  @Parameter
  @Optional(defaultValue = REFRESH_TOKEN_EXPRESSION)
  protected ParameterResolver<String> responseRefreshToken;

  /**
   * MEL expression to extract the expiresIn parameter from the response of the call to tokenUrl.
   */
  @Parameter
  @Optional(defaultValue = EXPIRATION_TIME_EXPRESSION)
  protected ParameterResolver<String> responseExpiresIn;

  @Parameter
  @Alias("custom-parameter-extractors")
  @Optional
  protected List<ParameterExtractor> parameterExtractors;

  /**
   * After executing an API call authenticated with OAuth it may be that the access token used was expired, so this attribute
   * allows a MEL expressions that will be evaluated against the http response of the API callback to determine if the request
   * failed because it was done using an expired token. In case the evaluation returns true (access token expired) then mule will
   * automatically trigger a refresh token flow and retry the API callback using a new access token. Default value evaluates if
   * the response status code was 401 or 403.
   */
  @Parameter
  @Optional(defaultValue = "#[attributes.statusCode == 401 or attributes.statusCode == 403]")
  private ParameterResolver<Boolean> refreshTokenWhen;

  /**
   * The oauth authentication server url to get access to the token. Mule, after receiving the authentication code from the oauth
   * server (through the redirectUrl) will call this url to get the access token.
   */
  @Parameter
  private String tokenUrl;

  private TlsContextFactory tlsContextFactory;

  private HttpClient client;
  private HttpRequestFactory eventToHttpRequest;
  private HttpResponseToResult httpResponseToMuleMessage;

  private static final int TOKEN_REQUEST_TIMEOUT_MILLIS = 60000;
  private static final HttpRequesterCookieConfig REQUESTER_NO_COOKIE_CONFIG = new HttpRequesterCookieConfig() {

    @Override
    public boolean isEnableCookies() {
      return false;
    }

    @Override
    public CookieManager getCookieManager() {
      return null;
    }
  };

  public ParameterResolver<Boolean> getRefreshTokenWhen() {
    return refreshTokenWhen;
  }

  protected MuleContext getMuleContext() {
    return muleContext;
  }

  public void setTokenUrl(String tokenUrl) {
    this.tokenUrl = tokenUrl;
  }

  public void setTlsContextFactory(final TlsContextFactory tlsContextFactory) {
    this.tlsContextFactory = tlsContextFactory;
  }

  protected Result<Object, HttpResponseAttributes> invokeTokenUrl(Map<String, String> tokenRequestFormToSend,
                                                                  String authorization)
      throws MuleException, TokenUrlResponseException {
    try {
      final HttpRequesterRequestBuilder requestBuilder = new HttpRequesterRequestBuilder();
      requestBuilder.setBody(new TypedValue<>(tokenRequestFormToSend, fromObject(tokenRequestFormToSend)));

      if (authorization != null) {
        requestBuilder.setHeaders(singletonMap(AUTHORIZATION, authorization));
      }

      // TODO MULE-11272 Support doing non-blocking requests
      final HttpResponse response = client.send(eventToHttpRequest.create(requestBuilder, null, muleContext),
                                                TOKEN_REQUEST_TIMEOUT_MILLIS, true, null);

      Result<Object, HttpResponseAttributes> responseResult = httpResponseToMuleMessage.convert(ANY, response, tokenUrl);
      final Result.Builder<Object, HttpResponseAttributes> responseBuilder =
          Result.<Object, HttpResponseAttributes>builder(responseResult);

      if (responseResult.getAttributes().get().getStatusCode() >= BAD_REQUEST.getStatusCode()) {
        throw new TokenUrlResponseException(getTokenUrl(), responseBuilder.build());
      }

      if (responseResult.getOutput() instanceof InputStream) {
        return responseBuilder.output(IOUtils.toString((InputStream) responseResult.getOutput())).build();
      } else {
        return responseBuilder.build();
      }
    } catch (IOException e) {
      throw new TokenUrlResponseException(e, getTokenUrl());
    } catch (TimeoutException e) {
      throw new TokenUrlResponseException(e, getTokenUrl());
    }
  }

  private InternalMessage buildMessageFromResult(Result<Object, HttpResponseAttributes> responseResult, Object payload) {
    return InternalMessage.builder()
        .payload(payload)
        .attributes(responseResult.getAttributes().get())
        .mediaType(responseResult.getMediaType().orElse(ANY))
        .build();
  }

  protected String getTokenUrl() {
    return tokenUrl;
  }

  public TokenResponse processTokenResponse(Result<Object, HttpResponseAttributes> tokenUrlResponse,
                                            boolean retrieveRefreshToken) {
    TokenResponse response = new TokenResponse();

    response.accessToken = resolver.resolveExpression(responseAccessToken, tokenUrlResponse);
    response.accessToken = isEmpty(response.accessToken) ? null : response.accessToken;
    if (response.accessToken == null) {
      LOGGER.error("Could not extract access token from token URL. "
          + "Expressions used to retrieve access token was " + responseAccessToken);
    }
    if (retrieveRefreshToken) {
      response.refreshToken = resolver.resolveExpression(responseRefreshToken, tokenUrlResponse);
      response.refreshToken = isEmpty(response.refreshToken) ? null : response.refreshToken;
    }
    response.expiresIn = resolver.resolveExpression(responseExpiresIn, tokenUrlResponse);
    if (!CollectionUtils.isEmpty(parameterExtractors)) {
      for (ParameterExtractor parameterExtractor : parameterExtractors) {
        response.customResponseParameters.put(parameterExtractor.getParamName(),
                                              resolver.resolveExpression(parameterExtractor.getValue(), tokenUrlResponse));
      }
    }

    return response;
  }

  protected boolean tokenResponseContentIsValid(TokenResponse response) {
    return response.getAccessToken() != null;
  }

  protected boolean isEmpty(String value) {
    return value == null || org.mule.runtime.core.util.StringUtils.isEmpty(value) || "null".equals(value);
  }

  protected class TokenUrlResponseException extends ModuleException {

    private static final long serialVersionUID = -570499835977961241L;

    private Object tokenUrlResponse;

    public TokenUrlResponseException(String tokenUrl, Result<Object, HttpResponseAttributes> build) {
      super(format("HTTP response from token URL %s returned a failure status code", tokenUrl), TOKEN_URL_FAIL);
      this.tokenUrlResponse = build.getOutput();
    }

    public TokenUrlResponseException(final Exception cause, String tokenUrl) {
      super(cause, TOKEN_URL_FAIL, format("Exception when calling token URL %s", tokenUrl));
    }

    public Object getTokenUrlResponse() {
      return tokenUrlResponse;
    }
  }

  protected static class TokenResponse {

    private String accessToken;
    private String refreshToken;
    private String expiresIn;
    private Map<String, Object> customResponseParameters = new HashMap<>();

    public String getAccessToken() {
      return accessToken;
    }

    public String getRefreshToken() {
      return refreshToken;
    }

    public String getExpiresIn() {
      return expiresIn;
    }

    public Map<String, Object> getCustomResponseParameters() {
      return customResponseParameters;
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    try {
      this.httpService = muleContext.getRegistry().lookupObject(HttpService.class);
    } catch (RegistrationException e) {
      throw new InitialisationException(e, this);
    }

    eventToHttpRequest = new HttpRequestFactory(REQUESTER_NO_COOKIE_CONFIG, tokenUrl, POST.name(), NEVER, ALWAYS, null);
    httpResponseToMuleMessage = new HttpResponseToResult(REQUESTER_NO_COOKIE_CONFIG, true, muleContext);

    String threadNamePrefix = format("%soauthToken.requester", getPrefix(muleContext));
    HttpClientConfiguration clientConfiguration = new HttpClientConfiguration.Builder()
        .setTlsContextFactory(tlsContextFactory)
        .setThreadNamePrefix(threadNamePrefix)
        .build();

    client = httpService.getClientFactory().create(clientConfiguration);
  }

  @Override
  public void start() {
    client.start();
  }

  @Override
  public void stop() {
    client.stop();
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
    this.resolver = new DeferredExpressionResolver(context);
  }
}
