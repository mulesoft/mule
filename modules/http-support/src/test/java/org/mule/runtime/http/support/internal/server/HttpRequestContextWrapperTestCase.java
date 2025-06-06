/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.server;

import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_FORWARD_COMPATIBILITY;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.request.HttpRequestContext;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Feature(HTTP_FORWARD_COMPATIBILITY)
@ExtendWith(MockitoExtension.class)
class HttpRequestContextWrapperTestCase {

  @Mock
  private HttpRequestContext mockContext;

  private HttpRequestContextWrapper ctxWrapper;

  @BeforeEach
  void setUp() {
    ctxWrapper = new HttpRequestContextWrapper(mockContext);
  }

  @Test
  void getScheme() {
    when(mockContext.getScheme()).thenReturn("https");
    assertThat(ctxWrapper.getScheme(), is("https"));
  }

  @Test
  void getRequest() {
    var request = HttpRequest.builder().uri("/test").build();
    when(mockContext.getRequest()).thenReturn(request);
    assertThat(ctxWrapper.getRequest().getUri().getPath(), is("/test"));
  }

  @Test
  void getServerConnection() {
    ctxWrapper.getServerConnection();
    verify(mockContext).getServerConnection();
  }

  @Test
  void getClientConnection() {
    ctxWrapper.getClientConnection();
    verify(mockContext).getClientConnection();
  }
}
