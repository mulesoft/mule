/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.server;

import static org.mockito.Mockito.verify;

import org.mule.runtime.http.api.server.RequestHandlerManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EndpointAvailabilityHandlerWrapperTestCase {

  @Mock
  private RequestHandlerManager mockHandler;

  private EndpointAvailabilityHandlerWrapper handlerWrapper;

  @BeforeEach
  void setUp() {
    handlerWrapper = new EndpointAvailabilityHandlerWrapper(mockHandler);
  }

  @Test
  void availableCallsStart() {
    handlerWrapper.available();
    verify(mockHandler).start();
  }

  @Test
  void unavailableCallsStop() {
    handlerWrapper.unavailable();
    verify(mockHandler).stop();
  }

  @Test
  void removeCallsDispose() {
    handlerWrapper.remove();
    verify(mockHandler).dispose();
  }
}
