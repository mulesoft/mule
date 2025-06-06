/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.server;

import static org.mockito.Mockito.verify;

import org.mule.runtime.http.api.sse.server.SseEndpointManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SseEndpointManagerWrapperTestCase {

  @Mock
  private SseEndpointManager mockDelegate;

  private SseEndpointManagerWrapper wrapper;

  @BeforeEach
  void setUp() {
    wrapper = new SseEndpointManagerWrapper(mockDelegate);
  }

  @Test
  void stop() {
    wrapper.stop();
    verify(mockDelegate).stop();
  }

  @Test
  void start() {
    wrapper.start();
    verify(mockDelegate).start();
  }

  @Test
  void dispose() {
    wrapper.dispose();
    verify(mockDelegate).dispose();
  }
}
