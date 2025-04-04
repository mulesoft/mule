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
import org.mule.runtime.core.api.construct.BackPressureReason;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Coverage tests for {@link StreamPerEventSink}
 */
@ExtendWith(MockitoExtension.class)
class StreamPerEventSinkTestCase {

  private StreamPerEventSink sink;
  @Mock
  private ReactiveProcessor processor;
  @Mock
  private Consumer<CoreEvent> consumer;
  @Mock
  private CoreEvent event;

  @BeforeEach
  void setUp() {
    sink = new StreamPerEventSink(processor, consumer);
    when(processor.apply(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void acceptForwardsEventToConsumer() {
    sink.accept(event);

    verify(consumer).accept(event);
  }

  @Test
  void acceptThrowsWhenComsumerThrows() {
    doThrow(new IllegalArgumentException("Imma tiny stoat")).when(consumer).accept(any());

    assertThrows(IllegalArgumentException.class, () -> sink.accept(event));

    verify(consumer).accept(event);
  }

  @Test
  void emitForwardsRequestToConsumer() {

    final BackPressureReason result = sink.emit(event);

    verify(consumer).accept(event);
    // We always return null from emit...
    assertNull(result);
  }
}
