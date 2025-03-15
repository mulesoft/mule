/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Sink;
import reactor.core.publisher.FluxSink;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class AbstrctCachedThreadReactorSinkProviderTestSuite {

  private AbstractCachedThreadReactorSinkProvider sinkProvider;

  @BeforeEach
  void setUp() {
    sinkProvider = mock(AbstractCachedThreadReactorSinkProvider.class,
                        withSettings().useConstructor(true).defaultAnswer(CALLS_REAL_METHODS));
  }

  @Test
  void dispose() {
    sinkProvider.dispose();
  }

  @Test
  void getSink() {
    final FluxSink<CoreEvent> result = sinkProvider.getSink();
  }
}
