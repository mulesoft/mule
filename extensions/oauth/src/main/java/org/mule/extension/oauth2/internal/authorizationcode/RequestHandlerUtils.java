/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.authorizationcode;

import static java.lang.Thread.currentThread;
import static java.util.Collections.singleton;
import static org.mule.extension.http.internal.listener.HttpRequestToResult.transform;
import static org.mule.service.http.api.HttpConstants.HttpStatus.BAD_REQUEST;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_LENGTH;

import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.extension.http.api.error.HttpMessageParsingException;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.http.internal.listener.ListenerPath;
import org.mule.service.http.api.HttpConstants;
import org.mule.service.http.api.HttpConstants.Method;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.EmptyHttpEntity;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.service.http.api.domain.message.response.HttpResponseBuilder;
import org.mule.service.http.api.server.HttpServer;
import org.mule.service.http.api.server.RequestHandlerManager;
import org.mule.service.http.api.server.async.HttpResponseReadyCallback;
import org.mule.service.http.api.server.async.ResponseStatusCallback;

import java.nio.charset.Charset;
import java.util.Map.Entry;
import java.util.function.Function;

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
   * @param encoding
   * @param callbackHandler
   * @param logger
   * @return
   */
  public static <T> RequestHandlerManager addRequestHandler(HttpServer server, Method method, String path,
                                                            Charset encoding,
                                                            Function<Result<Object, HttpRequestAttributes>, Result<T, HttpResponseAttributes>> callbackHandler,
                                                            Logger logger) {
    // MULE-11277 Support non-blocking in OAuth http listeners
    return server.addRequestHandler(singleton(method), path, (requestContext, responseCallback) -> {
      final ClassLoader previousCtxClassLoader = currentThread().getContextClassLoader();
      try {
        currentThread().setContextClassLoader(RequestHandlerUtils.class.getClassLoader());

        Result<?, HttpResponseAttributes> processed =
            callbackHandler.apply(transform(requestContext, encoding, true, new ListenerPath(null, path)));

        final String body = (String) processed.getOutput();
        final HttpResponseAttributes responseAttributes = processed.getAttributes().get();
        final HttpResponseBuilder responseBuilder = HttpResponse.builder()
            .setStatusCode(responseAttributes.getStatusCode())
            .setReasonPhrase(responseAttributes.getReasonPhrase());

        if (body != null) {
          responseBuilder.setEntity(new ByteArrayHttpEntity(body.getBytes())).addHeader(CONTENT_LENGTH, "" + body.length());
        } else {
          responseBuilder.setEntity(new EmptyHttpEntity()).addHeader(CONTENT_LENGTH, "" + 0);
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
      } catch (HttpMessageParsingException e) {
        logger.warn("Exception occurred parsing request:", e);
        sendErrorResponse(BAD_REQUEST, e.getMessage(), responseCallback, logger);
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
