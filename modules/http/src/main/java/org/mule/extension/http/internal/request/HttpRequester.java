/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request;

import static org.mule.runtime.core.config.i18n.CoreMessages.createStaticMessage;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_REQUEST_BEGIN;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_REQUEST_END;
import static org.mule.runtime.module.http.api.HttpConstants.Protocols.HTTPS;

import java.io.InputStream;

import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.extension.http.api.HttpSendBodyMode;
import org.mule.extension.http.api.HttpStreamingType;
import org.mule.extension.http.api.request.authentication.HttpAuthentication;
import org.mule.extension.http.api.request.authentication.UsernamePasswordAuthentication;
import org.mule.extension.http.api.request.builder.HttpRequesterRequestBuilder;
import org.mule.extension.http.api.request.client.UriParameters;
import org.mule.extension.http.api.request.validator.ResponseValidator;
import org.mule.extension.http.internal.request.client.HttpExtensionClient;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.TransformationService;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.context.notification.ConnectorMessageNotification;
import org.mule.runtime.core.context.notification.NotificationHelper;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.service.http.api.client.HttpRequestAuthentication;
import org.mule.service.http.api.client.async.ResponseHandler;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
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
  private final Scheduler scheduler;

  public HttpRequester(MuleEventToHttpRequest eventToHttpRequest, boolean followRedirects, HttpAuthentication authentication,
                       boolean parseResponse, int responseTimeout, ResponseValidator responseValidator,
                       HttpRequesterConfig config, Scheduler scheduler) {
    this.followRedirects = followRedirects;
    this.authentication = authentication;
    this.parseResponse = parseResponse;
    this.responseTimeout = responseTimeout;
    this.responseValidator = responseValidator;
    this.config = config;
    this.scheduler = scheduler;
    this.eventToHttpRequest = eventToHttpRequest;
    this.notificationHelper =
        new NotificationHelper(config.getMuleContext().getNotificationManager(), ConnectorMessageNotification.class, false);
  }

  public void doRequest(Event muleEvent, HttpExtensionClient client, HttpRequesterRequestBuilder requestBuilder,
                        boolean checkRetry, MuleContext muleContext, FlowConstruct flowConstruct,
                        CompletionCallback<Object, HttpResponseAttributes> callback)
      throws MuleException {
    HttpRequest httpRequest = eventToHttpRequest.create(muleEvent, requestBuilder, authentication, muleContext);

    notificationHelper.fireNotification(this, muleEvent, httpRequest.getUri(), flowConstruct, MESSAGE_REQUEST_BEGIN);
    client.send(httpRequest, responseTimeout, followRedirects, resolveAuthentication(authentication),
                createResponseHandler(muleEvent, muleContext, flowConstruct, requestBuilder, client, httpRequest, checkRetry,
                                      callback));
  }

  private ResponseHandler createResponseHandler(Event muleEvent, MuleContext muleContext, FlowConstruct flowConstruct,
                                                HttpRequesterRequestBuilder requestBuilder, HttpExtensionClient client,
                                                HttpRequest httpRequest,
                                                boolean checkRetry, CompletionCallback<Object, HttpResponseAttributes> callback) {
    return new ResponseHandler() {

      @Override
      public void onCompletion(HttpResponse response) {
        //TODO: MULE-11310 - Make HTTP request scheduling fine grained
        scheduler.execute(() -> {
          HttpResponseToMuleMessage httpResponseToMuleMessage = new HttpResponseToMuleMessage(config, parseResponse, muleContext);
          try {
            Message responseMessage = httpResponseToMuleMessage.convert(muleEvent, response, httpRequest.getUri());
            notificationHelper.fireNotification(this, muleEvent, httpRequest.getUri(), flowConstruct, MESSAGE_REQUEST_END);
            // Create a new muleEvent based on the old and the response so that the auth can use it
            Event responseEvent =
                Event.builder(muleEvent).message(InternalMessage.builder(responseMessage).build())
                    .synchronous(muleEvent.isSynchronous()).build();
            if (resendRequest(responseEvent, checkRetry, authentication)) {
              consumePayload(responseEvent, muleContext);
              doRequest(responseEvent, client, requestBuilder, false, muleContext, flowConstruct, callback);
            } else {
              responseValidator.validate(responseMessage, muleContext);
              callback.success(Result.<Object, HttpResponseAttributes>builder(responseMessage).build());
            }
          } catch (MuleException e) {
            callback.error(e);
          }
        });
      }

      @Override
      public void onFailure(Exception exception) {
        checkIfRemotelyClosed(exception, client.getDefaultUriParameters());
        callback.error(new MessagingException(createStaticMessage(getErrorMessage(httpRequest)), muleEvent, exception));
      }

    };
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
    private HttpAuthentication authentication;

    private int responseTimeout;
    private boolean parseResponse;
    private ResponseValidator responseValidator;

    private HttpRequesterConfig config;
    private TransformationService transformationService;
    private Scheduler scheduler;

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

    public Builder setTransformationService(TransformationService transformationService) {
      this.transformationService = transformationService;
      return this;
    }

    public Builder setScheduler(Scheduler scheduler) {
      this.scheduler = scheduler;
      return this;
    }

    public HttpRequester build() {
      MuleEventToHttpRequest eventToHttpRequest =
          new MuleEventToHttpRequest(config, uri, method, requestStreamingMode, sendBodyMode, transformationService);
      return new HttpRequester(eventToHttpRequest, followRedirects, authentication, parseResponse, responseTimeout,
                               responseValidator, config, scheduler);
    }
  }
}
