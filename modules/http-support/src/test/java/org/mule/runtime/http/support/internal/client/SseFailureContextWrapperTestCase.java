/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.runtime.http.api.sse.client.SseFailureContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SseFailureContextWrapperTestCase {

  @Mock
  private SseFailureContext actualContext;

  @Mock
  private HttpResponse muleResponse;

  private SseFailureContextWrapper contextWrapper;

  @BeforeEach
  void setUp() {
    contextWrapper = new SseFailureContextWrapper(actualContext);
  }

  @Test
  void delegateError() {
    Throwable error = new RuntimeException("Test error");
    when(actualContext.error()).thenReturn(error);

    Throwable result = contextWrapper.error();
    assertThat(result, is(error));
  }

  @Test
  void errorReturnsNull() {
    when(actualContext.error()).thenReturn(null);
    Throwable result = contextWrapper.error();
    assertThat(result, is((Throwable) null));
  }

  @Test
  void delegateResponse() {
    int mockStatusCode = 207;
    when(muleResponse.getStatusCode()).thenReturn(mockStatusCode);
    when(actualContext.response()).thenReturn(muleResponse);
    var responseWrapper = contextWrapper.response();
    assertThat(responseWrapper.getStatusCode(), is(mockStatusCode));
  }

  @Test
  void delegateStopRetrying() {
    contextWrapper.stopRetrying();
    verify(actualContext).stopRetrying();
  }
}
