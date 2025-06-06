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
import org.mule.runtime.http.api.sse.server.SseRequestContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SseRequestContextWrapperTestCase {

  @Mock
  private SseRequestContext mockContext;

  private SseRequestContextWrapper ctxWrapper;

  @BeforeEach
  void setUp() {
    ctxWrapper = new SseRequestContextWrapper(mockContext);
  }

  @Test
  void setClientId() {
    ctxWrapper.setClientId("clientId");
    verify(mockContext).setClientId("clientId");
  }

  @Test
  void reject() {
    ctxWrapper.reject(507, "Whatever");
    verify(mockContext).reject(507, "Whatever");
  }

  @Test
  void customizeResponse() {
    ctxWrapper.customizeResponse(c -> {
    });
    verify(mockContext).customizeResponse(any());
  }

  @Test
  void getRequestContext() {
    HttpRequestContext requestContext = mock(HttpRequestContext.class);
    when(requestContext.getScheme()).thenReturn("test-scheme");
    when(mockContext.getRequestContext()).thenReturn(requestContext);
    var wrappedRequestContext = ctxWrapper.getRequestContext();
    assertThat(wrappedRequestContext.getScheme(), is("test-scheme"));
  }
}
