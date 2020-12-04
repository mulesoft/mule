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
import static java.util.Collections.singletonMap;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.construct.Flow.INITIAL_STATE_STOPPED;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.api.processor.strategy.AsyncProcessingStrategyFactory.DEFAULT_MAX_CONCURRENCY;
import static org.mule.runtime.core.api.rx.Exceptions.propagateWrappingFatal;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContext;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.deployment.management.ComponentInitialStateManager;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.construct.DefaultFlowBuilder.DefaultFlow;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.processor.strategy.BlockingProcessingStrategyFactory;
import org.mule.runtime.core.internal.processor.strategy.ProactorStreamEmitterProcessingStrategyFactory;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.core.lifecycle.LifecycleTrackerProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InOrder;
import org.reactivestreams.Publisher;

import io.qameta.allure.Issue;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

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
    flow.setAnnotations(singletonMap(LOCATION_KEY, fromSingleComponent("flow")));
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

    startFlow();

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
    flow.setAnnotations(singletonMap(LOCATION_KEY, fromSingleComponent("flow")));

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
    startFlow();
    assertThat(flow.getMaxConcurrency(), equalTo(DEFAULT_MAX_CONCURRENCY));
    // When max concurrency is default Integer.MAX_VALUE then the Scheduler is created with no withMaxConcurrentTasks
    // configuration.
    verify(muleContext.getSchedulerService())
        .cpuLightScheduler(eq(muleContext.getSchedulerBaseConfig().withName(flow.getName() + "." + CPU_LITE.name())));
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
      customFlow.setAnnotations(singletonMap(LOCATION_KEY, fromSingleComponent("flow")));
      customFlow.initialise();
      customFlow.start();
      assertThat(customFlow.getMaxConcurrency(), equalTo(customMaxConcurrency));
      verify(muleContext.getSchedulerService())
          .cpuLightScheduler(eq(muleContext.getSchedulerBaseConfig().withName(flow.getName() + "." + CPU_LITE.name())));
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

    startFlow();

    InOrder inOrder = inOrder(sink, processor, processingStrategy);

    inOrder.verify((Startable) processingStrategy).start();
    inOrder.verify((Startable) processor).start();
    inOrder.verify(processingStrategy).createSink(any(FlowConstruct.class), any(ReactiveProcessor.class));

    flow.stop();

    inOrder.verify((Disposable) sink).dispose();
    inOrder.verify((Stoppable) processor).stop();
    inOrder.verify((Stoppable) processingStrategy).stop();
  }

  @Test
  public void lifecycleOrderWithErrorHandler() throws MuleException {
    Sink sink = mock(Sink.class, withSettings().extraInterfaces(Disposable.class));
    Processor processor = mock(Processor.class, withSettings().extraInterfaces(Startable.class, Stoppable.class));
    ProcessingStrategy processingStrategy =
        mock(ProcessingStrategy.class, withSettings().extraInterfaces(Startable.class, Stoppable.class));
    when(processingStrategy.createSink(any(FlowConstruct.class), any(ReactiveProcessor.class)))
        .thenReturn(sink);

    final FlowExceptionHandler errorHandler = mock(FlowExceptionHandler.class, withSettings().extraInterfaces(Lifecycle.class));

    flow = (DefaultFlow) Flow.builder(FLOW_NAME, muleContext)
        .source(directInboundMessageSource)
        .processors(singletonList(processor))
        .processingStrategyFactory((muleContext, s) -> processingStrategy)
        .messagingExceptionHandler(errorHandler)
        .build();

    startFlow();

    InOrder inOrder = inOrder(sink, processor, errorHandler, processingStrategy);

    inOrder.verify((Startable) processingStrategy).start();
    inOrder.verify((Startable) errorHandler).start();
    inOrder.verify((Startable) processor).start();
    inOrder.verify(processingStrategy).createSink(any(FlowConstruct.class), any(ReactiveProcessor.class));

    flow.stop();

    inOrder.verify((Disposable) sink).dispose();
    inOrder.verify((Stoppable) processor).stop();
    inOrder.verify((Stoppable) errorHandler).stop();
    inOrder.verify((Stoppable) processingStrategy).stop();
  }

  @Test
  public void lifecycleOrderDispose() throws MuleException {
    Sink sink = mock(Sink.class, withSettings().extraInterfaces(Disposable.class));
    Processor processor = mock(Processor.class, withSettings().extraInterfaces(Disposable.class));
    ProcessingStrategy processingStrategy =
        mock(ProcessingStrategy.class, withSettings().extraInterfaces(Disposable.class));
    when(processingStrategy.createSink(any(FlowConstruct.class), any(ReactiveProcessor.class)))
        .thenReturn(sink);
    flow = (DefaultFlow) Flow.builder(FLOW_NAME, muleContext)
        .source(directInboundMessageSource)
        .processors(singletonList(processor))
        .processingStrategyFactory((muleContext, s) -> processingStrategy)
        .build();

    flow.setAnnotations(singletonMap(LOCATION_KEY, fromSingleComponent("flow")));
    flow.initialise();

    InOrder inOrder = inOrder(sink, processor, processingStrategy);

    flow.dispose();

    inOrder.verify((Disposable) processor).dispose();
    inOrder.verify((Disposable) processingStrategy).dispose();
  }

  @Test
  public void lifecycleOrderDisposeWithErrorHandler() throws MuleException {
    Sink sink = mock(Sink.class, withSettings().extraInterfaces(Disposable.class));
    Processor processor = mock(Processor.class, withSettings().extraInterfaces(Disposable.class));
    ProcessingStrategy processingStrategy =
        mock(ProcessingStrategy.class, withSettings().extraInterfaces(Disposable.class));
    when(processingStrategy.createSink(any(FlowConstruct.class), any(ReactiveProcessor.class)))
        .thenReturn(sink);

    final FlowExceptionHandler errorHandler = mock(FlowExceptionHandler.class, withSettings().extraInterfaces(Lifecycle.class));

    flow = (DefaultFlow) Flow.builder(FLOW_NAME, muleContext)
        .source(directInboundMessageSource)
        .processors(singletonList(processor))
        .processingStrategyFactory((muleContext, s) -> processingStrategy)
        .messagingExceptionHandler(errorHandler)
        .build();
    flow.setAnnotations(singletonMap(LOCATION_KEY, fromSingleComponent("flow")));

    flow.initialise();

    InOrder inOrder = inOrder(sink, processor, errorHandler, processingStrategy);

    flow.dispose();

    inOrder.verify((Disposable) errorHandler).dispose();
    inOrder.verify((Disposable) processor).dispose();
    inOrder.verify((Disposable) processingStrategy).dispose();
  }

  @Test
  @Issue("MULE-18089")
  public void lifecycleWithStartSourceFalse() throws Exception {
    MuleContextWithRegistry mcwr = (MuleContextWithRegistry) muleContext;
    final MuleRegistry registry = spy(mcwr.getRegistry());
    final ComponentInitialStateManager initialStateManager = mock(ComponentInitialStateManager.class);
    when(initialStateManager.mustStartMessageSource(any())).thenReturn(false);
    when(registry.lookupObject(ComponentInitialStateManager.SERVICE_ID)).thenReturn(initialStateManager);
    when(mcwr.getRegistry()).thenReturn(registry);
    directInboundMessageSource = spy(directInboundMessageSource);

    muleContext.start();

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

    startFlow();

    InOrder inOrder = inOrder(sink, processor, processingStrategy);

    verify((Startable) directInboundMessageSource, never()).start();
    inOrder.verify((Startable) processingStrategy).start();
    inOrder.verify((Startable) processor).start();
    inOrder.verify(processingStrategy).createSink(any(FlowConstruct.class), any(ReactiveProcessor.class));

    flow.stop();

    verify((Stoppable) directInboundMessageSource, never()).stop();
    inOrder.verify((Disposable) sink).dispose();
    inOrder.verify((Stoppable) processor).stop();
    inOrder.verify((Stoppable) processingStrategy).stop();

    muleContext.stop();
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
  @Issue("MULE-17386")
  public void doNotExecuteOnErrorContinueDefinedOutsideTheFlow() throws MuleException {
    AtomicBoolean nonExpectedError = new AtomicBoolean();
    final BiFunction<Processor, CoreEvent, CoreEvent> triggerFunction =
        (listener, event) -> just(event).transform(listener).onErrorContinue((e, o) -> nonExpectedError.set(true)).block();

    flow = (DefaultFlow) Flow.builder(FLOW_NAME, muleContext)
        .source(flow.getSource())
        .processors(singletonList(new ErrorProcessor()))
        .processingStrategyFactory(new BlockingProcessingStrategyFactory())
        .build();
    startFlow();

    CoreEvent response = triggerFunction.apply(directInboundMessageSource.getListener(), testEvent());
    assertThat(response, nullValue());
    assertThat(nonExpectedError.get(), is(false));
  }

  @Test
  @Issue("MULE-18873")
  public void flowInsideProcessWithChildContextMustNotDropEvents()
      throws MuleException {
    CoreEvent testEvent = testEvent();
    flow = (DefaultFlow) Flow.builder(FLOW_NAME, muleContext)
        .source(flow.getSource())
        .processors(singletonList(new BlockMessageProcessor()))
        .processingStrategyFactory(new ProactorStreamEmitterProcessingStrategyFactory())
        .build();
    startFlow();
    Flux<CoreEvent> flowProcessing = Flux
        .from(processWithChildContext(testEvent, flow, of(fromSingleComponent(FLOW_NAME))));
    StepVerifier.create(flowProcessing)
        .expectNext(testEvent)
        .expectComplete()
        .verifyThenAssertThat()
        .hasNotDroppedElements();
  }

  private void startFlow() throws MuleException {
    flow.setAnnotations(singletonMap(LOCATION_KEY, fromSingleComponent("flow")));
    flow.initialise();
    flow.start();
  }

  public static class ErrorProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      throw new MessagingException(createStaticMessage("Test error"), event);
    }

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
      return Flux.from(publisher)
          .doOnNext(event -> {
            throw propagateWrappingFatal(new MessagingException(createStaticMessage("message"), event));
          });
    }
  }

  public static class BlockMessageProcessor extends AbstractComponent implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      return sleepFor(event, muleContext.getConfiguration().getShutdownTimeout() * 2);
    }

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> eventPublisher) {
      return Flux.from(eventPublisher).map(event -> sleepFor(event, muleContext.getConfiguration().getShutdownTimeout() * 2));
    }
  }

}
