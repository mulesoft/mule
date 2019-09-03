/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.construct;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.core.api.construct.Flow.INITIAL_STATE_STOPPED;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.strategy.AsyncProcessingStrategyFactory.DEFAULT_MAX_CONCURRENCY;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.construct.DefaultFlowBuilder.DefaultFlow;
import org.mule.runtime.core.internal.processor.strategy.BlockingProcessingStrategyFactory;
import org.mule.runtime.core.internal.processor.strategy.WorkQueueStreamProcessingStrategyFactory;
import org.mule.runtime.core.internal.transformer.simple.StringAppendTransformer;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.core.lifecycle.LifecycleTrackerProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InOrder;

@RunWith(Parameterized.class)
public class DefaultFlowTestCase extends AbstractFlowConstructTestCase {

  private static final String FLOW_NAME = "test-flow";

  private DefaultFlowBuilder.DefaultFlow flow;
  private DefaultFlowBuilder.DefaultFlow stoppedFlow;
  private SensingNullMessageProcessor sensingMessageProcessor;
  private final BiFunction<Processor, CoreEvent, CoreEvent> triggerFunction;

  @Rule
  public ExpectedException expectedException = none();

  public DefaultFlowTestCase(String strategyName, BiFunction<Processor, CoreEvent, CoreEvent> triggerFunction) {
    this.triggerFunction = triggerFunction;
  }

  @Parameters(name = "{0}")
  public static Collection<Object[]> parameters() {
    BiFunction<Processor, CoreEvent, CoreEvent> blocking = (listener, event) -> just(event).transform(listener).block();
    BiFunction<Processor, CoreEvent, CoreEvent> async = (listener, event) -> {
      try {
        return listener.process(event);
      } catch (MuleException e) {
        throw new RuntimeException(e);
      }
    };
    return asList(new Object[][] {{"Blocking", blocking}, {"Async", async}});
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();

    sensingMessageProcessor = getSensingNullMessageProcessor();

    List<Processor> processors = new ArrayList<>();
    processors.add(new StringAppendTransformer("a"));
    processors.add(new StringAppendTransformer("b"));
    processors.add(new StringAppendTransformer("c"));
    processors.add(event -> CoreEvent.builder(event).addVariable("thread", currentThread()).build());
    processors.add(sensingMessageProcessor);

    flow = (DefaultFlow) Flow.builder(FLOW_NAME, muleContext)
        .source(directInboundMessageSource)
        .processors(processors)
        .build();

    stoppedFlow = (DefaultFlow) Flow.builder(FLOW_NAME, muleContext)
        .source(directInboundMessageSource)
        .processors(processors)
        .initialState(INITIAL_STATE_STOPPED)
        .build();
  }

  @Override
  protected AbstractFlowConstruct getFlowConstruct() throws Exception {
    return flow;
  }

  @Override
  protected AbstractFlowConstruct getStoppedFlowConstruct() throws Exception {
    return stoppedFlow;
  }

  @After
  public void after() throws MuleException {
    if (flow.isStarted()) {
      flow.stop();
    }

    if (flow.getLifecycleState().isInitialised()) {
      flow.dispose();
    }

    if (stoppedFlow.getLifecycleState().isInitialised()) {
      stoppedFlow.dispose();
    }
  }

  @Test
  public void testProcessStopped() throws Exception {
    flow.initialise();

    try {
      triggerFunction.apply(directInboundMessageSource.getListener(), testEvent());
      fail("exception expected");
    } catch (Exception e) {
    }
  }

  @Test
  public void restartWithBlockingProcessingStrategy() throws Exception {
    after();

    flow = (DefaultFlow) Flow.builder(FLOW_NAME, muleContext)
        .source(flow.getSource())
        .processors(flow.getProcessors())
        .processingStrategyFactory(new BlockingProcessingStrategyFactory())
        .build();

    flow.initialise();
    flow.start();

    flow.stop();
    flow.start();

    CoreEvent response = triggerFunction.apply(directInboundMessageSource.getListener(), testEvent());
    assertThat(response, not(nullValue()));
  }

  @Test
  public void testFailStartingMessageSourceOnLifecycleShouldStopStartedPipelineProcesses() throws Exception {
    // Need to start mule context to have endpoints started during flow start
    muleContext.start();

    MessageSource mockMessageSource = mock(MessageSource.class, withSettings().extraInterfaces(Startable.class, Stoppable.class));
    doThrow(new LifecycleException(mock(I18nMessage.class), mockMessageSource)).when(((Startable) mockMessageSource))
        .start();

    final List<Processor> processors = new ArrayList<>(flow.getProcessors());
    Processor mockMessageProcessor = spy(new LifecycleTrackerProcessor());
    processors.add(mockMessageProcessor);

    after();

    flow = (DefaultFlow) Flow.builder(FLOW_NAME, muleContext)
        .source(mockMessageSource)
        .processors(processors).build();

    flow.initialise();
    try {
      flow.start();
      fail();
    } catch (LifecycleException e) {
    }

    verify((Startable) mockMessageProcessor, times(1)).start();
    verify((Stoppable) mockMessageProcessor, times(1)).stop();

    verify((Startable) mockMessageSource, times(1)).start();
    verify((Stoppable) mockMessageSource, times(1)).stop();
  }

  @Test
  public void defaultMaxConcurrency() throws Exception {
    flow.initialise();
    flow.start();
    assertThat(flow.getMaxConcurrency(), equalTo(DEFAULT_MAX_CONCURRENCY));
    // When max concurrency is default Integer.MAX_VALUE then the Scheduler is created with no withMaxConcurrentTasks
    // configuration.
    verify(muleContext.getSchedulerService())
        .ioScheduler(eq(muleContext.getSchedulerBaseConfig().withName(flow.getName() + "." + BLOCKING.name())));
  }

  @Test
  public void customMaxConcurrency() throws Exception {
    int customMaxConcurrency = 1;
    Flow customFlow = Flow.builder(FLOW_NAME, muleContext)
        .source(directInboundMessageSource)
        .processors(getSensingNullMessageProcessor())
        .maxConcurrency(customMaxConcurrency)
        .build();
    try {
      customFlow.initialise();
      customFlow.start();
      assertThat(customFlow.getMaxConcurrency(), equalTo(customMaxConcurrency));
      verify(muleContext.getSchedulerService())
          .ioScheduler(eq(muleContext.getSchedulerBaseConfig().withName(flow.getName() + "." + BLOCKING.name())));
      customFlow.stop();

    } finally {
      customFlow.dispose();
    }
  }

  @Test
  public void illegalCustomMaxConcurrency() {
    expectedException.expect(IllegalArgumentException.class);
    Flow.builder(FLOW_NAME, muleContext)
        .source(directInboundMessageSource)
        .processors(getSensingNullMessageProcessor())
        .maxConcurrency(0)
        .build();
  }

  @Test
  public void lifecycleOrder() throws MuleException {
    Sink sink = mock(Sink.class, withSettings().extraInterfaces(Disposable.class));
    Processor processor = mock(Processor.class, withSettings().extraInterfaces(Startable.class, Stoppable.class));
    ProcessingStrategy processingStrategy =
        mock(ProcessingStrategy.class, withSettings().extraInterfaces(Startable.class, Stoppable.class));
    when(processingStrategy.createSink(any(FlowConstruct.class), any(ReactiveProcessor.class)))
        .thenReturn(sink);
    flow = (DefaultFlow) Flow.builder(FLOW_NAME, muleContext)
        .source(directInboundMessageSource)
        .processors(singletonList(processor))
        .processingStrategyFactory((muleContext, s) -> processingStrategy)
        .build();

    flow.initialise();
    flow.start();

    InOrder inOrder = inOrder(sink, processor, processingStrategy);

    inOrder.verify((Startable) processingStrategy).start();
    inOrder.verify(processingStrategy).createSink(any(FlowConstruct.class), any(ReactiveProcessor.class));
    inOrder.verify((Startable) processor).start();

    flow.stop();

    inOrder.verify((Disposable) sink).dispose();
    inOrder.verify((Stoppable) processor).stop();
    inOrder.verify((Stoppable) processingStrategy).stop();

  }

  @Test
  @Ignore("MULE-16210")
  public void originalExceptionThrownWhenStartAndStopOfProcessorBothFail() throws Exception {
    final Exception startException = new IllegalArgumentException();
    final Exception stopException = new IllegalStateException();

    Processor processor = mock(Processor.class, withSettings().extraInterfaces(Startable.class, Stoppable.class));
    doThrow(startException).when((Startable) processor).start();
    doThrow(stopException).when((Stoppable) processor).stop();

    flow = (DefaultFlow) Flow.builder(FLOW_NAME, muleContext)
        .source(directInboundMessageSource)
        .processors(singletonList(processor))
        .build();

    flow.initialise();
    try {
      flow.start();
      fail("was expecting failure");
    } catch (LifecycleException e) {
      assertThat(e.getCause(), instanceOf(MuleException.class));
      assertThat(e.getCause().getCause(), sameInstance(startException));
    }
  }

  @Test
  @Ignore("MULE-16210")
  public void originalExceptionThrownWhenStartAndStopOfSourceBothFail() throws Exception {
    final Exception startException = new IllegalArgumentException();
    final Exception stopException = new IllegalStateException();

    MessageSource source = mock(MessageSource.class, withSettings().extraInterfaces(Startable.class, Stoppable.class));
    doThrow(startException).when((Startable) source).start();
    doThrow(stopException).when((Stoppable) source).stop();

    muleContext = spy(muleContext);
    flow = (DefaultFlow) Flow.builder(FLOW_NAME, muleContext)
        .source(source)
        .processors(singletonList(mock(Processor.class)))
        .build();

    flow.initialise();
    muleContext.start();
    try {
      flow.start();
      fail("was expecting failure");
    } catch (LifecycleException e) {
      assertThat(e.getCause(), instanceOf(MuleException.class));
      assertThat(e.getCause().getCause(), sameInstance(startException));
    }
  }


  @Test
  public void workQueueSchedulerRejectsDoesntStartFlow() throws Exception {
    Processor processor = mock(Processor.class, withSettings().extraInterfaces(Startable.class, Stoppable.class));

    final Scheduler rejectingScheduler = mock(Scheduler.class);
    doThrow(new RejectedExecutionException("rejected by test")).when(rejectingScheduler).execute(any());

    final ProcessingStrategy processingStrategy = new WorkQueueStreamProcessingStrategyFactory() {

      @Override
      protected Supplier<Scheduler> getRingBufferSchedulerSupplier(MuleContext muleContext, String schedulersNamePrefix) {
        return () -> rejectingScheduler;
      }
    }.create(muleContext, FLOW_NAME);

    flow = (DefaultFlow) Flow.builder(FLOW_NAME, muleContext)
        .source(directInboundMessageSource)
        .processors(singletonList(processor))
        .processingStrategyFactory((muleContext, s) -> processingStrategy)
        .build();

    flow.initialise();

    expectedException.expectMessage("No subscriptions active for processor.");

    try {
      flow.start();
    } finally {
      verify(rejectingScheduler).shutdownNow();
    }
  }
}
