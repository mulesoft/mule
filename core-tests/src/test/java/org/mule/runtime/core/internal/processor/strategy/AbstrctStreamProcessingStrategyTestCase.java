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
import org.mule.runtime.core.api.construct.FlowConstruct;

import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Coverage tests for {@link AbstractStreamProcessingStrategyFactory.AbstractStreamProcessingStrategy} Note name is mis-spelled
 * intentionally as test cases with names starting with 'Abstract' are skipped.
 */
@ExtendWith(MockitoExtension.class)
class AbstrctStreamProcessingStrategyTestCase {

  private AbstractStreamProcessingStrategyFactory.AbstractStreamProcessingStrategy strategy;
  @Mock
  private FlowConstruct flow;
  @Mock
  private CountDownLatch countDown;
  @Mock
  private Supplier<Thread> threadSupplier;
  @Mock
  private Thread thread;

  @BeforeEach
  void setUp() {
    when(threadSupplier.get()).thenReturn(thread);
    strategy = mock(AbstractStreamProcessingStrategyFactory.AbstractStreamProcessingStrategy.class,
                    withSettings().useConstructor(2, 3, true, threadSupplier).defaultAnswer(CALLS_REAL_METHODS));
  }

  @Test
  void awaitSubscriberCompletion() throws InterruptedException {
    strategy.awaitSubscribersCompletion(flow,
                                        1L,
                                        countDown,
                                        1000);

    verify(countDown).await(anyLong(), eq(MILLISECONDS));
  }

  @Test
  void awaitSubscriberCompletion_interrupted() throws InterruptedException {
    when(countDown.await(anyLong(), any())).thenThrow(new InterruptedException("Oy - what's takin' so long!"));
    strategy.awaitSubscribersCompletion(flow,
                                        1L,
                                        countDown,
                                        1000);

    verify(thread).interrupt();
  }
}
