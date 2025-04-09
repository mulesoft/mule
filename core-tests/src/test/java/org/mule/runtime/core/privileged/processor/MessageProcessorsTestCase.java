/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor;

import static org.mule.functional.junit4.matchers.ThrowableRootCauseMatcher.hasRootCause;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SUB_FLOW;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.CoreEvent.builder;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.WITHIN_PROCESS_WITH_CHILD_CONTEXT;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.applyWithChildContext;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.applyWithChildContextDontPropagateErrors;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChildContext;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApplyWithChildContext;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContext;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContextBlocking;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContextDontComplete;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.from;

import static java.util.Arrays.asList;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.internal.matchers.ThrowableCauseMatcher.hasCause;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.component.location.LocationPart;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.construct.DefaultFlowBuilder.DefaultFlow;
import org.mule.runtime.core.internal.policy.DefaultPolicyInstance;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.event.DefaultFlowCallStack;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.exception.MessagingException;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import org.reactivestreams.Publisher;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Issue;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;
import reactor.util.context.Context;

@SmallTest
public class MessageProcessorsTestCase extends AbstractMuleContextTestCase {


  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private final RuntimeException exception = new IllegalArgumentException();
  private BaseEventContext eventContext;
  private CoreEvent input;
  private CoreEvent output;
  private CoreEvent response;
  private Flow flow;
  private Publisher<CoreEvent> responsePublisher;

  private Processor chain;

  @Before
  public void setup() throws MuleException {
    flow = mock(Flow.class, RETURNS_DEEP_STUBS);
    eventContext = (BaseEventContext) create(flow, TEST_CONNECTOR_LOCATION);
    input = builder(eventContext).message(of(TEST_MESSAGE)).build();
    output = builder(eventContext).message(of(TEST_MESSAGE)).build();
    response = builder(eventContext).message(of(TEST_MESSAGE)).build();
    responsePublisher = eventContext.getResponsePublisher();
  }

  @After
  public void tearDown() throws MuleException {
    if (flow != null) {
      flow.stop();
      flow.dispose();
    }

    if (chain != null) {
      stopIfNeeded(chain);
      disposeIfNeeded(chain, getLogger(getClass()));

      chain = null;
    }
  }

  private final InternalReactiveProcessor map = publisher -> from(publisher).map(in -> output);
  private final InternalReactiveProcessor ackAndStop = publisher -> from(publisher).flatMap(in -> {
    ((BaseEventContext) in.getContext()).success();
    return empty();
  });
  private final InternalReactiveProcessor respondAndStop = publisher -> from(publisher).flatMap(in -> {
    ((BaseEventContext) in.getContext()).success(response);
    return empty();
  });
  private final InternalReactiveProcessor ackAndMap =
      publisher -> from(publisher).doOnNext(in -> ((BaseEventContext) in.getContext()).success()).map(in -> output);
  private final InternalReactiveProcessor respondAndMap =
      publisher -> from(publisher).doOnNext(in -> ((BaseEventContext) in.getContext()).success(response)).map(in -> output);
  private final InternalReactiveProcessor error = publisher -> from(publisher).map(in -> {
    throw exception;
  });

  @Test
  public void processToApplyMap() throws Exception {
    assertThat(processToApply(input, map), is(output));
    assertThat(from(responsePublisher).toFuture().isDone(), is(false));
  }

  @Test
  public void processToApplyMapInChain() throws Exception {
    chain = createChain(map);
    assertThat(processToApply(input, chain), is(output));
    assertThat(from(responsePublisher).toFuture().isDone(), is(false));
  }

  @Test
  public void processToApplyMapInFlow() throws Exception {
    assertThat(processToApply(input, createFlow(map)).getMessage(), is(output.getMessage()));
    assertThat(from(responsePublisher).block(), is(output));
  }

  @Test
  public void processToApplyAckAndStop() throws Exception {
    assertThat(processToApply(input, ackAndStop), is(nullValue()));
    assertThat(from(responsePublisher).block(), is(nullValue()));
  }

  @Test
  public void processToApplyAckAndStopInChain() throws Exception {
    chain = createChain(ackAndStop);
    assertThat(processToApply(input, chain), is(nullValue()));
    assertThat(from(responsePublisher).block(), is(nullValue()));
  }

  @Test
  public void processToApplyAckAndStopInFlow() throws Exception {
    assertThat(processToApply(input, createFlow(ackAndStop)), is(nullValue()));
    assertThat(from(responsePublisher).block(), is(nullValue()));
  }

  @Test
  public void processToApplyRespondAndStop() throws Exception {
    assertThat(processToApply(input, respondAndStop), is(response));
    assertThat(from(responsePublisher).block(), is(response));
  }

  @Test
  public void processToApplyRespondAndStopInChain() throws Exception {
    chain = createChain(respondAndStop);
    assertThat(processToApply(input, chain), is(response));
    assertThat(from(responsePublisher).block(), is(response));
  }

  @Test
  public void processToApplyRespondAndStopInFlow() throws Exception {
    assertThat(processToApply(input, createFlow(respondAndStop)).getMessage(), is(response.getMessage()));
    assertThat(from(responsePublisher).block(), is(response));
  }

  @Test
  public void processToApplyAckAndMap() throws Exception {
    assertThat(processToApply(input, ackAndMap), is(output));
    assertThat(from(responsePublisher).block(), is(nullValue()));
  }

  @Test
  public void processToApplyAckAndMapInChain() throws Exception {
    chain = createChain(ackAndMap);
    assertThat(processToApply(input, chain), is(output));
    assertThat(from(responsePublisher).block(), is(nullValue()));
  }

  @Test
  public void processToApplyAckAndMapInFlow() throws Exception {
    assertThat(processToApply(input, createFlow(ackAndMap)), is(nullValue()));
    assertThat(from(responsePublisher).block(), is(nullValue()));
  }

  @Test
  public void processToApplyRespondAndMap() throws Exception {
    assertThat(processToApply(input, respondAndMap), is(output));
    assertThat(from(responsePublisher).block(), is(response));
  }

  @Test
  public void processToApplyRespondAndMapInChain() throws Exception {
    chain = createChain(respondAndMap);
    assertThat(processToApply(input, chain), is(output));
    assertThat(from(responsePublisher).block(), is(response));
  }

  @Test
  public void processToApplyRespondAndMapInFlow() throws Exception {
    assertThat(processToApply(input, createFlow(respondAndMap)).getMessage(), is(response.getMessage()));
    assertThat(from(responsePublisher).block(), is(response));
  }

  @Test
  public void processToApplyError() throws Exception {
    thrown.expect(is(exception));
    try {
      processToApply(input, error);
    } finally {
      assertThat(from(responsePublisher).toFuture().isDone(), is(false));
    }
  }

  @Test
  public void processToApplyWithChildContextCompletes() throws MuleException {
    AtomicBoolean completed = new AtomicBoolean();
    ((BaseEventContext) (input.getContext())).onComplete((e, t) -> completed.set(true));

    final CoreEvent result = processToApplyWithChildContext(input, publisher -> from(publisher));
    ((BaseEventContext) result.getContext()).success();

    assertThat(((BaseEventContext) result.getContext()).isComplete(), is(true));
    assertThat(completed.get(), is(true));
  }

  @Test
  public void processWrappedInAMonoAlwaysRecognizesItselfAsAProcessWithChildContext() {
    AtomicBoolean completed = new AtomicBoolean();
    ((BaseEventContext) (input.getContext())).onComplete((e, t) -> completed.set(true));

    final CoreEvent result = from(MessageProcessors.process(input,
                                                            pub -> Flux
                                                                .from(applyWithChildContext(pub, eventPub -> Flux.from(eventPub)
                                                                    .contextWrite(ctx -> subscriberContextRecognizesChildContext(ctx)),
                                                                                            Optional.empty()))))
        .block();

    assertThat(((BaseEventContext) result.getContext()).isComplete(), is(true));
    assertThat(completed.get(), is(true));
  }


  private Context subscriberContextRecognizesChildContext(Context ctx) {
    assertThat(ctx.get(WITHIN_PROCESS_WITH_CHILD_CONTEXT), is(true));
    return ctx;
  }

  @Test
  public void processToApplyWithChildContextEmptyCompletes() throws MuleException {
    AtomicBoolean completed = new AtomicBoolean();
    ((BaseEventContext) (input.getContext())).onComplete((e, t) -> completed.set(true));

    final CoreEvent result = processToApplyWithChildContext(input, publisher -> from(publisher)
        .doOnNext(ev -> ((BaseEventContext) ev.getContext()).success()));
    ((BaseEventContext) result.getContext()).success();

    assertThat(((BaseEventContext) result.getContext()).isComplete(), is(true));
    assertThat(completed.get(), is(true));
  }

  @Test
  public void processToApplyWithChildContextProcessNested() throws MuleException {
    AtomicBoolean completed = new AtomicBoolean();
    ((BaseEventContext) (input.getContext())).onComplete((e, t) -> completed.set(true));

    final CoreEvent result = processToApplyWithChildContext(input, publisher -> from(publisher)
        .flatMap(e -> Mono.from(processWithChildContext(e, p -> from(p), Optional.empty()))));
    ((BaseEventContext) result.getContext()).success();

    assertThat(((BaseEventContext) result.getContext()).isComplete(), is(true));
    assertThat(completed.get(), is(true));
  }

  @Test
  public void processWithChildContextBlockingErrorInChainRegainsParentContext() throws Exception {
    chain = createChain(error);
    try {
      processWithChildContextBlocking(input, chain, Optional.empty());
      fail("Exception expected");
    } catch (Throwable t) {
      assertThat(t, is(instanceOf(MessagingException.class)));
      assertThat(t.getCause(), is(exception));
      assertThat(((MessagingException) t).getEvent().getContext(), sameInstance(input.getContext()));
    }
  }

  @Test
  @Issue("MULE-16892")
  public void processWithChildContextErrorInChainMantainsChildContext() throws Exception {
    Reference<EventContext> contextReference = new Reference<>();
    chain = createChain(error);
    from(processWithChildContext(input, chain, Optional.empty()))
        .doOnError(e -> {
          if (e instanceof MessagingException) {
            contextReference.set(((MessagingException) e).getEvent().getContext());
          }
        })
        .subscribe();

    assertThat(contextReference.get(), notNullValue());
    assertThat(contextReference.get(), is(not(input.getContext())));
    BaseEventContext context = (BaseEventContext) contextReference.get();
    assertThat(context.getParentContext().get(), is(input.getContext()));
  }

  @Test
  @Issue("MULE-16892")
  public void handleErrorWithChildAndParentStack() throws Exception {

    List<FlowStackElement> parentStack = asList(new FlowStackElement("flow", "processor"));

    List<FlowStackElement> childStack = asList(
                                               new FlowStackElement("sub-flow-1", "processor-1"),
                                               new FlowStackElement("sub-flow-2", "processor-2"));

    assertHandleErrorWithStack(parentStack, childStack);
  }

  @Test
  @Issue("MULE-16892")
  public void handleErrorWithEmptyParentStack() throws Exception {

    List<FlowStackElement> childStack = asList(
                                               new FlowStackElement("sub-flow-1", "processor-1"),
                                               new FlowStackElement("sub-flow-2", "processor-2"));

    assertHandleErrorWithStack(EMPTY_LIST, childStack);
  }

  @Test
  @Issue("MULE-16892")
  public void handleErrorWithEmptyStack() throws Exception {
    assertHandleErrorWithStack(EMPTY_LIST, EMPTY_LIST);
  }

  private void assertHandleErrorWithStack(List<FlowStackElement> parentStack, List<FlowStackElement> childStack)
      throws InitialisationException {

    final DefaultFlowCallStack inputFlowCallStack = (DefaultFlowCallStack) input.getFlowCallStack();
    Reference<DefaultFlowCallStack> childFlowCallStack = new Reference<>();

    chain = createChain(error);

    parentStack.stream().forEachOrdered(inputFlowCallStack::push);

    ReactiveProcessor childWithStack = p -> from(p).flatMap(e -> {
      DefaultFlowCallStack stack = (DefaultFlowCallStack) e.getFlowCallStack();

      childStack.stream().forEachOrdered(stack::push);

      childFlowCallStack.set(stack);

      return from(processWithChildContext(e, error, Optional.empty()));
    });

    AtomicReference<Throwable> handledError = new AtomicReference<>();

    from(applyWithChildContextDontPropagateErrors(
                                                  applyWithChildContextDontPropagateErrors(just(input), childWithStack,
                                                                                           Optional.empty()),
                                                  chain, Optional.empty()))
        .subscribe(e -> {
        },
                   handledError::set);

    assertThat(handledError.get(), isA(MessagingException.class));
    assertThat(childFlowCallStack.get(), is(not(nullValue())));
    assertThat(inputFlowCallStack.getElements(), is(childFlowCallStack.get().getElements()));
  }

  @Test
  @Issue("MULE-16892")
  public void processWithChildContextDontCompleteErrorInChainRegainsParentContext() throws Exception {
    Reference<EventContext> contextReference = new Reference<>();
    chain = createChain(error);
    from(processWithChildContextDontComplete(input, chain, Optional.empty()))
        .doOnError(e -> {
          if (e instanceof MessagingException) {
            contextReference.set(((MessagingException) e).getEvent().getContext());
          }
        })
        .subscribe();

    assertThat(contextReference.get(), notNullValue());
    assertThat(contextReference.get(), is(input.getContext()));
  }


  @Test
  @Issue("MULE-16892")
  public void applyWithChildContextErrorInChainRegainsParentContext() throws Exception {
    Reference<EventContext> contextReference = new Reference<>();

    chain = createChain(error);
    Processor errorProcessor = chain;
    just(input).transform(inputPub -> from(applyWithChildContext(inputPub, errorProcessor, Optional.empty()))
        .doOnError(e -> {
          if (e instanceof MessagingException) {
            contextReference.set(((MessagingException) e).getEvent().getContext());
          }
        }))
        .subscribe();

    assertThat(contextReference.get(), notNullValue());
    assertThat(contextReference.get(), is(input.getContext()));
  }

  @Test
  public void applyWithChildContextDontPropagateErrorInChainRegainsParentContext() throws Exception {
    Reference<EventContext> contextReference = new Reference<>();

    ((DefaultFlowCallStack) input.getFlowCallStack()).push(new FlowStackElement("flow", "processor"));

    chain = createChain(error);
    Processor errorProcessor = chain;
    just(input).transform(inputPub -> from(applyWithChildContextDontPropagateErrors(inputPub, errorProcessor, Optional.empty()))
        .doOnError(e -> {
          if (e instanceof MessagingException) {
            contextReference.set(((MessagingException) e).getEvent().getContext());
          }
        }))
        .subscribe();

    assertThat(contextReference.get(), notNullValue());
    assertThat(contextReference.get(), is(input.getContext()));
  }

  @Test
  public void processWithChildContextBlockingSuccessInChainRegainsParentContext() throws Exception {
    chain = createChain(map);
    CoreEvent event = processWithChildContextBlocking(input, chain, Optional.empty());
    assertThat(event.getContext(), sameInstance(input.getContext()));
  }

  @Test
  public void applyWithinProcessSuccess() {
    final CoreEvent result = from(processWithChildContext(input,
                                                          pub -> applyWithChildContext(pub, eventPub -> eventPub,
                                                                                       Optional.empty()),
                                                          newChildContext(input, Optional.empty())))
        .block();

    assertThat(result, sameInstance(input));
  }

  @Test
  public void applyWithinProcessError() {
    final NullPointerException expected = new NullPointerException();

    thrown.expect(hasRootCause(sameInstance(expected)));

    from(processWithChildContext(input,
                                 pub -> Flux.from(pub)
                                     .transform(ep -> applyWithChildContext(ep,
                                                                            eventPub -> Flux.from(eventPub)
                                                                                .handle(failWithExpected(expected)),
                                                                            Optional.empty())),
                                 newChildContext(input, Optional.empty())))
        .block();
  }

  @Test
  public void applyWithinProcessErrorDontPropagate() {
    final NullPointerException expected = new NullPointerException();

    thrown.expect(hasRootCause(sameInstance(expected)));

    from(processWithChildContext(input,
                                 pub -> Flux.from(pub)
                                     .transform(ep -> applyWithChildContextDontPropagateErrors(ep,
                                                                                               eventPub -> Flux.from(eventPub)
                                                                                                   .handle(failWithExpected(expected)),
                                                                                               Optional.empty())),
                                 newChildContext(input, Optional.empty())))
        .block();
  }

  @Test
  public void processWithinProcessError() {
    final NullPointerException expected = new NullPointerException();

    thrown.expect(hasRootCause(sameInstance(expected)));

    from(processWithChildContext(input,
                                 pub -> Flux.from(pub)
                                     .flatMap(event -> processWithChildContext(event,
                                                                               eventPub -> Flux.from(eventPub)
                                                                                   .handle(failWithExpected(expected)),
                                                                               Optional.empty())),
                                 newChildContext(input, Optional.empty())))
        .block();
  }

  @Test
  public void applyWithinProcessErrorWithChainErrorHandling() {
    nestedChildContextsProcessApply(true, completeWithErrorPropagate(), completeWithErrorPropagate());
  }

  @Test
  public void processWithinProcessErrorWithChainErrorHandling() {
    nestedChildContextsProcessProcess(true, completeWithErrorPropagate(), completeWithErrorPropagate());
  }

  @Test
  public void applyWithinApplyErrorWithChainErrorHandling() {
    nestedChildContextsApplyApply(true, completeWithErrorPropagate(), completeWithErrorPropagate());
  }

  @Test
  public void processWithinApplyErrorWithChainErrorHandling() {
    nestedChildContextsApplyProcess(true, completeWithErrorPropagate(), completeWithErrorPropagate());
  }

  @Test
  public void applyErrorContinueWithinProcessWithChainErrorHandling() {
    nestedChildContextsProcessApply(false, completeWithErrorPropagate(), completeWithErrorContinue());
  }

  @Test
  public void processErrorContinueWithinProcessWithChainErrorHandling() {
    nestedChildContextsProcessProcess(false, completeWithErrorPropagate(), completeWithErrorContinue());
  }

  @Test
  public void applyErrorContinueWithinApplyWithChainErrorHandling() {
    nestedChildContextsApplyApply(false, completeWithErrorPropagate(), completeWithErrorContinue());
  }

  @Test
  public void processErrorContinueWithinApplyWithChainErrorHandling() {
    nestedChildContextsApplyProcess(false, completeWithErrorPropagate(), completeWithErrorContinue());
  }

  @Test
  public void applyWithinProcessErrorContinueWithChainErrorHandling() {
    nestedChildContextsProcessApply(false, completeWithErrorContinue(), completeWithErrorPropagate());
  }

  @Test
  public void processWithinProcessErrorContinueWithChainErrorHandling() {
    nestedChildContextsProcessProcess(false, completeWithErrorContinue(), completeWithErrorPropagate());
  }

  @Test
  public void applyWithinApplyErrorWithContinueChainErrorHandling() {
    nestedChildContextsApplyApply(false, completeWithErrorContinue(), completeWithErrorPropagate());
  }

  @Test
  public void processWithinApplyErrorContinueWithChainErrorHandling() {
    nestedChildContextsApplyProcess(false, completeWithErrorContinue(), completeWithErrorPropagate());
  }

  private void nestedChildContextsApplyProcess(boolean exceptionExpected, BiConsumer<Throwable, Object> outerErrorConsumer,
                                               BiConsumer<Throwable, Object> innerErrorConsumer) {
    final NullPointerException expected = new NullPointerException();

    if (exceptionExpected) {
      thrown.expect(hasRootCause(sameInstance(expected)));
    }

    just(input).transform(inputPub -> applyWithChildContext(inputPub,
                                                            pub -> Flux.from(pub)
                                                                .flatMap(event -> processWithChildContextDontComplete(event,
                                                                                                                      innerChain(innerErrorConsumer,
                                                                                                                                 expected),
                                                                                                                      Optional
                                                                                                                          .empty()))
                                                                .onErrorContinue(outerErrorConsumer),
                                                            Optional.empty()))
        .block();
  }

  private void nestedChildContextsApplyApply(boolean exceptionExpected, BiConsumer<Throwable, Object> outerErrorConsumer,
                                             BiConsumer<Throwable, Object> innerErrorConsumer) {
    final NullPointerException expected = new NullPointerException();

    if (exceptionExpected) {
      thrown.expect(hasRootCause(sameInstance(expected)));
    }

    just(input).transform(inputPub -> applyWithChildContext(inputPub,
                                                            pub -> Flux.from(pub)
                                                                .transform(ep -> applyWithChildContext(ep,
                                                                                                       innerChain(innerErrorConsumer,
                                                                                                                  expected),
                                                                                                       Optional.empty()))
                                                                .onErrorContinue(outerErrorConsumer),
                                                            Optional.empty()))
        .block();
  }

  private void nestedChildContextsProcessApply(boolean exceptionExpected, BiConsumer<Throwable, Object> outerErrorConsumer,
                                               BiConsumer<Throwable, Object> innerErrorConsumer) {
    final NullPointerException expected = new NullPointerException();

    if (exceptionExpected) {
      thrown.expect(hasRootCause(sameInstance(expected)));
    }

    from(processWithChildContext(input,
                                 pub -> Flux.from(pub)
                                     .transform(ep -> applyWithChildContext(ep,
                                                                            innerChain(innerErrorConsumer, expected),
                                                                            Optional.empty()))
                                     .onErrorContinue(outerErrorConsumer),
                                 newChildContext(input, Optional.empty())))
        .block();
  }

  private void nestedChildContextsProcessProcess(boolean exceptionExpected, BiConsumer<Throwable, Object> outerErrorConsumer,
                                                 BiConsumer<Throwable, Object> innerErrorConsumer) {
    final NullPointerException expected = new NullPointerException();

    Mono<CoreEvent> eventMono = from(processWithChildContext(input,
                                                             pub -> Flux.from(pub)
                                                                 .flatMap(event -> processWithChildContextDontComplete(event,
                                                                                                                       innerChain(innerErrorConsumer,
                                                                                                                                  expected),
                                                                                                                       Optional
                                                                                                                           .empty()))
                                                                 .onErrorContinue(outerErrorConsumer),
                                                             newChildContext(input, Optional.empty())));

    if (exceptionExpected) {
      thrown.expect(hasRootCause(sameInstance(expected)));
    }
    eventMono.block();
  }

  private ReactiveProcessor innerChain(BiConsumer<Throwable, Object> innerErrorConsumer, final NullPointerException expected) {
    return eventPub -> Flux.from(eventPub)
        .handle(failWithExpected(expected))
        .onErrorContinue(innerErrorConsumer);
  }

  private BiConsumer<? super CoreEvent, SynchronousSink<CoreEvent>> failWithExpected(final Exception expected) {
    return (event, sink) -> sink.error(new MessagingException(event, expected));
  }

  private BiConsumer<Throwable, Object> completeWithErrorPropagate() {
    return (error, event) -> {
      final PrivilegedEvent errorEvent = (PrivilegedEvent) ((MessagingException) error).getEvent();
      errorEvent.getContext().error(error);
    };
  }

  private BiConsumer<Throwable, Object> completeWithErrorContinue() {
    return (error, event) -> {
      final PrivilegedEvent errorEvent = (PrivilegedEvent) ((MessagingException) error).getEvent();
      (errorEvent.getContext()).success(errorEvent);
    };
  }

  @Test
  public void processToApplyErrorInChain() throws Exception {
    chain = createChain(error);
    try {
      processToApply(input, chain);
      fail("Exception expected");
    } catch (Throwable t) {
      assertThat(t, is(instanceOf(MessagingException.class)));
      assertThat(t.getCause(), is(exception));
    }

    assertThat(from(responsePublisher).toFuture().isDone(), is(true));

    thrown.expectCause((is(instanceOf(MessagingException.class))));
    thrown.expectCause(hasCause(is(exception)));
    from(responsePublisher).block();
  }

  @Test
  public void processToApplyErrorInFlow() throws Exception {
    try {
      processToApply(input, createFlow(error));
      fail("Exception expected");
    } catch (Throwable t) {
      assertThat(t, is(instanceOf(MessagingException.class)));
      assertThat(t.getCause(), is(exception));
    }

    assertThat(from(responsePublisher).toFuture().isDone(), is(true));

    thrown.expectCause((is(instanceOf(MessagingException.class))));
    thrown.expectCause(hasCause(is(exception)));
    from(responsePublisher).block();
  }

  @Test
  public void processMap() throws Exception {
    assertThat(from(MessageProcessors.process(input, map)).block(), is(output));
    assertThat(from(responsePublisher).toFuture().get(), equalTo(output));
  }

  @Test
  public void processMapInChain() throws Exception {
    chain = createChain(map);
    assertThat(from(MessageProcessors.process(input, chain)).block(), is(output));
    assertThat(from(responsePublisher).toFuture().get(), equalTo(output));
  }

  @Test
  public void processMapInFlow() throws Exception {
    assertThat(from(MessageProcessors.process(input, createFlow(map))).block().getMessage(), is(output.getMessage()));
    assertThat(from(responsePublisher).block(), is(output));
  }

  @Test
  public void processAckAndStop() throws Exception {
    assertThat(from(MessageProcessors.process(input, ackAndStop)).block(), is(nullValue()));
    assertThat(from(responsePublisher).block(), is(nullValue()));
  }

  @Test
  public void processAckAndStopInChain() throws Exception {
    chain = createChain(ackAndStop);
    assertThat(from(MessageProcessors.process(input, chain)).block(), is(nullValue()));
    assertThat(from(responsePublisher).block(), is(nullValue()));
  }

  @Test
  public void processAckAndStopInFlow() throws Exception {
    assertThat(from(MessageProcessors.process(input, createFlow(ackAndStop))).block(), is(nullValue()));
    assertThat(from(responsePublisher).block(), is(nullValue()));
  }

  @Test
  public void processRespondAndStop() throws Exception {
    assertThat(from(MessageProcessors.process(input, respondAndStop)).block(), is(response));
    assertThat(from(responsePublisher).block(), is(response));
  }

  @Test
  public void processRespondAndStopInChain() throws Exception {
    chain = createChain(respondAndStop);
    assertThat(from(MessageProcessors.process(input, chain)).block(), is(response));
    assertThat(from(responsePublisher).block(), is(response));
  }

  @Test
  public void processRespondAndStopInFlow() throws Exception {
    assertThat(from(MessageProcessors.process(input, createFlow(respondAndStop))).block().getMessage(),
               is(response.getMessage()));
    assertThat(from(responsePublisher).block(), is(response));
  }

  @Test
  public void processAckAndMap() throws Exception {
    assertThat(from(MessageProcessors.process(input, ackAndMap)).block(), is(output));
    assertThat(from(responsePublisher).block(), is(nullValue()));
  }

  @Test
  public void processAckAndMapInChain() throws Exception {
    chain = createChain(ackAndMap);
    assertThat(from(MessageProcessors.process(input, chain)).block(), is(output));
    assertThat(from(responsePublisher).block(), is(nullValue()));
  }

  @Test
  public void processAckAndMapInFlow() throws Exception {
    assertThat(from(MessageProcessors.process(input, createFlow(ackAndMap))).block(), is(nullValue()));
    assertThat(from(responsePublisher).block(), is(nullValue()));
  }

  @Test
  public void processRespondAndMap() throws Exception {
    assertThat(from(MessageProcessors.process(input, respondAndMap)).block(), is(output));
    assertThat(from(responsePublisher).block(), is(response));
  }

  @Test
  public void processRespondAndMapInChain() throws Exception {
    chain = createChain(respondAndMap);
    assertThat(from(MessageProcessors.process(input, chain)).block(), is(output));
    assertThat(from(responsePublisher).block(), is(response));
  }

  @Test
  public void processRespondAndMapInFlow() throws Exception {
    assertThat(from(MessageProcessors.process(input, createFlow(respondAndMap))).block().getMessage(), is(response.getMessage()));
    assertThat(from(responsePublisher).block(), is(response));
  }

  @Test
  public void processError() throws Exception {
    thrown.expectCause((is(instanceOf(MessagingException.class))));
    thrown.expectCause(hasCause(is(exception)));
    try {
      from(MessageProcessors.process(input, error)).block();
    } finally {
      assertThat(from(responsePublisher).toFuture().isDone(), is(true));
      from(responsePublisher).toFuture()
          .whenComplete((event, throwable) -> assertThat(throwable.getCause(), equalTo(exception)));
    }
  }

  @Test
  public void processErrorInChain() throws Exception {
    chain = createChain(error);
    try {
      from(MessageProcessors.process(input, chain)).block();
      fail("Exception expected");
    } catch (Throwable t) {
      assertThat(t.getCause(), is(instanceOf(MessagingException.class)));
      assertThat(t.getCause().getCause(), is(exception));
    }

    assertThat(from(responsePublisher).toFuture().isDone(), is(true));

    thrown.expectCause((is(instanceOf(MessagingException.class))));
    thrown.expectCause(hasCause(is(exception)));
    from(responsePublisher).block();
  }

  @Test
  public void processErrorInFlow() throws Exception {
    try {
      from(MessageProcessors.process(input, createFlow(error))).block();
      fail("Exception expected");
    } catch (Throwable t) {
      assertThat(t.getCause(), is(instanceOf(MessagingException.class)));
      assertThat(t.getCause().getCause(), is(exception));
    }

    assertThat(from(responsePublisher).toFuture().isDone(), is(true));

    thrown.expectCause((is(instanceOf(MessagingException.class))));
    thrown.expectCause(hasCause(is(exception)));
    from(responsePublisher).block();
  }

  @Test
  @Issue("MULE-16952")
  public void applyWithChildContextPublisherCompleted() {
    AtomicBoolean fluxCompleted = new AtomicBoolean(false);

    final FluxSinkRecorder<CoreEvent> emitter = new FluxSinkRecorder<>();
    emitter.flux()
        .transform(pub -> applyWithChildContext(pub, pubInner -> pubInner, Optional.empty()))
        .subscribe(event -> {
        }, e -> {
        }, () -> fluxCompleted.set(true));
    emitter.complete();

    assertThat(fluxCompleted.get(), is(true));
  }

  @Test
  @Issue("MULE-18087")
  public void processingStrategyFromFlow() {
    final ProcessingStrategy ps = mock(ProcessingStrategy.class);

    final ConfigurationComponentLocator locator = mock(ConfigurationComponentLocator.class);
    Location location = builderFromStringRepresentation("myFlow").build();
    final DefaultFlow flow = mock(DefaultFlow.class);
    when(flow.getProcessingStrategy()).thenReturn(ps);
    when(locator.find(location)).thenReturn(Optional.of(flow));

    assertThat(getProcessingStrategy(locator, location).get(), sameInstance(ps));
  }

  @Test
  @Issue("MULE-18087")
  public void processingStrategyFromPolicy() {
    final ProcessingStrategy ps = mock(ProcessingStrategy.class);

    final ConfigurationComponentLocator locator = mock(ConfigurationComponentLocator.class);
    Location location = builderFromStringRepresentation("myPolicy").build();
    final DefaultPolicyInstance policy = mock(DefaultPolicyInstance.class);
    when(policy.getProcessingStrategy()).thenReturn(ps);
    when(locator.find(location)).thenReturn(Optional.of(policy));

    assertThat(getProcessingStrategy(locator, location).get(), sameInstance(ps));
  }

  @Test
  @Issue("MULE-19924")
  public void processingStrategyFromFlowWhenComponentInSubFlow() {
    final ProcessingStrategy ps = mock(ProcessingStrategy.class);

    // The Flow acting as root container will return the processing strategy
    final Flow flow = mock(DefaultFlow.class);
    when(flow.getProcessingStrategy()).thenReturn(ps);

    final Location rootContainerLocation = builderFromStringRepresentation("myFlow").build();

    // When the locator is requested to find the root container location, it finds the Flow
    final ConfigurationComponentLocator locator = mock(ConfigurationComponentLocator.class);
    when(locator.find(rootContainerLocation)).thenReturn(Optional.of(flow));

    // A ComponentLocation that has a Subflow as top level
    final TypedComponentIdentifier typedComponentIdentifier = mock(TypedComponentIdentifier.class);
    when(typedComponentIdentifier.getType()).thenReturn(SUB_FLOW);
    final LocationPart locationPart = mock(LocationPart.class);
    when(locationPart.getPartIdentifier()).thenReturn(Optional.of(typedComponentIdentifier));
    final ComponentLocation componentLocation = mock(ComponentLocation.class);
    when(componentLocation.getParts()).thenReturn(singletonList(locationPart));

    // Any component that has the Flow as root container but a Subflow as top level
    final Component nestedComponent = mock(AbstractComponent.class);
    when(nestedComponent.getRootContainerLocation()).thenReturn(rootContainerLocation);
    when(nestedComponent.getLocation()).thenReturn(componentLocation);

    assertThat(getProcessingStrategy(locator, nestedComponent), is(Optional.of(ps)));
  }

  private Processor createChain(ReactiveProcessor processor) throws InitialisationException {
    MessageProcessorChain chain = newChain(Optional.empty(), new ReactiveProcessorToProcessorAdaptor(processor));
    initialiseIfNeeded(chain, muleContext);
    return chain;
  }

  private Processor createFlow(ReactiveProcessor processor) throws MuleException {
    flow = Flow.builder("test", muleContext).processors(new ReactiveProcessorToProcessorAdaptor(processor)).build();
    flow.setAnnotations(singletonMap(LOCATION_KEY, from("flow")));
    flow.initialise();
    flow.start();
    return flow;
  }

  private static class ReactiveProcessorToProcessorAdaptor implements Processor, InternalProcessor {

    ReactiveProcessor delegate;

    ReactiveProcessorToProcessorAdaptor(ReactiveProcessor delegate) {
      this.delegate = delegate;
    }

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      throw new UnsupportedOperationException();
    }

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
      return delegate.apply(publisher);
    }
  }

  @FunctionalInterface
  private interface InternalReactiveProcessor extends ReactiveProcessor, InternalProcessor {

  }
}
