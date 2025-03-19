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
import reactor.core.publisher.FluxSink;

import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Coverage tests for {@link org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategy.DefaultReactorSink
 * DefaultReactorSink}
 */
@ExtendWith(MockitoExtension.class)
class DefaultReactorSinkTestCase {

  private AbstractProcessingStrategy.DefaultReactorSink sink;
  @Mock
  private FluxSink<CoreEvent> flux;
  @Mock
  private Consumer<Long> disposer;
  @Mock
  private Consumer<CoreEvent> consumer;
  @Mock
  private CoreEvent event;

  @BeforeEach
  void setUp() {
    sink = mock(AbstractProcessingStrategy.DefaultReactorSink.class,
                withSettings().useConstructor(flux, disposer, consumer, 2).defaultAnswer(CALLS_REAL_METHODS));
  }

  @Test
  void acceptSendsToFluxAndConsumer() {
    sink.accept(event);

    verify(flux).next(event);
    verify(consumer).accept(event);
  }

  @Test
  void emitWithNoRemainingCapacityReturnsBackpressure() {
    final BackPressureReason result = sink.emit(event);

    assertThat(result, is(BackPressureReason.EVENTS_ACCUMULATED));
    verify(flux).requestedFromDownstream();
    // Check that we don't call next...
    verifyNoMoreInteractions(flux);
  }

  @Test
  void emitWithLimitedCapacity() {
    when(flux.requestedFromDownstream()).thenReturn(1L);
    final BackPressureReason result = sink.emit(event);

    // No backpressure
    assertThat(result, is(nullValue()));
    // We process the event
    verify(flux).next(event);
  }

  /**
   * The code has this branch - not sure of the circumstances that generate it...
   */
  @Test
  void emitWithDownstreamRequestsNotZeroOrMore() {
    when(flux.requestedFromDownstream()).thenReturn(-1L);
    final BackPressureReason result = sink.emit(event);

    assertThat(result, is(BackPressureReason.EVENTS_ACCUMULATED));
    verify(flux).requestedFromDownstream();
    // Check that we don't call next...
    verifyNoMoreInteractions(flux);
  }
}
