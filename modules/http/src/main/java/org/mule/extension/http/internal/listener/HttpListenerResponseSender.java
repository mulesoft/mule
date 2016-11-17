/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.listener;

import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import org.mule.extension.http.api.HttpStreamingType;
import org.mule.extension.http.api.listener.builder.HttpListenerSuccessResponseBuilder;
import org.mule.runtime.core.TransformationService;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.module.http.internal.domain.response.HttpResponse;
import org.mule.runtime.module.http.internal.domain.response.HttpResponseBuilder;
import org.mule.runtime.module.http.internal.listener.async.HttpResponseReadyCallback;
import org.mule.runtime.module.http.internal.listener.async.ResponseStatusCallback;

public class HttpListenerResponseSender {

  private final HttpResponseFactory responseFactory;

  public HttpListenerResponseSender(HttpResponseFactory responseFactory) {
    this.responseFactory = responseFactory;
  }

  public HttpListenerResponseSender(TransformationService transformationService) {
    this.responseFactory = new HttpResponseFactory(HttpStreamingType.NEVER, transformationService);
  }

  public void sendResponse(HttpResponseContext context, HttpListenerSuccessResponseBuilder responseBuilder) throws Exception {
    HttpResponse httpResponse = buildResponse(responseBuilder, context.isSupportStreaming());
    final HttpResponseReadyCallback responseCallback = context.getResponseCallback();
    responseCallback.responseReady(httpResponse, getResponseFailureCallback(responseCallback));
  }

  protected HttpResponse buildResponse(HttpListenerSuccessResponseBuilder listenerResponseBuilder, boolean supportStreaming)
      throws Exception {
    HttpResponseBuilder responseBuilder = new HttpResponseBuilder();

    return doBuildResponse(responseBuilder, listenerResponseBuilder, supportStreaming);
  }

  protected HttpResponse doBuildResponse(HttpResponseBuilder responseBuilder,
                                         HttpListenerSuccessResponseBuilder listenerResponseBuilder,
                                         boolean supportsStreaming)
      throws Exception {
    return responseFactory.create(responseBuilder, listenerResponseBuilder, supportsStreaming);
  }

  private ResponseStatusCallback getResponseFailureCallback(HttpResponseReadyCallback responseReadyCallback) {
    return new ResponseStatusCallback() {

      @Override
      public void responseSendFailure(Throwable throwable) {
        responseReadyCallback.responseReady(buildErrorResponse(), this);
      }

      @Override
      public void responseSendSuccessfully() {
        // TODO: MULE-9749 Figure out how to handle this. Maybe doing nothing is right since this will be executed later if
        // everything goes right.
        // responseCompletationCallback.responseSentSuccessfully();
      }
    };
  }

  protected HttpResponse buildErrorResponse() {
    final HttpResponseBuilder errorResponseBuilder = new HttpResponseBuilder();
    final HttpResponse errorResponse = errorResponseBuilder.setStatusCode(INTERNAL_SERVER_ERROR.getStatusCode())
        .setReasonPhrase(INTERNAL_SERVER_ERROR.getReasonPhrase())
        .build();
    return errorResponse;
  }

}
