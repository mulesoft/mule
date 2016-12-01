/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.listener;

import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.request.HttpRequestContext;
import org.mule.service.http.api.server.RequestHandler;
import org.mule.service.http.api.server.async.HttpResponseReadyCallback;
import org.mule.service.http.api.server.async.ResponseStatusCallback;

import java.io.ByteArrayInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorRequestHandler implements RequestHandler {

  private Logger logger = LoggerFactory.getLogger(getClass());

  private int statusCode;
  private String reasonPhrase;
  private String entityFormat;

  public ErrorRequestHandler(int statusCode, String reasonPhrase, String entityFormat) {
    this.statusCode = statusCode;
    this.reasonPhrase = reasonPhrase;
    this.entityFormat = entityFormat;
  }

  @Override
  public void handleRequest(HttpRequestContext requestContext, HttpResponseReadyCallback responseCallback) {
    String resolvedEntity = String.format(entityFormat, requestContext.getRequest().getUri());
    responseCallback.responseReady(new org.mule.runtime.module.http.internal.domain.response.HttpResponseBuilder()
        .setStatusCode(statusCode).setReasonPhrase(reasonPhrase)
        .setEntity(new InputStreamHttpEntity(new ByteArrayInputStream(resolvedEntity.getBytes()))).build(),
                                   new ResponseStatusCallback() {

                                     @Override
                                     public void responseSendFailure(Throwable exception) {
                                       logger.warn(String.format("Error while sending %s response %s", statusCode,
                                                                 exception.getMessage()));
                                       if (logger.isDebugEnabled()) {
                                         logger.debug("exception thrown", exception);
                                       }
                                     }

                                     @Override
                                     public void responseSendSuccessfully() {}
                                   });
  }
}
