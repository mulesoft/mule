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
import org.reactivestreams.Publisher;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreamPerEventSinkTest {

  private StreamPerEventSink sink;
  @Mock
  private ReactiveProcessor processor;
  @Mock
  private Consumer<CoreEvent> consumer;
  @Mock
  private CoreEvent event;
  @Mock
  private Publisher<CoreEvent> publisher;

  @BeforeEach
  void setUp() {
    sink = new StreamPerEventSink(processor, consumer);
    when(processor.apply(any())).thenReturn(publisher);
  }

  @Test
  void accept() {
    sink.accept(event);

    // We process the event
    verify(consumer).accept(eq(event));
  }

  @Test
  void acceptThrows() {
    doThrow(new IllegalArgumentException("Misbegotten son of a wombat")).when(consumer).accept(any());

    assertThrows(IllegalArgumentException.class, () -> sink.accept(event));
  }

  @Test
  void emit() {
    final BackPressureReason result = sink.emit(event);

    assertNull(result);
    verify(consumer).accept(eq(event));
  }
}
