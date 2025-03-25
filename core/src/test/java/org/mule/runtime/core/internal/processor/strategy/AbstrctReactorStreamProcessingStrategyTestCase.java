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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.BackPressureReason;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import javax.lang.model.util.Types;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Tests to check some details in the @{@link AbstractReactorStreamProcessingStrategy}. Note the misspelling of the test name -
 * abstract test suites are skipped, so we can't call it by the usual name.
 */
@ExtendWith(MockitoExtension.class)
class AbstrctReactorStreamProcessingStrategyTestCase {

  private AbstractReactorStreamProcessingStrategy strategy;
  @Mock
  private CoreEvent event;
  @Mock
  private MuleContext context;
  @Mock
  private SchedulerService schedulerService;
  @Mock
  private BaseEventContext eventContext;
  @Captor
  private ArgumentCaptor<BiConsumer<CoreEvent, Throwable>> consumerCaptor;


  @BeforeEach
  void setUp() {
    // Use 'mock' but with 'calls real methods' because I want a spy but I also want to call a specific constructor...
    strategy = mock(AbstractReactorStreamProcessingStrategy.class, withSettings()
        .useConstructor(2, ((Supplier<SchedulerService>) () -> schedulerService), 2, 2, true).defaultAnswer(CALLS_REAL_METHODS));
    MockInjector.injectMocksFromSuite(this, strategy);
    when(event.getContext()).thenReturn(eventContext);
  }

  @Test
  void checkBackpressureEmittingWhenCapacityOver() {
    // First check should be free
    BackPressureReason firstResult = strategy.checkBackpressureEmitting(event);
    BackPressureReason secondResult = strategy.checkBackpressureEmitting(event);

    final BackPressureReason result = strategy.checkBackpressureEmitting(event);

    assertThat(firstResult, is(nullValue()));
    assertThat(secondResult, is(nullValue()));
    assertThat(result, is(BackPressureReason.MAX_CONCURRENCY_EXCEEDED));
  }

  @Test
  void backPressureCheckSetsBeforeOnResponseOnTheEventContext() {
    // First check should be free
    BackPressureReason firstResult = strategy.checkBackpressureEmitting(event);

    verify(eventContext).onBeforeResponse(consumerCaptor.capture());
    // This is really just for coverage... we don't really care what it does
    consumerCaptor.getValue().accept(event, new NullPointerException("testException"));
  }
}
