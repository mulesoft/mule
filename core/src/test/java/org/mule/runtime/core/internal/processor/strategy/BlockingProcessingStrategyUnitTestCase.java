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
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.reactivestreams.Publisher;
import reactor.test.publisher.TestPublisher;
import reactor.test.subscriber.TestSubscriber;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlockingProcessingStrategyUnitTestCase {

  private ProcessingStrategy strategy;
  @Mock
  private MuleContext context;
  @Mock
  private ReactiveProcessor processor;
  @Mock
  private CoreEvent event;

  @BeforeEach
  void setUp() {
    strategy = new BlockingProcessingStrategyFactory().create(context, "foo");
  }

  @Test
  void onProcessor() {
    TestPublisher<CoreEvent> testPublisher = TestPublisher.create();
    when(processor.apply(any())).thenAnswer(inv -> inv.getArgument(0));
    when(processor.getProcessingType()).thenReturn(ReactiveProcessor.ProcessingType.CPU_LITE_ASYNC);

    final ReactiveProcessor result = strategy.onProcessor(processor);
    final Publisher<CoreEvent> wrappedPublisher = result.apply(testPublisher);
    TestSubscriber<CoreEvent> subscriber = TestSubscriber.create();
    wrappedPublisher.subscribe(subscriber);
    testPublisher.next(event);

    verifyNoMoreInteractions(processor);
  }
}
