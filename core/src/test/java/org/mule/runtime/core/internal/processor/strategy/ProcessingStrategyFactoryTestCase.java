/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.construct.BackPressureReason;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.internal.profiling.InternalProfilingService;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.publisher.TestPublisher;
import reactor.test.subscriber.TestSubscriber;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.config.MuleRuntimeFeature.USE_TRANSACTION_SINK_INDEX;

/**
 * This test suite exists mostly to generate coverage - the actual checks in it are not strong. This covers a lot of basic method
 * calls, many of which have very little code but still lines that count for coverage, so... do 'em all and have a few more
 * specific tests for things that are more meaningful. That said, this test does check some easy things, including that we call
 * the feature flags service when we expected to, and that the results returned from methods are 'real'.
 */
@ExtendWith(MockitoExtension.class)
class ProcessingStrategyFactoryTestCase {

  static final String TEST_NAME = "[{index}]{displayName}_{0}";
  static final List<String> NON_SYNCHRONOUS =
      List.of("transactionAwareStreamEmitterFactory", "proacterWossnameStreamFactory", "streamEmitterProcessingStrategyFactory",
              "transactionAwareProactorWossnameStrategyFactory");
  @Mock
  private MuleContext context;
  @Mock
  private MuleConfiguration configuration;
  @Mock
  private FeatureFlaggingService featureFlaggingService;
  @Mock
  private InternalProfilingService profilingService;
  @Mock
  private FlowConstruct flow;
  @Mock
  private ReactiveProcessor pipeline;
  @Mock
  private Injector injector;
  @Mock
  private SchedulerConfig schedulerConfig;
  @Mock
  private SchedulerService schedulerService;
  @Mock
  private CoreEvent event;
  @Mock
  private Scheduler scheduler;
  @Mock
  private BaseEventContext eventContext;
  @Captor
  private ArgumentCaptor<Publisher<CoreEvent>> publisherCaptor;

  @ParameterizedTest(name = TEST_NAME)
  @MethodSource("strategyFactories")
  void createSink(String testName, ProcessingStrategyFactory factory,
                  String ignored, Consumer<ProcessingStrategyFactoryTestCase> setup,
                  BiConsumer<ProcessingStrategyFactoryTestCase, Object> assertions) {
    setup.accept(this);
    lenient().when(context.getConfiguration()).thenReturn(configuration);
    lenient().when(flow.getMuleContext()).thenReturn(context);

    final Sink result = getStrategy(factory, testName).createSink(flow, pipeline);
    if (result instanceof Disposable) {
      ((Disposable) result).dispose();
    }

    assertThat(result, is(notNullValue()));
    assertions.accept(this, result);
  }

  @ParameterizedTest(name = TEST_NAME)
  @MethodSource("strategyFactories")
  void accept(String testName, ProcessingStrategyFactory factory,
              String ignored, Consumer<ProcessingStrategyFactoryTestCase> setup,
              BiConsumer<ProcessingStrategyFactoryTestCase, Object> ignored2) {
    setup.accept(this);
    when(pipeline.apply(any())).thenAnswer(inv -> inv.getArgument(0));

    getStrategy(factory, testName).createSink(flow, pipeline).accept(event);

    verify(pipeline).apply(publisherCaptor.capture());
    // Hm. Limited things we can do - checking things about the type is too specific to the strategy...
  }

  @ParameterizedTest(name = TEST_NAME)
  @MethodSource("strategyFactories")
  void emit(String testName, ProcessingStrategyFactory factory,
            String ignored, Consumer<ProcessingStrategyFactoryTestCase> setup,
            BiConsumer<ProcessingStrategyFactoryTestCase, Object> ignored2) {
    setup.accept(this);
    when(pipeline.apply(any())).thenAnswer(inv -> inv.getArgument(0));

    final BackPressureReason result = getStrategy(factory, testName).createSink(flow, pipeline).emit(event);

    assertThat(testName, result, is(nullValue()));
  }

  @ParameterizedTest(name = TEST_NAME)
  @MethodSource("strategyFactories")
  void backpressureAccepting(String testName, ProcessingStrategyFactory factory,
                             String ignored, Consumer<ProcessingStrategyFactoryTestCase> setup,
                             BiConsumer<ProcessingStrategyFactoryTestCase, Object> ignored2) {
    setup.accept(this);

    getStrategy(factory, testName).checkBackpressureAccepting(event);
    // THere's not much to do here - the check throws an exception if the answer is 'no'... maybe set that up?
    verifyNoMoreInteractions(event);
  }

  @ParameterizedTest(name = TEST_NAME)
  @MethodSource("strategyFactories_byProcessType")
  void onProcessor(String testName, ProcessingStrategyFactory factory,
                   Consumer<ProcessingStrategyFactoryTestCase> setup,
                   ReactiveProcessor.ProcessingType type)
      throws MuleException {
    setup.accept(this);
    lenient().when(pipeline.getProcessingType()).thenReturn(type);
    lenient().when(context.getConfiguration()).thenReturn(configuration);
    lenient().when(profilingService.enrichWithProfilingEventFlux(any(), any(), any())).thenAnswer(inv -> inv.getArgument(0));
    lenient().when(profilingService.setCurrentExecutionContext((Flux<?>) any(), any())).thenAnswer(inv -> inv.getArgument(0));

    final ProcessingStrategy strategy = getStrategy(factory, testName);
    if (strategy instanceof Lifecycle) {
      setupStartStopContext();
      ((Lifecycle) strategy).start();
    }
    final ReactiveProcessor result = strategy.onProcessor(pipeline);
    TestPublisher<CoreEvent> testPublisher = TestPublisher.create();
    result.apply(testPublisher);
    testPublisher.next(event);

    verify(pipeline, atMostOnce()).getProcessingType();
    verify(pipeline, atMost(2)).apply(any());
    verifyNoMoreInteractions(pipeline);
  }

  @ParameterizedTest(name = TEST_NAME)
  @MethodSource("strategyFactories")
  void onPipeline(String testName, ProcessingStrategyFactory factory,
                  String ignored, Consumer<ProcessingStrategyFactoryTestCase> setup,
                  BiConsumer<ProcessingStrategyFactoryTestCase, Object> ignored2) {
    setup.accept(this);
    lenient().when(context.getConfiguration()).thenReturn(configuration);
    lenient().when(context.getArtifactType()).thenReturn(ArtifactType.APP);
    lenient().when(context.getSchedulerBaseConfig()).thenReturn(schedulerConfig);
    lenient().when(schedulerConfig.withName(any())).thenReturn(schedulerConfig);
    lenient().when(context.getSchedulerService()).thenReturn(schedulerService);
    lenient().when(schedulerService.cpuLightScheduler(any())).thenReturn(scheduler);

    getStrategy(factory, testName).onPipeline(pipeline);

    verifyNoMoreInteractions(pipeline);
  }

  @ParameterizedTest(name = TEST_NAME)
  @MethodSource("strategyFactories")
  void checkBackpressureEmitting(String testName, ProcessingStrategyFactory factory,
                                 String ignored,
                                 Consumer<ProcessingStrategyFactoryTestCase> setup,
                                 BiConsumer<ProcessingStrategyFactoryTestCase, Object> ignored2) {
    setup.accept(this);
    lenient().when(event.getContext()).thenReturn(eventContext);
    if (factory instanceof AbstractProcessingStrategyFactory) {
      ((AbstractProcessingStrategyFactory) factory).setMaxConcurrency(10);
      ((AbstractProcessingStrategyFactory) factory).setMaxConcurrencyEagerCheck(true);
    }

    BackPressureReason result = getStrategy(factory, testName).checkBackpressureEmitting(event);

    assertThat(result, is(nullValue()));
  }

  @ParameterizedTest(name = TEST_NAME)
  @MethodSource("strategyFactories")
  void registerInternalSink(String testName, ProcessingStrategyFactory factory, String ignored,
                            Consumer<ProcessingStrategyFactoryTestCase> setup,
                            BiConsumer<ProcessingStrategyFactoryTestCase, Object> ignored2) {
    setup.accept(this);

    TestPublisher<CoreEvent> internalSink = TestPublisher.create();
    getStrategy(factory, testName).registerInternalSink(internalSink, "testName");
    internalSink.emit(event);

    assertThat(internalSink.subscribeCount(), is(1L));
  }

  @ParameterizedTest(name = TEST_NAME)
  @MethodSource("strategyFactories")
  void isSynchronous(String testName, ProcessingStrategyFactory factory, String ignored,
                     Consumer<ProcessingStrategyFactoryTestCase> setup,
                     BiConsumer<ProcessingStrategyFactoryTestCase, Object> ignored2) {
    setup.accept(this);

    final boolean result = getStrategy(factory, testName).isSynchronous();

    assertThat(testName, result, is(!NON_SYNCHRONOUS.contains(testName)));
  }

  @ParameterizedTest(name = TEST_NAME)
  @MethodSource("strategyFactories")
  void configureInternalPublisher(String testName, ProcessingStrategyFactory factory, String ignored,
                                  Consumer<ProcessingStrategyFactoryTestCase> setup,
                                  BiConsumer<ProcessingStrategyFactoryTestCase, Object> ignored2) {
    setup.accept(this);

    TestPublisher<CoreEvent> internalSink = TestPublisher.create();
    final Publisher<CoreEvent> result = getStrategy(factory, testName).configureInternalPublisher(internalSink);
    TestSubscriber<CoreEvent> subscriber = TestSubscriber.create();
    result.subscribe(subscriber);

    assertThat(testName, result, is(notNullValue()));
  }

  @ParameterizedTest(name = TEST_NAME)
  @MethodSource("strategyFactories")
  void getProcessingStrategyType(String testName, ProcessingStrategyFactory factory, String strategyName,
                                 Consumer<ProcessingStrategyFactoryTestCase> ignored,
                                 BiConsumer<ProcessingStrategyFactoryTestCase, Object> ignored2) {

    final Class<? extends ProcessingStrategy> result = factory.getProcessingStrategyType();

    assertThat(result, is(notNullValue()));
    assertThat(testName, result.getSimpleName(), is(strategyName));
  }

  static List<Arguments> strategyFactories() {
    return List.of(
                   args("directStreamPerThreadFactory", new DirectProcessingStrategyFactory(),
                        ""),
                   args("transactionAwareStreamEmitterFactory", new TransactionAwareStreamEmitterProcessingStrategyFactory(),
                        "TransactionAwareStreamEmitterProcessingStrategyDecorator",
                        ProcessingStrategyFactoryTestCase::setupInjection,
                        (t, r) -> verify(t.featureFlaggingService).isEnabled(USE_TRANSACTION_SINK_INDEX)),
                   args("proacterWossnameStreamFactory", new ProactorStreamEmitterProcessingStrategyFactory(),
                        "ProactorStreamEmitterProcessingStrategy",
                        t -> lenient().when(t.pipeline.apply(any())).thenAnswer(inv -> inv.getArgument(0)), (t, r) -> {
                        }),
                   args("directStreamPerThreadStrategyFactory", new DirectStreamPerThreadProcessingStrategyFactory(),
                        "ProcessingStrategy"),
                   args("streamEmitterProcessingStrategyFactory", new StreamEmitterProcessingStrategyFactory(),
                        "StreamEmitterProcessingStrategy",
                        t -> lenient().when(t.pipeline.apply(any())).thenAnswer(inv -> inv.getArgument(0)), (t, r) -> {
                        }),
                   args("transactionAwareProactorWossnameStrategyFactory",
                        new TransactionAwareProactorStreamEmitterProcessingStrategyFactory(),
                        "TransactionAwareStreamEmitterProcessingStrategyDecorator",
                        ProcessingStrategyFactoryTestCase::setupInjection,
                        (t, r) -> {
                        }),
                   args("directProcessingStrategyFactory", new DirectProcessingStrategyFactory(),
                        ""),
                   args("blockingProcessStrategyFactory", new BlockingProcessingStrategyFactory(),
                        "BlockingProcessingStrategy"));
  }

  static List<Arguments> strategyFactories_byProcessType() {
    return strategyFactories().stream()
        .flatMap(args -> Arrays.stream(ReactiveProcessor.ProcessingType.values())
            .map(type -> Arguments.of(args.get()[0], args.get()[1], args.get()[3], type)))
        .toList();
  }

  @Test
  void transactionAwareDirectStreamFactory_initializationError() {
    setupInitFailure();
    final TransactionAwareStreamEmitterProcessingStrategyFactory factory =
        new TransactionAwareStreamEmitterProcessingStrategyFactory();

    assertThrows(MuleRuntimeException.class, () -> getStrategy(factory, "foo").createSink(flow, pipeline));

  }

  @ParameterizedTest(name = TEST_NAME)
  @MethodSource("lifecycleFactories")
  void start_stop(String testName, ProcessingStrategyFactory factory, String ignored3,
                  Consumer<ProcessingStrategyFactoryTestCase> setup,
                  BiConsumer<ProcessingStrategyFactoryTestCase, Object> ignored2)
      throws MuleException {
    setupStartStopContext();
    setup.accept(this);
    ProcessingStrategy strategy = getStrategy(factory, testName);
    MockInjector.injectMocksFromSuite(this, strategy);
    Lifecycle lifecycle = (Lifecycle) strategy;

    try {
      lifecycle.initialise();
      lifecycle.start();
    } finally {
      lifecycle.stop();
      lifecycle.dispose();
    }

    verify(context, atLeastOnce()).getArtifactType();
  }

  static List<Arguments> lifecycleFactories() {
    return strategyFactories().stream()
        .filter(args -> Lifecycle.class.isAssignableFrom(((ProcessingStrategyFactory) args.get()[1]).getProcessingStrategyType()))
        .toList();
  }

  @Test
  void transactionAwareProactorWossnameStrategyFactory_initializationFails() {
    setupInitFailure();
    final TransactionAwareProactorStreamEmitterProcessingStrategyFactory factory =
        new TransactionAwareProactorStreamEmitterProcessingStrategyFactory();

    assertThrows(MuleRuntimeException.class, () -> getStrategy(factory, "foo").createSink(flow, pipeline));
  }

  /*
   *
   * Utility methods for setting up tests.
   *
   */

  private void setupInjection() {
    try {
      lenient().when(pipeline.apply(any())).thenAnswer(inv -> inv.getArgument(0));
      lenient().when(context.getInjector()).thenReturn(injector);
      lenient().when(injector.inject(any())).thenAnswer(inv -> {
        final Object o = inv.getArgument(0);
        MockInjector.injectMocksFromSuite(this, o);
        return o;
      });
    } catch (MuleException e) {
      throw new RuntimeException(e);
    }
  }

  private void setupInitFailure() {
    try {
      when(context.getInjector()).thenReturn(injector);
      when(injector.inject(any()))
          .thenThrow(new MuleException(I18nMessageFactory.createStaticMessage("Testing initialization errors")) {});
    } catch (MuleException e) {
      throw new RuntimeException(e);
    }
  }

  private void setupStartStopContext() {
    when(context.getSchedulerBaseConfig()).thenReturn(schedulerConfig);
    when(context.getConfiguration()).thenReturn(configuration);
    when(context.getSchedulerService()).thenReturn(schedulerService);
    when(schedulerService.cpuLightScheduler(any())).thenReturn(scheduler);
    when(context.getArtifactType()).thenReturn(ArtifactType.APP);
  }

  private ProcessingStrategy getStrategy(ProcessingStrategyFactory factory, String testName) {
    ProcessingStrategy strategy = factory.create(context, testName);
    MockInjector.injectMocksFromSuite(this, strategy);
    return strategy;
  }

  private static Arguments args(String testName, ProcessingStrategyFactory factory, String strategyName) {
    return args(testName, factory, strategyName, t -> {
    }, (t, r) -> {
    });
  }

  private static Arguments args(String testName, ProcessingStrategyFactory factory, String strategyName,
                                Consumer<ProcessingStrategyFactoryTestCase> setup,
                                BiConsumer<ProcessingStrategyFactoryTestCase, Object> assertions) {
    return Arguments.of(testName, factory, strategyName, setup, assertions);
  }
}
