/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.authorizationcode;

import static java.lang.Thread.currentThread;
import static org.mule.extension.http.api.HttpConstants.HttpStatus.BAD_REQUEST;
import static org.mule.extension.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.mule.extension.http.api.HttpHeaders.Names.CONTENT_LENGTH;
import static org.mule.extension.http.internal.listener.HttpRequestToResult.transform;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.core.DefaultEventContext.create;

import org.mule.extension.http.api.HttpConstants;
import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.http.internal.listener.HttpRequestParsingException;
import org.mule.runtime.module.http.internal.listener.ListenerPath;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.EmptyHttpEntity;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.service.http.api.domain.message.response.HttpResponseBuilder;
import org.mule.service.http.api.server.HttpServer;
import org.mule.service.http.api.server.PathAndMethodRequestMatcher;
import org.mule.service.http.api.server.RequestHandlerManager;
import org.mule.service.http.api.server.async.HttpResponseReadyCallback;
import org.mule.service.http.api.server.async.ResponseStatusCallback;

import java.util.Map.Entry;

import org.slf4j.Logger;

class RequestHandlerUtils {

  private RequestHandlerUtils() {
    // Nothing to do
  }

  /**
   * Registers a request handler to execute the provided {@code flow}.
   * 
   * @param server
   * @param matcher
   * @param listenerPath
   * @param flow
   * @param logger
   * @return
   */
  public static RequestHandlerManager addRequestHandler(HttpServer server, PathAndMethodRequestMatcher matcher,
                                                        Flow flow, Logger logger) {
    // MULE-11277 Support non-blocking in OAuth http listeners
    return server.addRequestHandler(matcher, (requestContext, responseCallback) -> {
      Result<Object, HttpRequestAttributes> result;
      final ClassLoader previousCtxClassLoader = currentThread().getContextClassLoader();
      try {
        currentThread().setContextClassLoader(RequestHandlerUtils.class.getClassLoader());

        result = transform(requestContext, flow.getMuleContext(), true, new ListenerPath(matcher.getPath(), "/"));
        final Message message = Message.builder()
            .payload(result.getOutput())
            .mediaType(result.getMediaType().orElse(ANY))
            .attributes(result.getAttributes().get())
            .build();

        final Event processed =
            flow.process(Event.builder(create(flow, "OAuthCallback")).message((InternalMessage) message).build());

        final String body = (String) processed.getMessage().getPayload().getValue();
        final HttpResponseAttributes responseAttributes = (HttpResponseAttributes) processed.getMessage().getAttributes();
        final HttpResponseBuilder responseBuilder = HttpResponse.builder()
            .setStatusCode(responseAttributes.getStatusCode())
            .setReasonPhrase(responseAttributes.getReasonPhrase());

        if (body != null) {
          responseBuilder.setEntity(new ByteArrayHttpEntity(body.getBytes()))
              .addHeader(CONTENT_LENGTH, "" + body.length());
        } else {
          responseBuilder.setEntity(new EmptyHttpEntity())
              .addHeader(CONTENT_LENGTH, "" + 0);
        }

        for (Entry<String, String> entry : responseAttributes.getHeaders().entrySet()) {
          responseBuilder.addHeader(entry.getKey(), entry.getValue());
        }

        responseCallback.responseReady(responseBuilder.build(), new ResponseStatusCallback() {

          @Override
          public void responseSendFailure(Throwable exception) {
            logger.warn("Error while sending {} response {}", responseAttributes.getStatusCode(), exception.getMessage());
            if (logger.isDebugEnabled()) {
              logger.debug("Exception thrown", exception);
            }
          }

          @Override
          public void responseSendSuccessfully() {}
        });
      } catch (HttpRequestParsingException e) {
        logger.warn("Exception occurred parsing request:", e);
        sendErrorResponse(BAD_REQUEST, e.getMessage(), responseCallback, logger);
      } catch (MuleException e) {
        logger.warn("Exception occurred processing request:", e);
        sendErrorResponse(INTERNAL_SERVER_ERROR, "Server encountered a problem", responseCallback, logger);
      } finally {
        currentThread().setContextClassLoader(previousCtxClassLoader);
      }
    });
  }

  private static void sendErrorResponse(final HttpConstants.HttpStatus status, String message,
                                        HttpResponseReadyCallback responseCallback, Logger logger) {
    responseCallback.responseReady(HttpResponse.builder()
        .setStatusCode(status.getStatusCode())
        .setReasonPhrase(status.getReasonPhrase())
        .setEntity(new ByteArrayHttpEntity(message.getBytes()))
        .addHeader(CONTENT_LENGTH, "" + message.length())
        .build(), new ResponseStatusCallback() {

          @Override
          public void responseSendFailure(Throwable exception) {
            logger.warn("Error while sending {} response {}", status.getStatusCode(), exception.getMessage());
            if (logger.isDebugEnabled()) {
              logger.debug("Exception thrown", exception);
            }
          }

          @Override
          public void responseSendSuccessfully() {}
        });
  }

}
