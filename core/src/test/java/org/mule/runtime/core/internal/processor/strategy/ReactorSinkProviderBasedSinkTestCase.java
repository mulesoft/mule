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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mule.runtime.core.api.event.CoreEvent;
import reactor.core.publisher.FluxSink;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Coverage tests for {@link ReactorSinkProviderBasedSink} Minimal checks - mostly just make sure the delegate gets called.
 */
@ExtendWith(MockitoExtension.class)
class ReactorSinkProviderBasedSinkTestCase {

  private ReactorSinkProviderBasedSink sink;
  @Mock
  private ReactorSinkProvider sinkProvider;
  @Mock
  private CoreEvent event;
  @Mock
  private FluxSink<CoreEvent> fluxSink;

  @BeforeEach
  void setUp() {
    sink = new ReactorSinkProviderBasedSink(sinkProvider);
  }

  @Test
  void accept() {
    when(sinkProvider.getSink()).thenReturn(fluxSink);

    sink.accept(event);

    verify(fluxSink).next(event);
  }

  @Test
  void accept_after_dispose() {
    sink.dispose();
    assertThrows(IllegalStateException.class, () -> sink.accept(event));
  }

  @Test
  void emit() {
    when(sinkProvider.getSink()).thenReturn(fluxSink);

    sink.emit(event);

    verify(fluxSink).next(event);
  }
}
