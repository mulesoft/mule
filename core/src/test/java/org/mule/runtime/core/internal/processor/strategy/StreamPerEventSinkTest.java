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
import reactor.test.publisher.TestPublisher;

import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
  private TestPublisher<CoreEvent> publisher;

  @BeforeEach
  void setUp() {
    publisher = TestPublisher.create();
    sink = new StreamPerEventSink(processor, consumer);
    when(processor.apply(any())).thenAnswer(inv -> inv.getArgument(0));
    when(processor.andThen(any())).thenAnswer(inv -> inv.getArgument(0));
    when(processor.compose(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void accept() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    doAnswer(inv -> {
      latch.countDown();
      return null;
    }).when(consumer).accept(any());

    sink.accept(event);
    latch.await();

    publisher.assertSubscribers(1);
    verify(processor).apply(any());
    verifyNoMoreInteractions(processor);
  }

  @Test
  void emit() {
    final BackPressureReason result = sink.emit(event);

    assertNull(result);
    publisher.assertSubscribers(1);
  }
}
