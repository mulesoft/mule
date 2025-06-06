/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.server;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.http.api.domain.request.HttpRequestContext;
import org.mule.runtime.http.api.server.async.HttpResponseReadyCallback;
import org.mule.sdk.api.http.server.RequestHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequestHandlerWrapperTestCase {

  @Mock
  private RequestHandler mockHandler;

  private RequestHandlerWrapper handlerWrapper;

  @BeforeEach
  void setUp() {
    handlerWrapper = new RequestHandlerWrapper(mockHandler);
  }

  @Test
  void handleRequest() {
    HttpRequestContext requestContext = mock(HttpRequestContext.class);
    HttpResponseReadyCallback responseCallback = mock(HttpResponseReadyCallback.class);
    handlerWrapper.handleRequest(requestContext, responseCallback);
    verify(mockHandler).handleRequest(any(), any());
  }

  @Test
  void getContextClassLoader() {
    ClassLoader classLoader = mock(ClassLoader.class);
    when(mockHandler.getContextClassLoader()).thenReturn(classLoader);
    var got = handlerWrapper.getContextClassLoader();
    assertThat(got, is(classLoader));
  }
}
