/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.listener;

import static org.mule.runtime.core.config.ExceptionHelper.getTransportErrorMapping;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.mule.runtime.module.http.api.HttpConstants.Protocols.HTTP;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.execution.AsyncResponseFlowProcessingPhaseTemplate;
import org.mule.runtime.core.execution.ResponseCompletionCallback;
import org.mule.runtime.core.execution.ThrottlingPhaseTemplate;
import org.mule.runtime.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.runtime.module.http.internal.domain.response.HttpResponse;
import org.mule.runtime.module.http.internal.listener.async.HttpResponseReadyCallback;
import org.mule.runtime.module.http.internal.listener.async.ResponseStatusCallback;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;

public class HttpMessageProcessorTemplate implements AsyncResponseFlowProcessingPhaseTemplate, ThrottlingPhaseTemplate {

  public static final int MESSAGE_DISCARD_STATUS_CODE =
      Integer.valueOf(System.getProperty("mule.transport.http.throttling.discardstatuscode", "429"));
  public static final String MESSAGE_DISCARD_MESSAGE_BODY = "API calls exceeded";
  public static final String MESSAGE_DISCARD_REASON_PHRASE = "Too Many Requests";
  public static final String X_RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";
  public static final String X_RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
  public static final String X_RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset";
  private static final int INTERNAL_SERVER_ERROR_STATUS_CODE = 500;
  private static final int OK_STATUS_CODE = 200;

  private static final Logger logger = getLogger(HttpMessageProcessorTemplate.class);
  private Event sourceMuleEvent;
  private Processor messageProcessor;
  private HttpResponseReadyCallback responseReadyCallback;
  private HttpResponseBuilder responseBuilder;
  private HttpResponseBuilder errorResponseBuilder;
  private HttpThrottlingHeadersMapBuilder httpThrottlingHeadersMapBuilder = new HttpThrottlingHeadersMapBuilder();

  public HttpMessageProcessorTemplate(Event sourceMuleEvent,
                                      Processor messageProcessor,
                                      HttpResponseReadyCallback responseReadyCallback,
                                      HttpResponseBuilder responseBuilder,
                                      HttpResponseBuilder errorResponseBuilder) {
    this.sourceMuleEvent = sourceMuleEvent;
    this.messageProcessor = messageProcessor;
    this.responseBuilder = responseBuilder;
    this.errorResponseBuilder = errorResponseBuilder;
    this.responseReadyCallback = responseReadyCallback;
  }

  @Override
  public Event getMuleEvent() throws MuleException {
    return this.sourceMuleEvent;
  }

  @Override
  public Event routeEvent(Event muleEvent) throws MuleException {
    return messageProcessor.process(muleEvent);
  }

  @Override
  public void sendResponseToClient(Event muleEvent, ResponseCompletionCallback responseCompletationCallback)
      throws MuleException {
    final org.mule.runtime.module.http.internal.domain.response.HttpResponseBuilder responseBuilder =
        new org.mule.runtime.module.http.internal.domain.response.HttpResponseBuilder();
    final HttpResponse httpResponse = buildResponse(muleEvent, responseBuilder, responseCompletationCallback);
    responseReadyCallback.responseReady(httpResponse, getResponseFailureCallback(responseCompletationCallback, muleEvent));
  }

  protected HttpResponse buildErrorResponse() {
    final org.mule.runtime.module.http.internal.domain.response.HttpResponseBuilder errorResponseBuilder =
        new org.mule.runtime.module.http.internal.domain.response.HttpResponseBuilder();
    final HttpResponse errorResponse = errorResponseBuilder.setStatusCode(INTERNAL_SERVER_ERROR.getStatusCode())
        .setReasonPhrase(INTERNAL_SERVER_ERROR.getReasonPhrase())
        .build();
    return errorResponse;
  }

  protected HttpResponse buildResponse(Event muleEvent,
                                       final org.mule.runtime.module.http.internal.domain.response.HttpResponseBuilder responseBuilder,
                                       ResponseCompletionCallback responseCompletationCallback) {
    addThrottlingHeaders(responseBuilder);
    final HttpResponse httpResponse;

    if (muleEvent == null) {
      // If the event was filtered, return an empty response with status code 200 OK.
      httpResponse = responseBuilder.setStatusCode(OK_STATUS_CODE).build();
    } else {
      httpResponse = doBuildResponse(muleEvent, responseBuilder, responseCompletationCallback);
    }
    return httpResponse;
  }

  protected HttpResponse doBuildResponse(Event muleEvent,
                                         final org.mule.runtime.module.http.internal.domain.response.HttpResponseBuilder responseBuilder,
                                         ResponseCompletionCallback responseCompletationCallback) {
    try {
      return this.responseBuilder.build(responseBuilder, muleEvent);
    } catch (Exception e) {
      try {
        // Handle errors that occur while building the response.
        Event exceptionStrategyResult =
            responseCompletationCallback.responseSentWithFailure(new MessagingException(muleEvent, e), muleEvent);
        // Send the result from the event that was built from the Exception Strategy.
        return this.responseBuilder.build(responseBuilder, exceptionStrategyResult);
      } catch (Exception innerException) {
        // The failure occurred while executing the ES, or while building the response from the result of the ES
        return buildErrorResponse();
      }
    }
  }

  private ResponseStatusCallback getResponseFailureCallback(final ResponseCompletionCallback responseCompletationCallback,
                                                            final Event muleEvent) {
    return new ResponseStatusCallback() {

      @Override
      public void responseSendFailure(Throwable throwable) {
        responseReadyCallback.responseReady(buildErrorResponse(), this);
      }

      @Override
      public void responseSendSuccessfully() {
        responseCompletationCallback.responseSentSuccessfully();
      }
    };
  }

  @Override
  public void sendFailureResponseToClient(MessagingException messagingException,
                                          ResponseCompletionCallback responseCompletationCallback)
      throws MuleException {
    // For now let's use the HTTP transport exception mapping since makes sense and the gateway depends on it.
    Throwable cause = messagingException.getCause();
    String exceptionStatusCode =
        getTransportErrorMapping(HTTP.getScheme(), cause.getClass(), responseBuilder.getMuleContext());
    Integer statusCodeFromException =
        exceptionStatusCode != null ? Integer.valueOf(exceptionStatusCode) : INTERNAL_SERVER_ERROR_STATUS_CODE;
    final org.mule.runtime.module.http.internal.domain.response.HttpResponseBuilder failureResponseBuilder =
        new org.mule.runtime.module.http.internal.domain.response.HttpResponseBuilder()
            .setStatusCode(statusCodeFromException)
            .setReasonPhrase(messagingException.getMessage());
    addThrottlingHeaders(failureResponseBuilder);
    Event event = messagingException.getEvent();
    Message errorMessage = resolveErrorMessage(event);
    event = Event.builder(event)
        .message(InternalMessage.builder(errorMessage).payload(messagingException.getMessage()).build()).build();
    final HttpResponse response = errorResponseBuilder.build(failureResponseBuilder, event);
    responseReadyCallback.responseReady(response, getResponseFailureCallback(responseCompletationCallback, event));
  }

  private Message resolveErrorMessage(Event event) {
    Optional<Error> error = event.getError();
    if (error.isPresent() && error.get().getErrorMessage() != null
        && "SECURITY".equals(error.get().getErrorType().getIdentifier())) {
      return error.get().getErrorMessage();
    } else {
      return event.getMessage();
    }
  }

  @Override
  public void discardMessageOnThrottlingExceeded() throws MuleException {
    final org.mule.runtime.module.http.internal.domain.response.HttpResponseBuilder throttledResponseBuilder =
        new org.mule.runtime.module.http.internal.domain.response.HttpResponseBuilder()
            .setStatusCode(MESSAGE_DISCARD_STATUS_CODE)
            .setReasonPhrase(MESSAGE_DISCARD_REASON_PHRASE)
            .setEntity(new InputStreamHttpEntity(new ByteArrayInputStream(MESSAGE_DISCARD_MESSAGE_BODY.getBytes())));
    addThrottlingHeaders(throttledResponseBuilder);
    responseReadyCallback.responseReady(throttledResponseBuilder.build(), getLogCompletionCallback());
  }

  private void addThrottlingHeaders(org.mule.runtime.module.http.internal.domain.response.HttpResponseBuilder throttledResponseBuilder) {
    final Map<String, String> throttlingHeaders = getThrottlingHeaders();
    for (String throttlingHeaderName : throttlingHeaders.keySet()) {
      throttledResponseBuilder.addHeader(throttlingHeaderName, throttlingHeaders.get(throttlingHeaderName));
    }
  }

  private ResponseStatusCallback getLogCompletionCallback() {
    return new ResponseStatusCallback() {

      @Override
      public void responseSendFailure(Throwable throwable) {
        logger.info("Failure sending throttled response " + throwable.getMessage());
        if (logger.isDebugEnabled()) {
          logger.debug(throwable.getMessage(), throwable);
        }
      }

      @Override
      public void responseSendSuccessfully() {
        if (logger.isDebugEnabled()) {
          logger.debug("throttled response sent successfully");
        }
      }
    };
  }

  @Override
  public void setThrottlingPolicyStatistics(long remainingRequestInCurrentPeriod, long maximumRequestAllowedPerPeriod,
                                            long timeUntilNextPeriodInMillis) {
    httpThrottlingHeadersMapBuilder.setThrottlingPolicyStatistics(remainingRequestInCurrentPeriod, maximumRequestAllowedPerPeriod,
                                                                  timeUntilNextPeriodInMillis);
  }

  private Map<String, String> getThrottlingHeaders() {
    return httpThrottlingHeadersMapBuilder.build();
  }
}
