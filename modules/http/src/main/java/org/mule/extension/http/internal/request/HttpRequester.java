/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request;

import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_REQUEST_BEGIN;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_REQUEST_END;
import static org.mule.runtime.module.http.api.HttpConstants.Protocols.HTTPS;

import org.mule.extension.http.api.HttpSendBodyMode;
import org.mule.extension.http.api.HttpStreamingType;
import org.mule.extension.http.api.request.authentication.HttpAuthentication;
import org.mule.extension.http.api.request.authentication.UsernamePasswordAuthentication;
import org.mule.extension.http.api.request.builder.HttpRequesterRequestBuilder;
import org.mule.extension.http.api.request.client.HttpClient;
import org.mule.extension.http.api.request.client.UriParameters;
import org.mule.extension.http.api.request.validator.ResponseValidator;
import org.mule.extension.http.internal.request.validator.HttpRequesterConfig;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.context.notification.ConnectorMessageNotification;
import org.mule.runtime.core.context.notification.NotificationHelper;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.module.http.internal.domain.request.HttpRequest;
import org.mule.runtime.module.http.internal.domain.request.HttpRequestAuthentication;
import org.mule.runtime.module.http.internal.domain.response.HttpResponse;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component capable of performing an HTTP request given a {@link Event}.
 *
 * @since 4.0
 */
public class HttpRequester {

  private static final Logger logger = LoggerFactory.getLogger(HttpRequester.class);
  private static final String REMOTELY_CLOSED = "Remotely closed";

  private final boolean followRedirects;
  private final HttpAuthentication authentication;
  private final boolean parseResponse;
  private final int responseTimeout;
  private final ResponseValidator responseValidator;

  private final HttpRequesterConfig config;
  private final NotificationHelper notificationHelper;
  private final MuleEventToHttpRequest eventToHttpRequest;

  public HttpRequester(MuleEventToHttpRequest eventToHttpRequest, boolean followRedirects, HttpAuthentication authentication,
                       boolean parseResponse, int responseTimeout, ResponseValidator responseValidator,
                       HttpRequesterConfig config) {
    this.followRedirects = followRedirects;
    this.authentication = authentication;
    this.parseResponse = parseResponse;
    this.responseTimeout = responseTimeout;
    this.responseValidator = responseValidator;
    this.config = config;
    this.eventToHttpRequest = eventToHttpRequest;
    this.notificationHelper =
        new NotificationHelper(config.getMuleContext().getNotificationManager(), ConnectorMessageNotification.class, false);
  }

  public Message doRequest(Event muleEvent, HttpClient client, HttpRequesterRequestBuilder requestBuilder,
                           boolean checkRetry, MuleContext muleContext, FlowConstruct flowConstruct)
      throws MuleException {
    HttpRequest httpRequest = eventToHttpRequest.create(muleEvent, requestBuilder, authentication, muleContext);

    HttpResponse response;
    try {
      notificationHelper.fireNotification(this, muleEvent, httpRequest.getUri(), flowConstruct, MESSAGE_REQUEST_BEGIN);
      response = client.send(httpRequest, responseTimeout, followRedirects, resolveAuthentication(authentication));
    } catch (Exception e) {
      checkIfRemotelyClosed(e, client.getDefaultUriParameters());
      throw new MessagingException(CoreMessages.createStaticMessage(getErrorMessage(httpRequest)), muleEvent, e);
    }

    HttpResponseToMuleMessage httpResponseToMuleMessage = new HttpResponseToMuleMessage(config, parseResponse, muleContext);
    Message responseMessage = httpResponseToMuleMessage.convert(muleEvent, response, httpRequest.getUri());

    // Create a new muleEvent based on the old and the response so that the auth can use it
    Event responseEvent =
        Event.builder(muleEvent).message(InternalMessage.builder(responseMessage).build())
            .synchronous(muleEvent.isSynchronous()).build();
    if (resendRequest(responseEvent, checkRetry, authentication)) {
      consumePayload(responseEvent, muleContext);
      responseMessage = doRequest(responseEvent, client, requestBuilder, false, muleContext, flowConstruct);
    }
    notificationHelper.fireNotification(this, muleEvent, httpRequest.getUri(), flowConstruct, MESSAGE_REQUEST_END);
    responseValidator.validate(responseMessage, muleContext);
    return responseMessage;
  }

  private String getErrorMessage(HttpRequest httpRequest) {
    return String.format("Error sending HTTP request to %s", httpRequest.getUri());
  }

  private boolean resendRequest(Event muleEvent, boolean retry, HttpAuthentication authentication) throws MuleException {
    return retry && authentication != null && authentication.shouldRetry(muleEvent);
  }

  private void consumePayload(final Event event, MuleContext muleContext) {
    if (event.getMessage().getPayload().getValue() instanceof InputStream) {
      try {
        event.getMessageAsBytes(muleContext);
      } catch (Exception e) {
        throw new MuleRuntimeException(e);
      }
    }
  }

  private HttpRequestAuthentication resolveAuthentication(HttpAuthentication authentication) {
    HttpRequestAuthentication requestAuthentication = null;
    if (authentication instanceof UsernamePasswordAuthentication) {
      requestAuthentication = ((UsernamePasswordAuthentication) authentication).buildRequestAuthentication();
    }
    return requestAuthentication;
  }

  private void checkIfRemotelyClosed(Exception exception, UriParameters uriParameters) {
    if (HTTPS.getScheme().equals(uriParameters.getScheme())
        && StringUtils.containsIgnoreCase(exception.getMessage(), REMOTELY_CLOSED)) {
      logger
          .error("Remote host closed connection. Possible SSL/TLS handshake issue. Check protocols, cipher suites and certificate set up. Use -Djavax.net.debug=handshake for further debugging.");
    }
  }

  public static class Builder {

    private String uri;
    private String method;
    private boolean followRedirects;
    private HttpStreamingType requestStreamingMode;
    private HttpSendBodyMode sendBodyMode;
    private String source;
    private HttpAuthentication authentication;

    private int responseTimeout;
    private boolean parseResponse;
    private ResponseValidator responseValidator;

    private HttpRequesterConfig config;

    public Builder setUri(String uri) {
      this.uri = uri;
      return this;
    }

    public Builder setMethod(String method) {
      this.method = method;
      return this;
    }

    public Builder setFollowRedirects(boolean followRedirects) {
      this.followRedirects = followRedirects;
      return this;
    }

    public Builder setRequestStreamingMode(HttpStreamingType requestStreamingMode) {
      this.requestStreamingMode = requestStreamingMode;
      return this;
    }

    public Builder setSendBodyMode(HttpSendBodyMode sendBodyMode) {
      this.sendBodyMode = sendBodyMode;
      return this;
    }

    public Builder setSource(String source) {
      this.source = source;
      return this;
    }

    public Builder setAuthentication(HttpAuthentication authentication) {
      this.authentication = authentication;
      return this;
    }

    public Builder setParseResponse(boolean parseResponse) {
      this.parseResponse = parseResponse;
      return this;
    }

    public Builder setResponseTimeout(int responseTimeout) {
      this.responseTimeout = responseTimeout;
      return this;
    }

    public Builder setResponseValidator(ResponseValidator responseValidator) {
      this.responseValidator = responseValidator;
      return this;
    }

    public Builder setConfig(HttpRequesterConfig config) {
      this.config = config;
      return this;
    }

    public HttpRequester build() {
      MuleEventToHttpRequest eventToHttpRequest =
          new MuleEventToHttpRequest(config, uri, method, requestStreamingMode, sendBodyMode, source);
      return new HttpRequester(eventToHttpRequest, followRedirects, authentication, parseResponse, responseTimeout,
                               responseValidator, config);
    }
  }
}
