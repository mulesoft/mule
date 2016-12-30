/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal;

import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static org.mule.extension.http.api.HttpConstants.HttpStatus.BAD_REQUEST;
import static org.mule.extension.http.api.HttpConstants.Methods.POST;
import static org.mule.extension.http.api.HttpHeaders.Names.AUTHORIZATION;
import static org.mule.extension.http.api.HttpSendBodyMode.ALWAYS;
import static org.mule.extension.http.api.HttpStreamingType.NEVER;
import static org.mule.extension.oauth2.internal.OAuthConstants.ACCESS_TOKEN_EXPRESSION;
import static org.mule.extension.oauth2.internal.OAuthConstants.EXPIRATION_TIME_EXPRESSION;
import static org.mule.extension.oauth2.internal.OAuthConstants.REFRESH_TOKEN_EXPRESSION;
import static org.mule.runtime.core.util.concurrent.ThreadNameHelper.getPrefix;

import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.extension.http.api.request.builder.HttpRequesterRequestBuilder;
import org.mule.extension.http.internal.request.HttpRequesterCookieConfig;
import org.mule.extension.http.internal.request.HttpResponseToMuleMessage;
import org.mule.extension.http.internal.request.MuleEventToHttpRequest;
import org.mule.extension.oauth2.internal.clientcredentials.OAuthAuthorizationAttributes;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.Event.Builder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.util.CollectionUtils;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
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
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTokenRequestHandler implements Initialisable, Startable, Stoppable, MuleContextAware {

  protected Logger logger = LoggerFactory.getLogger(getClass());

  protected MuleContext muleContext;

  private HttpService httpService;

  /**
   * MEL expression to extract the access token parameter from the response of the call to tokenUrl.
   */
  @Parameter
  @Optional(defaultValue = ACCESS_TOKEN_EXPRESSION)
  protected Function<Event, String> responseAccessToken;

  @Parameter
  @Optional(defaultValue = REFRESH_TOKEN_EXPRESSION)
  protected Function<Event, String> responseRefreshToken;

  /**
   * MEL expression to extract the expiresIn parameter from the response of the call to tokenUrl.
   */
  @Parameter
  @Optional(defaultValue = EXPIRATION_TIME_EXPRESSION)
  protected Function<Event, String> responseExpiresIn;

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
  @Optional(defaultValue = "#[message.attributes.statusCode == 401 || message.attributes.statusCode == 403]")
  private Function<Event, Boolean> refreshTokenWhen;

  /**
   * The oauth authentication server url to get access to the token. Mule, after receiving the authentication code from the oauth
   * server (through the redirectUrl) will call this url to get the access token.
   */
  @Parameter
  private String tokenUrl;

  private TlsContextFactory tlsContextFactory;

  private HttpClient client;
  private MuleEventToHttpRequest eventToHttpRequest;
  private HttpResponseToMuleMessage httpResponseToMuleMessage;
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

  public Function<Event, Boolean> getRefreshTokenWhen() {
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

  protected Event invokeTokenUrl(final Event event) throws MuleException, TokenUrlResponseException {
    try {
      final HttpRequesterRequestBuilder requestBuilder = new HttpRequesterRequestBuilder();
      requestBuilder.setBody(event.getMessage().getPayload().getValue());

      final Attributes attributes = event.getMessage().getAttributes();
      if (attributes instanceof OAuthAuthorizationAttributes
          && ((OAuthAuthorizationAttributes) attributes).getAuthorization() != null) {
        requestBuilder.setHeaders(singletonMap(AUTHORIZATION, ((OAuthAuthorizationAttributes) attributes).getAuthorization()));
      }

      // TODO MULE-11272 Support doing non-blocking requests
      final HttpResponse response =
          client.send(eventToHttpRequest.create(event, requestBuilder, null, muleContext), TOKEN_REQUEST_TIMEOUT_MILLIS, true,
                      null);

      Message responseMessage = httpResponseToMuleMessage.convert(event, response, tokenUrl);
      final Builder responseEventBuilder = Event.builder(event).message((InternalMessage) responseMessage);

      if (((HttpResponseAttributes) responseMessage.getAttributes()).getStatusCode() >= BAD_REQUEST.getStatusCode()) {
        throw new TokenUrlResponseException(responseEventBuilder.build());
      }

      if (responseMessage.getPayload().getDataType().isStreamType()) {
        return responseEventBuilder.message(InternalMessage.builder(responseMessage)
            .payload(org.mule.runtime.core.util.IOUtils.toString((InputStream) responseMessage.getPayload().getValue())).build())
            .build();
      } else {
        return responseEventBuilder.build();
      }
    } catch (IOException e) {
      throw new DefaultMuleException(e);
    } catch (TimeoutException e) {
      throw new DefaultMuleException(e);
    }
  }

  protected String getTokenUrl() {
    return tokenUrl;
  }

  public TokenResponse processTokenResponse(Event muleEvent, boolean retrieveRefreshToken) {
    TokenResponse response = new TokenResponse();

    response.accessToken = responseAccessToken.apply(muleEvent);
    response.accessToken = isEmpty(response.accessToken) ? null : response.accessToken;
    if (response.accessToken == null) {
      logger.error("Could not extract access token from token URL. "
          + "Expressions used to retrieve access token was " + responseAccessToken);
    }
    if (retrieveRefreshToken) {
      response.refreshToken = responseRefreshToken.apply(muleEvent);
      response.refreshToken = isEmpty(response.refreshToken) ? null : response.refreshToken;
    }
    response.expiresIn = responseExpiresIn.apply(muleEvent);
    if (!CollectionUtils.isEmpty(parameterExtractors)) {
      for (ParameterExtractor parameterExtractor : parameterExtractors) {
        response.customResponseParameters.put(parameterExtractor.getParamName(),
                                              parameterExtractor.getValue().apply(muleEvent));
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

  protected class TokenUrlResponseException extends Exception {

    private static final long serialVersionUID = -570499835977961241L;

    private Event tokenUrlResponse;

    public TokenUrlResponseException(final Event tokenUrlResponse) {
      this.tokenUrlResponse = tokenUrlResponse;
    }

    public Event getTokenUrlResponse() {
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

    eventToHttpRequest = new MuleEventToHttpRequest(REQUESTER_NO_COOKIE_CONFIG, tokenUrl, POST.name(), NEVER, ALWAYS, null);
    httpResponseToMuleMessage = new HttpResponseToMuleMessage(REQUESTER_NO_COOKIE_CONFIG, true, muleContext);

    String threadNamePrefix = format("%soauthToken.requester", getPrefix(muleContext));
    HttpClientConfiguration clientConfiguration = new HttpClientConfiguration.Builder()
        .setTlsContextFactory(tlsContextFactory)
        .setThreadNamePrefix(threadNamePrefix)
        .build();

    client = httpService.getClientFactory().create(clientConfiguration);
  }

  @Override
  public void start() throws MuleException {
    client.start();
  }

  @Override
  public void stop() {
    client.stop();
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }
}
