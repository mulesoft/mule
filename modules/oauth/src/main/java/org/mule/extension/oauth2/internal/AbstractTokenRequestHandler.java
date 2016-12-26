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
import static org.mule.extension.oauth2.internal.OAuthConstants.DEFAULT_REFRESH_TOKEN_WHEN_EXPRESSION;
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
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.client.HttpClient;
import org.mule.service.http.api.client.HttpClientConfiguration;
import org.mule.service.http.api.domain.message.response.HttpResponse;

import java.io.IOException;
import java.net.CookieManager;
import java.util.concurrent.TimeoutException;

public abstract class AbstractTokenRequestHandler implements MuleContextAware, Initialisable, Startable, Stoppable {

  protected MuleContext muleContext;
  private String refreshTokenWhen = DEFAULT_REFRESH_TOKEN_WHEN_EXPRESSION;
  private String tokenUrl;
  private TlsContextFactory tlsContextFactory;

  private HttpClient client;
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

  /**
   * @param refreshTokenWhen expression to use to determine if the response from a request to the API requires a new token
   */
  public void setRefreshTokenWhen(String refreshTokenWhen) {
    this.refreshTokenWhen = refreshTokenWhen;
  }

  public String getRefreshTokenWhen() {
    return refreshTokenWhen;
  }

  @Override
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
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

      final MuleEventToHttpRequest toHttpRequest =
          new MuleEventToHttpRequest(REQUESTER_NO_COOKIE_CONFIG, tokenUrl, POST.name(), NEVER, ALWAYS, null);
      // TODO MULE-11272 Support doing non-blocking requests
      final HttpResponse response =
          client.send(toHttpRequest.create(event, requestBuilder, null, muleContext), 60000, true, null);

      HttpResponseToMuleMessage httpResponseToMuleMessage =
          new HttpResponseToMuleMessage(REQUESTER_NO_COOKIE_CONFIG, true, muleContext);
      Message responseMessage = httpResponseToMuleMessage.convert(event, response, tokenUrl);
      Event responseEvent = Event.builder(event).message(InternalMessage.builder(responseMessage).build()).build();

      if (((HttpResponseAttributes) responseMessage.getAttributes()).getStatusCode() >= BAD_REQUEST.getStatusCode()) {
        throw new TokenUrlResponseException(responseEvent);
      }

      return responseEvent;
    } catch (IOException e) {
      throw new DefaultMuleException(e);
    } catch (TimeoutException e) {
      throw new DefaultMuleException(e);
    }
  }

  protected String getTokenUrl() {
    return tokenUrl;
  }

  protected class TokenUrlResponseException extends Exception {

    private Event tokenUrlResponse;

    public TokenUrlResponseException(final Event tokenUrlResponse) {
      this.tokenUrlResponse = tokenUrlResponse;
    }

    public Event getTokenUrlResponse() {
      return tokenUrlResponse;
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    String threadNamePrefix = format("%soauthToken.requester", getPrefix(muleContext));

    HttpClientConfiguration clientConfiguration = new HttpClientConfiguration.Builder()
        .setTlsContextFactory(tlsContextFactory)
        .setThreadNamePrefix(threadNamePrefix)
        .build();

    try {
      client = muleContext.getRegistry().lookupObject(HttpService.class).getClientFactory().create(clientConfiguration);
    } catch (RegistrationException e) {
      throw new InitialisationException(e, this);
    }
  }

  @Override
  public void start() throws MuleException {
    client.start();
  }

  @Override
  public void stop() {
    client.stop();
  }
}
