/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor;

import static java.util.Collections.singletonList;
import static java.util.concurrent.ConcurrentHashMap.newKeySet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.internal.matchers.ThrowableCauseMatcher.hasCause;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.functional.junit4.matchers.ThrowableRootCauseMatcher.hasRootCause;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.CoreEvent.builder;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.applyWithChildContext;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChildContext;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApplyWithChildContext;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContext;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContextBlocking;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContextDontComplete;
import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.tck.util.MuleContextUtils.getNotificationDispatcher;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.event.DefaultEventBuilder;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.exception.OnErrorPropagateHandler;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.reactivestreams.Publisher;

import io.qameta.allure.Issue;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;

@SmallTest
public class MessageProcessorsTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private final RuntimeException exception = new IllegalArgumentException();
  private OnErrorPropagateHandler exceptionHandler;
  private BaseEventContext eventContext;
  private CoreEvent input;
  private CoreEvent output;
  private CoreEvent response;
  private Flow flow;
  private Publisher<CoreEvent> responsePublisher;
  static private Set<Object> droppedNexts;

  static {
    droppedNexts = newKeySet();
    Hooks.onNextDropped(dropped -> droppedNexts.add(dropped));
  }

  @Before
  public void setup() throws MuleException {
    flow = mock(Flow.class, RETURNS_DEEP_STUBS);
    exceptionHandler = new OnErrorPropagateHandler();
    exceptionHandler.setMuleContext(muleContext);
    exceptionHandler
        .setNotificationFirer(getNotificationDispatcher(muleContext));
    exceptionHandler.initialise();
    when(flow.getExceptionListener()).thenReturn(exceptionHandler);
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
    exceptionHandler.dispose();
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
    assertThat(processToApply(input, createChain(map)), is(output));
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
    assertThat(processToApply(input, createChain(ackAndStop)), is(nullValue()));
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
    assertThat(processToApply(input, createChain(respondAndStop)), is(response));
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
    assertThat(processToApply(input, createChain(ackAndMap)), is(output));
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
    assertThat(processToApply(input, createChain(respondAndMap)), is(output));
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
    try {
      processWithChildContextBlocking(input, createChain(error), Optional.empty());
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
    from(processWithChildContext(input, createChain(error), Optional.empty()))
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
  public void processWithChildContextDontCompleteErrorInChainRegainsParentContext() throws Exception {
    Reference<EventContext> contextReference = new Reference<>();
    from(processWithChildContextDontComplete(input, createChain(error), Optional.empty()))
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

    Processor errorProcessor = createChain(error);
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
  public void processWithChildContextBlockingSuccessInChainRegainsParentContext() throws Exception {
    CoreEvent event = processWithChildContextBlocking(input, createChain(map), Optional.empty());
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

  @Test
  public void applyWithinApplyWithinApplyErrorWithChainErrorHandling() {
    nestedChildContextsApplyApplyApply(false, completeWithErrorContinue());
  }

  @Test
  public void propagateErrorFlux() throws MuleException {
    List<String> results = new ArrayList<>();
    AtomicBoolean completeWithError = new AtomicBoolean(false);

    final NullPointerException expected = new NullPointerException();

    DefaultEventBuilder builder = getEventBuilder();
    builder.message(Message.of("Hello"));

    Flux.<CoreEvent>create(sink -> {
      sink.next(builder.build());
      sink.error(expected);
    }).transform(ep -> {
      final FluxSinkRecorder<CoreEvent> emitter = new FluxSinkRecorder<>();
      return applyWithChildContext(ep, innerPub -> transfromer(emitter, innerPub), Optional.empty());
    })
        .subscribe(s -> results.add(s.getMessage().getPayload().getValue().toString()),
                   e -> completeWithError.set(true));

    probe(() -> {
      return completeWithError.get();
    });
  }

  private Publisher<CoreEvent> transfromer(final FluxSinkRecorder<CoreEvent> emitter, Publisher<CoreEvent> pub) {
    Flux<CoreEvent> transformedPub = Flux.from(pub);
    try {
      final DefaultEventBuilder builder = getEventBuilder();
      return transformedPub.doOnNext(s -> {
        builder.message(Message.of(s.getMessage().getPayload().getValue() + " world"));
        emitter.next(builder.build());
      });
    } catch (MuleException e) {
      e.printStackTrace();
    }
    return null;
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

    assertThat(droppedNexts.isEmpty(), is(true));
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

    assertThat(droppedNexts.isEmpty(), is(true));
  }

  private void nestedChildContextsApplyApplyApply(boolean exceptionExpected, BiConsumer<Throwable, Object> innerErrorConsumer) {
    final NullPointerException expected = new NullPointerException();

    if (exceptionExpected) {
      thrown.expect(hasRootCause(sameInstance(expected)));
    }

    just(input).transform(inputPub -> applyWithChildContext(inputPub, innerChain(innerErrorConsumer, expected), Optional.empty()))
        .block();


    assertThat(droppedNexts.isEmpty(), is(true));
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

    assertThat(droppedNexts.isEmpty(), is(true));
  }

  private void nestedChildContextsProcessProcess(boolean exceptionExpected, BiConsumer<Throwable, Object> outerErrorConsumer,
                                                 BiConsumer<Throwable, Object> innerErrorConsumer) {
    final NullPointerException expected = new NullPointerException();

    if (exceptionExpected) {
      thrown.expect(hasRootCause(sameInstance(expected)));
    }

    from(processWithChildContext(input,
                                 pub -> Flux.from(pub)
                                     .flatMap(event -> processWithChildContextDontComplete(event,
                                                                                           innerChain(innerErrorConsumer,
                                                                                                      expected),
                                                                                           Optional.empty()))
                                     .onErrorContinue(outerErrorConsumer),
                                 newChildContext(input, Optional.empty())))
                                     .block();

    assertThat(droppedNexts.isEmpty(), is(true));
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
    try {
      processToApply(input, createChain(error));
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
    assertThat(from(MessageProcessors.process(input, createChain(map))).block(), is(output));
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
    assertThat(from(MessageProcessors.process(input, createChain(ackAndStop))).block(), is(nullValue()));
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
    assertThat(from(MessageProcessors.process(input, createChain(respondAndStop))).block(), is(response));
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
    assertThat(from(MessageProcessors.process(input, createChain(ackAndMap))).block(), is(output));
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
    assertThat(from(MessageProcessors.process(input, createChain(respondAndMap))).block(), is(output));
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
    try {
      from(MessageProcessors.process(input, createChain(error))).block();
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

  private Processor createChain(ReactiveProcessor processor) throws InitialisationException {
    MessageProcessorChain chain = newChain(Optional.empty(), new ReactiveProcessorToProcessorAdaptor(processor));
    chain.setMuleContext(muleContext);
    return chain;
  }

  private Processor createFlow(ReactiveProcessor processor) throws MuleException {
    flow = Flow.builder("test", muleContext).processors(new ReactiveProcessorToProcessorAdaptor(processor)).build();
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
