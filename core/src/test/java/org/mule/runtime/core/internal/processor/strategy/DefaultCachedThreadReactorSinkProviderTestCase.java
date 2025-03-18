/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import reactor.core.publisher.FluxSink;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultCachedThreadReactorSinkProviderTestCase {

  private static ExecutorService service;
  private DefaultCachedThreadReactorSinkProvider provider;
  @Mock
  private FlowConstruct flow;
  @Mock
  private ReactiveProcessor processor;
  @Mock
  private MuleContext context;
  @Mock
  private MuleConfiguration configuration;
  @Mock
  private Consumer<CoreEvent> consumer;
  @Mock
  private FeatureFlaggingService flaggingService;
  @Mock
  private CoreEvent event;

  @BeforeAll
  static void setupExecutor() {
    service = Executors.newSingleThreadExecutor();
  }

  @AfterAll
  static void teardownExecutor() {
    service.shutdown();
  }

  @BeforeEach
  void setUp() {
    provider = new DefaultCachedThreadReactorSinkProvider(flow, processor, consumer, flaggingService);
    when(processor.apply(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void consume_error() {
    doThrow(new NullPointerException("Bitey bitey bite!")).when(consumer).accept(any());

    final FluxSink<CoreEvent> result = provider.createSink();
    result.next(event);
    result.complete();

    verify(consumer).accept(event);
  }

  @Test
  void createSink() {

    final FluxSink<CoreEvent> result = provider.createSink();
    result.next(event);

    verify(consumer).accept(event);
  }

  @Test
  void dispose_withAllFinished() throws ExecutionException, InterruptedException, TimeoutException {
    when(flow.getMuleContext()).thenReturn(context);
    when(context.getConfiguration()).thenReturn(configuration);
    when(configuration.getShutdownTimeout()).thenReturn(10L);
    final FluxSink<CoreEvent> sink = provider.createSink(); // Create a sink so there's something to dispose...
    final Future<?> fut = disposeInFuture(10);
    sink.complete();
    fut.get(100L, TimeUnit.MILLISECONDS);

    verify(flow, never()).getName(); // We do this as part of the logging...
  }

  @Test
  void dispose_withInterrupt() throws InterruptedException {
    when(flow.getMuleContext()).thenReturn(context);
    when(context.getConfiguration()).thenReturn(configuration);
    when(configuration.getShutdownTimeout()).thenReturn(10L);
    final FluxSink<CoreEvent> sink = provider.createSink(); // Create a sink so there's something to dispose...
    final Future<?> fut = disposeInFuture(1000);
    fut.cancel(true);
    Thread.sleep(10L);
    assertThrows(CancellationException.class, () -> fut.get(100L, TimeUnit.MILLISECONDS));

    assertThat(fut.isCancelled(), is(true));
    verify(flow).getName(); // We do this as part of the logging...
  }

  @Test
  void dispose_withTimeout() throws ExecutionException, InterruptedException, TimeoutException {
    when(flow.getMuleContext()).thenReturn(context);
    when(context.getConfiguration()).thenReturn(configuration);
    when(configuration.getShutdownTimeout()).thenReturn(-10L);
    final FluxSink<CoreEvent> sink = provider.createSink(); // Create a sink so there's something to dispose...
    final Future<?> fut = disposeInFuture(100);
    fut.get(100L, TimeUnit.MILLISECONDS);

    verify(flow).getName(); // We do this as part of the logging...
  }

  private Future<?> disposeInFuture(int timeout) throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);

    final Future<?> fut = service.submit(() -> {
      latch.countDown();
      provider.dispose();
    });
    latch.await(timeout, TimeUnit.MILLISECONDS);
    return fut;
  }
}
