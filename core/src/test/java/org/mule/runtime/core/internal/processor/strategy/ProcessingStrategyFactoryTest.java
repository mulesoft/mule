/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import org.apache.logging.log4j.core.tools.picocli.CommandLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.reactivestreams.Publisher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.config.MuleRuntimeFeature.USE_TRANSACTION_SINK_INDEX;

@ExtendWith(MockitoExtension.class)
class ProcessingStrategyFactoryTest {

  @Mock
  private MuleContext context;
  @Mock
  private MuleConfiguration configuration;
  @Mock
  private FeatureFlaggingService featureFlaggingService;
  @Mock
  private FlowConstruct flow;
  @Mock
  private ReactiveProcessor pipeline;
  @Mock
  private Injector injector;
  @Mock
  private Publisher<CoreEvent> publisher;
  @Mock
  private SchedulerConfig schedulerConfig;
  @Mock
  private SchedulerService schedulerService;

  @Test
  void directStreamPerThreadFactory() {
    final DirectStreamPerThreadProcessingStrategyFactory factory = new DirectStreamPerThreadProcessingStrategyFactory();

    final Sink result = factory.create(context, "foo").createSink(flow, pipeline);

    assertThat(result, is(notNullValue()));
  }

  @Test
  void transactionAwareDirectStreamFactory() throws MuleException {
    setupInjection();
    final TransactionAwareStreamEmitterProcessingStrategyFactory factory =
        new TransactionAwareStreamEmitterProcessingStrategyFactory();

    final Sink result = factory.create(context, "foo").createSink(flow, pipeline);

    assertThat(result, is(notNullValue()));
    // Maybe not worth checking... but we do check it.
    verify(featureFlaggingService).isEnabled(USE_TRANSACTION_SINK_INDEX);
  }

  @Test
  void transactionAwareDirectStreamFactory_initializationError() throws MuleException {
    setupInitFailure();
    final TransactionAwareStreamEmitterProcessingStrategyFactory factory =
        new TransactionAwareStreamEmitterProcessingStrategyFactory();

    assertThrows(MuleRuntimeException.class, () -> factory.create(context, "foo").createSink(flow, pipeline));

  }

  @Test
  void proacterWossnameStreamFactory() {
    when(pipeline.apply(any())).thenAnswer(inv -> inv.getArgument(0));
    final ProactorStreamEmitterProcessingStrategyFactory factory = new ProactorStreamEmitterProcessingStrategyFactory();

    final Sink result = factory.create(context, "foo").createSink(flow, pipeline);

    assertThat(result, is(notNullValue()));
  }

  @Test
  void directStreamPerThreadStrategyFactory() {
    final DirectStreamPerThreadProcessingStrategyFactory factory = new DirectStreamPerThreadProcessingStrategyFactory();

    final Sink result = factory.create(context, "foo").createSink(flow, pipeline);

    assertThat(result, is(notNullValue()));
  }

  @Test
  void streamEmitterProcessingStrategyFactory_createSink() {
    when(pipeline.apply(any())).thenAnswer(inv -> inv.getArgument(0));
    final StreamEmitterProcessingStrategyFactory factory = new StreamEmitterProcessingStrategyFactory();

    final Sink result = factory.create(context, "foo").createSink(flow, pipeline);

    assertThat(result, is(notNullValue()));
  }

  @Test
  void streamEmitterProcessingStrategyFactory_start_stop() throws MuleException {
    when(context.getSchedulerBaseConfig()).thenReturn(schedulerConfig);
    when(context.getConfiguration()).thenReturn(configuration);
    when(context.getSchedulerService()).thenReturn(schedulerService);
    when(context.getArtifactType()).thenReturn(ArtifactType.APP);
    final StreamEmitterProcessingStrategyFactory factory = new StreamEmitterProcessingStrategyFactory();

    StreamEmitterProcessingStrategyFactory.StreamEmitterProcessingStrategy strategy =
        (StreamEmitterProcessingStrategyFactory.StreamEmitterProcessingStrategy) factory.create(context, "foo");
    MockInjector.injectMocksFromSuite(this, strategy);

    try {
      strategy.initialise();
      strategy.start();
    } finally {
      strategy.stop();
      strategy.dispose();
    }

    verify(context).getArtifactType();
  }

  @Test
  void streamEmitterProcessingStrategyFactory_checkBackpressure() {
    final StreamEmitterProcessingStrategyFactory factory = new StreamEmitterProcessingStrategyFactory();

    factory.create(context, "foo").registerInternalSink(publisher, "stinkingSinkTest");

    verify(publisher).subscribe(any());
  }

  @Test
  void transactionAwareProactorWossnameStrategyFactory() throws MuleException {
    setupInjection();
    final TransactionAwareProactorStreamEmitterProcessingStrategyFactory factory =
        new TransactionAwareProactorStreamEmitterProcessingStrategyFactory();

    final Sink result = factory.create(context, "foo").createSink(flow, pipeline);

    assertThat(result, is(notNullValue()));
  }

  @Test
  void transactionAwareProactorWossnameStrategyFactory_initializationFails() throws MuleException {
    setupInitFailure();
    final TransactionAwareProactorStreamEmitterProcessingStrategyFactory factory =
        new TransactionAwareProactorStreamEmitterProcessingStrategyFactory();

    assertThrows(MuleRuntimeException.class, () -> factory.create(context, "foo").createSink(flow, pipeline));
  }

  /*
   *
   * Utility methods for setting up tests.
   *
   */

  private void setupInjection() throws MuleException {
    when(pipeline.apply(any())).thenAnswer(inv -> inv.getArgument(0));
    when(context.getInjector()).thenReturn(injector);
    when(injector.inject(any())).thenAnswer(inv -> {
      final Object o = inv.getArgument(0);
      MockInjector.injectMocksFromSuite(this, o);
      return o;
    });
  }

  private void setupInitFailure() throws MuleException {
    when(context.getInjector()).thenReturn(injector);
    when(injector.inject(any()))
        .thenThrow(new MuleException(I18nMessageFactory.createStaticMessage("Testing initialization errors")) {});
  }
}
